# Data Model: Phase 2 — Business Logic

**Generated**: 2026-04-20  
**Feature Branch**: `004-phase2-business-logic`  
**Status**: Final — no NEEDS CLARIFICATION items remain

---

## Overview

Phase 2 introduces two new entities, extends three existing entities, adds five
Flyway migrations (V7–V11), and corrects `application-local.yml`'s `ddl-auto`.
All schema changes are additive and backward-compatible with Phase 1 data.

---

## New Entities

### 1. `ProcessedStripeEvent`

**Purpose**: Deduplication table for Stripe webhook events. An event ID is written
here atomically after successful processing; the existence check before processing
enforces exactly-once semantics.

**Java class**: `com.syncdoc.collaboration.subscription.model.ProcessedStripeEvent`

| Field | Java Type | Column | Constraints |
|---|---|---|---|
| `stripeEventId` | `String` | `stripe_event_id VARCHAR(255)` | PRIMARY KEY, NOT NULL |
| `processedAt` | `Instant` | `processed_at TIMESTAMP` | NOT NULL, DEFAULT NOW() |

**Repository**: `ProcessedStripeEventRepository`
- `boolean existsByStripeEventId(String stripeEventId)`
- `void save(ProcessedStripeEvent event)`

**Flyway Migration**: `V11__stripe_idempotency.sql`

---

### 2. `SyncLog`

**Purpose**: Audit trail for every synchronisation (extraction/sync) event linked to
a project. Allows users and operators to diagnose failures per project.

**Java class**: `com.syncdoc.collaboration.sync.model.SyncLog`

| Field | Java Type | Column | Constraints |
|---|---|---|---|
| `id` | `String` (UUID) | `id UUID` | PRIMARY KEY, DEFAULT gen_random_uuid() |
| `projectId` | `String` | `project_id UUID` | NOT NULL, FK → projects.id |
| `eventType` | `String` | `event_type VARCHAR(50)` | NOT NULL |
| `githubEventId` | `String` | `github_event_id VARCHAR(100)` | nullable |
| `status` | `SyncStatus` | `status VARCHAR(20)` | NOT NULL, CHECK IN (PENDING, PROCESSING, COMPLETED, FAILED) |
| `startedAt` | `Instant` | `started_at TIMESTAMP` | NOT NULL, DEFAULT NOW() |
| `completedAt` | `Instant` | `completed_at TIMESTAMP` | nullable |
| `errorMessage` | `String` | `error_message TEXT` | nullable |
| `metadata` | `String` (JSON) | `metadata JSONB` | nullable |

**Repository**: `SyncLogRepository` (JpaRepository — queries by projectId and status)

**Flyway Migration**: `V9__add_sync_logs_table.sql`

---

## Modified Entities

### 3. `Project` — Soft Delete

**Change**: Add `deletedAt` field + Hibernate 6 soft-delete annotations.

**New field**:

| Field | Java Type | Column | Constraints |
|---|---|---|---|
| `deletedAt` | `Instant` | `deleted_at TIMESTAMP` | nullable |

**Entity annotation additions**:
```java
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE projects SET deleted_at = NOW() WHERE id = ?")
```

> **Note**: Use `@SQLRestriction` (Hibernate 6.3+) — NOT the deprecated `@Where`.
> Spring Boot 3.2.x ships Hibernate 6.4.x. Using `@Where` emits deprecation warnings.

**Flyway Migration**: `V10__projects_soft_delete.sql`

---

### 4. `WebhookEvent` — Status Extension + Error Message + JSONB

**Changes**:
1. Add `PROCESSING` and `FAILED` values to `WebhookStatus` enum (no migration — stored as VARCHAR)
2. Add `errorMessage TEXT` field
3. Change `payloadSnapshot` column type from `TEXT` to `JSONB` (V7 migration)

**New / modified fields**:

| Field | Java Type | Column | Change |
|---|---|---|---|
| `payloadSnapshot` | `String` | `payload_snapshot JSONB` | type change TEXT → JSONB (V7) |
| `errorMessage` | `String` | `error_message TEXT` | new field (add to V7 migration) |
| `status` | `WebhookStatus` | `status VARCHAR(20)` | add `PROCESSING`, `FAILED` variants |

