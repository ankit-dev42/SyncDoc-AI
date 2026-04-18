# Feature Specification: SyncDoc AI Business Validation & Deployment Readiness

**Feature Branch**: `002-business-validation`
**Created**: 2026-04-14
**Status**: Specified
**Input**: Validation prompts and deployment checklist from SyncDoc_AI_Test_Execution.md

## User Scenarios & Testing

### User Story 1 - Subscription-Gated Sync Authorization (Priority: P1)

**What this user does**: A signed-in user attempts to run a repository sync and must be allowed or blocked based on their active Stripe-backed subscription tier.

**Why this priority**: Sync entitlement is directly tied to monetization and abuse prevention. If this gate is wrong, paid users are blocked or free-tier users exceed allowed usage.

**Independent Test**: A unit test can fully validate the decision logic by mocking Stripe status responses and repository counts.

**Acceptance Scenarios**:

1. **Given** a user has an active paid subscription, **When** they request a sync, **Then** the authorization check allows the operation.
2. **Given** a user is on the free tier with one synced repository already, **When** they request another sync, **Then** the authorization check denies the operation.
3. **Given** Stripe returns an inactive or canceled subscription, **When** the sync gate is evaluated, **Then** the user is treated as non-paying.

### User Story 2 - Project Documentation Access Isolation (Priority: P1)

**What this user does**: A user opens project documentation and must only see resources they own or are explicitly authorized to access.

**Why this priority**: Cross-project access is a direct security failure with data leakage implications.

**Independent Test**: A MockMvc integration test can verify success for the owner and `403 Forbidden` for another user's `projectId`.

**Acceptance Scenarios**:

1. **Given** a user owns a project, **When** they request that project's documentation endpoint, **Then** the response is successful.
2. **Given** a user requests another user's `projectId`, **When** authorization runs, **Then** the response is `403 Forbidden`.
3. **Given** an invalid or missing project identifier, **When** the endpoint is called, **Then** a client-safe error response is returned.

### User Story 3 - GitHub Webhook Verification & Dispatch (Priority: P1)

**What this user does**: GitHub posts a signed webhook event that SyncDoc AI must authenticate, accept, and dispatch onto the internal event bus.

**Why this priority**: If webhook validation is weak, the system can ingest forged events. If delivery is unreliable, automation breaks.

**Independent Test**: An integration test can simulate a valid HMAC-signed webhook and assert `202 Accepted` plus event-bus publication.

**Acceptance Scenarios**:

1. **Given** a webhook payload with a valid HMAC signature, **When** it is posted to the webhook endpoint, **Then** the controller returns `202 Accepted`.
2. **Given** a valid webhook payload, **When** the controller accepts it, **Then** the internal event bus receives the expected dispatch.
3. **Given** a webhook payload with an invalid signature, **When** it is posted, **Then** the request is rejected.

### User Story 4 - Payment Success Flow Verification (Priority: P2)

**What this user does**: A user upgrades from the dashboard, completes test checkout, and returns to the product with a success confirmation.

**Why this priority**: The payment loop is the highest-value user journey and must remain stable release to release.

**Independent Test**: An end-to-end browser test can validate dashboard entry, checkout redirect, test-card completion, and post-payment success handling.

**Acceptance Scenarios**:

1. **Given** a logged-in dashboard user, **When** they click `Upgrade to Pro`, **Then** they are redirected to checkout.
2. **Given** checkout is completed with test payment data, **When** the flow finishes, **Then** the user is redirected back to the app.
3. **Given** the redirect completes successfully, **When** the dashboard loads, **Then** a clear success message is shown.

### User Story 5 - AI Documentation Extraction Quality (Priority: P2)

**What this user does**: A user submits content for AI-generated documentation and expects key sections such as `Key Changes` and `Action Items` to be extracted and persisted.

**Why this priority**: Documentation quality is a product differentiator; incorrect extraction reduces trust in the AI workflow.

**Independent Test**: A unit test can mock the OpenAI response and verify parsing plus persistence behavior.

**Acceptance Scenarios**:

1. **Given** an OpenAI response containing Markdown headings for `Key Changes` and `Action Items`, **When** processing runs, **Then** both sections are extracted correctly.
2. **Given** extracted sections are available, **When** persistence runs, **Then** the structured result is saved once.
3. **Given** the AI response is malformed or missing a section, **When** processing runs, **Then** the service returns a controlled error or partial result per design.

## Deployment Readiness Checks

1. Production code must remain Java 17 compatible.
2. Secrets such as `STRIPE_API_KEY`, `GITHUB_CLIENT_ID`, and `OPENAI_API_KEY` must not be hardcoded.
3. Database migration coverage must verify the `user_subscriptions` schema and upgrade path.

## Key Entities *(include if feature involves data)*

