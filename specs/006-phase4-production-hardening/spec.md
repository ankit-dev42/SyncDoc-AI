# Feature Specification: Phase 4 — Production Hardening

**Feature Branch**: `006-phase4-production-hardening`
**Created**: 2026-05-07
**Status**: Draft
**Priority**: P2 — Final gate before production release
**Prerequisite**: Phase 3 (`005-phase3-frontend-devops`) merged to `develop`

## User Scenarios & Testing

### User Story 1 — Operational Health Visibility (Priority: P1)

As an operator, I want the health endpoint to show real component status — database and Redis — so I can detect degradation immediately without accessing internal dashboards.

**Why this priority**: If the system goes down and health checks return misleading data, on-call engineers cannot triage quickly. This is a production safety requirement.

**Independent Test**: Can be fully tested by calling `GET /actuator/health` with and without an ADMIN token, and by simulating a DB outage to confirm the status reflects DOWN.

**Acceptance Scenarios**:

1. **Given** the system is running with DB and Redis healthy, **When** `GET /actuator/health` is called without a token, **Then** response is `{ "status": "UP" }` (summary only, no component detail)
2. **Given** the system is running, **When** `GET /actuator/health` is called with a valid ADMIN token, **Then** response includes `{ "status": "UP", "components": { "db": { "status": "UP" }, "redis": { "status": "UP" }, "app": { "status": "UP" } } }`
3. **Given** the database is unreachable, **When** `GET /actuator/health` is called, **Then** response is `{ "status": "DOWN" }`
4. **Given** no authentication, **When** `GET /actuator/env` is called, **Then** response is 401 Unauthorized
5. **Given** no authentication, **When** `GET /actuator/metrics` is called, **Then** response is 401 Unauthorized
6. **Given** a valid ADMIN token, **When** `GET /actuator/metrics/subscription.check.total` is called, **Then** a counter value is returned

---

### User Story 2 — ENTERPRISE Tier Behavior (Priority: P2)

As an ENTERPRISE subscriber, I want every sync authorization check to use fresh subscription data — never from a stale cache — and I should never be blocked by a repo count limit.

**Why this priority**: ENTERPRISE customers pay a premium for guaranteed real-time access. Stale cache causing denied syncs would be a contractual and reputational issue.

**Independent Test**: Can be fully tested by calling the can-sync endpoint as an ENTERPRISE user and verifying the response is ALLOW without any count check being evaluated, and confirming a Stripe API call was made (not a Redis hit).

**Acceptance Scenarios**:

1. **Given** an authenticated ENTERPRISE user, **When** the sync authorization check is evaluated, **Then** the result is ALLOW regardless of how many repos the user has
2. **Given** an ENTERPRISE user, **When** the sync authorization check runs, **Then** `forceRefreshFromStripe()` is called — Redis cache is bypassed entirely
3. **Given** an authenticated PRO user, **When** the sync authorization check runs, **Then** existing PRO count-based logic is unchanged (no regression)
4. **Given** an authenticated FREE user, **When** the sync authorization check runs, **Then** existing FREE limit logic is unchanged (no regression)

---

### User Story 3 — Structured Audit Logging (Priority: P2)

As a security operations engineer, I want all audit events to emit structured JSON so I can ingest them into a SIEM tool (Splunk, Datadog) and correlate events by user, IP, and trace ID.

**Why this priority**: Audit logging is a compliance requirement for any production SaaS. Without structured output, SIEM ingestion is impossible.

**Independent Test**: Can be fully tested by triggering each event type (login, access denied, billing webhook, AI extraction) and verifying the captured log output contains all mandatory envelope fields and no sensitive data.

**Acceptance Scenarios**:

1. **Given** a successful login, **When** an `AUTH_LOGIN` event is emitted, **Then** the log entry contains `event`, `userId`, `ip`, `timestamp`, `traceId` — no password or token present
2. **Given** an access denial, **When** an `ACCESS_DENIED` event is emitted, **Then** the log entry contains `event`, `userId`, `path`, `timestamp` — no authorization token logged
3. **Given** a Stripe webhook received, **When** `BILLING_WEBHOOK_RECEIVED` is emitted, **Then** the log entry contains `stripeEventId` and `type` in addition to the envelope fields
4. **Given** AI extraction completes, **When** `AI_EXTRACTION_COMPLETED` is emitted, **Then** the log entry contains `docId`, `qualityScore`, `durationMs`
5. **Given** a non-local Spring profile is active, **When** any audit event is emitted, **Then** the log output is formatted as JSON
6. **Given** the local Spring profile is active, **When** any audit event is emitted, **Then** the log output is human-readable plain text

