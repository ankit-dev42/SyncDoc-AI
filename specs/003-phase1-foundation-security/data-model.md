# Data Model: Phase 1 — Foundation Security

**Generated**: 2026-04-18
**Branch**: `003-phase1-foundation-security`
**Source**: `spec.md` FR-001 through FR-008, FR-015, FR-017, FR-018

---

## Overview

Phase 1 introduces two new persistent entities (`User`, `RefreshToken`) and one
response-only record (`ProjectDto`). Existing entities `Project` and `UserSubscription`
gain FK constraints to `users.id` via V6 migration.

---

## Entity: User

**Table**: `users`
**JPA class**: `com.syncdoc.collaboration.auth.model.User`
**Migration**: `V4__add_users_table.sql`

### Fields

| Column | Java Field | Type | Constraints | Notes |
|---|---|---|---|---|
| `id` | `id` | `UUID` | PK, `DEFAULT gen_random_uuid()` | Never expose raw UUID to clients — always as string in response |
| `email` | `email` | `String` | NOT NULL, UNIQUE | Validated with `@Email @NotBlank` on request DTO |
| `password_hash` | `passwordHash` | `String` | NOT NULL | BCrypt output (~60 chars); **never** included in any response DTO |
| `display_name` | `displayName` | `String` | nullable, max 100 | Optional at registration |
| `email_verified` | `emailVerified` | `boolean` | NOT NULL, DEFAULT FALSE | Stored; email verification flow is out of scope Phase 1 |
| `created_at` | `createdAt` | `Instant` | NOT NULL, DEFAULT NOW() | Set by `BaseEntity` / `@CreationTimestamp` |
| `updated_at` | `updatedAt` | `Instant` | NOT NULL, DEFAULT NOW() | Set by `BaseEntity` / `@UpdateTimestamp` |

### Indexes

| Index | Columns | Type | Purpose |
|---|---|---|---|
| `idx_users_email` | `email` | UNIQUE | Fast lookup on login; enforced uniqueness |

### Relationships

| Relationship | Target | Cardinality | FK Column |
|---|---|---|---|
| `refreshTokens` | `RefreshToken` | One-to-Many | `refresh_tokens.user_id` |
| `subscription` | `UserSubscription` | One-to-One (deferred) | `user_subscriptions.user_id` (V6) |
| `projects` | `Project` | One-to-Many (deferred) | `projects.owner_id` (V6) |

### Validation Rules (Request DTO)

`RegisterRequest`:
```java
record RegisterRequest(
  @Email @NotBlank String email,
  @NotBlank @Size(min = 8, max = 128) String password,
  @Size(max = 100) String displayName
) {}
```
- `password` min 8 characters (spec FR-002 implies BCrypt; 8-char minimum is the practical floor)
- `displayName` optional — `null` is stored as-is (nullable column)

### State Transitions

`emailVerified`: `false` → `true` (email verification flow, out of scope Phase 1)

---

## Entity: RefreshToken

**Table**: `refresh_tokens`
**JPA class**: `com.syncdoc.collaboration.auth.model.RefreshToken`
**Migration**: `V5__add_refresh_tokens_table.sql`

### Fields

| Column | Java Field | Type | Constraints | Notes |
|---|---|---|---|---|
| `id` | `id` | `UUID` | PK | |
| `user_id` | `userId` | `UUID` | NOT NULL, FK → `users.id` ON DELETE CASCADE | Enables family revocation via `deleteAllByUserId` |
| `hashed_token` | `hashedToken` | `String` | NOT NULL, UNIQUE, VARCHAR(64) | SHA-256 hex of raw UUID token; 64 chars |
| `expires_at` | `expiresAt` | `Instant` | NOT NULL | Set to `NOW() + 7 days` at creation |
| `revoked` | `revoked` | `boolean` | NOT NULL, DEFAULT FALSE | Set `true` on rotation or explicit logout |
| `replaced_at` | `replacedAt` | `Instant` | nullable | Set to `NOW()` when this token is rotated (enables ≤2s grace window) |
| `bound_user_agent` | `boundUserAgent` | `String` | nullable, VARCHAR(500) | `User-Agent` header at token issuance; must match on future refresh |
| `bound_ip` | `boundIp` | `String` | nullable, VARCHAR(45) | First `X-Forwarded-For` value (or remote address); must match on future refresh |
| `created_at` | `createdAt` | `Instant` | NOT NULL, DEFAULT NOW() | |

### Indexes

| Index | Columns | Type | Purpose |
|---|---|---|---|
| `idx_refresh_tokens_user` | `user_id` | Non-unique | Family revocation: `deleteAllByUserId` scan |
| `idx_refresh_tokens_hashed` | `hashed_token` | UNIQUE | O(log n) lookup on every refresh request |

### Validation Rules

