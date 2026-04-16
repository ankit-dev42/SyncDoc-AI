# Research: SyncDoc AI Business Validation & Deployment Readiness

**Input**: Specification from [spec.md](spec.md)  
**Phase**: Phase 0 — Technical research and clarification resolution  
**Version**: 1.0  
**Date**: 2026-04-14  

---

## Executive Summary

All technical unknowns from the feature specification have been resolved. Five user stories map to concrete, documented technology choices with clear implementation paths. No specification clarifications remain blocking Phase 1 design.

---

## Resolved Technical Unknowns

### 1. Subscription Entitlement Gate Implementation

**Unknown**: How is subscription status retrieved and cached to minimize dependency on external Stripe API calls?

**Decision**: Local cache with 5-minute TTL + on-demand refresh on enterprise tiers.

**Rationale**:
- Stripe API calls add 50-200ms latency per request; caching via Redis reduces sync authorization from network-dependent to local lookup (≤20ms)
- 5-minute TTL balances freshness (user perceives tier change quickly) with cache hit rate (majority of checks within 5 minutes)
- Enterprise tiers get on-demand refresh on every check to honor trial/subscription changes immediately

**Alternatives Considered**:
- No caching: Stripe on every check → 100ms+ latency per sync, creates cascading failures if Stripe is slow. Rejected.
- Infinite cache: User upgrades but sync gate never updates. Rejected.
- 1-second TTL: Too many Stripe API calls; hits rate limits. Rejected.

**Implementation**:
- `SubscriptionService.getSubscriptionTier(userId)` checks Redis first
- If cache miss or TTL expired: fetch from Stripe, store in Redis with 5-min expiry
- Enterprise tier fetches always hit Stripe (no cache) and update Redis immediately

---

### 2. Project Ownership Verification

**Unknown**: Should project ownership check be a database query or in-memory lookup?

**Decision**: In-memory lookup with lazy-loaded user context; no database query for authorization check.

**Rationale**:
- Authorization checks happen on every API call; database queries at this frequency are a bottleneck
- User context (role, project membership) is already loaded during request authentication
- Project ownership is binary (user_id == owner_id) — no complex RBAC rules in MVP

**Alternatives Considered**:
- Database query per check: adds ≤200ms per request; unacceptable for high-traffic endpoints. Rejected.
- Redis-cached membership: adds complexity for MVP; no performance win over in-memory. Rejected.

**Implementation**:
- `ProjectAccessService.isOwner(userId, projectId)` loads from Spring Security principal's project claims
- Falls back to database only if claims are missing/stale (rare case on replayed tokens)

---

### 3. GitHub Webhook Signature Verification

**Unknown**: Should webhook signature verification be synchronous or asynchronous?

**Decision**: Synchronous signature verification (blocking) before accepting payload; async processing of accepted events.

**Rationale**:
- Signature verification must happen before internal state changes; cannot risk processing forged events
- Verification is CPU-bound (HMAC-SHA256 hash comparison), not I/O bound — no latency cost to sync
- Async processing of accepted events prevents webhook handler from blocking; GitHub has retry budget

**Alternatives Considered**:
- Async verification: Could process a forged event while verification runs in background. Unacceptable. Rejected.
- Inline processing: Handler blocks until event is persisted; GitHub webhook hangs. Rejected.

**Implementation**:
- `WebhookController.receive()` verifies signature synchronously; returns `202 Accepted` or `403 Unauthorized` immediately
- If accepted, publishes Spring event `WebhookReceivedEvent`
- `WebhookEventDispatcher` listens and processes async via `@Async`

---

### 4. AI Documentation Extraction Approach

**Unknown**: Should extraction happen inline during upload or asynchronously in a background job?

**Decision**: Async extraction via Spring `@Async` task queue.

**Rationale**:
- OpenAI API calls add 2-5s latency per request; making user wait is unacceptable
- Background processing allows extraction quality to be improved without blocking user interaction
- Extraction errors don't cascade to user-facing endpoints; handled safely in job failure handler

**Alternatives Considered**:
- Inline extraction: User waits 5s+ for OpenAI response. Unacceptable UX. Rejected.
- Webhook from OpenAI: Adds complexity of polling/polling; not available in test mocks. Rejected.

**Implementation**:
- `AIProcessingService.submitForExtraction(userId, documentId)` queues task and returns immediately with status="pending"
- `@Async` task calls OpenAI, parses response, persists to `GeneratedDocumentation`
- UI polls status endpoint or receives WebSocket update on completion

