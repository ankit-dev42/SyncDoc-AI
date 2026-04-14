---
description: "Executable, dependency-ordered task list for 002-business-validation"
---

# Tasks: SyncDoc AI Business Validation & Deployment Readiness

**Input**: Design docs from `/specs/002-business-validation/` (`spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/rest-endpoints.yml`, `quickstart.md`)
**Prerequisites**: Phase 0 research complete, implementation plan approved
**Tests**: Test-first is mandatory for every user story (Constitution Principle II)
**Organization**: Setup -> Foundational -> User Stories (P1 to P2) -> Release Gates

## Phase 1: Setup (Project Initialization)

**Purpose**: Align workspace, harnesses, and feature scaffolding before implementation.

- [x] T001 Document feature module mapping and resolved file paths in `specs/002-business-validation/research.md`
- [x] T002 [P] Add Playwright runner and scripts (`test:e2e`, `test:e2e:headed`) in `frontend/package.json`
- [x] T003 [P] Create Playwright configuration for local/CI execution in `frontend/playwright.config.ts`
- [x] T004 [P] Add shared E2E test fixtures for authenticated dashboard flows in `frontend/tests/integration/fixtures/auth.fixture.ts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared infrastructure that blocks all user stories until complete.

- [x] T005 Align core JPA entities to `data-model.md` in `backend/src/main/java/com/syncdoc/collaboration/subscription/model/UserSubscription.java`, `backend/src/main/java/com/syncdoc/collaboration/project/model/Project.java`, `backend/src/main/java/com/syncdoc/collaboration/webhook/model/WebhookEvent.java`, and `backend/src/main/java/com/syncdoc/collaboration/ai/model/GeneratedDocumentation.java`
- [x] T006 Add Flyway migration for `user_subscriptions`, `projects`, `webhook_events`, and `generated_documentation` in `backend/src/main/resources/db/migration/V3__business_validation_entities.sql`
- [x] T007 [P] Create repositories for new entities in `backend/src/main/java/com/syncdoc/collaboration/subscription/repository/UserSubscriptionRepository.java`, `backend/src/main/java/com/syncdoc/collaboration/project/repository/ProjectRepository.java`, `backend/src/main/java/com/syncdoc/collaboration/webhook/repository/WebhookEventRepository.java`, and `backend/src/main/java/com/syncdoc/collaboration/ai/repository/GeneratedDocumentationRepository.java`
- [x] T008 [P] Add typed environment configuration for Stripe/GitHub/OpenAI secrets in `backend/src/main/resources/application.yml` and `backend/src/main/java/com/syncdoc/collaboration/config/BusinessValidationProperties.java`
- [x] T009 Implement shared error payload mapping for business-validation APIs in `backend/src/main/java/com/syncdoc/collaboration/exception/BusinessValidationExceptionHandler.java`
- [x] T010 Add user-facing i18n keys for payment and access-denied flows in `frontend/src/i18n/en.json`

**Checkpoint**: Foundation complete; user stories can now proceed.

---

## Phase 3: User Story 1 - Subscription-Gated Sync Authorization (Priority: P1) 🎯 MVP

**Goal**: Allow sync for active paid subscriptions and block free-tier overages or inactive subscriptions.

**Independent Test**: Subscription unit/integration tests pass using mocked Stripe responses and repository count inputs only.

### Tests for User Story 1 (Write and fail first)

- [x] T011 [P] [US1] Create failing contract tests for `GET /api/v1/subscriptions/{userId}/tier` and `POST /api/v1/subscriptions/{userId}/can-sync` in `backend/src/test/java/com/syncdoc/collaboration/subscription/contract/SubscriptionContractTest.java`
- [x] T012 [P] [US1] Create failing unit tests for active/inactive/canceled subscription evaluation in `backend/src/test/java/com/syncdoc/collaboration/subscription/unit/SubscriptionServiceTest.java`
- [x] T013 [P] [US1] Create failing unit tests for free-tier repository limit decisions in `backend/src/test/java/com/syncdoc/collaboration/subscription/unit/SyncAuthorizationServiceTest.java`

### Implementation for User Story 1

- [x] T014 [US1] Implement Stripe dependency seam and test stub contract in `backend/src/main/java/com/syncdoc/collaboration/subscription/client/StripeClient.java`
- [x] T015 [US1] Implement cached subscription status evaluation (5-minute TTL) in `backend/src/main/java/com/syncdoc/collaboration/subscription/service/SubscriptionService.java`
- [x] T016 [US1] Implement sync entitlement decision policy in `backend/src/main/java/com/syncdoc/collaboration/subscription/service/SyncAuthorizationService.java`
- [x] T017 [US1] Implement REST endpoints matching contract in `backend/src/main/java/com/syncdoc/collaboration/subscription/controller/SubscriptionController.java`
- [x] T018 [US1] Add performance-focused tests for subscription check latency budget (<=50ms service-level target) in `backend/src/test/java/com/syncdoc/collaboration/subscription/performance/SubscriptionLatencyPerformanceTest.java`

**Checkpoint**: US1 is independently deployable and testable.

---

## Phase 4: User Story 2 - Project Documentation Access Isolation (Priority: P1)

**Goal**: Enforce owner-only access to project documentation resources.

**Independent Test**: MockMvc tests verify 200 for owner and 403 for non-owner without dependency on other stories.

### Tests for User Story 2 (Write and fail first)

- [x] T019 [P] [US2] Create failing contract test for `GET /api/v1/projects/{projectId}` authorization responses in `backend/src/test/java/com/syncdoc/collaboration/project/contract/ProjectAccessContractTest.java`
- [x] T020 [P] [US2] Create failing integration tests for owner/non-owner/missing-id paths in `backend/src/test/java/com/syncdoc/collaboration/project/integration/ProjectControllerSecurityIntegrationTest.java`

### Implementation for User Story 2

- [x] T021 [US2] Implement owner authorization policy service in `backend/src/main/java/com/syncdoc/collaboration/project/security/ProjectAccessService.java`
- [x] T022 [US2] Implement project documentation endpoint for `GET /api/v1/projects/{projectId}` in `backend/src/main/java/com/syncdoc/collaboration/project/controller/ProjectController.java`
- [x] T023 [US2] Wire user-safe 403 and validation error messages through shared exception mapping in `backend/src/main/java/com/syncdoc/collaboration/exception/BusinessValidationExceptionHandler.java`

**Checkpoint**: US2 is independently deployable and testable.

---

## Phase 5: User Story 3 - GitHub Webhook Verification & Dispatch (Priority: P1)

**Goal**: Accept valid signed GitHub webhooks, reject forged payloads, and dispatch accepted events.

**Independent Test**: Integration and contract tests verify signature handling and event dispatch with local mocks only.

### Tests for User Story 3 (Write and fail first)

- [x] T024 [P] [US3] Create failing signature-format contract test for `POST /api/v1/webhooks/github` and failing event-schema contract test for internal webhook publish payload in `backend/src/test/java/com/syncdoc/collaboration/webhook/contract/WebhookContractTest.java`
- [x] T025 [P] [US3] Create failing integration tests for valid signature (202 + dispatch) and invalid signature (403) in `backend/src/test/java/com/syncdoc/collaboration/webhook/integration/WebhookControllerIntegrationTest.java`

### Implementation for User Story 3

- [x] T026 [US3] Implement HMAC-SHA256 verifier for `X-Hub-Signature-256` in `backend/src/main/java/com/syncdoc/collaboration/webhook/security/GitHubWebhookSignatureVerifier.java`
- [x] T027 [US3] Implement webhook ingestion endpoint for `POST /api/v1/webhooks/github` in `backend/src/main/java/com/syncdoc/collaboration/webhook/controller/WebhookController.java`
- [x] T028 [US3] Implement async Spring event dispatch seam for accepted webhook payloads in `backend/src/main/java/com/syncdoc/collaboration/webhook/service/WebhookEventDispatcher.java`
- [x] T029 [US3] Persist accepted/rejected webhook audit records in `backend/src/main/java/com/syncdoc/collaboration/webhook/service/WebhookAuditService.java`
- [x] T030 [US3] Implement `GithubEventPayload` DTO and JSON schema validator (versioned contract check before publish) in `backend/src/main/java/com/syncdoc/collaboration/webhook/dto/GithubEventPayload.java` and `backend/src/main/java/com/syncdoc/collaboration/webhook/service/GithubEventSchemaValidator.java`

**Checkpoint**: US3 is independently deployable and testable.

---

## Phase 6: User Story 4 - Payment Success Flow Verification (Priority: P2)

**Goal**: Verify dashboard upgrade -> checkout -> redirect -> success confirmation flow.

**Independent Test**: Playwright flow passes in test mode under 30s without relying on unrelated backend stories.

### Tests for User Story 4 (Write and fail first)

- [x] T031 [P] [US4] Create failing Playwright E2E test for upgrade success path in `frontend/tests/integration/payment-success.spec.ts`; wrap flow with timer and assert `durationSeconds < 30` to hard-gate build
- [x] T032 [P] [US4] Create failing frontend component test for success banner rendering and i18n text in `frontend/src/features/billing/components/PaymentSuccessBanner.test.tsx`

### Implementation for User Story 4

- [x] T033 [US4] Implement checkout session/redirect client in `frontend/src/features/billing/api/billingApi.ts`
- [x] T034 [US4] Implement upgrade flow hook for dashboard CTA in `frontend/src/features/billing/hooks/useUpgradeFlow.ts`
- [x] T035 [US4] Implement `/success` route banner component with i18n strings in `frontend/src/features/billing/components/PaymentSuccessBanner.tsx`

**Checkpoint**: US4 is independently deployable and testable.

---

## Phase 7: User Story 5 - AI Documentation Extraction Quality (Priority: P2)

**Goal**: Extract and persist `Key Changes` and `Action Items` from AI markdown reliably.

**Independent Test**: Unit/integration tests pass with mocked OpenAI responses and repository persistence checks.

### Tests for User Story 5 (Write and fail first)

- [ ] T036 [P] [US5] Create failing parser unit tests for heading extraction and malformed markdown handling in `backend/src/test/java/com/syncdoc/collaboration/ai/unit/GeneratedDocumentationParserTest.java`
- [ ] T037 [P] [US5] Create failing service unit tests for single-write persistence and controlled error behavior in `backend/src/test/java/com/syncdoc/collaboration/ai/unit/AIProcessingServiceTest.java`
- [ ] T038 [P] [US5] Create failing contract tests for `GET /api/v1/ai/extract-status/{docId}` and `GET /api/v1/ai/extract-result/{docId}` in `backend/src/test/java/com/syncdoc/collaboration/ai/contract/AIExtractionContractTest.java`

### Implementation for User Story 5

- [ ] T039 [US5] Implement markdown section parser in `backend/src/main/java/com/syncdoc/collaboration/ai/parser/GeneratedDocumentationParser.java`
- [ ] T040 [US5] Implement AI processing orchestration and persistence in `backend/src/main/java/com/syncdoc/collaboration/ai/service/AIProcessingService.java`
- [ ] T041 [US5] Implement extraction status/result endpoints in `backend/src/main/java/com/syncdoc/collaboration/ai/controller/AIExtractionController.java`

**Checkpoint**: US5 is independently deployable and testable.

---

## Phase 8: Release Gates & Cross-Cutting Concerns

**Purpose**: Final compliance and release-readiness checks across all stories.

### Blockers (Must Pass for Functional Acceptance)

- [ ] T042 [P] Add Java 17 bytecode compatibility validation test in `backend/src/test/java/com/syncdoc/collaboration/quality/runtime/Java17CompatibilityTest.java`
- [ ] T043 [P] Add hardcoded-secret scan test for backend/frontend source in `backend/src/test/java/com/syncdoc/collaboration/quality/security/SecretExposureAuditTest.java`
- [ ] T044 [P] Add migration integrity test for `user_subscriptions` schema and indexes in `backend/src/test/java/com/syncdoc/collaboration/subscription/integration/UserSubscriptionsMigrationIntegrationTest.java`
- [ ] T045 [P] Add webhook dispatch latency test (<=100ms from receipt to publish) in `backend/src/test/java/com/syncdoc/collaboration/webhook/performance/WebhookDispatchPerformanceTest.java`
- [ ] T046 [P] Add multi-tenant leak harness `MultiTenantSecurityAuditTest` with 1000 unauthorized cross-project requests and strict assertion of 100% `403` responses in `backend/src/test/java/com/syncdoc/collaboration/project/security/MultiTenantSecurityAuditTest.java`
- [ ] T047 [P] Add AI extraction accuracy harness fixture corpus (20 pairs of `sample_git_diff.txt` and `expected_summary.md`) under `backend/src/test/resources/fixtures/ai-accuracy/`
- [ ] T048 [P] Add AI extraction scoring test that computes average similarity against fixture corpus and fails when score < 0.95 in `backend/src/test/java/com/syncdoc/collaboration/ai/performance/AIExtractionAccuracyGateTest.java`

### Non-Blockers (Post-Acceptance Documentation)

- [ ] T049 Update execution commands and validation evidence in `specs/002-business-validation/quickstart.md`
- [ ] T050 Update feature traceability matrix (FR/SC to tasks) in `specs/002-business-validation/research.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- Setup (Phase 1) has no dependencies.
- Foundational (Phase 2) depends on Phase 1 and blocks all user stories.
- User Stories (Phases 3-7) all depend on Phase 2.
- Release Gates (Phase 8) depends on completion of all selected user stories.

