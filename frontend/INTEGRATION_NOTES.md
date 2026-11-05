# Integration notes

The frontend is now matched to the controllers and DTOs in this repository. See README.md for exact route mappings and run instructions.

- Default backend URL: http://localhost:8081 (matches application.properties).
- Removed calls to nonexistent /api/modules, /api/departments and /api/timetable/generate.
- Semester loading sends the selected academicYear as a query parameter. Changing years clears semester and module selections; stale responses are ignored. Module filtering sends { semesterIds }.
- Setup sends { startingDate, endingDate, academicYear, eventType, semesterIds } and retains the returned periodId.
- Exam requirements send { periodIds: [periodId], modules: [{ moduleId, hasExam, examDurationMinutes }] }.
- Successful requirements writes return 204 No Content.
- Generate saves the calendar period and requirements before calling the exam preview endpoint. The exam dashboard handles edits, validation messages, manual review and versioned saves.
- The existing MIS login screen now submits to the frontend login proxy, which uses Spring form login and verifies ADMIN access before navigating. Session cookies are forwarded on GET and POST requests; CSRF tokens use the same session, and rotated cookies are returned to the browser.
- Backend login redirects produce a sign-in error instead of a misleading connection failure. The backend API base path is `/api/admin/timetable-setup`.
- Exam dashboard requests go through the existing session and CSRF proxy to /api/admin/exam-timetables.
