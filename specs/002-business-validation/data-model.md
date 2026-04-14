# Data Model: SyncDoc AI Business Validation & Deployment Readiness

**Input**: Specification from [spec.md](../spec.md) and research findings from [research.md](./research.md)  
**Phase**: Phase 1 — Entity definitions, relationships, and persistence design  
**Version**: 1.0  
**Date**: 2026-04-14  

---

## Entity Definitions

### 1. UserSubscription

Tracks subscription tier, entitlement, and Stripe integration for a user.

```java
@Entity
@Table(name = "user_subscriptions", indexes = {
    @Index(name = "idx_user_subscriptions_user_id", columnList = "user_id", unique = true),
    @Index(name = "idx_user_subscriptions_status", columnList = "status")
})
public class UserSubscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false, unique = true)
    private String userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionTier tier; // ENUM: FREE, PRO, ENTERPRISE
    
    @Column(nullable = false)
    private String stripeCustomerId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status; // ENUM: ACTIVE, CANCELED, EXPIRED, PAST_DUE
    
    @Column(nullable = false, updatedDate = true)
    private LocalDateTime createdAt;
    
    @Column(updatable = true)
    private LocalDateTime renewedAt;
    
    @Column
    private LocalDateTime expiresAt;
}

public enum SubscriptionTier {
    FREE(1, "One synced repository"),
    PRO(Integer.MAX_VALUE, "Unlimited repositories"),
    ENTERPRISE(Integer.MAX_VALUE, "Unlimited + SSO");
    
    private final int maxRepositories;
    private final String description;
}

public enum SubscriptionStatus {
    ACTIVE("Subscription is current"),
    CANCELED("User cancelled; grace period may apply"),
    EXPIRED("Subscription lapsed; restore available"),
    PAST_DUE("Payment pending; downgrade risk");
}
```

**Validation Rules**:
- `userId` must match an authenticated user in auth layer
- `stripeCustomerId` must be valid Stripe customer ID
- `tier` must be one of defined enum values
- `status` must be one of defined enum values

**Queries**:
- `find(userId)`: Return subscription for user (implements `Optional<UserSubscription>`)
- `findByStatus(status)`: Return all subscriptions with given status (for reporting)

---

### 2. Project

User-owned project with documentation scope and access control.

```java
@Entity
@Table(name = "projects", indexes = {
    @Index(name = "idx_project_owner", columnList = "owner_id"),
    @Index(name = "idx_project_created", columnList = "created_at")
})
public class Project {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String ownerId; // maps to authenticated user.id
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccessControl accessControl; // ENUM: PRIVATE, SHARED
    
    @Column(nullable = false, updatedDate = true)
    private LocalDateTime createdAt;
    
    @Column(updatable = true)
    private LocalDateTime updatedAt;
}

public enum AccessControl {
    PRIVATE("Only owner can view/edit documentation"),
    SHARED("Invited collaborators can view (future feature)");
}
```

**Validation Rules**:
- `ownerId` must match authenticated user
- `name` must be non-empty and ≤255 characters
- `accessControl` must be one of enum values

**Queries**:
- `findById(projectId)`: Return single project
- `findByOwnerId(userId)`: Return all projects owned by user

---

### 3. WebhookEvent

Log of received webhook deliveries for audit and debugging.

```java
@Entity
@Table(name = "webhook_events", indexes = {
    @Index(name = "idx_webhook_source_status", columnList = "source, status"),
    @Index(name = "idx_webhook_created", columnList = "created_at")
})
public class WebhookEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WebhookSource source; // ENUM: GITHUB
    
    @Column(nullable = false, length = 64)
    private String payloadHash; // SHA-256 hash of received payload for idempotency
    
    @Column(columnDefinition = "JSONB")
    private String payloadSnapshot; // Snapshot of webhook payload for debugging
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WebhookStatus status; // ENUM: ACCEPTED, REJECTED
    
    @Column(length = 255)
    private String rejectionReason; // If REJECTED, why (e.g., "Invalid HMAC")
    
    @Column(nullable = false, updatedDate = true)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime dispatchedAt; // Timestamp when event was processed
}

public enum WebhookSource {
    GITHUB("GitHub repository webhooks");
}

public enum WebhookStatus {
    ACCEPTED("Valid signature; enqueued for processing"),
    REJECTED("Invalid signature or malformed payload");
}
```

**Validation Rules**:
- `payloadHash` must be 64-character hex string (SHA-256)
- `status` must be ACCEPTED or REJECTED
- If REJECTED, `rejectionReason` must be populated

**Queries**:
- `findByPayloadHash(hash)`: Idempotency check (ensure same webhook not processed twice)
- `findByStatus(status)`: Return all accepted/rejected webhooks for audit

---

### 4. GeneratedDocumentation

Extracted AI output from OpenAI processing.