---

### 5. Payment Success-Path Verification

**Unknown**: Should payment E2E test use real Stripe test-mode or mock entirely?

**Decision**: Use Stripe test-mode with test-card tokens in CI environment.

**Rationale**:
- Real test-mode validates actual Stripe integration; mocks don't catch breaking API changes
- Test-card data is deterministic and PCI-compliant (Stripe handles security)
- CI environment has isolated payment account; no risk of cross-pollinating production

**Alternatives Considered**:
- Full mock: Cannot detect Stripe SDK version incompatibilities. Rejected.
- Live-mode test cards: Too risky even in test; violates security policies. Rejected.
- Skip payment E2E: No way to verify checkout redirect loop; blind spot in critical flow. Rejected.

**Implementation**:
- Playwright test in `frontend/tests/integration/payment-success.spec.ts`
- Uses Stripe test-mode key from CI environment variables
- Submits test card (4242 4242 4242 4242) with any future expiry
- Verifies redirect URL matches expected callback endpoint

---

### 6. Deployment-Readiness Automation

**Unknown**: How are hardcoded secrets detected before they reach version control?

**Decision**: Static analysis via regex scan in CI + pre-commit hook (optional for developers).

**Rationale**:
- Regex patterns catch 95%+ of hardcoded secrets (common key prefixes, API key formats)
- false-negatives (undetected hardcoded keys) require human review; cannot be 100% automated
- CI failure on secret detection is non-negotiable; pre-commit hook is convenience

**Alternatives Considered**:
- Manual code review only: Too slow; secrets may reach production before review. Rejected.
- AST-based analysis: Overkill for simple pattern detection; adds build complexity. Rejected.
- Post-deployment scanning: Too late if secret is in distributed images. Rejected.

**Implementation**:
- `SecretExposureAuditTest.java` runs regex patterns against `src/` and `resources/` trees
- Patterns: `STRIPE.*KEY`, `GITHUB.*TOKEN`, `OPENAI.*API`, `DATABASE.*PASSWORD`, etc.
- CI workflow fails if matches are found; developer must use environment variables instead

---

### 7. Java 17 Compatibility Verification

**Unknown**: How is Java 17 compliance verified beyond "it compiles"?

**Decision**: Bytecode version inspection + feature-use audit.

**Rationale**:
- Compiler flag alone doesn't prevent accidentally using Java 21+ features (sealed classes, records with default methods)
- Bytecode version 61 (Java 17) is verifiable at build time
- Feature audit catches edge cases (e.g., using Sequenced Collections which are Java 21)

**Alternatives Considered**:
- Compile flag only: Doesn't catch transitive dependencies at Java 21. Rejected.
- Manual audit: Too slow; non-scalable. Rejected.

**Implementation**:
- `Java17CompatibilityTest.java` uses Javassist or bytecode inspection library
- Verifies all `.class` files in `/target/classes/` have version 61 bytecode
- Regex audit of source files for Java 21+ feature keywords (e.g., `SequencedCollection`, `SequencedMap`, unnamed classes)

---

## Technology Stack Confirmation

All technologies map to SyncDoc AI Constitution (Section 3, Stack Decisions):

| Domain | Technology | Version | Confirmation |
|---|---|---|---|
| Backend Language | Java | 17 (LTS) | ✅ Per constitution; verified in Phase 0 |
| Backend Framework | Spring Boot | 3.x | ✅ Per constitution; WebSocket config exists |
| Testing | JUnit 5 + Mockito | Current | ✅ Per constitution; pom.xml verified |
| Database | PostgreSQL | 16 | ✅ Per constitution; schema migration framework in place |
| Caching | Redis | 7.x | ✅ Per constitution; docker-compose verified |
| Frontend | React | 18 | ✅ Per constitution; package.json verified |
| Frontend Test | Vitest + RTL | Current | ✅ Per constitution; existing test infrastructure |
| E2E | Playwright | 1.40+ | ✅ Added in frontend package scripts and config scaffold |
| API | REST + WebSocket | N/A | ✅ Per constitution; WebSocket broker configured |
| Secrets | Environment variables | N/A | ✅ Per constitution; `.env.example` pattern |

---

## Integration Points with Existing Codebase

### Inherited from SyncDoc AI

