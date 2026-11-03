# ResearchTrace frontend

React, TypeScript, and Vite interface for ResearchTrace. It loads project `1` from the Spring Boot API and shows the dashboard, meetings, tasks, milestones, and research trace.

Full setup, database variables, and API notes are in the repository root `README.md`.

## Run locally

The backend should already be running at `http://localhost:8080`.

```powershell
cd frontend
npm install
npm run dev
```

Open the URL Vite prints. `VITE_API_BASE_URL` overrides the API host. The checked-in `frontend/.env` uses `http://localhost:8080`.

## Build

```powershell
npm run build
```

This typechecks with `tsc -b` and then builds the production bundle. There is no separate frontend unit-test script.