```java
@Entity
@Table(name = "generated_documentation", indexes = {
    @Index(name = "idx_generated_doc_user", columnList = "user_id"),
    @Index(name = "idx_generated_doc_status", columnList = "status")
})
public class GeneratedDocumentation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String userId; // User who submitted content for extraction
    
    @Column(nullable = false, length = 50)
    private String sourceContentId; // Reference to raw upload/document
    
    @Column(columnDefinition = "TEXT")
    private String sourceContent; // Raw input to OpenAI
    
    @Column(columnDefinition = "TEXT")
    private String keyChanges; // Extracted "Key Changes" section
    
    @Column(columnDefinition = "TEXT")
    private String actionItems; // Extracted "Action Items" section
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus status; // ENUM: PENDING, COMPLETED, FAILED
    
    @Column
    private Double qualityScore; // Human or automated quality metric (0.0 - 1.0)
    
    @Column(columnDefinition = "TEXT")
    private String processingError; // If FAILED, error message from OpenAI
    
    @Column(nullable = false, updatedDate = true)
    private LocalDateTime createdAt;
    
    @Column(updatable = true)
    private LocalDateTime completedAt;
}

public enum ProcessingStatus {
    PENDING("Awaiting OpenAI processing"),
    COMPLETED("Extraction successful"),
    FAILED("OpenAI returned error or timeout");
}
```

**Validation Rules**:
- `userId` must match authenticated user
- `status` must be one of enum values
- `qualityScore` if populated must be between 0.0 and 1.0
- If FAILED, `processingError` must be populated

**Queries**:
- `findByUserId(userId)`: Return all extraction history for user
- `findByStatus(status)`: Return pending/completed extractions for monitoring

---

## Relationships & Constraints

```
User (from auth layer)
  ├─→ UserSubscription (1:1) — each user has exactly one subscription record
  ├─→ Project* (1:N) — user owns zero or more projects
  ├─→ GeneratedDocumentation* (1:N) — user creates zero or more AI extractions
  └─→ WebhookEvent (indirect) — events are logged globally, not per-user

Project
  └─→ owner_id references User.id (no explicit FK; enforced in service)

WebhookEvent
  └─→ No direct FK; logs all events regardless of user
```

**Foreign Key Constraints**:
- `UserSubscription.userId` → `{auth_user}.id` (ON DELETE CASCADE)
- `Project.ownerId` → `{auth_user}.id` (ON DELETE CASCADE)
- `GeneratedDocumentation.userId` → `{auth_user}.id` (ON DELETE CASCADE)

---

## State Machines

### SubscriptionStatus Lifecycle

```
ACTIVE  ←→  PAST_DUE  →  EXPIRED
  ↓                        ↑
CANCELED (no restore)     (restore possible)
```

**Transitions**:
- ACTIVE → PAST_DUE: Payment method fails during renewal
- PAST_DUE → ACTIVE: Payment recovered or manual retry
- PAST_DUE → EXPIRED: Grace period expired without payment
- ACTIVE → CANCELED: User cancels subscription
- EXPIRED → ACTIVE: User restores subscription (creates new billing cycle)
- CANCELED: Terminal state (no restore without new signup)

### ProcessingStatus Lifecycle

```
PENDING  →  COMPLETED
  ↓           ↑
  └─→ FAILED ─┘
```

**Transitions**:
- PENDING → COMPLETED: OpenAI processing succeeded; sections extracted
- PENDING → FAILED: OpenAI returned error (timeout, rate limit, malformed response)
- FAILED → Never retried automatically (manual intervention required per design)

### WebhookStatus (no transitions)

- ACCEPTED: Valid signature; immutable
- REJECTED: Invalid signature; immutable

---

## Database Indexes

| Table | Index | Purpose |
|---|---|---|
| `user_subscriptions` | `idx_user_subscriptions_user_id` (UNIQUE) | PK lookup; enforce 1:1 user-subscription |
| `user_subscriptions` | `idx_user_subscriptions_status` | Query all PAST_DUE for billing operations |
| `projects` | `idx_project_owner` | List user's projects |
| `projects` | `idx_project_created` | Pagination by creation date |
| `webhook_events` | `idx_webhook_source_status` | Audit accepted/rejected by source |
| `webhook_events` | `idx_webhook_created` | Historical queries by timestamp |
| `generated_documentation` | `idx_generated_doc_user` | User's extraction history |
| `generated_documentation` | `idx_generated_doc_status` | Monitoring: pending count, failure rate |

---

## Caching Strategy

| Entity | Cache | TTL | Rationale |
|---|---|---|---|
| `UserSubscription` | Redis | 5 minutes | Frequent reads; user perceives change within minutes |
| `Project` | Redis (optional L2) | 1 hour | Less frequent than subscription; project ownership rarely changes |
| `WebhookEvent` | None | N/A | Immutable audit log; no cache needed |
| `GeneratedDocumentation` | None | N/A | Mostly written once; reads after completion are infrequent |

---

## Migration Path (Flyway)

```sql
-- V1__initial_schema.sql
CREATE TABLE user_subscriptions (
    id UUID PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL UNIQUE,
    tier VARCHAR(20) NOT NULL,
    stripe_customer_id VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    renewed_at TIMESTAMP,
    expires_at TIMESTAMP
);

CREATE TABLE projects (
    id UUID PRIMARY KEY,
    owner_id VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    access_control VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE webhook_events (
    id UUID PRIMARY KEY,
    source VARCHAR(20) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    payload_snapshot JSONB,
    status VARCHAR(20) NOT NULL,
    rejection_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dispatched_at TIMESTAMP
);

CREATE TABLE generated_documentation (
    id UUID PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    source_content_id VARCHAR(50) NOT NULL,
    source_content TEXT,
    key_changes TEXT,
    action_items TEXT,
    status VARCHAR(20) NOT NULL,
    quality_score DECIMAL(3,2),
    processing_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);
```

---

## Next Steps

→ Phase 1 (continued): Generate API contracts in `contracts/` and quickstart guide
