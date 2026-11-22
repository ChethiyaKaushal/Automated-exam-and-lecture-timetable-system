# AI scheduling architecture (LangGraph + CP-SAT)

Sprint: 21 Nov – 4 Dec 2026. Owner of this document: Chethiya (API architecture + LangGraph design).

## Goal

The admin asks the system to generate an exam timetable. An AI agent receives that task and completes it by
calling tools. The tools do the exact work, so the output is clash-free:

- **CP-SAT solver** (Google OR-Tools) places every exam in a start time and venues. It either returns a
  timetable that satisfies every hard constraint, or proves that no such timetable exists.
- **Gemini** (through LangChain) runs the workflow, explains the result, and when the solver finds no
  solution, works out why and what the admin could change.

Gemini never chooses exam times or rooms itself. A language model cannot guarantee that hundreds of
constraints hold, but a solver can. This is what makes the output "efficient and error free".

## Components

```
Next.js admin UI
   │  POST /api/timetable/exams/{periodId}/preview?engine=agent      (existing proxy, 11 min timeout)
   ▼
Spring Boot  (backend/, port 8081)
   │  1. ExamSchedulingInputService.loadDraft(periodId)   → ExamSchedulingInput   (existing)
   │  2. POST {AI_SERVICE_URL}/v1/exam-timetables/generate                         (new: AgentExamTimetableService)
   │  3. ExamTimetableReviewValidator.review(...)          → issues, room seating   (existing, final check)
   │  4. Admin reviews and saves with POST /drafts                                  (existing)
   ▼
FastAPI AI service  (ai-service/, port 8000)
   └─ LangGraph workflow
        ├─ tool: solve_exam_timetable   → app/solver  (CP-SAT, Nimmitha + Malsha)
        ├─ tool: check_timetable        → quick Python re-check of the solver output
        └─ Gemini: diagnose infeasible input, explain result
```

Spring Boot stays the only component that talks to PostgreSQL and the only one the frontend calls. The AI
service is stateless: it receives everything it needs in the request. Student identities never leave Spring
Boot; the solver only receives counts and clashing requirement pairs.

## What we reuse (do not duplicate)

| Sprint task | Existing code to build on |
|---|---|
| SolverInput schema (Nimmitha) | `dto/ExamSchedulingInput.java`. `ai-service/app/schemas.py` mirrors it field for field (camelCase JSON). Add fields to both together. |
| Solver input mapping (Malsha) | `service/ExamSchedulingInputService.java` already maps JPA entities to `ExamSchedulingInput`. Extend it. |
| Validation (Malsha) | `validation/ExamTimetableReviewValidator.java` re-checks every agent result before it is shown or saved. |
| Persistence (Malsha) | `POST /api/admin/exam-timetables/{periodId}/drafts` already saves versioned DRAFT timetables. |
| Generation API (Chethiya) | Existing `POST /{periodId}/preview?engine=` gets a third engine, `agent`. `java` stays as the fallback. |
| Generate button and display (Oshadi) | `GeneratePage.tsx` and `ExamTimetablePage.tsx` already call preview, review and save. |

Once the agent engine is accepted, `GeminiExamTimetableService` and `ExamTimetablePromptBuilder` (Gemini
placing exams directly) can be removed. The Gemini API key then lives only in the AI service.

## Solver contract

The JSON shapes are the contract between Spring Boot, FastAPI and the solver. The Python models are in
`ai-service/app/schemas.py`.

### Request: `POST /v1/exam-timetables/generate`

```json
{
  "input": {
    "periodId": 1,
    "exams": [{"requirementId": 10, "moduleId": 3, "moduleCode": "EE1201", "moduleName": "Circuits",
               "curriculum": "C-23", "durationMinutes": 180, "studentCount": 240,
               "groups": [{"groupId": 7, "studentCount": 240}]}],
    "timeslots": [{"timeslotId": 101, "date": "2026-12-18", "startTime": "09:00", "endTime": "09:30"}],
    "venues": [{"venueId": 2, "venueName": "Main Hall", "examCapacity": 300, "unavailableTimeslotIds": []}],
    "studentConflicts": [{"firstRequirementId": 10, "secondRequirementId": 11}]
  },
  "membershipComplete": true,
  "options": {"timeLimitSeconds": 60, "explain": true}
}
```

### Response

```json
{
  "status": "SCHEDULED",
  "solver": "cp-sat",
  "assignments": [{"requirementId": 10, "startTimeslotId": 101, "venueIds": [2]}],
  "unscheduled": [],
  "explanation": "All 42 exams were placed in 9 days without student clashes ...",
  "steps": [{"node": "solve", "message": "CP-SAT found an optimal timetable in 3.2 s."}]
}
```

