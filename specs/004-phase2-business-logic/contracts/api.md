# API Contracts: Phase 2 — Business Logic

**Generated**: 2026-04-20  
**Base URL**: `/api/v1`  
**Auth**: All endpoints require `Authorization: Bearer <jwt>` unless noted as public.  
**Error schema**: All errors use the unified `ErrorResponse` from Phase 1:
```json
{
  "error": "ERROR_CODE",
  "message": "Human readable message",
  "details": [],
  "timestamp": "2026-04-20T12:00:00Z",
  "path": "/api/v1/..."
}
```

---

## Billing Endpoints (`/api/v1/billing`)

### POST `/api/v1/billing/checkout`

**Purpose**: Create a Stripe Checkout Session for a PRO subscription upgrade.  
**Auth**: Required (JWT)  
**Request Content-Type**: `application/json`

**Request Body**:
```json
{
  "priceId": "price_1234abcd"
}
```

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `priceId` | `String` | `@NotBlank` | Stripe price ID from the pricing table |

**Response `200 OK`**:
```json
{
  "success": true,
  "message": "Checkout session created",
  "data": {
    "checkoutUrl": "https://checkout.stripe.com/c/pay/cs_test_...",
    "sessionId": "cs_test_..."
  }
}
```

**Error cases**:
- `400` — missing or blank `priceId`
- `503` — Stripe API unavailable

---

### GET `/api/v1/billing/portal`

**Purpose**: Create a Stripe Customer Portal session for subscription management.  
**Auth**: Required (JWT)

**Response `200 OK`**:
```json
{
  "success": true,
  "message": "Portal session created",
  "data": {
    "portalUrl": "https://billing.stripe.com/session/..."
  }
}
```

**Error cases**:
- `404` — user has no `UserSubscription` record (no Stripe customer ID)
- `503` — Stripe API unavailable

---

### POST `/api/v1/billing/stripe-webhook`

**Purpose**: Receive and process Stripe lifecycle events.  
**Auth**: **Public** — signature verified via `Stripe-Signature` header (not JWT).  
**Request Content-Type**: `application/json` (raw body MUST be preserved before parsing)

**Headers**:
| Header | Required | Notes |
|---|---|---|
| `Stripe-Signature` | Yes | Verified by `Webhook.constructEvent()` as the FIRST operation |

**Supported events**:
| Event | Effect |
|---|---|
| `checkout.session.completed` | Create `UserSubscription` with `tier=PRO`, `status=ACTIVE` |
| `invoice.paid` | Update `UserSubscription.status` to `ACTIVE` |
| `customer.subscription.deleted` | Update `UserSubscription.status` to `CANCELED` |

**Response `200 OK`** (on valid event, including recognised duplicates):
```json
{ "success": true, "message": "Webhook acknowledged", "data": null }
```

**Response `200 OK`** (on duplicate — event ID already in `processed_stripe_events`):
Same body as above — return 200, no re-processing.

**Error cases**:
- `400` — invalid `Stripe-Signature` (signature verification failed)
- `400` — unrecognised event type (log and return 200 to avoid Stripe retries; non-actionable events are not errors)

> **Note**: For unrecognised event types, return 200 rather than 400 to prevent Stripe
> from retrying indefinitely. The handler logs an INFO entry and returns successfully.

---

## AI Extraction Endpoints (`/api/v1/ai`)

### POST `/api/v1/ai/extract`

**Purpose**: Submit content for async AI extraction. Returns immediately with a tracking ID.  
**Auth**: Required (JWT)  
**Response time guarantee**: ≤200ms regardless of OpenAI model latency (async delegation).

**Request Body**:
```json
{
  "sourceContent": "Raw diff or commit content to extract documentation from",
  "sourceContentId": "commit-sha-abc123"
}
```

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `sourceContent` | `String` | `@NotBlank`, `@Size(max = 100000)` | Content to extract |
| `sourceContentId` | `String` | `@NotBlank`, `@Size(max = 50)` | Client-provided identifier for the content |

**Response `202 Accepted`**:
```json
{
  "success": true,
  "message": "Extraction submitted",
  "data": {
    "docId": "d4e5f6a7-...",
    "status": "PROCESSING"
  }
}
```

**Error cases**:
- `400` — blank `sourceContent` or `sourceContentId`
- `503` — async executor queue full (capacity 100)

---

### GET `/api/v1/ai/extract-status/{docId}`

**Purpose**: Poll the processing status of a submitted extraction.  
**Auth**: Required (JWT)

**Path Parameters**:
| Param | Type | Constraints |
|---|---|---|
| `docId` | `String` | `@NotBlank` |

**Response `200 OK`**:
```json
{
  "success": true,
  "message": "Extraction status retrieved",
  "data": {
    "docId": "d4e5f6a7-...",
    "status": "PROCESSING"
  }
}
```

**Possible `status` values**: `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`

**Error cases**:
- `404` — `docId` not found

---

### GET `/api/v1/ai/extract-result/{docId}`

**Purpose**: Retrieve the completed extraction result.  
**Auth**: Required (JWT)

**Response `200 OK`**:
```json
{
  "success": true,
  "message": "Extraction result retrieved",
  "data": {
    "docId": "d4e5f6a7-...",
    "status": "COMPLETED",
    "keyChanges": "## Key Changes\n- Added rate limiting\n- Updated JWT secret rotation",
    "actionItems": "## Action Items\n- Review security config\n- Deploy to staging",
    "qualityScore": 0.94
  }
}
```

