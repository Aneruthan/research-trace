# ResearchTrace

ResearchTrace is a CS5013 project for tracing research decisions, experiments, results, and student progress from supervision meetings. A professor or student can record a meeting, review extracted tasks and research findings, and then see progress and a research timeline for a project.

This repository is the integrated `main` branch. It includes project management and AI extraction (Lalitha Gayathri V) and progress plus research trace (Aneruthan Subbiah K).

## Architecture

- **Backend:** Spring Boot 4.1.1, Java 17, Spring Data JPA. The running application uses PostgreSQL. Tests use an in-memory H2 database.
- **Frontend:** React, TypeScript, and Vite. The UI calls `http://localhost:8080` unless `VITE_API_BASE_URL` is set. The sidebar stores the active project id and every project page uses that id.
- **Persistence:** `spring.jpa.hibernate.ddl-auto=update` updates the schema on startup. It does not drop existing tables.

### Module responsibilities

**Aneruthan Subbiah K (CS26E002)**

- Task and progress calculations
- Research decisions, experiments, results, and the research-event timeline
- Accepted-review persistence and decision supersession
- Progress and research views

**Lalitha Gayathri V (CS26E011)**

- Users, projects, members, milestones, and meetings
- Meeting-note extraction and human review of task suggestions
- Research change detection
- Dashboard composition that reads progress and research data

The modules meet at a meeting review. Accepting a task suggestion creates a task. Accepting a research review stores decisions, experiments, results, and timeline events. Progress is calculated from completed tasks. The dashboard and Research Trace page read those stored records.

## Stakeholder

Prof. Pulkit Verma, IIT Madras.

Contact email and phone are not recorded in this repository. Use the contact channel given by the course staff. Do not put passwords or API keys in this file.

## Docker Quickstart

For automated multi-container deployment without installing JDK 17, Node.js, or PostgreSQL locally:

```bash
# 1. Copy environment template
cp .env.example .env

# 2. Build and launch database, backend, and frontend
docker compose up -d --build

# 3. Open application in browser
# Frontend: http://localhost:5173
# Backend API: http://localhost:8080/api/projects/1/dashboard
```

For complete container networking, health checks, persistence verification, and image packaging details, see [docs/docker-deployment.md](docs/docker-deployment.md).

## Prerequisites

- JDK 17
- Node.js with npm
- PostgreSQL, listening on the host and port you put in `DB_URL`
- An existing database and a user that can connect to it

The app does not create the PostgreSQL server or the database. Create the empty database yourself in PostgreSQL, then let Hibernate add tables.

## Database configuration

The backend reads these environment variables in `backend/src/main/resources/application.properties`:

| Variable | Purpose |
| --- | --- |
| `DB_URL` | JDBC URL, for example `jdbc:postgresql://localhost:5432/your_database_name` |
| `DB_USERNAME` | Existing PostgreSQL user |
| `DB_PASSWORD` | Password for that user |

Set them in the shell that starts the backend. Do not commit them, and do not paste them into chat or into this README.

PowerShell example, using your own values:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/your_database_name"
$env:DB_USERNAME = "your_database_user"
$env:DB_PASSWORD = "your_database_password"
```

Optional extraction settings:

| Variable | Default in `application.properties` | Offline value |
| --- | --- | --- |
| `TASK_EXTRACTOR_MODE` | `gemini` | `stub` |
| `RESEARCH_EXTRACTOR_MODE` | `gemini` | `stub` |
| `GEMINI_CHAT` | `google-genai` | `none` |
| `GEMINI_API_KEY` | empty | leave unset for stub mode |

For a demo without calling Gemini, set `TASK_EXTRACTOR_MODE=stub`, `RESEARCH_EXTRACTOR_MODE=stub`, and `GEMINI_CHAT=none` before starting the backend. Stub research extraction reads structured lines from the meeting notes. Live Gemini extraction needs a key in `GEMINI_API_KEY`.

Tests do not use these variables. `backend/src/test/resources/application.properties` forces H2 and stub mode.

## Backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The API listens on `http://localhost:8080`.

Run tests:

