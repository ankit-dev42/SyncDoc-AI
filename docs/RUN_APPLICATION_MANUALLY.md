# Run and Test the Application Manually (End-to-End)

Need a fast terminal-by-terminal version instead of full details?
See [RUN_APPLICATION_QUICK_TERMINALS.md](RUN_APPLICATION_QUICK_TERMINALS.md).

This guide is a complete manual checklist to run the full stack locally:
- Infrastructure: PostgreSQL, Redis, Elasticsearch
- Backend: Spring Boot (port 8080)
- Frontend: Vite React app (port 5173)
- Manual API and UI smoke tests

## 1. Prerequisites

## 1.1 Required tools

Install these first:

```bash
# macOS (Homebrew)
brew install openjdk@17 maven node docker
```

Also install Docker Desktop and ensure it is running.

Verify tools:

```bash
java -version
mvn -version
node -v
npm -v
docker -v
docker compose version
```

Expected major versions:
- Java 17+
- Maven 3.8+
- Node 18+
- Docker + Docker Compose plugin

## 1.2 Repository root

All commands in this document assume you are in:

```bash
cd /Users/ankitkumar/Documents/building-something/my-project
```

---

## 2. One-Time Project Install

## 2.1 Frontend dependencies

```bash
cd frontend
npm install
cd ..
```

## 2.2 Backend dependency check

```bash
cd backend
mvn -q -DskipTests compile
cd ..
```

---

## 3. Start Infrastructure (Recommended: Docker Compose)

This project is configured for:
- Postgres: `localhost:5432`
- Redis: `localhost:6379`
- Elasticsearch: `localhost:9200`

## 3.1 Start Docker Desktop

Open Docker Desktop manually and wait until status is "running".

## 3.2 Start services

```bash
docker compose up -d
```

## 3.3 Verify containers

```bash
docker compose ps
```

You should see `postgres`, `redis`, `elasticsearch` in `running` state.

## 3.4 Verify endpoints

```bash
curl -sS http://localhost:9200 | head
```

Optional checks:

```bash
# PostgreSQL port
nc -zv localhost 5432

# Redis port
nc -zv localhost 6379
```

---

## 4. Start Backend

Backend config uses:
- DB: `jdbc:postgresql://localhost:5432/collaboration`
- Username: `postgres`
- Password: `password`

(from `backend/src/main/resources/application.yml`)

## 4.1 Run backend

```bash
cd backend
mvn spring-boot:run
```

Wait for logs indicating server startup on port `8080`.

## 4.2 Backend health check

In a new terminal:

```bash
curl -sS -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/v1/workspaces
```

Expected:
- `401` or `403` without headers (normal)
- Not connection refused

---

## 5. Start Frontend

In a separate terminal:

```bash
cd frontend
npm run dev -- --host 0.0.0.0 --port 5173
```

Check:

```bash
curl -I http://localhost:5173
```

Expected: `HTTP/1.1 200 OK`

Open in browser:
- http://localhost:5173

---

## 6. Manual API Smoke Test (Important Headers)

This backend uses header-based auth + workspace scoping.

For almost all workspace APIs, include both headers:
- `X-User-Id: user-1`
- `X-Workspace-Id: workspace-1`

## 6.1 Create workspace membership (seed)

```bash
curl -i -X POST \
  -H "X-User-Id: user-1" \
  -H "X-Workspace-Id: workspace-1" \
  http://localhost:8080/api/v1/workspaces/workspace-1/members/user-1
```

Expected: 200 OK.

## 6.2 List workspaces

```bash
curl -i \
  -H "X-User-Id: user-1" \
  -H "X-Workspace-Id: workspace-1" \
  http://localhost:8080/api/v1/workspaces
```

Expected: 200 OK with workspace list JSON.

## 6.3 Presence list

```bash
curl -i \
  -H "X-User-Id: user-1" \
  -H "X-Workspace-Id: workspace-1" \
  http://localhost:8080/api/v1/workspaces/workspace-1/presence
```

Expected: 200 OK.

## 6.4 Send a message

```bash
curl -i -X POST \
  -H "Content-Type: application/json" \
  -H "X-User-Id: user-1" \
  -H "X-Workspace-Id: workspace-1" \
  -d '{"senderId":"user-1","content":"hello from manual test","idempotencyKey":"manual-1"}' \
  http://localhost:8080/api/v1/workspaces/workspace-1/channels/general/messages
```

Expected: 200 OK with message payload.

## 6.5 Get messages

```bash
curl -i \
  -H "X-User-Id: user-1" \
  -H "X-Workspace-Id: workspace-1" \
  "http://localhost:8080/api/v1/workspaces/workspace-1/channels/general/messages?page=0&size=20"
```

Expected: 200 OK with paged response.

---

## 7. Manual UI Smoke Test

After frontend and backend are both running:

1. Open http://localhost:5173
2. Verify workspace panel loads
3. Select workspace `workspace-1`
4. Verify presence section loads (no red error)
5. In search box, submit a query
6. Verify threads panel loads and no 403 errors for active workspace

If you see the message:
"You do not have access to this workspace. Select another workspace to continue."
then check:
- `X-Workspace-Id` matches URL workspace path on backend requests
- You seeded membership for that user/workspace

---

## 8. Common Failures and Fixes

## 8.1 Docker daemon not running

Error:
`Cannot connect to the Docker daemon ...`

Fix:
1. Start Docker Desktop
2. Re-run:

```bash
docker compose up -d
```

## 8.2 Backend fails: password authentication failed for user postgres

Cause: local Postgres credentials do not match `application.yml`.

Fix options:
1. Preferred: run Docker Compose (uses `postgres/password`)
2. Or update local DB user password to `password`
3. Or temporarily override Spring datasource via environment variables:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/collaboration \
SPRING_DATASOURCE_USERNAME=postgres \
SPRING_DATASOURCE_PASSWORD=<your-password> \
cd backend && mvn spring-boot:run
```

## 8.3 Backend returns 403 on workspace endpoints

Cause: header mismatch or missing headers.

Fix:
- Always send both headers:
  - `X-User-Id`
  - `X-Workspace-Id`
- Ensure workspace in header equals workspace in URL path

## 8.4 Port already in use

Check and free ports:

```bash
lsof -i :8080
lsof -i :5173
lsof -i :5432
lsof -i :6379
lsof -i :9200
```

Stop conflicting process or change app port.

---

## 9. Optional: Run Automated Checks Before Manual Testing

```bash
# Backend
cd backend
mvn -q -DskipTests compile
mvn -q -Dtest=MultiTenantIsolationIntegrationTest,CrossWorkspaceAccessSecurityTest test

# Frontend
cd ../frontend
npm run build
```

---

## 10. Stop Everything

## 10.1 Stop frontend/backend

Use Ctrl+C in each running terminal.

## 10.2 Stop infrastructure

```bash
cd /Users/ankitkumar/Documents/building-something/my-project
docker compose down
```

To remove volumes too:

```bash
docker compose down -v
```

---

## 11. Quick Minimal Checklist

1. Docker Desktop running
2. `docker compose up -d`
3. `cd backend && mvn spring-boot:run`
4. `cd frontend && npm run dev -- --host 0.0.0.0 --port 5173`
5. `curl -I http://localhost:5173` -> 200
6. Seed membership API
7. Hit `/api/v1/workspaces` with `X-User-Id` + `X-Workspace-Id`
8. Open UI and verify workspace/presence/search/threads sections load