- **UserSubscription**: Tracks subscription tier and entitlement. Attributes: `user_id`, `subscription_tier` (free/pro/enterprise), `stripe_customer_id`, `status` (active/canceled/expired), `created_at`, `renewed_at`
- **Project**: User-owned project (documentation scope). Attributes: `project_id`, `owner_id`, `name`, `created_at`, `access_control` (private/shared)
- **WebhookEvent**: Log of received webhook deliveries. Attributes: `event_id`, `source` (github), `payload_hash`, `status` (accepted/rejected), `reason`, `dispatched_at`
- **GithubEventPayload**: Internal Spring Event DTO published after webhook validation. Attributes: `event_id`, `event_type`, `repository_id`, `delivery_id`, `payload_hash`, `occurred_at`, `schema_version`
- **GeneratedDocumentation**: Extracted AI output. Attributes: `doc_id`, `user_id`, `source_content`, `key_changes` (text), `action_items` (text), `created_at`, `quality_score`

## Requirements

### Functional Requirements

- **FR-001**: System MUST validate subscription entitlement before permitting a sync operation.
- **FR-002**: System MUST enforce the free-tier limit of one synced repository unless a paid subscription is active.
- **FR-003**: System MUST restrict project documentation access to authorized users only.
- **FR-004**: System MUST return `403 Forbidden` for cross-project access attempts.
- **FR-005**: System MUST verify GitHub webhook HMAC signatures before accepting events.
- **FR-006**: System MUST dispatch accepted webhook events to the internal event bus.
- **FR-007**: System MUST return `202 Accepted` for valid webhook submissions.
- **FR-008**: System MUST support an automated payment success-path verification flow.
- **FR-009**: System MUST extract `Key Changes` and `Action Items` from AI-generated Markdown responses.
- **FR-010**: System MUST persist extracted AI documentation results.
- **FR-011**: System MUST remain compatible with Java 17 in production code.
- **FR-012**: System MUST prevent hardcoded secrets from entering tracked source files.
- **FR-013**: System MUST verify migrations for `user_subscriptions` schema integrity.
- **FR-014**: System MUST validate internal `GithubEventPayload` messages against a versioned JSON schema before dispatching to downstream processors.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Subscription entitlement checks return allow/deny with 100% accuracy across paid, free-tier-limit (>1 repo), and inactive subscription scenarios
- **SC-002**: Project access isolation returns `200 OK` for owner requests and `403 Forbidden` for non-owner requests; zero cross-project leaks in 1000+ request audit
- **SC-003**: Webhook verification accepts valid HMAC signatures and rejects 100% of forged payloads; accepted webhooks dispatch to event bus within ≤100ms
- **SC-004**: Payment success-path E2E test executes end-to-end (dashboard → checkout → redirect → success banner) with deterministic test-card in <30s wall-clock
- **SC-005**: AI documentation parser extracts `Key Changes` and `Action Items` sections with ≥95% accuracy on sample OpenAI responses; parsing errors are logged and tracked
- **SC-006**: Java 17 compatibility verified by bytecode inspection; secret-hygiene scan detects hardcoded keys; migration schema verified for `user_subscriptions` integrity

## Constitution Alignment *(mandatory)*

### Code Quality (Principle I)
- [ ] Authorization logic (subscription, project access, webhook verification) extracted into Named service classes with clear single responsibility
- [ ] No handler method exceeds 40 lines; complex validation rules use dedicated policy classes
- [ ] Deployment-readiness checks (Java 17 compatibility, secret hygiene) automated as test utilities

### Testing Standards (Principle II)
- [ ] Test cases written first for all 5 user stories (subscription, access, webhook, payment, AI extraction)
- [ ] Unit tests: ≥90% coverage for authorization gates (subscription, project access)
- [ ] Integration tests: MockMvc tests for project controller, webhook controller signature verification
- [ ] E2E tests: Browser test for payment success-path using test-mode Stripe
- [ ] Contract tests: Webhook signature format, event-bus message schema
- [ ] Deployment checks: Java 17 bytecode inspection, secret-hygiene regex scan, migration schema validation

### User Experience Consistency (Principle III)
- [ ] Error states defined: "Sync blocked due to free-tier limit", "Access denied: you don't own this project", "Invalid payment information"
- [ ] Notification: Success banner after payment with clear next-step guidance
- [ ] i18n: All error messages and status labels externalized

### Performance Requirements (Principle IV)
- [ ] Subscription check: ≤50ms (cache Stripe status with 5-min TTL)
- [ ] Project access check: ≤20ms (local authorization, no database query for owner check)
- [ ] Webhook dispatch: ≤100ms from receipt to event-bus publish
- [ ] Payment redirect: ≤2s from checkout return to dashboard success render

## Assumptions

- **Stripe integration**: A Stripe test-mode account exists; keys are injected via environment variables at deployment
- **OpenAI integration**: API key is injected via environment; responses are mocked in tests with sample Markdown payloads
- **Event bus**: MVP uses Spring Application Events with `@Async` dispatch; dispatcher contracts remain Redis-compatible for future rollout
- **Authentication**: User authentication and session handling are inherited from SyncDoc AI auth layer; no new auth requirements
- **Browser test harness**: Playwright 1.40+ is available; test-card payment data is configured in CI environment
- **Project ownership**: "Ownership" is modeled as the `user_id` matching the `owner_id` field in the Project entity; no role-based access control (RBAC) required for MVP
- **Webhook signature algorithm**: GitHub uses HMAC-SHA256 with a pre-shared secret; verification is straightforward digest comparison