```powershell
cd backend
.\mvnw.cmd test
```

Frontend checks, from `frontend`:

```powershell
npm test
npm run lint
npx tsc -b --noEmit
npm run build
```

## Frontend

```powershell
cd frontend
npm install
npm run dev
```

Open the URL Vite prints, usually `http://localhost:5173`. The default API base is `http://localhost:8080` (`frontend/.env` may set `VITE_API_BASE_URL`).

Production build:

```powershell
cd frontend
npm run build
```

## Database integrity

Hibernate `ddl-auto=update` adds missing nullable columns. It does not add foreign keys. Fresh Docker databases are created from `docker/initdb/01-init.sql`. An existing PostgreSQL database is not modified by that script.

To add indexes, unique email and membership checks, and foreign keys without deleting rows, apply `docker/initdb/02-integrity.sql` yourself. A brand-new Docker database runs it once after the schema script. An existing database volume does not. The script contains no `DELETE`, `TRUNCATE`, or `DROP TABLE`. Do not run it against a database you have not backed up. It was executed on a disposable PostgreSQL 18.1 cluster, not on the existing demo database.

Apply it with `psql` against the database named in `DB_URL`:

```powershell
psql -d your_database_name -f docker/initdb/02-integrity.sql
```

Constraints are added `NOT VALID`, so existing orphan rows stay in place and new writes are checked. Unique indexes on lower-cased email and on `(project_id, user_id)` are created only when no duplicates exist. If duplicates exist, that index is skipped and PostgreSQL prints a notice that no rows were changed. The other constraints are still added.

After a clean apply, validate each foreign key. Validation fails while an orphan remains and does not delete the row:

```sql
ALTER TABLE public.task VALIDATE CONSTRAINT fk_task_milestone;
```

Repeat that statement for every `fk_*` constraint the script created. A failure names the orphan key. Fix or archive that relationship yourself, then run the statement again. User deletion stays blocked while a membership or an assigned task exists. Deleting a project that still has research records returns 409; archive does not remove that history.

The same disposable cluster covered four cases: a clean database, existing valid rows, existing duplicate emails and memberships, and an existing orphan milestone reference. Valid rows remained. Duplicates skipped only the affected unique index. The orphan row remained, new invalid writes were rejected, and `VALIDATE CONSTRAINT` failed while that orphan was still present.

## Demo path

This is a script for a stakeholder to execute. It is not a record that the stakeholder has already done so. There is no authentication.

1. Start PostgreSQL, the backend, and the frontend.
2. Open **Projects** and create a project, or select one in the sidebar. Optional demo rows are not inserted unless you start the backend with `researchtrace.demo.seed=true`. That flag only adds a missing GreenRoute project and the alex@research.edu student. It does not update existing rows and is off by default.
3. Open **Students**. Create a student or add an existing one to the selected project.
4. Open **Dashboard**. It loads the selected project's progress, milestones, meetings, tasks, and overdue count.
5. Open **Meetings**, then a meeting. Paste realistic notes, for example:

```text
ACTION: Compare the baseline and the balanced split
TODO: Asha measures the gender gap by Friday
Decision: Keep the baseline
Reason: The bias numbers are not ready
Experiment: Bias audit
Objective: The baseline has a measurable gap
Result: Gender gap is 4 points
Experiment: Bias audit
Metrics: accuracy gap = 0.04
```

6. Extract task suggestions. Edit, accept, or reject them. An accepted suggestion becomes a task. Create a milestone, assign the task to a project student, and associate the task with the milestone. Mark a task **COMPLETED** on the **Tasks** page and confirm project progress changes on the Dashboard and on the student page.
7. Open **Research Trace**. Extract research information. An item missing a decision title, an experiment title, or a result description stays visible and names the missing field. Fill that field yourself; the app does not invent it. **Accept** is rejected until the required fields are present. **Save as Edited** stores the edited text. The original extraction stays on the review, and the source reference records `origin:ai`, `origin:edited`, or `origin:unknown`. **PENDING**, **EDITED**, and **REJECTED** stay in review history. Only **ACCEPTED** creates decisions, experiments, and results. Switch projects in the sidebar and confirm the other project's records are not shown. Opening another project's meeting or task returns "not found".
8. Supersede an active decision from the Research Trace page when two decisions exist. The earlier decision becomes superseded and the replacement points at it.

