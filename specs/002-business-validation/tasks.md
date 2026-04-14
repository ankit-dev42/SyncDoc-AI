---

description: "Task list for SyncDoc AI business validation and deployment readiness"

---

# Tasks: SyncDoc AI Business Validation & Deployment Readiness

**Input**: Design documents from `/specs/002-business-validation/`
**Prerequisites**: plan.md (required), spec.md (required)

**Tests**: Test tasks are mandatory. Write tests first, validate failing state, then implement production code.

## Phase 1: Discovery & Harness Setup

- [ ] T001 Inventory existing modules and map requested capabilities from SyncDoc_AI_Test_Execution.md into `specs/002-business-validation/research.md`
- [ ] T002 [P] Add backend domain package skeletons under `backend/src/main/java/com/syncdoc/` for `subscription`, `project`, `webhook`, `ai`, and `quality`
- [ ] T003 [P] Add backend test package skeletons under `backend/src/test/java/com/syncdoc/` for `subscription`, `project`, `webhook`, `ai`, and `quality`
- [ ] T004 [P] Add browser E2E harness configuration in `frontend/playwright.config.ts` and supporting `package.json` scripts if no E2E runner exists

## Phase 2: User Story 1 - Subscription-Gated Sync Authorization

### Tests

- [ ] T005 [P] [US1] Create unit tests for entitlement decisions in `backend/src/test/java/com/syncdoc/subscription/service/SubscriptionServiceTest.java`
- [ ] T006 [P] [US1] Add tests covering free-tier one-repository sync limit in `backend/src/test/java/com/syncdoc/subscription/service/SyncAuthorizationServiceTest.java`

### Implementation

- [ ] T007 [US1] Implement or align Stripe subscription client seam in `backend/src/main/java/com/syncdoc/subscription/client/StripeClient.java`
- [ ] T008 [US1] Implement subscription validation logic in `backend/src/main/java/com/syncdoc/subscription/service/SubscriptionService.java`
- [ ] T009 [US1] Implement sync entitlement policy in `backend/src/main/java/com/syncdoc/subscription/service/SyncAuthorizationService.java`

## Phase 3: User Story 2 - Project Documentation Access Isolation

### Tests

- [ ] T010 [P] [US2] Create MockMvc integration tests for project access ownership rules in `backend/src/test/java/com/syncdoc/project/controller/ProjectControllerSecurityIntegrationTest.java`

### Implementation

- [ ] T011 [US2] Implement or align project documentation endpoint in `backend/src/main/java/com/syncdoc/project/controller/ProjectController.java`
- [ ] T012 [US2] Implement ownership and authorization checks in `backend/src/main/java/com/syncdoc/project/security/ProjectAccessService.java`

## Phase 4: User Story 3 - GitHub Webhook Verification & Dispatch

### Tests

- [ ] T013 [P] [US3] Add GitHub signature test utility in `backend/src/test/java/com/syncdoc/webhook/support/GitHubWebhookTestUtils.java`
- [ ] T014 [P] [US3] Create webhook integration tests for valid and invalid HMAC requests in `backend/src/test/java/com/syncdoc/webhook/controller/WebhookControllerIntegrationTest.java`

### Implementation

- [ ] T015 [US3] Implement webhook signature verification in `backend/src/main/java/com/syncdoc/webhook/security/GitHubWebhookSignatureVerifier.java`
- [ ] T016 [US3] Implement or align webhook endpoint in `backend/src/main/java/com/syncdoc/webhook/controller/WebhookController.java`
- [ ] T017 [US3] Implement internal event dispatch on accepted webhook events in `backend/src/main/java/com/syncdoc/webhook/service/WebhookEventDispatcher.java`

## Phase 5: User Story 4 - Payment Success Path Verification

### Tests

- [ ] T018 [P] [US4] Add browser E2E test for upgrade and checkout success flow in `frontend/tests/integration/payment-success.spec.ts`

### Implementation

- [ ] T019 [US4] Implement or align dashboard upgrade trigger and redirect handling in `frontend/src/features/billing/`
- [ ] T020 [US4] Implement success-state rendering after checkout return in `frontend/src/features/billing/components/PaymentSuccessBanner.tsx`

## Phase 6: User Story 5 - AI Documentation Extraction Quality

### Tests

- [ ] T021 [P] [US5] Create unit tests for Markdown extraction and persistence in `backend/src/test/java/com/syncdoc/ai/service/AIProcessingServiceTest.java`

### Implementation

- [ ] T022 [US5] Implement or align Markdown section parsing in `backend/src/main/java/com/syncdoc/ai/parser/GeneratedDocumentationParser.java`
- [ ] T023 [US5] Implement AI processing orchestration and persistence in `backend/src/main/java/com/syncdoc/ai/service/AIProcessingService.java`

## Phase 7: Deployment Readiness Gates

- [ ] T024 [P] Add Java 17 compatibility verification in `backend/src/test/java/com/syncdoc/quality/runtime/Java17CompatibilityTest.java`
- [ ] T025 [P] Add secret hygiene scan for tracked source/config files in `backend/src/test/java/com/syncdoc/quality/security/SecretExposureAuditTest.java`
- [ ] T026 [P] Add migration integrity test for `user_subscriptions` schema in `backend/src/test/java/com/syncdoc/subscription/integration/UserSubscriptionsMigrationIntegrationTest.java`
- [ ] T027 Document local and CI execution commands in `specs/002-business-validation/research.md`

## Execution Notes

- Use T001 first to confirm whether an equivalent module already exists under a different name before creating new production classes.
- If a requested capability already exists, update the task implementation target to the discovered file path in `specs/002-business-validation/research.md` before coding.
- Keep all external integrations mocked except the browser redirect flow required for payment verification.