- `status`: `SCHEDULED` (every exam placed), `INFEASIBLE` (solver proved no timetable exists), or
  `FAILED` (time limit reached or an internal error).
- `assignments`: one entry per placed exam. `startTimeslotId` is the first 30-minute slot. The exam uses
  `durationMinutes / 30` consecutive slots on the same day.
- `unscheduled`: exams that could not be placed, with a short reason.
- `explanation`: plain-language summary for the admin (Gemini, or a fixed text when Gemini is off).
- `steps`: what the workflow did, for the UI and the demo.

### Python solver interface

The solver team implements one function. The rest of the service only depends on this signature:

```python
# ai-service/app/solver/__init__.py
def solve(problem: SchedulingInput, time_limit_seconds: float) -> SolverResult: ...
```

Until the CP-SAT model is merged, `app/solver/placeholder.py` is a small greedy scheduler so the end-to-end
flow can be tested. It is replaced, not extended.

### Hard constraints (solver)

1. Every enabled exam gets exactly one start time.
2. An exam occupies consecutive slots on one day and cannot cross the lunch break.
3. Two exams in `studentConflicts` never overlap.
4. A venue holds at most one exam per slot and is never used in its unavailable slots.
5. The venues assigned to an exam seat all of its students (multi-hall allowed).
6. ME1201 C-23 Engineering Drawing fixed sessions (`DrawingExamRule`).

Soft goals (objective): spread each student's exams across days, then use as few and as well-filled halls as
possible.

## LangGraph workflow

```
START
  │
  ▼
check_input ── invalid ─────────────────────────────┐
  │ ok                                              │
  ▼                                                 │
solve  (tool: CP-SAT)                               │
  │                                                 │
  ├─ SCHEDULED ──► verify (tool: check_timetable) ──┤
  │                                                 │
  └─ INFEASIBLE / FAILED ──► diagnose (Gemini + tools, max 1 retry with an
                                       admin-approved relaxation) ─┤
                                                                   ▼
                                                                explain (Gemini or fixed text)
                                                                   │
                                                                  END
```

State carried through the graph: the request, the solver result, issues found, the step log and the
explanation. Each node appends one entry to `steps`.

- **check_input**: rejects empty exams, slots or venues, and exams longer than any session, before the
  solver runs.
- **solve**: calls the solver tool with the time limit.
- **verify**: re-checks clashes, capacity and venue double-booking in Python. Spring Boot checks again.
- **diagnose**: only when the solver fails. Gemini receives counts and bottlenecks (for example: "exam X
  needs 600 seats, largest free combination is 450") and the tools to compute them, and proposes changes such
  as adding a day or a venue. It never edits the timetable.
- **explain**: writes the summary shown to the admin.

### Fallback

- No `GEMINI_API_KEY`, or Gemini is down: the workflow still runs; diagnose and explain use fixed text.
- AI service down: Spring Boot returns 503 with a readable issue. The admin can choose the Java engine.

## API summary

| Service | Method and path | Purpose |
|---|---|---|
| AI service | `GET /health` | Liveness, solver name, whether Gemini is configured. |
| AI service | `POST /v1/exam-timetables/generate` | Run the workflow and return the contract above. |
| Spring Boot | `POST /api/admin/exam-timetables/{periodId}/preview?engine=agent` | Load input, call the AI service, validate, return the existing `Preview`. |

Configuration:

| Setting | Where | Default |
|---|---|---|
| `AI_SERVICE_URL` | Spring Boot env (`timetable.ai-service.url`) | `http://localhost:8000` |
| `GEMINI_API_KEY`, `GEMINI_MODEL` | AI service env only | unset, `gemini-3.6-flash` |
| `SOLVER_TIME_LIMIT_SECONDS` | AI service env | `60` |

## Ownership

| Area | Folder | Owner |
|---|---|---|
| FastAPI app, LangGraph workflow, Gemini, Spring Boot client | `ai-service/app/` (except `solver/`), `backend/.../service/AgentExamTimetableService.java` | Chethiya |
| CP-SAT model: exam variables, period, duration, student clash, spacing | `ai-service/app/solver/` | Nimmitha |
| CP-SAT model: venue variables, capacity, availability, multi-hall, utilisation | `ai-service/app/solver/` | Malsha |
| Input mapping, validation, persistence | existing backend services above | Malsha |
| Venue/timeslot management UI, generate and display | `frontend/` | Oshadi |