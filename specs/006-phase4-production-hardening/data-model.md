# Data Model: Phase 4 — Production Hardening

**Generated**: 2026-05-07  
**Branch**: `006-phase4-production-hardening`

---

## New Classes (No New DB Tables)

Phase 4 introduces no new database tables. All changes are additive to existing classes or introduce new Java classes only.

---

## Modified Entity: `AuditLogger`

**Package**: `com.syncdoc.collaboration.observability`  
**Type**: Spring `@Component`  
**Current State**: Emits plain-text SLF4J log lines. `Action` enum has collaboration-focused events (`MESSAGE_SENT`, `WORKSPACE_ACCESSED`, etc.).  
**Phase 4 Changes**: Add 11 Phase 4 audit event types to `Action` enum; switch to MDC-based structured log emission compatible with `logstash-logback-encoder`.

**Updated `Action` Enum** (additions only — existing values retained for backwards compatibility):
```
AUTH_LOGIN
AUTH_LOGOUT
AUTH_REFRESH_FAILED
ACCESS_DENIED              ← already exists, re-used
BILLING_WEBHOOK_RECEIVED
BILLING_SUBSCRIPTION_CHANGED
AI_EXTRACTION_COMPLETED
AI_EXTRACTION_FAILED
WEBHOOK_ACCEPTED
WEBHOOK_REJECTED
WEBHOOK_DUPLICATE
```

**Structured Audit Event Envelope** (MDC keys set before each `logger.info()` call):

| MDC Key     | Type    | Required | Description                         | Never Contains      |
|-------------|---------|----------|-------------------------------------|---------------------|
| `event`     | String  | Yes      | Event type (enum name)              |                     |
| `userId`    | String  | Yes      | UUID of acting user; `"-"` if anon  | email, display name |
| `ip`        | String  | Yes      | Source IP address                   |                     |
| `timestamp` | String  | Yes      | ISO-8601 UTC instant                |                     |
| `traceId`   | String  | Yes      | From `MDC.get("traceId")` or `"-"`  |                     |

**Event-Specific Extension Fields** (set in addition to envelope):

| Event                         | Additional MDC Keys                       |
|-------------------------------|-------------------------------------------|
| `BILLING_WEBHOOK_RECEIVED`    | `stripeEventId`, `type`                   |
| `BILLING_SUBSCRIPTION_CHANGED`| `stripeCustomerId` (masked), `newTier`    |
| `AI_EXTRACTION_COMPLETED`     | `docId`, `qualityScore`, `durationMs`     |
| `AI_EXTRACTION_FAILED`        | `docId`, `reason`                         |
| `WEBHOOK_ACCEPTED`            | `payloadHash`, `eventType`                |
| `WEBHOOK_REJECTED`            | `payloadHash`, `reason`                   |
| `WEBHOOK_DUPLICATE`           | `payloadHash`                             |
| `AUTH_REFRESH_FAILED`         | `reason`                                  |

**Validation Rules**:
- `userId` MUST NOT contain `@` (no email addresses logged)
- `detail` fields must be sanitized: newlines and control characters stripped (existing `sanitize()` method retained)
- No key named `accessToken`, `password`, `secret`, or `rawToken` may ever be added to MDC in `AuditLogger`

---

## New Class: `AbstractIntegrationTest`

**Package**: `com.syncdoc.collaboration` (test source tree)  
**Type**: Abstract JUnit 5 base class (no Spring annotation — concrete subclasses add `@SpringBootTest`)  
**Purpose**: Provides a shared, static PostgreSQL 16 Testcontainers instance for all `*IT.java` integration tests.

**Fields**:

| Field     | Type                        | Static | Description                                    |
|-----------|-----------------------------|--------|------------------------------------------------|
| `POSTGRES`| `PostgreSQLContainer<?>` | Yes    | Shared container, started once per test run    |

**Methods**:

| Method                                             | Description                                          |
|----------------------------------------------------|------------------------------------------------------|
| `overrideDataSourceProps(DynamicPropertyRegistry)` | `@DynamicPropertySource` — wires datasource to TC    |

**State Transitions**: Container starts on first test class load; reused by all subclasses; stopped by JVM shutdown hook. No per-test container restarts.

**Assumptions**:
- Flyway runs all V1–V12 migrations automatically on each test application context start (via `spring.flyway.enabled=true` in test application.yml)
- Test data is isolated per test via `@Transactional` + rollback, or via explicit cleanup in `@AfterEach`

---

## Modified Class: `SubscriptionService`

**Package**: `com.syncdoc.collaboration.subscription.service`  
**Current State**: Has partial ENTERPRISE awareness (skips Redis cache for ENTERPRISE tier in `getSubscription()`) but no `forceRefreshFromStripe()` method.

**New Method**:

| Method                                | Returns             | Description                                                     |
|---------------------------------------|---------------------|-----------------------------------------------------------------|
| `forceRefreshFromStripe(String userId)` | `UserSubscription` | Deletes Redis cache key, calls `stripeClient.fetchSubscription`, persists, returns fresh subscription |

---

## Modified Class: `SyncAuthorizationService`

**Package**: `com.syncdoc.collaboration.subscription.service`  
**Current State**: Has `evaluate(String userId, int currentSyncCount)` — no ENTERPRISE case.

**New Method**:

| Method                                          | Returns               | Description                                                    |
|-------------------------------------------------|-----------------------|----------------------------------------------------------------|
| `authorize(String userId, int currentSyncCount)` | `AuthorizationResult` | ENTERPRISE → force-refresh + ALLOW; delegates to `evaluate()` for other tiers |

**`AuthorizationResult` enum** (inner record or standalone):
```
ALLOW   — sync is permitted
DENY    — sync is blocked (reason + recommended action provided)
```

**State Transitions**:
```
ENTERPRISE tier → forceRefreshFromStripe() called → ALLOW (always)
PRO tier        → evaluate() → ALLOW if subscription active
FREE tier       → evaluate() → DENY if currentSyncCount >= 1
```

---

## Modified Class: `CollaborationHealthIndicator`

**Package**: `com.syncdoc.collaboration.observability`  
**Current State**: `@RestController` at `/api/v1/health`. Directly returns `ResponseEntity`.  
**Phase 4 Change**: Add `implements HealthIndicator` so Actuator `/actuator/health` picks up this component automatically. The `@RestController` path is retained as a backwards-compat endpoint.

**`health()` method contract**:
- Pings Redis via `RedisTemplate`
- Returns `Health.up().withDetail("redis", "OK").withDetail("db", "OK")` on success
- Returns `Health.down().withException(ex)` on failure

---

## Infrastructure: `logback-spring.xml`

**Location**: `backend/src/main/resources/logback-spring.xml`  
**Purpose**: Profile-switched structured logging

| Spring Profile     | Appender           | Format                      |
|--------------------|--------------------|-----------------------------|
| `local` (default)  | Console (pattern)  | Human-readable colored text |
| `!local` (all else)| Console (JSON)     | `LogstashEncoder` JSON      |

The `AUDIT` logger uses `additivity="false"` to prevent duplicate entries.

---

## No Schema Changes

Phase 4 introduces no Flyway migrations. All V1–V12 migrations are already applied.
