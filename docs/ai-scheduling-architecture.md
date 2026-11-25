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
   │  1. ExamSchedulingInputService loads the period from PostgreSQL               (existing)
   │  2. Map it to the solver contract (SolverInput)                                (Malsha, Day 6)
   │  3. AiServiceClient → POST {AI_SERVICE_URL}/v1/exam-timetables/generate        (Chethiya, Day 2–3)
   │  4. ExamTimetableReviewValidator re-checks the result                          (existing, final check)
   │  5. Admin reviews and saves with POST /drafts                                  (existing)
   ▼
FastAPI AI service  (ai-service/, port 8000)
   ├─ app/main.py             GET /health, POST /v1/exam-timetables/generate
   ├─ app/solver_gateway.py   the single place that calls the solver
   └─ LangGraph workflow (Day 5)
        ├─ tool: CP-SAT solver   → app/solver/  (Nimmitha + Malsha)
        ├─ tool: check result
        └─ Gemini: diagnose infeasible input, explain result (Day 7–8)
```

Spring Boot stays the only component that talks to PostgreSQL and the only one the frontend calls. The AI
service is stateless: it receives everything it needs in the request. Student identities never leave Spring
Boot; the solver only receives counts, groups and clashing requirement pairs.

## What we reuse (do not duplicate)

| Sprint task | Existing code to build on |
|---|---|
| Solver contract (Nimmitha) | `ai-service/app/schemas/solver_input.py` and `solver_output.py` are **the** contract. Every service uses them; do not create another schema. |
| Solver input mapping (Malsha) | `service/ExamSchedulingInputService.java` already loads exams, slots, venues and clashes. Extend it to produce the `SolverInput` fields (venue type, priority, conflict type, shared papers, fixed sessions). |
| Validation (Malsha) | `validation/ExamTimetableReviewValidator.java` re-checks every agent result before it is shown or saved. |
| Persistence (Malsha) | `POST /api/admin/exam-timetables/{periodId}/drafts` already saves versioned DRAFT timetables. |
| Generation API (Chethiya) | Existing `POST /{periodId}/preview?engine=` gets a third engine, `agent`. `java` stays as the fallback. |
| Error responses | `GeminiProviderException` and `GlobalExceptionHandler` turn AI service failures into `{"issues":[...]}`. No new exception class. |
| Generate button and display (Oshadi) | `GeneratePage.tsx` and `ExamTimetablePage.tsx` already call preview, review and save. |

Once the agent engine is accepted, `GeminiExamTimetableService` and `ExamTimetablePromptBuilder` (Gemini
placing exams directly) can be removed. The Gemini API key then lives only in the AI service.

## Solver contract

Defined by Nimmitha in `ai-service/app/schemas/`. JSON uses camelCase; unknown fields are rejected.

**Input, `SolverInput`:** `periodId`, `exams` (with `durationMinutes`, `studentCount`, `groups`,
`examGapDays`, `requiredVenueType`), `timeslots` (exactly 30 minutes each), `venues` (with `venueType`,
`examCapacity`, `priority`, `unavailableTimeslotIds`), `conflictPairs` (with `conflictType`),
`sharedExamGroups` and `fixedAssignments`. Example: `ai-service/tests/fixtures/small_input.json`.

**Output, `SolverOutput`:** `status`, `periodId`, `assignments` (date, start/end time, timeslot IDs and
venue allocations per group), `unscheduled`, `diagnostics` and `stats`. Example:
`ai-service/tests/fixtures/small_output.json`.

| Status | Meaning |
|---|---|
| `OPTIMAL` / `FEASIBLE` | A timetable was found. A partial one lists the missing exams in `unscheduled` with a `PARTIAL_SCHEDULE` diagnostic. |
| `INFEASIBLE` | The solver proved no timetable exists. At least one diagnostic explains why. |
| `UNKNOWN` | Time limit reached without an answer (`TIME_LIMIT_REACHED` diagnostic). |
| `MODEL_INVALID` | The solver could not run (`INTERNAL_ERROR` diagnostic). Also used while the CP-SAT solver is not connected yet. |

### HTTP wrapper (`ai-service/app/schemas/api.py`)

```
Request:   { "input": SolverInput, "options": { "timeLimitSeconds": 60, "explain": true } }
Response:  { "solver": "cp-sat", "output": SolverOutput, "explanation": "..." }
```

`timeLimitSeconds` is optional (default `SOLVER_TIME_LIMIT_SECONDS`, at most 600). `explanation` is filled
from Day 5 (workflow) and Day 7 (Gemini); until then it is `null`.

### Python solver interface

The solver team implements one function. The rest of the service only depends on this signature:

```python
def solve(problem: SolverInput, time_limit_seconds: float) -> SolverOutput: ...
```

`app/solver_gateway.py` calls it. Until the CP-SAT solver is merged, the gateway returns a valid
`MODEL_INVALID` output ("solver not connected yet"). Connecting the real solver is a two-line change there
(`solve` and `SOLVER_NAME = "cp-sat"`). If the solver raises an exception, the gateway returns an
`INTERNAL_ERROR` diagnostic instead of crashing the service.

### Hard constraints (solver)

1. Every exam gets one start time within the exam period.
2. An exam occupies consecutive slots on one day and cannot cross the lunch break.
3. `conflictPairs`: `DIFFERENT_DAY` exams are on different days; `NO_TIME_OVERLAP` exams never overlap.
4. `examGapDays` days between related exams.
5. A venue holds at most one exam per slot and is never used in its unavailable slots.
6. Venues match `requiredVenueType` and seat every student (multi-hall allowed).
7. `sharedExamGroups` start in the same slot; `fixedAssignments` are kept (for example ME1201 drawing sessions).

Soft goals (objective): spread each student's exams across days, then use as few and as well-filled halls as
possible, preferring venues by `priority`.

## LangGraph workflow (Day 5)

```
START
  │
  ▼