Expected stored shape after an accepted review that has a decision title, an experiment title, and a result whose experiment name matches that title:

- One active decision, with text and rationale when the notes included them
- One planned experiment, with the objective stored as the hypothesis
- One result linked to that experiment, with metrics when present
- One timeline event for each of those records
- No new task from the research-review buttons

What still requires the stakeholder: they must run this flow themselves on their own meeting notes and data. A passing test suite does not replace that.

## API endpoints

Project and progress:

- `GET /api/projects/{projectId}/dashboard`
- `GET /api/projects/{projectId}/progress`
- `GET /api/projects/{projectId}/milestones`
- `GET /api/projects/{projectId}/milestones/{milestoneId}/progress`
- `GET /api/projects/{projectId}/students`
- `GET /api/projects/{projectId}/students/{studentId}/progress`
- `GET /api/projects/{projectId}/tasks`
- `POST /api/projects/{projectId}/tasks`
- `PUT /api/projects/{projectId}/tasks/{taskId}`

Meetings and review:

- `GET /api/projects/{projectId}/meetings`
- `POST /api/projects/{projectId}/meetings/{meetingId}/task-suggestions/extract`
- `POST /api/projects/{projectId}/meetings/{meetingId}/task-suggestions/{suggestionId}/accept`
- `POST /api/projects/{projectId}/meetings/{meetingId}/task-suggestions/{suggestionId}/reject`
- `POST /api/projects/{projectId}/meetings/{meetingId}/research-extraction`
- `POST /api/projects/{projectId}/meetings/{meetingId}/review`
- `GET /api/projects/{projectId}/meetings/{meetingId}/reviews`

Research records:

- `GET /api/projects/{projectId}/research-decisions`
- `GET /api/projects/{projectId}/experiments`
- `GET /api/projects/{projectId}/results`
- `GET /api/projects/{projectId}/research-events`
- `GET /api/projects/{projectId}/research/summary`
- `GET /api/projects/{projectId}/research/timeline`
- `GET /api/projects/{projectId}/research/decisions/{decisionId}/lineage`
- `POST /api/projects/{projectId}/research/decisions/{decisionId}/supersede`
- `POST /api/projects/{projectId}/research-decisions/{decisionId}/supersede`

More detail is in `docs/task-and-progress-api.md`, `docs/research-traceability-api.md`, and `docs/architecture.md`.

## Troubleshooting

- **Backend exits before Tomcat starts.** `DB_URL`, `DB_USERNAME`, or `DB_PASSWORD` is missing, or PostgreSQL rejected the login. Set the three variables in that same shell. Do not commit them.
- **Port 5432 closed.** Start the existing PostgreSQL service. Do not create a new database unless you intend to point `DB_URL` at an empty database you created.
- **Port 8080 already in use.** Stop the other process or the previous backend before `spring-boot:run`.
- **Frontend "Failed to fetch".** The backend is not running, or the page origin is not allowed. The UI expects the API at `VITE_API_BASE_URL`.
- **Research extraction returns 503.** Gemini is selected and `GEMINI_API_KEY` is unset. Use stub mode for an offline demo.
- **Research Trace is empty.** Select a project, then accept a review only after each decision has a title, each experiment has a title, and each result has a description. The review screen lists any missing field. A result is stored only when its experiment name matches an experiment in that same extraction.
- **Tests.** `.\mvnw.cmd test` uses H2. A passing H2 suite does not prove the PostgreSQL migration has been applied.
- **No login.** The API does not authenticate callers. Do not expose it on a public network.

## Team

- Aneruthan Subbiah K — CS26E002 — progress and research trace
- Lalitha Gayathri V — CS26E011 — project management and AI extraction
- Stakeholder: Prof. Pulkit Verma, IIT Madras
- Personal contact details: `[add the contact channel supplied by the course]`