**Extended enum**:
```java
public enum WebhookStatus {
    ACCEPTED,    // signature valid, dispatched
    REJECTED,    // signature invalid
    PROCESSING,  // async dispatch started
    FAILED       // async dispatch threw exception
}
```

**Repository addition**:
- `boolean existsByPayloadHash(String payloadHash)` — replaces `findByPayloadHash().isPresent()` at the controller layer

**Flyway Migrations**: `V7__fix_payload_snapshot_jsonb.sql`, `V8__add_payload_hash_unique_index.sql`

---

### 5. `GeneratedDocumentation` — Processing Status

**Change**: Add `PROCESSING` state to `ProcessingStatus` enum. The async extraction
handler sets status to `PROCESSING` when dispatch begins, so polling clients can
distinguish "queued" from "in progress".

**Extended enum**:
```java
public enum ProcessingStatus {
    PENDING,      // created but not yet submitted to async executor
    PROCESSING,   // @Async extraction running
    COMPLETED,    // AI returned valid sections
    FAILED        // AI failed after retries
}
```

> No database migration needed — stored as VARCHAR string.

---

### 6. `UserSubscription` — Implicit Additions via Webhook Handler

**Changes**: No new columns. The existing `stripeCustomerId`, `tier`, and `status`
fields are already present. `StripeWebhookHandler` will call `save(subscription)` to
create or update records when Stripe events are received.

---

## Entity Relationship Summary

```
users (Phase 1)
  ├── user_subscriptions (1:1, via userId)
  │     └── processed_stripe_events (independent, keyed by Stripe event ID)
  └── projects (1:N, via ownerId)
        └── sync_logs (1:N, via projectId)

webhook_events (standalone, keyed by payloadHash)
generated_documentation (standalone, keyed by userId + sourceContentId)
```

---

## Flyway Migration Plan

| Version | File | Description |
|---------|------|-------------|
| V7 | `V7__fix_payload_snapshot_jsonb.sql` | ALTER `webhook_events.payload_snapshot` to JSONB; ADD `error_message TEXT` |
| V8 | `V8__add_payload_hash_unique_index.sql` | CREATE UNIQUE INDEX on `webhook_events.payload_hash` |
| V9 | `V9__add_sync_logs_table.sql` | CREATE TABLE `sync_logs` with two indexes |
| V10 | `V10__projects_soft_delete.sql` | ALTER `projects` ADD COLUMN `deleted_at TIMESTAMP` |
| V11 | `V11__stripe_idempotency.sql` | CREATE TABLE `processed_stripe_events` |

**Config fix** (not a migration):
`backend/src/main/resources/application-local.yml`:
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate   # was: update
```

---

## Validation Rules

### `Project`
- `name`: `@NotBlank`, `@Size(max = 255)` (existing)
- `accessControl`: `@NotNull` (existing)
- `deletedAt`: set only via `@SQLDelete` — never set directly in application code

### `SyncLog`
- `projectId`: `@NotBlank` — FK must reference an existing project
- `eventType`: `@NotBlank`, `@Size(max = 50)`
- `status`: `@NotNull`
- `startedAt`: set in `@PrePersist` if null

### `ProcessedStripeEvent`
- `stripeEventId`: `@NotBlank`, `@Size(max = 255)`
- `processedAt`: set in `@PrePersist` if null

---

## State Machines

### `GeneratedDocumentation.ProcessingStatus`

```
PENDING ──► PROCESSING ──► COMPLETED
                 │
                 └──────────► FAILED
```

- `PENDING` → `PROCESSING`: set by `AIExtractionController` when async task is submitted
- `PROCESSING` → `COMPLETED`: set by `AIProcessingService` on successful parse
- `PROCESSING` → `FAILED`: set by `AIProcessingService` catch block after 3 retries

### `WebhookEvent.WebhookStatus`

```
ACCEPTED ──► PROCESSING ──► (no terminal state needed; event is consumed)
ACCEPTED                └──► FAILED
REJECTED (terminal)
```

- `ACCEPTED` → `PROCESSING`: set by `WebhookAuditService.recordAccepted()` before dispatch
- `PROCESSING` → `FAILED`: set by `WebhookAuditService.recordFailed()` in async catch
