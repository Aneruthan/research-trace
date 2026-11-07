# ResearchTrace — Docker Deployment Guide

This document describes the production Docker packaging and orchestration setup for ResearchTrace (`d70f86132b36f8da104f64b3c697ce054172e7ba`). It enables course evaluators, teaching assistants, and stakeholders to deploy the full-stack system with a single command without installing JDK, Node.js, or a local PostgreSQL instance.

---

## 1. Architecture Overview

ResearchTrace runs as a three-tier orchestrated container topology:

```
[ Client Browser ]
        │
        ▼ (Port 5173:80)
┌──────────────────────────────────────────────┐
│  researchtrace-frontend (Nginx Alpine)       │
│  - Serves React 19 production static bundle  │
│  - Reverse-proxies /api/* to backend:8080    │
└───────────────────────┬──────────────────────┘
                        │ internal Docker network
                        ▼ (Port 8080)
┌──────────────────────────────────────────────┐
│  researchtrace-backend (Temurin JRE 17)      │
│  - Spring Boot 4.1.1 executable JAR          │
│  - Google Gemini AI / Stub extractor engine  │
└───────────────────────┬──────────────────────┘
                        │ internal Docker network
                        ▼ (Port 5432)
┌──────────────────────────────────────────────┐
│  researchtrace-db (PostgreSQL 17 Alpine)     │
│  - Persistent named volume (postgres_data)   │
│  - Auto-initialization from docker/initdb    │
│  - Host port mapped to 5433 (no 5432 conflict)│
└──────────────────────────────────────────────┘
```

---

## 2. Prerequisites

- **Docker Engine:** Version 20.10 or later
- **Docker Compose:** Version 2.0 or later (included with Docker Desktop)
- An active terminal shell (Bash, Zsh, or PowerShell)

---

## 3. Quick Start (Teaching Assistant / Evaluator)

### Step 1: Clone and Configure Environment

```bash
git checkout docker-submission
cp .env.example .env
```

Edit `.env` to configure your Gemini API key (or use offline stub mode):
- **Live AI Mode:** Set `GEMINI_API_KEY=your_key_here` (from [Google AI Studio](https://aistudio.google.com/)).
- **Offline Stub Mode:** If you do not have an API key, set:
  ```ini
  TASK_EXTRACTOR_MODE=stub
  RESEARCH_EXTRACTOR_MODE=stub
  GEMINI_CHAT=none
  ```

### Step 2: Build and Launch Containers

```bash
docker compose up -d --build
```

Docker Compose will:
1. Build the backend multi-stage image (`eclipse-temurin:17-jdk` -> `eclipse-temurin:17-jre`).
2. Build the frontend multi-stage image (`node:22-alpine` -> `nginx:alpine`).
3. Start the PostgreSQL 17 container and initialize seed data from `docker/initdb/01-init.sql`.
4. Wait for PostgreSQL health check (`pg_isready`) before launching the backend.
5. Wait for backend health check (`/api/projects`) before launching the frontend.

### Step 3: Access the Application

- **Frontend Application:** Open [http://localhost:5173](http://localhost:5173) in your browser.
- **Backend API Directly:** Test [http://localhost:8080/api/projects/1/dashboard](http://localhost:8080/api/projects/1/dashboard).
- **PostgreSQL Database:** Accessible on host port `5433` (e.g. `psql -h localhost -p 5433 -U postgres -d researchtrace`).

---

## 4. Verification and Health Checks

### Check Container Status

```bash
docker compose ps
```
*All three services (`researchtrace-db`, `researchtrace-backend`, `researchtrace-frontend`) should report status `Up (healthy)`.*

### Test Backend Health

```bash
curl -f http://localhost:8080/api/projects/1/dashboard
```

### Test Research Summary Endpoint

```bash
curl -f http://localhost:8080/api/projects/1/research/summary
```

### Test Database Persistence Across Restarts

```bash
# Restart the containers
docker compose down
docker compose up -d

# Verify that project 1 and prior data remain intact
curl -f http://localhost:8080/api/projects/1/dashboard
```
*The `postgres_data` Docker named volume persists all data across container restarts and updates.*

---

## 5. Port Configuration and Host Isolation

To prevent conflicts with any local PostgreSQL service already running on standard host port `5432`, the container ports are mapped as follows:

| Service | Container Port | Host Port | Purpose |
|---|---|---|---|
| `frontend` | 80 | **5173** | User interface & API reverse proxy |
| `backend` | 8080 | **8080** | REST API endpoints & health checks |
| `postgres` | 5432 | **5433** | Container database (no host 5432 conflict) |

All ports can be customized in `.env` (`FRONTEND_PORT`, `BACKEND_PORT`, `DB_HOST_PORT`).

---

## 6. Packaging for Course Portal Submission (Docker Image Upload)

Per the course submission guidelines (`PAI.pdf`), teams must upload a Docker image to Google Drive with view access to the course staff Google group:

```bash
# 1. Build and tag the images with the submission commit SHA
docker compose build

# 2. Save the frontend and backend images into a tarball archive
docker save \
  researchtrace-backend:d70f861 \
  researchtrace-frontend:d70f861 \
  postgres:17-alpine \
  -o researchtrace_submission_d70f861.tar

# 3. Compress the tarball (optional, recommended)
gzip researchtrace_submission_d70f861.tar

# 4. Upload researchtrace_submission_d70f861.tar.gz to Google Drive
# 5. Share with VIEW ACCESS to the course staff Google group email
# 6. Paste the share link into the report header and the submission portal
```

---

## 7. Clean Teardown

```bash
# Stop and remove containers and network
docker compose down

# To also remove persistent database volume (resets to initial seed):
docker compose down -v
```
