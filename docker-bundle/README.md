# ResearchTrace — Standalone Evaluator Deployment Bundle

This bundle contains everything required to deploy the complete ResearchTrace application using Docker Compose without needing the full Git repository or development environment (JDK, Node.js, Maven, etc.).

---

## 1. Quick Start (Evaluator / Teaching Assistant)

### Step 1: Extract and Load Images (if using submission archive)
If you received `researchtrace_submission.tar.gz` from the course Google Drive link:

```bash
# Decompress and load container images into Docker
docker load -i researchtrace_submission.tar.gz
```

### Step 2: Configure Environment
Copy the configuration template:

```bash
# On Linux/macOS
cp .env.example .env

# On Windows PowerShell
Copy-Item .env.example .env
```

*Note: By default, `.env` runs in **Offline Evaluation Mode** (`TASK_EXTRACTOR_MODE=stub`, `RESEARCH_EXTRACTOR_MODE=stub`, `GEMINI_CHAT=none`). No Google Gemini API key or external network access is needed to evaluate all application features.*

*(Optional)* If you wish to test live Google Gemini extraction:
1. Open `.env`
2. Set `GEMINI_API_KEY=your_gemini_api_key_here`
3. Set `GEMINI_CHAT=google-genai`
4. Set `TASK_EXTRACTOR_MODE=gemini` and `RESEARCH_EXTRACTOR_MODE=gemini`

### Step 3: Start the Application

```bash
docker compose up -d
```

### Step 4: Verify Deployment Status

```bash
docker compose ps
```

All three services should show `Up (healthy)`:
- `researchtrace-db`
- `researchtrace-backend`
- `researchtrace-frontend`

---

## 2. Access Points

| Service | URL / Port | Description |
|---|---|---|
| **Web Application** | [http://localhost:5173](http://localhost:5173) | Main UI (Vite + React 19) served via Nginx |
| **Backend API** | [http://localhost:8080/api/projects/1/dashboard](http://localhost:8080/api/projects/1/dashboard) | Spring Boot REST API |
| **PostgreSQL DB** | `localhost:5433` | Host-isolated container database (`researchtrace` / `postgres`) |

---

## 3. Verification Commands

### Check Project Dashboard API
```bash
curl -f http://localhost:8080/api/projects/1/dashboard
```

### Check Research Summary API
```bash
curl -f http://localhost:8080/api/projects/1/research/summary
```

### Check Frontend
```bash
curl -f http://localhost:5173/
```

---

## 4. Teardown

To stop the containers:
```bash
docker compose down
```

To stop and remove persistent data (reset database):
```bash
docker compose down -v
```