---

### User Story 4 — Real Database Integration Tests (Priority: P3)

As QA, I want integration tests that run against a real PostgreSQL container so schema bugs and query edge cases that mocks cannot catch are detected before production.

**Why this priority**: Schema errors found in production are expensive. Testcontainers provides high confidence with lower risk than purely mocked tests.

**Independent Test**: Can be fully tested by running `mvn verify` and confirming the `*IT.java` test classes complete against a real pg:16 container, including verifying all Flyway migrations apply cleanly.

**Acceptance Scenarios**:

1. **Given** a fresh PostgreSQL 16 container, **When** all Flyway migrations V1–V12 are applied, **Then** the schema applies without errors
2. **Given** a project with `deleted_at` set, **When** `ProjectRepository.findAll()` is called, **Then** the soft-deleted project is absent from the results
3. **Given** a `WebhookEvent` with `payload_hash = X` already persisted, **When** a second insert with the same `payload_hash` is attempted, **Then** `DataIntegrityViolationException` is thrown
4. **Given** Testcontainers IT tests, **When** `mvn test` is run (Surefire), **Then** Testcontainers tests do NOT run (fast unit loop is unaffected)
5. **Given** Testcontainers IT tests, **When** `mvn verify` is run (Failsafe), **Then** all `*IT.java` tests execute against the shared pg:16 container

---

### User Story 5 — Constitution Compliance Sign-Off (Priority: P3)

As an engineering lead, I want a formal compliance document committed to the repo showing all 10 Inviolable Rules are PASS, so I can approve the production release PR with confidence.

**Why this priority**: This is the final gate. Without a verified compliance document, the team has no audit trail proving each rule was checked.

**Independent Test**: Can be fully tested by reading `docs/CONSTITUTION_COMPLIANCE.md` and confirming all 10 rows show PASS with traceable evidence.

**Acceptance Scenarios**:

1. **Given** Phase 4 implementation complete, **When** `docs/CONSTITUTION_COMPLIANCE.md` is read, **Then** all 10 rules (01–10) show `PASS` status with an evidence reference (file path or test name)
2. **Given** a PR to merge `006-phase4-production-hardening` into `main`, **When** the engineering lead reviews, **Then** the PR requires an approval (Rule 05 branch protection) before merge is possible
3. **Given** the compliance document, **When** Rule 01 is reviewed, **Then** evidence shows zero hardcoded secrets in the git history

---

### Edge Cases

- What happens when Redis is unavailable during an ENTERPRISE subscription check? (The `forceRefreshFromStripe()` call goes directly to Stripe — no cache fallback needed)
- What happens when a Testcontainers container fails to start in CI? (Failsafe marks the test run as failed; the CI pipeline blocks the PR)
- What happens if an audit event handler throws before writing to MDC? (Exception is caught and logged at ERROR level; the original request is not failed by audit log errors)
- What if the ADMIN role is not assigned to any user? (`/actuator/env` returns 403 Forbidden for authenticated non-ADMIN users, 401 for unauthenticated users)

## Requirements

### Functional Requirements

- **FR-001**: System MUST expose `/actuator/health` and `/actuator/info` without authentication
- **FR-002**: System MUST require `ADMIN` role for `/actuator/env`, `/actuator/metrics`, `/actuator/loggers`, and all other Actuator endpoints not in the public list
- **FR-003**: `CollaborationHealthIndicator` MUST implement `HealthIndicator` and return UP status with metadata
- **FR-004**: System MUST expose a Redis health contributor so `/actuator/health` reports Redis component status
- **FR-005**: `SubscriptionService` MUST record a counter and timer via `MeterRegistry` on each subscription check
- **FR-006**: `SyncAuthorizationService.authorize()` MUST return ALLOW for ENTERPRISE tier unconditionally, with no repo count evaluated
- **FR-007**: Every ENTERPRISE authorization check MUST call `SubscriptionService.forceRefreshFromStripe(userId)` to bypass the Redis cache
- **FR-008**: ENTERPRISE behavior MUST be additive — PRO and FREE paths must remain unmodified
- **FR-009**: All audit log entries MUST include the envelope fields: `event`, `userId`, `ip`, `timestamp`, `traceId`
- **FR-010**: Audit log entries MUST NEVER contain: `accessToken`, `password`, raw Stripe secrets, or raw email addresses
- **FR-011**: System MUST emit structured JSON audit logs in all non-local Spring profiles
- **FR-012**: System MUST emit 11 defined audit event types: `AUTH_LOGIN`, `AUTH_LOGOUT`, `AUTH_REFRESH_FAILED`, `ACCESS_DENIED`, `BILLING_WEBHOOK_RECEIVED`, `BILLING_SUBSCRIPTION_CHANGED`, `AI_EXTRACTION_COMPLETED`, `AI_EXTRACTION_FAILED`, `WEBHOOK_ACCEPTED`, `WEBHOOK_REJECTED`, `WEBHOOK_DUPLICATE`
- **FR-013**: Testcontainers integration tests (`*IT.java`) MUST run only during `mvn verify` (Failsafe), not during `mvn test` (Surefire)
- **FR-014**: All `*IT.java` tests MUST share a single static PostgreSQL 16 container per class via `AbstractIntegrationTest`
- **FR-015**: `docs/CONSTITUTION_COMPLIANCE.md` MUST contain one row per rule (01–10) with columns: Rule, Description, Status, Evidence

