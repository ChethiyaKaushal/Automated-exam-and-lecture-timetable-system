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
| `exception` | Handle setup and exam errors in GlobalExceptionHandler. Exam review and Gemini errors include the issues list used by the admin page. |

Open `http://localhost:8081/admin/timetable.html` and sign in with an enabled account whose role is ADMIN or ROLE_ADMIN. Use the existing application's PostgreSQL settings and keep schema mode at validate.

If there is no admin account, create one by adding `--timetable-admin.create=YOUR_USERNAME --timetable-admin.email=YOUR_EMAIL` to IntelliJ Program arguments and adding a strong `TIMETABLE_ADMIN_PASSWORD` environment variable. Run once, then remove those arguments and that password variable and restart. An existing account is never overwritten.

Create an exam period with `POST /api/admin/timetable-setup/setup` while signed in as an admin. Get the CSRF header name and token from `GET /api/admin/timetable-setup/csrf` first. The request takes the start and end dates, academic year, event type, and selected semester IDs. Send any additional holiday dates in the optional `holidayDates` array.

    {
      "startingDate": "2026-12-18",
      "endingDate": "2026-12-28",
      "academicYear": "2025/2026",
      "eventType": "EXAM",
      "semesterIds": [1, 3],
      "holidayDates": ["2026-12-24"]
    }

The EXAM period and its timeslots are saved together. The date range is inclusive. Eligible weekdays get 30-minute slots from 09:00 to 12:00 and 13:30 to 18:00. The backend also skips dates in the application setting `timetable.exam.holiday-dates`. Add dates to that setting or send them with the request for periods beyond the configured calendar. A period with zero eligible days returns 400. Lecture periods keep their existing slot behavior.

Use the returned `periodId` when saving module requirements and generating a preview. `POST /api/admin/timetable-setup/requirements` saves exam durations and requires the same admin session and CSRF header.

The exam period must have linked semesters and available slots. Use **Semester exams and durations** to choose which modules have exams and set their duration. Generate a fresh draft after changing requirements. Use **Check move** or **Check swap** to review the new start times; selected venues are checked again, and rooms are allocated for exams without a venue selection.

A hard error blocks saving. Missing enrolments require manual review acknowledgement; the saved version stays DRAFT. Use only the reported exam details to handle repeat-student complaints. The application does not require access to the university's full student database for draft editing. A warning is not evidence that no other students have clashes.

## API contract

All requests below require a signed-in admin session. Mutations also require the CSRF token from `GET /api/admin/exam-timetables/csrf`.

| Method and path under /api/admin/exam-timetables | Purpose |
|---|---|
| GET /periods | List exam periods. |
| GET /{periodId} | Read exam setup, possible start slots and saved draft IDs. |
| PUT /{periodId}/exams/{moduleId} | Save `enabled` and `durationMinutes` for a module in the period. |
| POST /{periodId}/preview?engine=java or gemini | Generate a draft without saving. Defaults to Java. Gemini proposes times and venues from the database inputs. Java validates them. |
| POST /{periodId}/review | Validate assignments with requirementId, startTimeslotId and optional venueIds and return issues and proposed room allocations. |
| POST /{periodId}/drafts | Revalidate the assignments and save a new DRAFT version. Include `manualStudentReview: true` after manual review if membership data is incomplete. |
| GET /{periodId}/drafts/{id} | Open the saved version's start times for review with the current rules. |

Save returns 201 with timetable ID and version, or 422 with validation issues or missing manual acknowledgement. No endpoint publishes a timetable. Editing a saved version creates a new draft on save.

Gemini requires `GEMINI_API_KEY` in the backend's environment. The key must never be put into frontend code. For a Gemini preview, call `POST /api/admin/exam-timetables/1/preview?engine=gemini`. Gemini assignments include venueIds. Send them back with the assignments when reviewing or saving to keep the proposed rooms. Gemini may take several minutes. If unavailable after retries the endpoint returns 503 and a readable issue. A rejected provider request or malformed response returns 502. If a complete proposal violates timetable rules, the backend sends the validation errors to Gemini for one revision. If the revised timetable still violates the rules, the endpoint returns 422. The admin can choose Java explicitly after a failure.

## Export a draft from IntelliJ

Set the working directory to the repository root and use these program arguments for a Gemini export.

    --exam-draft.period-id=1 --exam-draft.engine=gemini

Each successful run creates a new JSON file in the local docs/timetables/runs directory. The console prints the full path. Use engine=java to export from the Java scheduler. If you set exam-draft.output to an exact path, that file must be new.
