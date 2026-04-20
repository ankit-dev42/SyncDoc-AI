# Quickstart: Phase 1 — Foundation Security

**Branch**: `003-phase1-foundation-security`
**Audience**: Engineers testing or running the app after Phase 1 is implemented

---

## Prerequisites

- Java 17 (`java -version`)
- Maven 3.9+ (`mvn -version`)
- Docker Desktop running (`docker ps`)
- `backend/env/.env.local` populated (see below)

---

## 1. Set Required Environment Variables

Create or update `backend/env/.env.local`:

```bash
# JWT — must be ≥32 ASCII characters (256 bits)
JWT_SECRET=change-me-to-a-long-random-string-of-at-least-32-chars

# GitHub webhook (must not be blank — startup guard will reject blank value)
GITHUB_WEBHOOK_SECRET=your-github-webhook-secret

# Frontend CORS origin
FRONTEND_ORIGIN=http://localhost:5173

# Database (Docker Compose default)
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/syncdoc
SPRING_DATASOURCE_USERNAME=syncdoc
SPRING_DATASOURCE_PASSWORD=syncdoc
```

> **If `JWT_SECRET` is shorter than 32 characters**, the application will throw
> `IllegalArgumentException` at startup via `@PostConstruct` in `JwtTokenService`.
>
> **If `GITHUB_WEBHOOK_SECRET` is blank**, the application will throw
> `IllegalStateException` at startup via `@PostConstruct` in `GitHubWebhookSignatureVerifier`.
> Both failures are intentional fail-fast guards.

---

## 2. Start the Database

```bash
docker-compose up -d postgres
```

Wait ~5 seconds for PostgreSQL to accept connections.

---

## 3. Run Flyway Migrations

```bash
cd backend
mvn flyway:migrate -pl backend
```

Expected output:
```
Successfully applied 6 migrations to schema "public"
(V1, V2, V3, V4, V5, V6)
```

Verify the new tables exist:
```bash
docker exec -it syncdoc_postgres psql -U syncdoc -d syncdoc -c "\dt"
```
You should see `users`, `refresh_tokens` in the table list.

---

## 4. Start the Application

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Wait for the log line:
```
Started CollaborationApplication in X.XXX seconds
```

---

## 5. Verify Auth Flow (curl)

### Register
```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"correct-horse-battery-staple","displayName":"Alice"}' \
  | jq .
```
Expected: `HTTP 201`, body `{"id":"...","email":"alice@example.com","displayName":"Alice"}`.

### Login
```bash
curl -s -c /tmp/cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"correct-horse-battery-staple"}' \
  | jq .
```
Expected: `HTTP 200`, body `{"accessToken":"eyJ...","tokenType":"Bearer","expiresIn":900}`.
The `Set-Cookie: refreshToken=...` header is saved by `-c /tmp/cookies.txt`.

### Save the access token
```bash
ACCESS_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"correct-horse-battery-staple"}' \
  | jq -r .accessToken)
```

### Access a protected route
```bash
curl -s http://localhost:8080/api/v1/projects/some-id \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  | jq .
```
Expected: `HTTP 404` (project doesn't exist in Phase 1) with `ErrorResponse` — NOT 401.
Without the header: `HTTP 401`.

### Verify 401 on protected route (no token)
```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/projects/some-id
# → 401
```

### Refresh tokens
```bash
curl -s -b /tmp/cookies.txt -c /tmp/cookies.txt \
  -X POST http://localhost:8080/api/auth/refresh \
  | jq .
```
Expected: new `accessToken` in body; new `refreshToken` cookie set; old cookie replaced.

### Logout
```bash
curl -s -b /tmp/cookies.txt -X POST http://localhost:8080/api/auth/logout
# → HTTP 204
```

### Verify refresh cookie is cleared after logout
```bash
curl -s -b /tmp/cookies.txt -X POST http://localhost:8080/api/auth/refresh
# → HTTP 401, error: "INVALID_TOKEN"
```

---

## 6. Verify Public Routes Still Work Without Token

```bash
# Actuator health — must return 200 without sending any Authorization header
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health
# → 200

# Webhook route — no JWT required (HMAC auth handled separately)
curl -s -o /dev/null -w "%{http_code}" \
  -X POST http://localhost:8080/api/v1/webhooks/github \
  -H "Content-Type: application/json" \
  -d '{}'
# → 400 or 401 from HMAC check — NOT 401 from JWT check
```

---

## 7. Verify DTO Boundaries

### stripeCustomerId must be absent
```bash
curl -s http://localhost:8080/api/v1/subscriptions/any-user-id/tier \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  | jq 'keys'
# → response data keys must NOT include "stripeCustomerId"
```

### ProjectDto must not leak entity fields
```bash
curl -s http://localhost:8080/api/v1/projects/any-id \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  | jq '.data | keys'
# → must contain exactly: ["accessControl","createdAt","id","name","updatedAt"]
# → must NOT contain: "owner","deletedAt","passwordHash"
```

---

## 8. Run the Test Suite

```bash
cd backend
mvn test -Dspring.profiles.active=test
```

All Phase 1 tests must pass:
- `JwtTokenServiceTest` — 5 cases
- `AuthControllerIntegrationTest` — 6 flows
- `JwtAuthenticationFilterTest` — 4 cases
- `WebSecurityConfigIntegrationTest` — route access matrix
- `RateLimitFilterIntegrationTest` — burst test
- `GlobalExceptionHandlerTest` — 6 exception types
- `ProjectControllerDtoTest` — DTO shape assertion
- `SubscriptionControllerDtoTest` — no stripeCustomerId
- `RefreshTokenFamilyRevocationTest` — SC-P1-7
- `RefreshTokenConcurrencyTest` — SC-P1-8
- `ApplicationStartupTest` — SC-P1-6

Expected: `BUILD SUCCESS` with zero failures.

---

## 9. Verify Startup Failures Fast

### Blank JWT_SECRET
```bash
JWT_SECRET="" mvn spring-boot:run -Dspring-boot.run.profiles=local -pl backend 2>&1 | grep "IllegalArgument"
# Must print: "JWT_SECRET must be at least 32 characters"
```

### Blank GITHUB_WEBHOOK_SECRET
```bash
JWT_SECRET="a-valid-32-char-secret-replace-me" \
GITHUB_WEBHOOK_SECRET="" \
mvn spring-boot:run -Dspring-boot.run.profiles=local -pl backend 2>&1 | grep "IllegalState"
# Must print: "GITHUB_WEBHOOK_SECRET must not be blank"
```

---

## Troubleshooting

| Symptom | Likely Cause | Fix |
|---|---|---|
| `IllegalArgumentException: JWT_SECRET must be...` at startup | `JWT_SECRET` env var too short or missing | Set a 32+ char value in `.env.local` |
| `IllegalStateException: GITHUB_WEBHOOK_SECRET...` at startup | `GITHUB_WEBHOOK_SECRET` blank | Set any non-blank value in `.env.local` |
| Flyway `V6` fails with FK violation | `user_subscriptions` / `projects` rows with non-existent `user_id` | Truncate those tables in dev DB before running V6 |
| `401` on `/actuator/health` | CORS or security misconfiguration | Check `WebSecurityConfig` — `/actuator/health` must be in `permitAll()` list |
| Refresh cookie not sent on `/api/auth/refresh` | Browser SameSite policy or wrong path | Ensure frontend origin matches `FRONTEND_ORIGIN` and cookie path is `/api/auth/refresh` |
