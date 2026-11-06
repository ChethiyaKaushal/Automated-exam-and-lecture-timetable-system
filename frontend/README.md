# Timetable frontend

Next.js frontend for the existing Spring Boot timetable setup service. All integration changes are inside `frontend/`; backend files are unchanged.

## Folder structure

```text
frontend/
|-- public/images/
|-- src/
|   |-- app/
|   |   |-- _pages/                 # All screen components (excluded from routing)
|   |   |   |-- LoginPage.tsx
|   |   |   `-- admin/
|   |   |       |-- ExamTimetablePage.tsx
|   |   |       |-- GeneratePage.tsx
|   |   |       |-- SettingsPage.tsx
|   |   |       `-- TimetablePage.tsx
|   |   |-- admin/                  # Thin route files and shared admin layout
|   |   |-- api/timetable/          # API route handlers
|   |   |-- page.tsx                # Login route
|   |   |-- layout.tsx
|   |   `-- globals.css
|   |-- components/layout/         # Shared header and sidebar
|   |-- features/timetable/        # Reusable components, API, types, and utilities
|   `-- services/server/           # Spring Boot proxy
|-- tests/
`-- configuration files
```

Edit screen content in `src/app/_pages`; all admin screens live together in `_pages/admin`. The underscore makes this a private Next.js folder, so these named components do not create extra URLs. A top-level `src/pages` would activate the separate Pages Router.

Route files simply export their screen component:

| Route file under `src/app` | Screen under `_pages` | URL |
| --- | --- | --- |
| `page.tsx` | `LoginPage.tsx` | `/` |
| `admin/generate/page.tsx` | `admin/GeneratePage.tsx` | `/admin/generate` |
| `admin/exam-timetable/page.tsx` | `admin/ExamTimetablePage.tsx` | `/admin/exam-timetable` |
| `admin/timetable/page.tsx` | `admin/TimetablePage.tsx` | `/admin/timetable` |
| `admin/settings/page.tsx` | `admin/SettingsPage.tsx` | `/admin/settings` |

The `@/` alias resolves to `src/`. Shared layout components belong in `components/layout`; reusable timetable logic belongs in `features/timetable`. Import `services/server` only from server code. Keep static assets, environment files, and build configuration at the frontend root.

## Run locally

1. Start PostgreSQL and your existing Spring Boot application. Its configured port is **8081**.
2. In this directory, optionally copy `.env.example` to `.env.local`. Set `SPRING_BOOT_API_URL` if the backend runs elsewhere. This is a server-only setting; restart Next.js after changing it.
3. Run `npm install` if dependencies are missing, then `npm run dev`.
4. Open http://localhost:3000 and sign in with the email of an existing ADMIN account in the backend users table. The existing login form now authenticates with Spring Boot before opening the timetable workspace.

For production: `npm run build`, then `npm start`.

## Supported workflow

- Choose an academic year from the backend dropdown to load only its semesters, then select semesters and enter dates. Changing years clears previous semester and module selections.
- Review modules grouped under semester headings and department subheadings. Each module retains its own exam selection and duration. Semester and department headings use semesterName and departmentName from the module response. Department groups use departmentName directly, combining matching names within each semester.
- For exams, select modules and enter durations in minutes. The default is 180. The exam grid accepts 30 to 270 minutes in 30-minute steps. Add any extra holidays before reviewing modules.
- Generate timetable saves the period and requirements, then requests a Gemini or Java preview. A valid draft with complete student data is saved immediately. When student data is incomplete, confirm manual review on the exam dashboard before saving.
- The exam dashboard shows scheduled dates, times, halls and seats. Check a move or swap, read any problems in the separate review panel, and save or reopen draft versions.
- For lectures, save the calendar period. The backend does not expose lecture requirement creation.
- View connection status under Service status.

The setup and requirement requests are separate transactions. If requirements fail after the period is created, its ID remains on screen and retries reuse that period. Keep the page open. If a request times out, verify the database before repeating it because the backend may have committed the write. Generation can be retried in the same form without resubmitting successfully saved requirements.

The backend uses the selected academic year and dates to label the exam period.

## API mapping

| Frontend route | Existing Spring Boot route |
| --- | --- |
| GET /api/timetable/academic-years | GET /api/admin/timetable-setup/academic-years |
| GET /api/timetable/semesters?academicYear=... | GET /api/admin/timetable-setup/semesters?academicYear=... |
| POST /api/timetable/modules | POST /api/admin/timetable-setup/filtermodule |
| POST /api/timetable/setup | POST /api/admin/timetable-setup/setup |
| POST /api/timetable/requirements | POST /api/admin/timetable-setup/requirements |
| GET /api/timetable/exams/periods | GET /api/admin/exam-timetables/periods |
| GET /api/timetable/exams/{periodId} | GET /api/admin/exam-timetables/{periodId} |
| POST /api/timetable/exams/{periodId}/preview?engine=gemini | POST /api/admin/exam-timetables/{periodId}/preview?engine=gemini |
| POST /api/timetable/exams/{periodId}/review | POST /api/admin/exam-timetables/{periodId}/review |
| POST /api/timetable/exams/{periodId}/drafts | POST /api/admin/exam-timetables/{periodId}/drafts |
| GET /api/timetable/exams/{periodId}/drafts/{draftId} | GET /api/admin/exam-timetables/{periodId}/drafts/{draftId} |

The server proxy uses no-store requests, a 15-second timeout for normal requests and an 11-minute timeout for Gemini previews. It preserves API errors and handles 204 responses without parsing JSON. Browser requests remain on the frontend origin, so no backend CORS changes are needed.

The `/api/auth/login` proxy fetches Spring's generated `/login` form to obtain its pre-authentication CSRF token, submits the email and password to Spring's form login, and checks admin authorization using the protected CSRF endpoint. It relays the rotated session cookie to the browser with frontend scope and HttpOnly. Backend cookie Domain is removed and Path is set to `/`; Secure and SameSite attributes are retained. The Remember me field is forwarded, but persistence remains controlled by Spring's configuration.

All timetable requests forward the browser's backend session cookie. Before each POST, the proxy calls `GET /api/admin/timetable-setup/csrf` with that same session and sends the returned header/token with the POST. An existing session does not need a new Set-Cookie response. Cookie rotations and deletions are relayed to the browser, including on error responses. Failed token requests prevent writes; saves are never automatically retried. Login redirects become JSON 401 errors, and access-denied responses remain 403.

## Current service limits

Timetable setup data is protected by Spring's existing admin authorization. Frontend page shells can still render without authentication; their backend data requests require an authorized session. The lecture timetable currently shows a placeholder.

Run `npm test` for proxy and login regression tests. To verify against running Next.js and Spring Boot servers, provide `TEST_ADMIN_EMAIL` and `TEST_ADMIN_PASSWORD` for an existing ADMIN account and run `npm run test:integration`. This checks login, academic years, semester filtering, module review and invalid-request handling without creating timetable records.

Run `npm run test:integration` while the frontend is running to verify rendered pages, real read endpoints, and safe validation failures. This check does not create database records.
