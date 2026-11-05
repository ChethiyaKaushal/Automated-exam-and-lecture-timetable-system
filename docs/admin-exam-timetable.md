# Exam timetable admin review

The backend packages follow the team layout under `com.university.exam_lecture_timetable`.

| Package | Exam timetable responsibility |
|---|---|
| `controller` | HTTP routes for the admin workflow. |
| `dto.request`, `dto.response` | Request and response shapes for the existing frontend. |
| `dto` | Scheduling input shared by Java and Gemini. |
| `service` | Load database data, generate drafts, call Gemini and save reviewed versions. |
| `validation` | Drawing rules, review of proposed edits and Gemini response validation. |
| `Mapper` | Convert scheduled exams into existing timetable event, slot and allocation entities. |
| `exception` | Format errors for the admin API. |

Open `http://localhost:8081/admin/timetable.html` and sign in with an enabled account whose role is ADMIN or ROLE_ADMIN. Use the existing application's PostgreSQL settings and keep schema mode at validate.

If there is no admin account, create one by adding `--timetable-admin.create=YOUR_USERNAME --timetable-admin.email=YOUR_EMAIL` to IntelliJ Program arguments and adding a strong `TIMETABLE_ADMIN_PASSWORD` environment variable. Run once, then remove those arguments and that password variable and restart. An existing account is never overwritten.

The exam period must already have linked semesters and available slots. Use **Semester exams and durations** to choose which modules have exams and set their duration. Generate a fresh draft after changing requirements. Use **Check move** or **Check swap** to review the new start times; rooms are recalculated for the complete draft.

A hard error blocks saving. Missing enrolments require manual review acknowledgement; the saved version stays DRAFT. Use only the reported exam details to handle repeat-student complaints. The application does not require access to the university's full student database for draft editing. A warning is not evidence that no other students have clashes.

## API contract

All requests below require a signed-in admin session. Mutations also require the CSRF token from `GET /api/admin/exam-timetables/csrf`.

| Method and path under /api/admin/exam-timetables | Purpose |
|---|---|
| GET /periods | List exam periods. |
| GET /{periodId} | Read exam setup, possible start slots and saved draft IDs. |
| PUT /{periodId}/exams/{moduleId} | Save `enabled` and `durationMinutes` for a module in the period. |
| POST /{periodId}/preview?engine=java or gemini | Generate a draft without saving. Defaults to Java. Gemini proposes times using the same database settings, and Java validates and allocates rooms. |
| POST /{periodId}/review | Validate `assignments: [{requirementId, startTimeslotId}]` and return issues and proposed room allocations. |
| POST /{periodId}/drafts | Revalidate the assignments and save a new DRAFT version. Include `manualStudentReview: true` after manual review if membership data is incomplete. |
| GET /{periodId}/drafts/{id} | Open the saved version's start times for review with the current rules. |

Save returns 201 with timetable ID and version, or 422 with validation issues or missing manual acknowledgement. No endpoint publishes a timetable. Editing a saved version creates a new draft on save.

Gemini requires `GEMINI_API_KEY` in the backend's environment. The key must never be put into frontend code. For a Gemini preview, call `POST /api/admin/exam-timetables/1/preview?engine=gemini`. The response has the same `assignments` and `review` shape as Java. Gemini may take several minutes. If unavailable after retries the endpoint returns 503 and a readable issue. A rejected provider request or malformed response returns 502. A proposal that violates timetable rules returns 422. The admin can choose Java explicitly after a failure.
