# Quickstart: Phase 4 — Production Hardening

**Generated**: 2026-05-07  
**Branch**: `006-phase4-production-hardening`

---

## Prerequisites

- Phase 3 (`005-phase3-frontend-devops`) merged into `develop`
- Docker Desktop running (for Testcontainers in CI and local `mvn verify`)
- Java 17, Maven 3.9.x

---

## Run Unit Tests Only (fast loop)

```bash
cd backend
mvn test
```

This runs all `*Test.java` classes via Surefire. Testcontainers `*IT.java` tests are **excluded**.

---

## Run Integration Tests (Testcontainers)

```bash
cd backend
mvn verify
```

This runs:
1. All `*Test.java` via Surefire
2. All `*IT.java` via Failsafe (real PostgreSQL 16 container via Testcontainers)

Docker must be running. First run will pull `postgres:16` image (~150 MB).

---

## Verify Actuator Endpoints

Start the application:
```bash
docker-compose up -d postgres redis
cd backend
mvn spring-boot:run
```

### Health (public)
```bash
curl http://localhost:8080/actuator/health
# → { "status": "UP" }
```

### Health with ADMIN token (shows component detail)
```bash
curl -H "Authorization: Bearer <ADMIN_JWT>" http://localhost:8080/actuator/health
# → { "status": "UP", "components": { "collaboration": {...}, "db": {...}, "redis": {...} } }
```

### Env without ADMIN (must return 401)
```bash
curl http://localhost:8080/actuator/env
# → 401 Unauthorized
```

### Metrics (ADMIN required)
```bash
curl -H "Authorization: Bearer <ADMIN_JWT>" \
  "http://localhost:8080/actuator/metrics/subscription.check.total"
```

---

## Verify Structured Audit Logs

Run with a non-local profile to activate JSON output:

```bash
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run 2>&1 | grep '"event"' | head -5
```

Expected sample line (formatted for readability):
```json
{
  "@timestamp": "2026-05-07T12:00:00.000Z",
  "level": "INFO",
  "logger_name": "AUDIT",
  "event": "AUTH_LOGIN",
  "userId": "550e8400-...",
  "ip": "127.0.0.1",
  "traceId": "abc123"
}
```

---

## Verify ENTERPRISE Tier Authorization

```bash
# Create an ENTERPRISE user and obtain their JWT (see scripts/create-master-player.sh)
curl -H "Authorization: Bearer <ENTERPRISE_JWT>" \
  http://localhost:8080/api/v1/subscriptions/authorize?currentSyncCount=9999
# → { "authorized": true, "reason": "ENTERPRISE tier" }
```

---

## Create CONSTITUTION_COMPLIANCE.md

After all Phase 4 tasks are complete and all tests pass:

```bash
# Rule 01: verify no hardcoded secrets
git grep -E "(sk_live|sk_test|OPENAI_API_KEY\s*=|JWT_SECRET\s*=)" -- '*.java' '*.yml' '*.properties'
# Must return zero lines

# Rule 02: webhook signature verifiers present
grep -r "Stripe-Signature\|X-Hub-Signature" backend/src/main/java --include="*.java" -l

# Rule 07: @Async on dispatch and AI methods
grep -r "@Async" backend/src/main/java --include="*.java" -n

# Rule 09: no @RequestBody without @Valid
grep -r "@RequestBody" backend/src/main/java --include="*.java" -n | grep -v "@Valid"
# Must return zero lines
```

When all checks pass, create `docs/CONSTITUTION_COMPLIANCE.md` with evidence and raise the Phase 4 PR for engineering lead approval.
