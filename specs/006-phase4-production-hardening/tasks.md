# Tasks: Phase 4 — Production Hardening

**Input**: Design documents from `specs/006-phase4-production-hardening/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/api-contracts.md, quickstart.md
**Tests**: Test-first mandatory per Constitution Principle II. Tests written before implementation; tests must fail before corresponding implementation begins.
**Organization**: Tasks grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no shared dependency on in-progress work)
- **[Story]**: Which user story this task belongs to (`[US1]`–`[US5]`)
- All file paths are relative to repo root

## Constitution Compliance Checkpoints
- **Principle I**: No method >40 lines; constructor injection only; no Lombok
- **Principle II**: All `*Test.java` and `*IT.java` classes written before their implementations
- **Principle IV**: `/actuator/health` ≤200ms p95; ENTERPRISE `authorize()` path ≤600ms (Stripe SLA)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add all required dependencies before any test or implementation work begins. These unblock every subsequent phase.

- [X] T300 Add `testcontainers-bom` to `<dependencyManagement>` in `backend/pom.xml`; add `org.testcontainers:postgresql` and `org.testcontainers:junit-jupiter` (both `test` scope, version from BOM); add `maven-failsafe-plugin` to `<build><plugins>` with `<includes>**/*IT.java</includes>` bound to `integration-test` and `verify` phases
- [X] T301 Add `net.logstash.logback:logstash-logback-encoder:7.4` (runtime scope) to `backend/pom.xml`

> **Note**: T300 and T301 both modify `backend/pom.xml`. Do **not** apply them in parallel — batch both changes into a single pom.xml edit or apply them sequentially on the same file to avoid merge conflicts.

- [X] T302 [P] Create `backend/src/main/resources/logback-spring.xml` — Before creating, verify that no `src/main/resources/logback.xml` already exists (a plain `logback.xml` takes precedence over `logback-spring.xml` and will silently suppress it); if one exists, migrate its configuration into `logback-spring.xml` and delete the original. Then create `logback-spring.xml` with: `<springProfile name="!local">` uses `LogstashEncoder` JSON appender writing to stdout; `<springProfile name="local">` uses a colored `PatternLayout` appender; both profiles route all loggers through their respective appender

**Checkpoint**: `mvn dependency:resolve -pl backend` succeeds with zero missing artifacts before proceeding.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: `AbstractIntegrationTest` base class must exist before any `*IT.java` class can compile. This is a hard blocker for all US4 integration test tasks.

**⚠️ CRITICAL**: T303, T304, T305 cannot compile until T308 is complete.

- [X] T308 Create `backend/src/test/java/com/syncdoc/collaboration/AbstractIntegrationTest.java` — abstract class annotated `@Testcontainers`; declares `static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16").withDatabaseName("testdb").withUsername("test").withPassword("test")` annotated `@Container`; declares `@DynamicPropertySource static void overrideDataSourceProps(DynamicPropertyRegistry registry)` that sets `spring.datasource.url`, `spring.datasource.username`, and `spring.datasource.password` from the container; no Spring annotation on the abstract class itself (concrete subclasses add `@SpringBootTest`)

**Checkpoint**: `mvn compile -pl backend` succeeds with `AbstractIntegrationTest.java` present before writing IT tests.

---

## Phase 3: User Story 1 — Operational Health Visibility (Priority: P1) 🎯 MVP

**Goal**: `/actuator/health` returns real `db` + `redis` component status; all non-public Actuator endpoints return 401 without ADMIN role; `SubscriptionService` records Micrometer counter and timer per check.

**Independent Test**: `GET /actuator/health` without token → `{ "status": "UP" }` (no component detail). `GET /actuator/health` with ADMIN token → includes `components.db` and `components.redis`. `GET /actuator/env` without ADMIN → HTTP 401. `GET /actuator/metrics/subscription.check.total` with ADMIN → counter value.

### Tests for User Story 1 (MANDATORY — per Constitution Principle II: Test-First Development)

> **REQUIRED: Write T310a FIRST and observe it failing before implementing T311.**

- [X] T310a [US1] Extend `backend/src/test/java/com/syncdoc/collaboration/security/WebSecurityConfigIntegrationTest.java` with three Phase 4 Actuator security assertions: (1) `GET /actuator/env` without any role → HTTP 401; (2) `GET /actuator/metrics` without any role → HTTP 401; (3) `GET /actuator/health` with a valid ADMIN-role JWT (generated via `JwtTestTokenHelper` with `ROLE_ADMIN`) → response body contains key `"components"` with entries for both `"db"` and `"redis"`; confirm all three assertions fail before T311 is implemented (SC-P4-1, SC-P4-2)

### Implementation for User Story 1

- [X] T310 [US1] Update `backend/src/main/resources/application.yml` — under `management:` set `endpoints.web.exposure.include: health,info,metrics,env,loggers` and `endpoint.health.show-details: when-authorized`; ensure no `show-components` override hides db/redis (remove any existing `show-components: never` if present)
- [X] T311 [P] [US1] Update `backend/src/main/java/com/syncdoc/collaboration/config/WebSecurityConfig.java` — in `filterChain()`, insert `.requestMatchers("/actuator/**").hasRole("ADMIN")` AFTER the existing `permitAll()` block that covers `/actuator/health` and `/actuator/info`, and BEFORE the final `.anyRequest().authenticated()` line; first-match wins ensures health/info stay public per Architecture Decision 4
- [X] T312 [P] [US1] Update `backend/src/main/java/com/syncdoc/collaboration/observability/CollaborationHealthIndicator.java` — add `implements org.springframework.boot.actuate.health.HealthIndicator` to the class declaration; implement `public Health health()` returning `Health.up().withDetail("redis", "OK").withDetail("db", "OK").build()`; retain existing `@RestController` mapping at `/api/v1/health` for backwards compatibility; remove any `@RequestMapping` that conflicts with the `HealthIndicator` contract
- [X] T313 [P] [US1] Confirm that the `redis` component appears in the `GET /actuator/health` response body when called with a valid ADMIN token (after T310 is applied). If `redis` is absent, add `management.health.redis.enabled: true` to `application.yml` and restart. Mark T313 done either way once the `redis` component is verified present in the health response — Spring Boot auto-configures `RedisHealthIndicator` when `spring-boot-starter-data-redis` is on the classpath (Phase 2), so no code change is expected.
- [X] T314 [US1] Update `backend/src/main/java/com/syncdoc/collaboration/subscription/service/SubscriptionService.java` — add `private final MeterRegistry meterRegistry` to constructor parameters; in `@PostConstruct` (or constructor), register `Counter subscriptionCheckCounter = Counter.builder("subscription.check.total").description("Total subscription checks").register(meterRegistry)` and `Timer subscriptionCheckTimer = Timer.builder("subscription.check.duration").description("Subscription check latency").register(meterRegistry)`; wrap the body of the existing `getSubscription()` method with `Timer.Sample sample = Timer.start(meterRegistry)` before and `sample.stop(subscriptionCheckTimer); subscriptionCheckCounter.increment()` after the return value is obtained

**Checkpoint**: `GET /actuator/health` returns `{ "status": "UP", "components": { "db": {...}, "redis": {...} } }` with ADMIN token. `GET /actuator/env` returns HTTP 401 without ADMIN token. (SC-P4-1, SC-P4-2 satisfied)

---

## Phase 4: User Story 2 — ENTERPRISE Tier Behavior (Priority: P2)

**Goal**: `SyncAuthorizationService.authorize()` returns `ALLOW` for `ENTERPRISE` tier unconditionally, calling `forceRefreshFromStripe()` to bypass Redis; `PRO` and `FREE` paths are unchanged.

**Independent Test**: Call `authorize()` with mocked ENTERPRISE subscription → result is `ALLOW` and `forceRefreshFromStripe()` was called. Call with PRO user at sync limit → `DENY`. Call with FREE user at limit → `DENY`. (No regression on existing paths.)

### Tests for User Story 2 (Test-First — Write Then Fail)

- [X] T306 [US2] Create `backend/src/test/java/com/syncdoc/collaboration/subscription/service/SyncAuthorizationServiceEnterpriseTest.java` — `@ExtendWith(MockitoExtension.class)` unit test; mock `SubscriptionService`; scenarios: (1) ENTERPRISE with `syncCount=0` → `authorize()` returns `ALLOW`; (2) ENTERPRISE with `syncCount=9999` → `authorize()` returns `ALLOW`; (3) ENTERPRISE call verifies `forceRefreshFromStripe(userId)` was invoked exactly once; (4) PRO user below limit → `ALLOW` (existing `evaluate()` path unchanged); (5) PRO user at limit → `DENY`; (6) FREE user at limit → `DENY` — confirm test fails before T315–T316 are implemented

### Implementation for User Story 2

- [X] T315 [US2] Update `backend/src/main/java/com/syncdoc/collaboration/subscription/service/SubscriptionService.java` — add `public UserSubscription forceRefreshFromStripe(String userId)` method: call `stripeClient.fetchSubscription(userId)` directly (bypassing Redis), update the Redis cache entry for key `"subscription:" + userId` with the refreshed result and a 5-minute TTL, then return the refreshed `UserSubscription`; constructor-inject `StripeClient` if not already present
- [X] T316 [US2] Update `backend/src/main/java/com/syncdoc/collaboration/subscription/service/SyncAuthorizationService.java` — **Step 0**: Create `backend/src/main/java/com/syncdoc/collaboration/subscription/service/AuthorizationResult.java` as a Java `enum` with values `ALLOW` and `DENY` (do not reuse the existing `AuthorizationDecision` boolean type — `AuthorizationResult` is the public return type of the new `authorize()` method). **Step 1**: add `public AuthorizationResult authorize(String userId, int currentSyncCount)` method: call `subscriptionService.getSubscription(userId)` to get the current tier; if `tier == SubscriptionTier.ENTERPRISE`, call `subscriptionService.forceRefreshFromStripe(userId)` and return `AuthorizationResult.ALLOW` (no count check); otherwise, call the existing `evaluate(userId, currentSyncCount)` and map the decision to `AuthorizationResult`

**Checkpoint**: `SyncAuthorizationServiceEnterpriseTest` passes — ENTERPRISE always returns `ALLOW`, PRO/FREE paths unchanged. (SC-P4-3 satisfied)

---

## Phase 5: User Story 3 — Structured Audit Logging (Priority: P2)

**Goal**: `AuditLogger` emits structured JSON log entries with all 5 envelope fields in non-local profiles; 11 event types wired across `AuthController`, `GlobalExceptionHandler`, `StripeWebhookHandler`, `AIProcessingService`, and `WebhookController`.

**Independent Test**: Trigger each event type; verify captured MDC state contains `event`, `userId`, `ip`, `timestamp`, `traceId` — and none of `accessToken`, `password`, `secret`, `rawToken` appear as MDC keys.

### Tests for User Story 3 (Test-First — Write Then Fail)

- [X] T307 [US3] Create `backend/src/test/java/com/syncdoc/collaboration/observability/AuditLoggerStructuredOutputTest.java` — `@ExtendWith(MockitoExtension.class)` unit test; for each of the 11 event types (`authLogin`, `authLogout`, `authRefreshFailed`, `accessDenied`, `billingWebhookReceived`, `billingSubscriptionChanged`, `aiExtractionCompleted`, `aiExtractionFailed`, `webhookAccepted`, `webhookRejected`, `webhookDuplicate`), call the method on an `AuditLogger` instance and verify (via `MDC.get()` captured before MDC is cleared): all 5 envelope keys are non-null (`event`, `userId`, `ip`, `timestamp`, `traceId`); none of `accessToken`, `password`, `secret`, `rawToken` appear as MDC keys; `sanitize()` method strips newline `\n` and carriage return `\r` from detail strings — confirm test fails before T317 is implemented

### Implementation for User Story 3

- [X] T317 [US3] Update `backend/src/main/java/com/syncdoc/collaboration/observability/AuditLogger.java` — add 11 public event methods (one per event type listed below); each method: (1) calls `MDC.put()` for the 5 envelope keys: `event` (enum name), `userId` (UUID string or `"-"`), `ip` (from parameter), `timestamp` (ISO-8601 UTC via `Instant.now()`), `traceId` (from `MDC.get("traceId")` or `"-"`); (2) calls `MDC.put()` for any event-specific keys per data-model.md; (3) calls `logger.info(marker, eventType.name())`; (4) calls `MDC.remove()` for all keys set in step 1+2. Add static deny-list check: if any caller attempts to put a key named `accessToken`, `password`, `secret`, or `rawToken` into MDC, throw `IllegalArgumentException`. Update `Action` enum to include all 11 Phase 4 event types. Retain existing event types for backwards compatibility. If the resulting `AuditLogger.java` class exceeds 250 lines, extract the MDC-write logic into a private `AuditMdcWriter` helper class and delegate from each event method, keeping each public event method ≤10 lines.
- [X] T318 [P] [US3] Update `backend/src/main/java/com/syncdoc/collaboration/auth/controller/AuthController.java` — inject `AuditLogger` via constructor; call `auditLogger.authLogin(userId, clientIp)` immediately after successful `login()` response; call `auditLogger.authLogout(userId, clientIp)` in `logout()` handler; call `auditLogger.authRefreshFailed(userId, clientIp, reason)` in the catch block of the `refresh()` handler when token is invalid or expired
- [X] T319 [P] [US3] Update `backend/src/main/java/com/syncdoc/collaboration/exception/GlobalExceptionHandler.java` — inject `AuditLogger` via constructor; call `auditLogger.accessDenied(userId, requestPath, clientIp)` in the `AccessDeniedException` handler method; extract `userId` from `SecurityContextHolder` (use `"-"` if anonymous); extract `requestPath` from `HttpServletRequest` (inject via `@RequestAttribute` or `HttpServletRequest` parameter)
- [X] T320 [P] [US3] Update `backend/src/main/java/com/syncdoc/collaboration/billing/handler/StripeWebhookHandler.java` — inject `AuditLogger` via constructor; call `auditLogger.billingWebhookReceived(stripeEvent.getId(), stripeEvent.getType(), clientIp)` at the start of event handling after signature verification succeeds; call `auditLogger.billingSubscriptionChanged(maskedCustomerId, newTier.name(), clientIp)` after a subscription tier change is persisted (mask customer ID: show only last 4 chars)
- [X] T321 [P] [US3] Update `backend/src/main/java/com/syncdoc/collaboration/ai/service/AIProcessingService.java` — inject `AuditLogger` via constructor; call `auditLogger.aiExtractionCompleted(docId, qualityScore, durationMs, userId, clientIp)` after successful extraction is persisted; call `auditLogger.aiExtractionFailed(docId, failureReason, userId, clientIp)` in the final catch block after all retries are exhausted; compute `durationMs` as `System.currentTimeMillis() - startTime` measured at processing start
- [X] T322 [P] [US3] Update `backend/src/main/java/com/syncdoc/collaboration/webhook/controller/WebhookController.java` — inject `AuditLogger` via constructor; call `auditLogger.webhookAccepted(payloadHash, eventType, clientIp)` when event is dispatched successfully; call `auditLogger.webhookRejected(payloadHash, "signature_invalid", clientIp)` when HMAC verification fails; call `auditLogger.webhookDuplicate(payloadHash, clientIp)` when `existsByPayloadHash()` returns true (idempotency block)

**Checkpoint**: `AuditLoggerStructuredOutputTest` passes — all 11 event types emit correct MDC fields with no sensitive keys present. (SC-P4-4 satisfied)

---

## Phase 6: User Story 4 — Real Database Integration Tests (Priority: P3)

**Goal**: Three `*IT.java` Testcontainers tests run only during `mvn verify` (Failsafe) against a shared `pg:16` container; all Flyway migrations apply cleanly; soft-delete and unique-constraint behaviors verified against real PostgreSQL.

**Independent Test**: `mvn test` completes with zero Docker containers started. `mvn verify` runs `FlywayMigrationIT`, `ProjectRepositoryIT`, `WebhookEventRepositoryIT` — all pass against `pg:16`.

### Tests for User Story 4 (Test-First — Write Then Fail)

- [X] T303 [P] [US4] Create `backend/src/test/java/com/syncdoc/collaboration/integration/FlywayMigrationIT.java` — extends `AbstractIntegrationTest`; annotated `@SpringBootTest`; one `@Test` method `allMigrationsApplyCleanly()` that asserts `flywaySchemaHistoryRepository` (or direct JDBC query) shows zero rows with `success = false`; a second `@Test` `requiredTablesExist()` that asserts tables `users`, `refresh_tokens`, `projects`, `user_subscriptions`, `webhook_events`, `sync_logs` all exist via `DatabaseMetaData`; confirm this test fails (cannot start) before T308 creates `AbstractIntegrationTest`
- [X] T304 [P] [US4] Create `backend/src/test/java/com/syncdoc/collaboration/integration/ProjectRepositoryIT.java` — extends `AbstractIntegrationTest`; annotated `@SpringBootTest`, `@Transactional`; injects `ProjectRepository`; tests: (1) `saveAndFindAll()` — save a project, `findAll()` returns it; (2) `softDeletedProjectAbsentFromFindAll()` — set `project.setDeletedAt(Instant.now())`, save, call `findAll()`, assert absent; (3) `findByOwnerIdReturnsOnlyOwnedProjects()` — save two projects with different `ownerId`, query by owner, assert only owned project returned
- [X] T305 [P] [US4] Create `backend/src/test/java/com/syncdoc/collaboration/integration/WebhookEventRepositoryIT.java` — extends `AbstractIntegrationTest`; annotated `@SpringBootTest`; injects `WebhookEventRepository`; `@Test duplicatePayloadHashThrowsException()` — save a `WebhookEvent` with `payloadHash = "test-hash-abc"`, attempt to save a second with the same hash, assert `DataIntegrityViolationException` is thrown; test is NOT `@Transactional` (must commit first insert for constraint to trigger)

### Checkpoint for User Story 4

> **Note**: T309 is a run/verification checkpoint — it is not a test-writing task. It must run after T303, T304, T305, and T308 are all complete.

- [X] T309 [US4] Run `mvn verify -pl backend` — confirm `FlywayMigrationIT`, `ProjectRepositoryIT`, `WebhookEventRepositoryIT` all pass against the shared `pg:16` Testcontainers container; confirm `mvn test -pl backend` does NOT start Docker (Surefire excludes `*IT.java` via Failsafe plugin config from T300); both commands must exit 0

**Checkpoint**: `mvn verify` green with 3 IT test classes passing. `mvn test` clean with no Docker. (SC-P4-5 satisfied)

---

## Phase 7: User Story 5 — Constitution Compliance Sign-Off (Priority: P3)

**Goal**: `docs/CONSTITUTION_COMPLIANCE.md` committed with all 10 Inviolable Rules showing `PASS` with traceable evidence references; final `mvn verify` and `npm run test:coverage` both pass.

**Independent Test**: Read `docs/CONSTITUTION_COMPLIANCE.md` — 10 rows, all `PASS`, each with a specific file path or test class as evidence.

### Implementation for User Story 5

- [X] T323 [US5] Create `docs/CONSTITUTION_COMPLIANCE.md` — Markdown table with columns: `Rule`, `Description`, `Status (PASS/FAIL)`, `Evidence (file or test name)`; one row per rule 01–10; for each rule, cite the specific file path or test class name that proves compliance; all 10 rows must show `PASS`. Use `project-contitution.md` at the repo root (lines 230–248) for the authoritative rule definitions. Evidence references by rule: Rule 01 → `git grep` zero-secrets scan result; Rule 02 → `GitHubWebhookSignatureVerifier.java` + `StripeWebhookSignatureVerifier.java`; Rule 03 → `ProjectDto.java` + `SubscriptionTierResponse.java`; Rule 04 → `SyncAuthorizationService.java`; Rule 05 → GitHub branch protection settings; Rule 06 → `BillingController.java` (Checkout session only); Rule 07 → `WebhookEventDispatcher.java` (@Async); Rule 08 → Phase 3 JaCoCo report (≥70%) + Vitest coverage report (≥60%) from T324; Rule 09 → `grep -r "@RequestBody" src/main \| grep "@Valid"` showing 100% match; Rule 10 → `grep -r "import lombok" src/main` zero results
- [X] T324 [US5] Run `mvn verify -pl backend` (Surefire unit tests + Failsafe IT tests + JaCoCo coverage check) and `cd frontend && npm run test:coverage` — both must exit 0; record backend line coverage percentage and frontend statement coverage percentage as evidence for Rule 08 in `docs/CONSTITUTION_COMPLIANCE.md`

**Checkpoint**: `docs/CONSTITUTION_COMPLIANCE.md` exists with all 10 rows `PASS`. (SC-P4-6 satisfied)

---

## Final Phase: Polish & Cross-Cutting Concerns

- [X] T325 [P] Run final constitution grep verifications: `git grep -E "(sk_live_|sk_test_)" -- backend/src/main` → zero matches (Rule 01 evidence); `grep -r "AuditLogger\|auditLogger" backend/src/main/java` → matches in `AuthController`, `GlobalExceptionHandler`, `StripeWebhookHandler`, `AIProcessingService`, `WebhookController` — confirm all 5 wiring points from US3 are present
- [X] T326 [P] Run `docker-compose up -d` from repo root; wait for startup; `curl -s http://localhost:8080/actuator/health | jq .status` → assert output is `"UP"`; `curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/env` → assert HTTP 401; `docker-compose down` when complete (SC-P4-1 smoke test)
- [X] T327 Push branch `006-phase4-production-hardening` — confirm GitHub Actions backend CI job runs `mvn verify` with the `pg:16` service container and all 3 `*IT.java` Testcontainers tests pass in CI (SC-P4-5 CI gate)

---

## Dependency Graph

```
T300 ──► T303, T304, T305, T309     (Failsafe plugin + TC BOM needed for IT tests to compile/run)
T301 ──► T302, T307, T317           (logstash dep needed for logback config and audit log tests)
T302 ──► T307, T317                  (logback-spring.xml needed for JSON audit output validation)
T308 ──► T303, T304, T305           (AbstractIntegrationTest base class needed before IT tests compile)
T303, T304, T305 ──► T309           (IT test files must exist before T309 confirms them passing)
T306 ──► T315, T316                 (ENTERPRISE unit test must fail before implementing)
T307 ──► T317                        (Audit test must fail before implementing AuditLogger)
T315 ──► T316                        (forceRefreshFromStripe() must exist before authorize() calls it)
T317 ──► T318, T319, T320, T321, T322  (AuditLogger event methods must exist before wiring)
T310 ──► T311, T312, T313, T314    (application.yml actuator exposure needed for all US1 tasks)
T310a ──► T311                     (Actuator security assertions must fail before T311 implements the ADMIN rule)
T309, T316, T322, T324 ──► T323    (All implementation complete before constitution doc)
T323 ──► T324                        (Constitution doc drafted before final verification run)
T324 ──► T325, T326, T327           (All passes before smoke test and CI gate)
```

---

## Parallel Execution Examples

**Phase 1** (T300, T301 edit same file — batch as one pom.xml edit; T302 is independent):
- T301 and T302 can be done in parallel with T300 if pom.xml and logback-spring.xml are edited simultaneously

**Phase 3 US1** (after T310 is applied):
- T311, T312, T313, T314 each touch different files — all four can be implemented in parallel

**Phase 5 US3** (after T317 is complete):
- T318, T319, T320, T321, T322 each modify a different controller/handler — fully parallelizable

**Phase 6 US4** (after T308 is complete):
- T303, T304, T305 each create a different test file — all three can be written in parallel

**Final Phase**:
- T325 and T326 are independent — can run concurrently

---

## Implementation Strategy

### MVP Scope (deliver US1 first — highest business priority)

Phase 1 (T300–T302) + Phase 2 (T308) + Phase 3 US1 (T310a, T310–T314)

**Delivers**: `/actuator/health` with real component detail; ADMIN-protected Actuator; Micrometer subscription metrics  
**Validates**: SC-P4-1, SC-P4-2

### Full Delivery Order

1. **Phase 1** (T300–T302) — unblocks all subsequent phases
2. **Phase 2** (T308) — unblocks US4 IT tests
3. **Phase 3 US1** (T310a → T310–T314) — write T310a test first (must fail), then implement T311–T314
4. **Phase 4 US2** (T306 → T315–T316) — write test first, implement after
5. **Phase 5 US3** (T307 → T317–T322) — write test first, implement AuditLogger, then wire all 5 call sites in parallel
6. **Phase 6 US4** (T303–T305 → T309) — write IT tests, run to confirm passing
7. **Phase 7 US5** (T323–T324) — constitution sign-off after all code verified
8. **Final** (T325–T327) — grep checks, smoke test, CI gate

---

## Task Summary

| Phase | Story | Task IDs | Count |
|-------|-------|----------|-------|
| Phase 1: Setup | — | T300–T302 | 3 |
| Phase 2: Foundational | — | T308 | 1 |
| Phase 3: Actuator/Health | US1 (P1) | T310a, T310–T314 | 6 |
| Phase 4: ENTERPRISE Tier | US2 (P2) | T306, T315–T316 | 3 |
| Phase 5: Audit Logging | US3 (P2) | T307, T317–T322 | 7 |
| Phase 6: Integration Tests | US4 (P3) | T303–T305, T309 | 4 |
| Phase 7: Constitution | US5 (P3) | T323–T324 | 2 |
| Final: Polish | — | T325–T327 | 3 |
| **Total** | | **T300–T327 + T310a** | **29** |

**Parallel opportunities**: 4 clusters (Phase 3 US1, Phase 5 US3 wiring, Phase 6 US4 IT tests, Final Phase)  
**Independent test criteria per story**: Each story has a defined Independent Test in its phase header above  
**Suggested MVP scope**: Phase 1 + Phase 2 + Phase 3 US1 (T300–T302, T308, T310a, T310–T314) = 10 tasks