- `hashedToken` must be exactly 64 hex characters (enforced in `JwtTokenService.hashToken()`)
- `expiresAt` must be in the future at creation time
- `boundIp` accepts IPv4 (max 15 chars), IPv4-mapped IPv6 (max 45 chars), and standard IPv6 (max 39 chars). VARCHAR(45) covers all cases.

### State Transitions

```
ACTIVE (revoked=false)
  │
  ├─ [rotation] ──► ROTATED (revoked=true, replaced_at=NOW())
  │                      │
  │                      └─ [within 2s grace] ──► re-accepted (no state change; serves new token)
  │                      └─ [after 2s] ──► REPLAY_DETECTED ──► ALL_FAMILY_REVOKED
  │
  ├─ [logout]   ──► REVOKED (revoked=true, replaced_at=null)
  │
  └─ [binding mismatch] ──► ALL_FAMILY_REVOKED
```

### Repository Methods

```java
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByHashedToken(String hashedToken);
    void deleteAllByUserId(UUID userId);
}
```

---

## Record: ProjectDto (Response Only)

**Type**: Java record (no persistence)
**JPA class**: N/A — response-only
**Replaces**: Direct return of `Project` entity in `ProjectController`

### Fields

| Field | Type | Source |
|---|---|---|
| `id` | `UUID` | `project.getId()` |
| `name` | `String` | `project.getName()` |
| `accessControl` | `String` | `project.getAccessControl()` |
| `createdAt` | `Instant` | `project.getCreatedAt()` |
| `updatedAt` | `Instant` | `project.getUpdatedAt()` |

**Intentionally excluded** from `ProjectDto` (present in `Project` entity):
- `owner` / `ownerId` — never expose
- `deletedAt` — soft-delete implementation detail
- Any JPA-relation field (`messages`, `threads`, etc.)
- `passwordHash` (not on Project, but as a reminder: entity fields must be audited)

**Mapping** (in `ProjectController`):
```java
new ProjectDto(
    project.getId(),
    project.getName(),
    project.getAccessControl().name(),
    project.getCreatedAt(),
    project.getUpdatedAt()
)
```

---

## Record: SubscriptionTierResponse (Updated)

**Current state** (BEFORE Phase 1):
```java
record SubscriptionTierResponse(String userId, String tier, String status, String stripeCustomerId)
```

**After Phase 1** (stripeCustomerId removed):
```java
record SubscriptionTierResponse(String userId, String tier, String status)
```

**Gap closed**: Gap 4.1 — `stripeCustomerId` leaked in API response.

---

## Record: ErrorResponse (New)

**Type**: Java record
**Package**: `com.syncdoc.collaboration.exception`
**Replaces**: `ApiResponse<Void>` for all error responses

```java
public record ErrorResponse(
    String error,          // machine-readable code: "VALIDATION_FAILED", "TOKEN_EXPIRED", etc.
    String message,        // human-readable description
    List<FieldError> details, // populated for 400 responses; empty list otherwise
    Instant timestamp,
    String path
) {
    public record FieldError(
        String field,
        Object rejectedValue,
        String message
    ) {}
}
```

### error Code Convention

| Code | HTTP Status | Trigger |
|---|---|---|
| `VALIDATION_FAILED` | 400 | `MethodArgumentNotValidException`, `ConstraintViolationException` |
| `INVALID_ARGUMENT` | 400 | `IllegalArgumentException` |
| `UNAUTHENTICATED` | 401 | Missing / invalid JWT (from `authenticationEntryPoint`) |
| `TOKEN_EXPIRED` | 401 | JWT expiry specifically (from `JwtAuthenticationFilter`) |
| `INVALID_TOKEN` | 401 | Malformed / tampered JWT |
| `TOKEN_REPLAYED` | 401 | Revoked refresh token presented outside grace window |
| `ACCESS_DENIED` | 403 | `AccessDeniedException` |
| `MULTI_TENANCY_VIOLATION` | 403 | `MultiTenancyViolationException` |
| `NOT_FOUND` | 404 | `ResourceNotFoundException` |
| `BUSINESS_VALIDATION` | 422 | `BusinessValidationException` |
| `INTERNAL_ERROR` | 500 | Unhandled `Exception` |

---

## Database Migration Summary

| Migration | V# | Change | Blocks |
|---|---|---|---|
| `V4__add_users_table.sql` | 4 | Create `users` table + index | `V5`, `V6`, `User` entity |
| `V5__add_refresh_tokens_table.sql` | 5 | Create `refresh_tokens` table + indexes | `RefreshToken` entity |
| `V6__add_user_fk_constraints.sql` | 6 | Add FK from `user_subscriptions` + `projects` → `users` | Referential integrity |

**Execution order**: V4 → V5 → V6 (enforced by Flyway version ordering)

**Risk note for V6**: If `user_subscriptions` or `projects` tables contain rows with
`user_id` / `owner_id` values that do not exist in `users`, the `ALTER TABLE` will fail
with a foreign key violation. In local dev/test environments the tables are empty — no
risk. In any pre-existing data environment, run a data audit first.