### Key Entities

- **AuditEvent**: Represents a structured log record — fields: `event` (event type enum), `userId` (UUID, never PII), `ip` (string), `timestamp` (ISO-8601), `traceId` (from MDC), plus event-specific extensions
- **AbstractIntegrationTest**: Base class for all Testcontainers tests — holds the shared static `PostgreSQLContainer`, provides `@DynamicPropertySource` for datasource injection, applies `@Testcontainers` annotation

## Success Criteria

### Measurable Outcomes

- **SC-P4-1**: `GET /actuator/health` returns a body containing `db` and `redis` components when called with an ADMIN token
- **SC-P4-2**: `GET /actuator/env` called without an ADMIN token returns HTTP 401 in under 100ms
- **SC-P4-3**: ENTERPRISE `SyncAuthorizationService.authorize()` returns ALLOW in 100% of test cases regardless of repo count value
- **SC-P4-4**: All 10 audit event types produce a log entry with `event`, `userId`, `ip`, `timestamp`, `traceId` fields present — verified by log capture in test
- **SC-P4-5**: All three `*IT.java` Testcontainers tests pass in CI against `pg:16` with zero test failures
- **SC-P4-6**: `docs/CONSTITUTION_COMPLIANCE.md` is committed with all 10 rules showing `PASS` status before the PR merges to `main`

## Constitution Alignment

### Code Quality (Principle I)
- [x] All new classes follow existing package structure under `com.syncdoc.collaboration`
- [x] No single method exceeds 40 lines; `SyncAuthorizationService.authorize()` ENTERPRISE case is a single conditional branch
- [x] `AuditLogger` event methods are self-documenting; each method name matches its audit event type

### Testing Standards (Principle II)
- [x] Test cases written before implementation (`*IT.java` classes are test-first)
- [x] Integration test coverage target: 100% of new `*IT.java` classes
- [x] Testcontainers integration tests planned for: PostgreSQL schema and query layer
- [x] TDD approach: migration test fails on fresh container → migrations applied → test passes

### User Experience Consistency (Principle III)
- [x] No frontend changes in this phase — N/A
- [x] Actuator error responses follow Spring Boot default format (no custom override needed)

### Performance Requirements (Principle IV)
- [x] Actuator health endpoint target: ≤200ms p95 (cached by Spring Boot's health endpoint)
- [x] ENTERPRISE `forceRefreshFromStripe()` latency is bounded by Stripe API SLA (~500ms) — acceptable for enterprise tier
- [x] Testcontainers container startup: shared static container keeps per-test overhead near zero

## Assumptions

- Phase 3 is fully merged and CI is active before Phase 4 begins; the JaCoCo and Vitest coverage gates are already enforced
- `SubscriptionService.forceRefreshFromStripe(userId)` does not yet exist; it will be created in Phase 4 (T315). The Phase 2 (T128) assumption was incorrect — the method is absent from the current codebase.
- `logstash-logback-encoder` is compatible with the Spring Boot 3.2.x logback version already on the classpath
- The ADMIN role is provisioned via environment configuration or a data migration; Phase 4 does not introduce a new role management UI
- `docs/CONSTITUTION_COMPLIANCE.md` is a one-time creation for this phase; subsequent phases append to it rather than replacing it
- PostgreSQL 16 is used both in CI (Testcontainers) and production; the shared container image matches production