1. **Authentication**: User identity + session handling from `com.syncdoc.auth`
2. **Multi-tenancy**: Workspace scoping model from realtime-collab feature (reuse `TenantScopedRepository`)
3. **Event Bus**: Spring Events infrastructure from realtime-collab webhooks
4. **Logging**: Structured logging via SLF4J + Logback

### Module Mapping (T001)

Resolved implementation paths anchored to the active Spring Boot package root `com.syncdoc.collaboration`:

1. **Subscription**: `backend/src/main/java/com/syncdoc/collaboration/subscription/`
2. **Project**: `backend/src/main/java/com/syncdoc/collaboration/project/`
3. **Webhook**: `backend/src/main/java/com/syncdoc/collaboration/webhook/`
4. **AI**: `backend/src/main/java/com/syncdoc/collaboration/ai/`
5. **Quality**: `backend/src/main/java/com/syncdoc/collaboration/quality/`

Associated test roots:

1. `backend/src/test/java/com/syncdoc/collaboration/subscription/`
2. `backend/src/test/java/com/syncdoc/collaboration/project/`
3. `backend/src/test/java/com/syncdoc/collaboration/webhook/`
4. `backend/src/test/java/com/syncdoc/collaboration/ai/`
5. `backend/src/test/java/com/syncdoc/collaboration/quality/`

---

## Feature Traceability Matrix (T050)

| Requirement | Covered By Tasks |
|---|---|
| FR-001 Subscription entitlement validation | T011-T018 |
| FR-002 Free-tier limit enforcement | T012-T018 |
| FR-003 Authorized project-only access | T019-T023 |
| FR-004 `403 Forbidden` on cross-project access | T020-T023, T046 |
| FR-005 GitHub HMAC verification | T024-T027, T045 |
| FR-006 Dispatch accepted webhook events | T025-T030, T045 |
| FR-007 Return `202 Accepted` for valid webhooks | T024-T027 |
| FR-008 Automated payment success-path verification | T031-T035 |
| FR-009 Extract `Key Changes` and `Action Items` | T036, T039, T048 |
| FR-010 Persist extracted AI documentation | T037-T041 |
| FR-011 Java 17 compatibility | T042 |
| FR-012 Secret hygiene enforcement | T043 |
| FR-013 `user_subscriptions` migration integrity | T044 |
| FR-014 Validate internal `GithubEventPayload` schema | T024, T028, T030 |

| Success Criterion | Covered By Tasks |
|---|---|
| SC-001 Subscription allow/deny accuracy | T011-T018 |
| SC-002 Zero cross-project leaks in 1000+ requests | T019-T023, T046 |
| SC-003 Valid webhooks accepted, forged rejected, dispatch <=100ms | T024-T030, T045 |
| SC-004 Payment success flow completes in <30s | T031-T035 |
| SC-005 AI extraction accuracy >=95% | T036-T041, T047-T048 |
| SC-006 Java 17 + secret hygiene + migration verification | T042-T044 |

Frontend E2E harness paths:

1. `frontend/playwright.config.ts`
2. `frontend/tests/integration/fixtures/auth.fixture.ts`

### New Modules to Create

1. **Subscription**: `com.syncdoc.collaboration.subscription.*` — no existing equivalent
2. **Project**: `com.syncdoc.collaboration.project.*` — no existing equivalent (separate from collaboration `Channel`)
3. **Webhook**: `com.syncdoc.collaboration.webhook.*` — similar to realtime-collab webhooks but GitHub-specific
4. **AI**: `com.syncdoc.collaboration.ai.*` — no existing equivalent
5. **Quality**: `com.syncdoc.collaboration.quality.*` — deployment-readiness checks

---

## Specification Compliance Gate

**Constitution Alignment Review**:

- ✅ Principle I (Code Quality): All modules follow single-responsibility pattern; max method length 40 lines per constitution
- ✅ Principle II (Testing Standards): Test-first methodology; unit + integration tests required before implementation
- ✅ Principle III (UX Consistency): Error states defined per spec; success messages documented; all strings externalized
- ✅ Principle IV (Performance): Latency budgets documented (subscription ≤50ms, access ≤20ms, webhook dispatch ≤100ms, payment <30s)

**Gate Status**: ✅ **PASS** — No constitution violations. Feature ready for Phase 1 design.

---

## Outstanding Items  

**None**. All technical decisions are documented, alternatives evaluated, and integration points identified. Ready for Phase 1 implementation design.

---

## Next Steps

→ Proceed to Phase 1: Generate `data-model.md`, `contracts/`, and `quickstart.md`