check_input ── invalid ─────────────────────────────┐
  │ ok                                              │
  ▼                                                 │
solve  (tool: CP-SAT)                               │
  │                                                 │
  ├─ OPTIMAL / FEASIBLE ──► verify (tool) ──────────┤
  │                                                 │
  └─ INFEASIBLE / UNKNOWN / MODEL_INVALID ──► diagnose (Gemini + tools, uses the
                                     solver's diagnostics; max 1 retry with an
                                     admin-approved relaxation) ─┤
                                                                  ▼
                                                               explain (Gemini or fixed text)
                                                                  │
                                                                 END
```

- **check_input**: input already passed `SolverInput` validation; this node checks things such as "no exam
  fits any session" before the solver runs.
- **solve**: calls the solver through `solver_gateway`.
- **verify**: re-checks clashes, capacity and venue double-booking. Spring Boot checks again.
- **diagnose**: only when there is no full timetable. Gemini reads the solver's `diagnostics` (for example
  `CAPACITY_TOO_SMALL`, `NOT_ENOUGH_DAYS`) and proposes changes such as adding a day or a venue. It never
  edits the timetable.
- **explain**: writes the summary shown to the admin (`explanation`).

### Fallback

- No `GEMINI_API_KEY`, or Gemini is down: the workflow still runs; diagnose and explain use fixed text.
- AI service down: Spring Boot returns 503 `AI_SERVICE_UNAVAILABLE` with a readable message. The admin can
  choose the Java engine.
- `AuthenticationExceptionHandler` only handles `AuthController` and `UserController`, so timetable errors
  keep their real status and message from `GlobalExceptionHandler`.

## API summary

| Service | Method and path | Purpose | Status |
|---|---|---|---|
| AI service | `GET /health` | Liveness, solver name, whether Gemini is configured. | Done (Day 2) |
| AI service | `POST /v1/exam-timetables/generate` | Validate `SolverInput`, run the solver, return `SolverOutput`. | Done (Day 2), solver pending |
| Spring Boot | `GET /api/admin/ai-service/health` | Admin check that Spring Boot can reach the AI service. | Done (Day 2) |
| Spring Boot | `POST /api/admin/exam-timetables/{periodId}/preview?engine=agent` | Load input, call the AI service, validate, return the existing `Preview`. | Day 3 |

Configuration:

| Setting | Where | Default |
|---|---|---|
| `AI_SERVICE_URL` | Spring Boot env (`timetable.ai-service.url`) | `http://localhost:8000` |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Spring Boot env | local `timetable_sprint1`, `postgres`, none |
| `SOLVER_TIME_LIMIT_SECONDS` | AI service env | `60` |
| `GEMINI_API_KEY`, `GEMINI_MODEL` | AI service env only | unset, `gemini-3.6-flash` |

## Running locally

1. PostgreSQL running with the `timetable_sprint1` database.
2. AI service: `cd ai-service`, activate `.venv`, `pip install -r requirements.txt`,
   `uvicorn app.main:app --port 8000`.
3. Spring Boot: run `ExamLectureTimetableApplication` with `DB_PASSWORD` and `APP_JWT_SECRET` set.
4. Sign in as an admin at `http://localhost:8081/login`, then open
   `http://localhost:8081/api/admin/ai-service/health`.

## Ownership

| Area | Folder | Owner |
|---|---|---|
| FastAPI app, solver gateway, LangGraph workflow, Gemini, Spring Boot client | `ai-service/app/` (except `schemas/` and `solver/`), `backend/.../service/AiServiceClient.java`, `backend/.../controller/AiServiceController.java` | Chethiya |
| Solver contract; CP-SAT model: exam variables, period, duration, student clash, spacing | `ai-service/app/schemas/`, `ai-service/app/solver/` | Nimmitha |
| CP-SAT model: venue variables, capacity, availability, multi-hall, utilisation | `ai-service/app/solver/` | Malsha |
| Input mapping, validation, persistence | existing backend services above | Malsha |
| Venue/timeslot management UI, generate and display | `frontend/` | Oshadi |