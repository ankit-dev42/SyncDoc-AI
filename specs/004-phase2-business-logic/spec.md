# Feature Specification: Phase 2 — Business Logic

**Feature Branch**: `004-phase2-business-logic`  
**Created**: 2026-04-20  
**Status**: Draft  
**Priority**: P0/P1 — Stripe and OpenAI are P0; CRUD and schema fixes are P1  
**Prerequisite**: Phase 1 (`003-phase1-foundation-security`) merged to develop

---

## Clarifications

### Session 2026-04-20

- Q1: Which Stripe events must the webhook handler process, and in what priority order? → A: `checkout.session.completed`, `invoice.paid`, `customer.subscription.deleted` — all P0, equal priority
- Q2: Does ENTERPRISE tier bypass Redis cache on every request or only on manual refresh? → A: Bypass Redis on **every** subscription check — no manual trigger required
- Q3: What constitutes a meaningful qualityScore algorithm for AI extraction? → A: `(sectionsFound / sectionsExpected) × min(1.0, contentLength / 500)` — deterministic, values in [0,1], two expected sections: Key Changes + Action Items
- Q4: Should @Async exceptions update DB status or only be logged? → A: Update `WebhookEvent.status` to `FAILED` with `error_message` stored **AND** log the exception at ERROR level
- Q5: Is soft-delete required for projects in MVP, or is hard-delete acceptable? → A: Soft-delete via `deleted_at` column; deleted records are transparent to all queries

---

## User Scenarios & Testing

### User Story 1 — Complete Stripe Billing Flow (Priority: P1)

As a paying user, I want to initiate checkout, complete payment, and have my subscription automatically provisioned so I can immediately use paid features.

**Why this priority**: Stripe billing is the primary revenue path. Without it, no users can be charged and no subscriptions can be created.

**Independent Test**: A user can navigate to the checkout endpoint, receive a redirect URL to the payment provider, complete payment, and verify their subscription status changes — without any other Phase 2 feature being present.

**Acceptance Scenarios**:

1. **Given** an authenticated user, **When** they request a checkout session, **Then** they receive a redirect URL to the payment provider's hosted page
2. **Given** a valid payment provider event notification for a completed checkout, **When** the system receives it, **Then** the user's subscription is provisioned at the paid tier
3. **Given** a valid payment provider event notification for a renewed invoice, **When** the system receives it, **Then** the subscription status is updated to active
4. **Given** a valid payment provider event notification for a cancelled subscription, **When** the system receives it, **Then** the subscription status is updated to cancelled
5. **Given** an authenticated subscriber, **When** they request a billing portal session, **Then** they receive a redirect URL to manage their subscription
6. **Given** the same payment event notification is delivered twice (duplicate), **When** the system receives the second copy, **Then** it is acknowledged but not re-processed
7. **Given** a payment event notification arrives without a valid signature, **When** the system receives it, **Then** it is rejected with a 400 error before any processing

---

### User Story 2 — Distributed Subscription Cache (Priority: P1)

As the system, I need subscription state cached so it survives service restarts and scales across multiple instances without hammering the external billing provider.

**Why this priority**: Without a distributed cache, every subscription check calls the external billing provider, causing latency and risk of rate limiting under load.

**Independent Test**: Two back-to-back subscription checks for the same user within the TTL window should result in only one external billing provider call — verifiable via provider call count.

**Acceptance Scenarios**:

1. **Given** a subscription check for a user with no cached state, **When** the check runs, **Then** state is fetched from the billing provider and stored in the cache with a 5-minute expiry
2. **Given** a cached subscription result exists within TTL, **When** a second check runs, **Then** the result is served from cache — zero billing provider API calls made
3. **Given** a billing provider webhook updates a user's subscription, **When** the webhook is processed, **Then** the cached entry for that user is immediately invalidated
4. **Given** an ENTERPRISE-tier user, **When** any subscription check runs, **Then** the cache is bypassed and state is fetched directly from the billing provider, regardless of cache TTL

---

### User Story 3 — Real AI Documentation Extraction (Priority: P1)

As a user, I want to submit source content and receive AI-extracted Key Changes and Action Items without waiting for the AI model to respond.

**Why this priority**: The AI extraction endpoint is a core product feature and currently a stub returning hardcoded data — no real value is delivered.

**Independent Test**: POST content to the extraction endpoint, receive an immediate response with a document ID and PROCESSING status, poll the status endpoint until COMPLETED, verify Key Changes and Action Items are present in the result.

**Acceptance Scenarios**:

1. **Given** an authenticated user, **When** they submit content for extraction, **Then** they receive an immediate response with a document ID and PROCESSING status (response arrives in under 200ms regardless of AI model latency)
2. **Given** an extraction is in progress, **When** the user polls the status endpoint, **Then** they receive the current processing status
3. **Given** extraction is complete, **When** the user requests the result, **Then** they receive the extracted Key Changes and Action Items
4. **Given** the AI model returns a response containing both Key Changes and Action Items, **When** parsing completes, **Then** both sections are persisted and the document quality score is calculated as `(sectionsFound / 2) × min(1.0, contentLength / 500)`
5. **Given** the AI model returns a response missing one or both expected sections, **When** parsing runs, **Then** status is set to FAILED with a descriptive reason and the partial quality score reflects sections found
6. **Given** the AI model is rate-limited, **When** a request fails, **Then** the system retries up to 3 times with exponential backoff before setting status to FAILED

---

### User Story 4 — Complete Project CRUD (Priority: P2)

As a user, I want to create, list, update, and delete my projects so I can manage the set of repositories I am tracking.

**Why this priority**: Project management is foundational to the app but less urgent than billing — users can use read-only access temporarily.

**Independent Test**: Create a project, verify it appears in the list, update its name, delete it, verify it no longer appears in the list — all using authenticated requests as the project owner.

**Acceptance Scenarios**:

1. **Given** an authenticated user, **When** they create a project with a name and access control setting, **Then** the project is created and owned by them
2. **Given** an authenticated user with existing projects, **When** they list projects, **Then** they receive a paginated response of their own projects
3. **Given** the owner of a project, **When** they update the project's name or access control, **Then** the changes are persisted
4. **Given** the owner of a project, **When** they delete the project, **Then** the project is soft-deleted (hidden from all listings) but the record is preserved in the data store for recovery
5. **Given** a user who does not own a project, **When** they attempt to update or delete it, **Then** they receive a 403 Forbidden response

---

### User Story 5 — Webhook Idempotency (Priority: P2)

As the system, duplicate webhook deliveries from source control providers must be silently discarded so that events are processed exactly once.

**Why this priority**: Duplicate processing causes data corruption (double-provisioning, double-logging). Current code processes every delivery.

**Independent Test**: Send the same webhook payload twice; verify the processing log shows exactly one entry, and the second delivery returns an accepted status without dispatching.

**Acceptance Scenarios**:

1. **Given** a new webhook delivery, **When** it is received and its payload fingerprint has not been seen before, **Then** it is dispatched for processing normally
2. **Given** a duplicate webhook delivery with the same payload fingerprint, **When** it arrives, **Then** it is acknowledged with a 202 status but no dispatch occurs
3. **Given** an async webhook dispatch that throws an unhandled exception, **When** the exception is caught, **Then** the webhook event record's status is set to FAILED with the exception message stored, and the error is logged

---

### Edge Cases

- What happens when a Stripe event arrives for a user with no existing subscription record? → A new subscription record is created
- How does the system handle an AI response that is not parseable JSON or markdown? → Document status set to FAILED, reason stored, qualityScore = 0
- What happens if the billing provider cache key is evicted mid-request (Redis OOM)? → Falls back to live provider call; non-fatal
- What happens when a project owner's account is deleted while they have active projects? → Soft-deleted projects remain in the data store; access controlled by ownership FK
- What happens when the async executor queue is full (100 tasks backpressure)? → New extraction requests receive a 503 with a retry-after header

---

## Requirements

### Functional Requirements

- **FR-001**: System MUST process three billing provider subscription lifecycle events (`checkout.session.completed`, `invoice.paid`, `customer.subscription.deleted`) and update subscription records accordingly
- **FR-002**: System MUST verify the billing provider webhook signature before processing any event payload
- **FR-003**: System MUST deduplicate billing provider events — the same event ID MUST never be processed more than once
- **FR-004**: System MUST cache subscription state with a 5-minute expiry for standard and PRO tier users
- **FR-005**: System MUST bypass subscription cache for ENTERPRISE-tier users on every check — no cached state served
- **FR-006**: System MUST invalidate the cached subscription state for a user whenever their subscription changes via webhook
- **FR-007**: System MUST accept AI extraction requests asynchronously and return a tracking document ID immediately
- **FR-008**: System MUST retry failed AI provider calls up to 3 times with exponential backoff before marking extraction as FAILED
- **FR-009**: AI extraction quality score MUST be computed as `(sectionsFound / 2) × min(1.0, contentLength / 500)` where sectionsFound is the count of Key Changes and Action Items present
- **FR-010**: System MUST expose endpoints to create, list (paginated), update, and delete projects
- **FR-011**: Only the authenticated owner of a project MAY update or delete it; all other users MUST receive a 403 response
- **FR-012**: Project deletion MUST be a soft-delete; deleted projects MUST be invisible to all list and lookup queries
- **FR-013**: System MUST deduplicate webhook deliveries by payload fingerprint — duplicate deliveries MUST be acknowledged but not dispatched
- **FR-014**: Asynchronous webhook dispatch failures MUST update the event record's status to FAILED with the error message persisted, and MUST emit an ERROR-level log entry
- **FR-015**: System MUST expose a subscription creation/upsert endpoint for internal use by the webhook event handler
- **FR-016**: The source-control event payload stored in the database MUST be in a structured, queryable format (not plain text)
- **FR-017**: A synchronisation log table MUST be created to record each extraction/sync event with status, timestamps, and error context
- **FR-018**: The local development database schema mode MUST prevent automatic schema mutation; changes MUST require explicit migrations