### User Story Dependencies

- US1 (P1) can start immediately after Foundational.
- US2 (P1) can start immediately after Foundational; independent from US1/US3.
- US3 (P1) can start immediately after Foundational; independent from US1/US2.
- US4 (P2) can start after Foundational and Playwright setup; independent from US1/US2/US3.
- US5 (P2) can start immediately after Foundational; independent from US1-US4.

### Within Each User Story

- Tests first: tasks with test files must be written and observed failing before implementation tasks.
- Services before controllers/endpoints when both exist.
- Story checkpoint must pass before marking story complete.

---

## Parallel Execution Examples

### US1

- Run T011, T012, and T013 in parallel (different test files).
- Run T014 and T018 in parallel after tests are written (client seam and perf test scaffolding).

### US2

- Run T019 and T020 in parallel (contract vs integration test files).

### US3

- Run T024 and T025 in parallel (contract and integration tests).
- Run T028 and T029 in parallel after T027 starts (dispatch seam and audit persistence are separate files).

### US4

- Run T031 and T032 in parallel (E2E and component test files).
- Run T033 and T035 in parallel after test baseline is in place (API client and UI component files).

### US5

- Run T036, T037, and T038 in parallel (parser/service/contract test files).
- Run T039 and T041 in parallel after test definitions are finalized (parser and controller files).

---

## Implementation Strategy

### MVP First

1. Complete Phase 1 and Phase 2.
2. Deliver US1, US2, and US3 (Phases 3-5).
3. Run Phase 8 blocker tasks T042-T048.
4. Validate MVP in `backend` and mark ready for release candidate.

### Incremental Delivery

1. Add US4 payment flow after MVP stability gate.
2. Add US5 AI extraction quality after payment flow signoff.
3. Re-run all Phase 8 checks before full feature release.

### Format Validation

- All tasks use checklist format: `- [ ] T### [P] [US#] Description with exact file path`.
- Setup, Foundational, and Phase 8 cross-cutting tasks intentionally omit `[US#]` labels.
- User Story tasks include `[US#]` labels on every line.