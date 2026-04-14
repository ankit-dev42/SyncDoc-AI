# Quick Manual Runbook (Terminal 1/2/3)

Use this when you want the fastest copy-paste flow.

## Goal

Start infra, backend, and frontend, then run manual smoke tests.

## Terminal 1: Infrastructure

Open Terminal 1 and run:

```bash
cd /Users/ankitkumar/Documents/building-something/my-project
docker compose up -d
docker compose ps
curl -sS http://localhost:9200 | head
```

Expected:
1. postgres, redis, elasticsearch are running
2. Elasticsearch responds on localhost:9200

If Docker is not running, start Docker Desktop first and re-run the same commands.

## Terminal 2: Backend

Open Terminal 2 and run:

```bash
cd /Users/ankitkumar/Documents/building-something/my-project/backend
mvn spring-boot:run
```

Expected:
1. Spring Boot starts on port 8080
2. No Flyway datasource auth errors

Quick check from a separate shell:

```bash
curl -sS -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/v1/workspaces
```

Expected:
1. 401 or 403 is fine without headers
2. Connection refused is not fine

## Terminal 3: Frontend

Open Terminal 3 and run:

```bash
cd /Users/ankitkumar/Documents/building-something/my-project/frontend
npm install
npm run dev -- --host 0.0.0.0 --port 5173
```

Quick check from a separate shell:

```bash
curl -I http://localhost:5173
```

Expected:
1. HTTP/1.1 200 OK

Open browser:
1. http://localhost:5173

## Terminal 4: Manual API Seed and Smoke Test

Use any extra terminal and run these in order.

## 1) Seed membership

```bash
curl -i -X POST \
  -H "X-User-Id: user-1" \
  -H "X-Workspace-Id: workspace-1" \
  http://localhost:8080/api/v1/workspaces/workspace-1/members/user-1
```

## 2) List workspaces

```bash
curl -i \
  -H "X-User-Id: user-1" \
  -H "X-Workspace-Id: workspace-1" \
  http://localhost:8080/api/v1/workspaces
```

## 3) Send a message

```bash
curl -i -X POST \
  -H "Content-Type: application/json" \
  -H "X-User-Id: user-1" \
  -H "X-Workspace-Id: workspace-1" \
  -d '{"senderId":"user-1","content":"quick terminal smoke test","idempotencyKey":"quick-1"}' \
  http://localhost:8080/api/v1/workspaces/workspace-1/channels/general/messages
```

## 4) Read messages

```bash
curl -i \
  -H "X-User-Id: user-1" \
  -H "X-Workspace-Id: workspace-1" \
  "http://localhost:8080/api/v1/workspaces/workspace-1/channels/general/messages?page=0&size=20"
```

## 5) Presence list

```bash
curl -i \
  -H "X-User-Id: user-1" \
  -H "X-Workspace-Id: workspace-1" \
  http://localhost:8080/api/v1/workspaces/workspace-1/presence
```

## UI Smoke Checklist

In the browser:
1. Workspace panel loads
2. Active workspace shows workspace-1
3. Presence area loads without red error
4. Search request returns result or empty state without crash
5. Threads panel loads

## Fast Troubleshooting Branches

## A) Backend fails with password authentication failed for user postgres

Reason:
1. Local Postgres credentials do not match backend config (postgres/password)

Fix options:
1. Use Docker compose as above (recommended)
2. Or run backend with env override:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/collaboration \
SPRING_DATASOURCE_USERNAME=postgres \
SPRING_DATASOURCE_PASSWORD=<your-real-password> \
cd /Users/ankitkumar/Documents/building-something/my-project/backend && mvn spring-boot:run
```

## B) Backend returns 403

Reason:
1. Missing headers or workspace mismatch

Fix:
1. Send both headers on workspace APIs:
   1. X-User-Id
   2. X-Workspace-Id
2. Ensure workspace in path equals X-Workspace-Id

## C) Port already in use

Check:

```bash
lsof -i :8080
lsof -i :5173
lsof -i :5432
lsof -i :6379
lsof -i :9200
```

Stop conflicting process and retry.

## Shutdown

1. Ctrl+C backend and frontend terminals
2. Stop infra:

```bash
cd /Users/ankitkumar/Documents/building-something/my-project
docker compose down
```