### Key Entities

- **Subscription**: Tracks a user's billing tier (FREE / PRO / ENTERPRISE), status (ACTIVE / CANCELED), and links to the billing provider's customer and subscription identifiers
- **Processed Billing Event**: Records billing provider event IDs that have been handled; used to enforce idempotency
- **AI Extraction Document**: Tracks submitted content extraction requests with status (PROCESSING / COMPLETED / FAILED), result content, quality score, and error reason
- **Project**: Represents a repository tracked by the user, with a name, access control setting, owner reference, and soft-deletion timestamp
- **Sync Log**: Audit record for each synchronisation event tied to a project, capturing event type, status, timing, and error context

---

## Success Criteria

### Measurable Outcomes

- **SC-P2-1**: Users can initiate checkout and receive a real redirect URL to the payment provider's hosted checkout page within 2 seconds
- **SC-P2-2**: Subscription state is served from cache on the second check within the TTL window — zero external billing provider calls on cache hit
- **SC-P2-3**: AI extraction submission returns a response in under 200ms regardless of the AI model's processing latency
- **SC-P2-4**: Duplicate webhook deliveries with the same payload fingerprint produce exactly one event record — second delivery is acknowledged without dispatch
- **SC-P2-5**: AI extraction correctly identifies Key Changes and Action Items from the fixture corpus at 95% accuracy or higher
- **SC-P2-6**: Attempting to modify or delete a project as a non-owner returns a 403 Forbidden response 100% of the time

---

## Constitution Alignment

### Code Quality (Principle I)
- [x] No single method exceeds 40 lines; async handlers are extracted into dedicated service methods
- [x] Constructor injection used throughout; no field injection
- [x] All new public service methods have Javadoc

### Testing Standards (Principle II)
- [x] All test cases written before implementation (test-first mandatory per constitution)
- [x] Unit test coverage target: ≥70% line coverage
- [x] Integration tests required for: billing provider events, cache layer, AI async flow, CRUD ownership, webhook deduplication
- [x] TDD: tests written red → implement → green

### User Experience Consistency (Principle III)
- [x] All new API responses use explicit DTO types — no entity classes in response bodies
- [x] All new request DTOs carry Bean Validation annotations
- [x] Error responses follow the unified `ErrorResponse` schema from Phase 1

### Performance Requirements (Principle IV)
- [x] AI extraction endpoint: ≤200ms p95 response time (async delegation enforced)
- [x] Subscription cache hit path: ≤50ms (Redis lookup, no external call)
- [x] Billing webhook handler: returns within 200ms (async dispatch, Rule 07)

### Inviolable Rules
- **Rule 02**: Billing provider webhook signature verified as the first operation — no processing before verification
- **Rule 06**: Billing provider hosted checkout sessions used — no card data touches the server
- **Rule 07**: All webhook processing dispatched asynchronously — handler thread never blocks on extraction or subscription logic
- **Rule 09**: All new request/response DTOs carry `@Valid` and Bean Validation annotations

---

## Assumptions

- Phase 1 authentication is fully merged and operational; all `/api/v1/**` endpoints require a valid JWT — this Phase 2 spec inherits that constraint
- The billing provider SDK and OpenAI client libraries are configured purely via environment variables (`STRIPE_API_KEY`, `STRIPE_WEBHOOK_SECRET`, `OPENAI_API_KEY`) — no secrets in code or config files
- The AI prompt template content (extraction instructions) is stored externally as a resource file, not embedded as a string literal in source code
- The async task executor for AI extraction uses a bounded queue (capacity 100) to prevent unbounded memory growth under load
- For the MVP, the ENTERPRISE tier cache-bypass behaviour applies to subscription checks only — other ENTERPRISE-specific features (unlimited repos, etc.) are Phase 4 scope
- `ddl-auto: update` in the local development profile is an existing bug; this phase replaces it with `validate` to enforce migration-only schema changes going forward
- The fixture corpus for the 95% extraction accuracy gate already exists in the test resources directory from prior phases