**Error cases**:
- `404` — `docId` not found
- `409` — extraction not yet `COMPLETED` (status is `PENDING`, `PROCESSING`, or `FAILED`)

---

## Project Endpoints (`/api/v1/projects`)

### POST `/api/v1/projects`

**Purpose**: Create a new project owned by the authenticated user.  
**Auth**: Required (JWT)

**Request Body**:
```json
{
  "name": "syncdoc-backend",
  "accessControl": "PRIVATE"
}
```

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `name` | `String` | `@NotBlank`, `@Size(max = 255)` | Project display name |
| `accessControl` | `String` | `@NotNull` | `PRIVATE` or `SHARED` |

**Response `201 Created`**:
```json
{
  "success": true,
  "message": "Project created",
  "data": {
    "id": "a1b2c3d4-...",
    "name": "syncdoc-backend",
    "accessControl": "PRIVATE",
    "createdAt": "2026-04-20T10:00:00Z",
    "updatedAt": "2026-04-20T10:00:00Z"
  }
}
```

**Error cases**:
- `400` — validation failure

---

### GET `/api/v1/projects`

**Purpose**: List paginated projects owned by the authenticated user.  
**Auth**: Required (JWT)

**Query Parameters**:
| Param | Type | Default | Constraints |
|---|---|---|---|
| `page` | `int` | `0` | `≥ 0` |
| `size` | `int` | `20` | `1–100` |

**Response `200 OK`**:
```json
{
  "success": true,
  "message": "Projects retrieved",
  "data": {
    "content": [
      {
        "id": "a1b2c3d4-...",
        "name": "syncdoc-backend",
        "accessControl": "PRIVATE",
        "createdAt": "2026-04-20T10:00:00Z",
        "updatedAt": "2026-04-20T10:00:00Z"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "page": 0,
    "size": 20
  }
}
```

---

### PUT `/api/v1/projects/{id}`

**Purpose**: Update a project's name or access control. Owner only.  
**Auth**: Required (JWT)

**Request Body**:
```json
{
  "name": "syncdoc-backend-v2",
  "accessControl": "SHARED"
}
```

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `name` | `String` | `@NotBlank`, `@Size(max = 255)` | |
| `accessControl` | `String` | `@NotNull` | `PRIVATE` or `SHARED` |

**Response `200 OK`**: Updated `ProjectDto`

**Error cases**:
- `400` — validation failure
- `403` — caller is not the project owner
- `404` — project not found (or soft-deleted)

---

### DELETE `/api/v1/projects/{id}`

**Purpose**: Soft-delete a project. Owner only.  
**Auth**: Required (JWT)

**Response `204 No Content`**

**Error cases**:
- `403` — caller is not the project owner
- `404` — project not found (already deleted)

---

## Subscription Endpoints (`/api/v1/subscriptions`)

### POST `/api/v1/subscriptions`

**Purpose**: Create or upsert a subscription record. Called internally by the
Stripe webhook handler after processing events.  
**Auth**: Required (JWT) — will be called as the system user via webhook processing context.

**Request Body**:
```json
{
  "userId": "user-uuid",
  "tier": "PRO",
  "status": "ACTIVE",
  "stripeCustomerId": "cus_abc123",
  "stripeSubscriptionId": "sub_xyz789"
}
```

| Field | Type | Constraints | Notes |
|---|---|---|---|
| `userId` | `String` | `@NotBlank` | Target user |
| `tier` | `String` | `@NotNull` | `FREE`, `PRO`, or `ENTERPRISE` |
| `status` | `String` | `@NotNull` | `ACTIVE` or `CANCELED` |
| `stripeCustomerId` | `String` | `@NotBlank` | |
| `stripeSubscriptionId` | `String` | nullable | Absent for FREE tier |

**Response `200 OK`** (updated) or **`201 Created`** (new):
```json
{
  "success": true,
  "message": "Subscription upserted",
  "data": {
    "userId": "user-uuid",
    "tier": "PRO",
    "status": "ACTIVE"
  }
}
```

---

## DTO Reference

### `CreateCheckoutRequest`
```java
public record CreateCheckoutRequest(
    @NotBlank String priceId
) {}
```

### `CheckoutResponse`
```java
public record CheckoutResponse(
    String checkoutUrl,
    String sessionId
) {}
```

### `PortalResponse`
```java
public record PortalResponse(String portalUrl) {}
```

### `ExtractionSubmitRequest`
```java
public record ExtractionSubmitRequest(
    @NotBlank @Size(max = 100000) String sourceContent,
    @NotBlank @Size(max = 50) String sourceContentId
) {}
```

### `ExtractionSubmitResponse`
```java
public record ExtractionSubmitResponse(String docId, String status) {}
```

### `CreateProjectRequest`
```java
public record CreateProjectRequest(
    @NotBlank @Size(max = 255) String name,
    @NotNull Project.AccessControl accessControl
) {}
```

### `UpdateProjectRequest`
```java
public record UpdateProjectRequest(
    @NotBlank @Size(max = 255) String name,
    @NotNull Project.AccessControl accessControl
) {}
```

### `UpsertSubscriptionRequest`
```java
public record UpsertSubscriptionRequest(
    @NotBlank String userId,
    @NotNull UserSubscription.SubscriptionTier tier,
    @NotNull UserSubscription.SubscriptionStatus status,
    @NotBlank String stripeCustomerId,
    String stripeSubscriptionId
) {}
```
