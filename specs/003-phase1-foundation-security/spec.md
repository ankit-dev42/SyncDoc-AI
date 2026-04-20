# Feature Specification: Phase 1 — Foundation Security

**Feature Branch**: `003-phase1-foundation-security`
**Created**: 2026-04-18
**Status**: Draft
**Priority**: P0 — All blockers. Nothing else is testable until this phase is complete.

---

## Overview

Replace every foundational security stub with a production-quality implementation.
This phase is a P0 blocker — all downstream features depend on real authentication
and properly locked-down routes.

---

## User Scenarios & Testing

### User Story 1 — Authenticated Access (Priority: P1)

As a registered user, I want to log in and receive a JWT token so that I can
access all protected API routes securely without impersonation risk.

**Why this priority**: All other user stories and all downstream phases depend on
real identity. Without this, every route is either locked (blocked) or open
(insecure). It is the single hardest blocker.

**Independent Test**: Register an account, log in, call `GET /api/v1/projects` with
the returned token and verify 200; call the same endpoint without a token and verify
401.

**Acceptance Scenarios**:

1. **Given** valid email and password, **When** `POST /api/auth/register`, **Then** user is created and `201` returned with `{id, email, displayName}` — no `password_hash` field.
2. **Given** correct credentials, **When** `POST /api/auth/login`, **Then** access token returned in response body and refresh token set as `HttpOnly; SameSite=Strict` cookie.
3. **Given** a valid refresh cookie, **When** `POST /api/auth/refresh`, **Then** new access token issued, new refresh cookie issued, old refresh token revoked.
4. **Given** a revoked refresh token is replayed after rotation, **When** `POST /api/auth/refresh`, **Then** all tokens for that user are deleted (family revocation) and `401` returned.
5. **Given** two concurrent requests within 2 seconds of a rotation, **When** old token presented, **Then** accepted within grace period (< 2 s via `replaced_at`; boundary at exactly 2 s is rejected).
6. **Given** a refresh token bound to `User-Agent` A is presented with `User-Agent` B, **When** `POST /api/auth/refresh`, **Then** family revocation + `401`.
7. **Given** a valid refresh cookie, **When** `POST /api/auth/logout`, **Then** that refresh token is revoked and cookie cleared.
8. **Given** any `/api/v1/**` request without a valid JWT, **When** request arrives, **Then** `401 Unauthorized` with `ErrorResponse` body.
9. **Given** 11 auth requests from the same IP in 60 seconds, **When** the 11th request arrives, **Then** `429 Too Many Requests`.

---

### User Story 2 — Secure Route Configuration (Priority: P1)

As a security engineer, I want all routes to require authentication by default
so that no endpoint is accidentally exposed.

**Why this priority**: The current `anyRequest().permitAll()` makes the entire API public. This must be fixed in the same phase as authentication or the JWT work has no value.

**Independent Test**: Use `curl` with no token to hit three routes — a protected routes returns `401`, a webhook route returns `202`, Actuator health returns `200`.

**Acceptance Scenarios**:

1. **Given** no token, **When** `GET /api/v1/projects`, **Then** `401`.
2. **Given** no JWT but a valid HMAC signature, **When** `POST /api/v1/webhooks/github`, **Then** `202 Accepted`.
3. **Given** no token, **When** `GET /actuator/health`, **Then** `200 OK`.
4. **Given** no token, **When** `POST /api/v1/billing/stripe-webhook` (valid Stripe-Signature), **Then** accepted (not 401) — JWT exemption reserved in Phase 1, handler wired in Phase 2.
5. **Given** an expired access token, **When** any `/api/v1/**` call, **Then** `401` with `error: "TOKEN_EXPIRED"`.

---

### User Story 3 — Consistent Error Responses (Priority: P2)

As an API consumer, I want all error responses to follow the same JSON schema
so that my client can handle them uniformly.

**Why this priority**: Auth errors, validation errors, and business errors currently return mixed formats. Clients cannot parse them reliably. Needed before any frontend integration.

**Independent Test**: Trigger a `400`, a `401`, a `403`, a `404`, and a `422` — all must return JSON with the same top-level fields.

**Acceptance Scenarios**:

1. **Given** a `MethodArgumentNotValidException`, **When** handled, **Then** `400` with `{error, message, details: [{field, rejectedValue, message}], timestamp, path}`.
2. **Given** a `BusinessValidationException`, **When** handled, **Then** `422`.
3. **Given** an `AccessDeniedException`, **When** handled, **Then** `403`.
4. **Given** a `ResourceNotFoundException`, **When** handled, **Then** `404`.
5. **Given** only one `@RestControllerAdvice` class exists, **When** two exceptions overlap, **Then** one deterministic handler fires — no `AmbiguousExceptionHandlerException`.

---

### User Story 4 — Safe API Responses (Priority: P2)

As a backend developer, I want all API responses to use explicit DTOs
so that no JPA entity fields are accidentally leaked.

**Why this priority**: `stripeCustomerId` is currently visible to any authenticated user. Entity-level leaks are a compliance risk. Must be fixed before any user-facing UI launches.

**Independent Test**: Call `GET /api/v1/projects/{id}` and confirm the response JSON does not contain `owner`, `deletedAt`, or any field not in `ProjectDto`. Call the subscription endpoint and confirm `stripeCustomerId` is absent.

**Acceptance Scenarios**:

1. **Given** `GET /api/v1/projects/{id}`, **When** response parsed, **Then** only `{id, name, accessControl, createdAt, updatedAt}` present — no entity relations, no `deletedAt`.
2. **Given** any subscription response, **When** parsed, **Then** `stripeCustomerId` is absent; only `{tier, status, expiresAt}` present.
3. **Given** any controller method, **When** inspected, **Then** return type is a DTO record or `Page<DTO>`, never a JPA entity class.

---

### Edge Cases

- What happens when `JWT_SECRET` is shorter than 256 bits? → Startup throws `IllegalArgumentException` via `@PostConstruct`.
- What happens when `GITHUB_WEBHOOK_SECRET` is blank at startup? → `IllegalStateException` with `"GITHUB_WEBHOOK_SECRET must not be blank"`.
- What happens when two requests race on the same refresh token within the grace window? → Both succeed; the second response re-issues the same new token (idempotent within the window).
- What happens when a `ConstraintViolationException` is thrown outside a controller (e.g., service layer)? → `GlobalExceptionHandler` catches it and returns `400`.
- What happens when CORS `Origin` header is absent? → Spring Security default: request passes; CORS headers not set (non-browser client).

---

## Requirements

### Functional Requirements

**Authentication**

- **FR-001**: The system MUST replace `HeaderAuthenticationFilter` with `JwtAuthenticationFilter`; `HeaderAuthenticationFilter.java` MUST be deleted.
- **FR-002**: Access tokens MUST use HS256, expire in 15 minutes, and be returned in the response body.
- **FR-003**: Refresh tokens MUST be random UUIDs, stored as SHA-256 hashes in the `refresh_tokens` table, delivered as `HttpOnly; SameSite=Strict` cookies, and expire after 7 days.
- **FR-004**: Every `POST /api/auth/refresh` call MUST issue a new refresh token AND revoke the old one (strict rotation).
- **FR-005**: If a revoked refresh token is presented, the system MUST delete all refresh tokens for that user and return `401` (family revocation).
- **FR-006**: The old refresh token MUST be accepted for strictly less than 2 seconds after rotation (< 2 s, `replaced_at` column) to handle concurrent mobile requests — see plan.md AD-005 and data-model.md grace-period logic.
- **FR-007**: Refresh tokens MUST be bound to the issuing `User-Agent` and client IP; a mismatch MUST trigger family revocation and `401`.
- **FR-008**: All integration tests that previously set `X-User-Id` MUST be migrated to use `JwtTestTokenHelper.signedToken(userId)`; no `X-User-Id` header processing MUST remain.

**Route Security**

- **FR-009**: `WebSecurityConfig.filterChain()` MUST require authentication for all `/api/v1/**` routes except explicitly listed exemptions.
- **FR-010**: Exempt routes (no JWT required): `/api/auth/**`, `/api/v1/webhooks/**`, `/api/v1/billing/stripe-webhook`, `/actuator/health`, `/actuator/info`.
- **FR-011**: `RateLimitFilter` MUST be registered in the Spring Security filter chain before `UsernamePasswordAuthenticationFilter`.
- **FR-012**: Rate limits: `/api/auth/**` → 10 req/min per IP; all other `/api/v1/**` → 240 req/min per IP. Breaching returns `429`.
- **FR-013**: `CorsConfigurationSource` bean MUST read `FRONTEND_ORIGIN` env var; MUST NOT use wildcard `*`; MUST set `allowCredentials = true`; dev default `http://localhost:5173`.

**Exception Handling**

- **FR-014**: `BusinessValidationExceptionHandler.java` MUST be deleted; all exception mappings MUST live in a single `GlobalExceptionHandler.java` annotated `@Order(1)`.
- **FR-015**: All error responses MUST conform to: `record ErrorResponse(String error, String message, List<FieldError> details, Instant timestamp, String path)`.
- **FR-016**: Exception → HTTP status mapping: `BusinessValidationException` → 422, `AccessDeniedException` → 403, `ResourceNotFoundException` → 404, `MethodArgumentNotValidException` → 400 (with per-field `details`), `ConstraintViolationException` → 400.

**DTO Boundaries**

- **FR-017**: `ProjectDto` record MUST expose exactly `{id, name, accessControl, createdAt, updatedAt}`; no JPA entity, no relation field, no `deletedAt`.
- **FR-018**: `SubscriptionTierResponse` MUST expose `{tier, status, expiresAt}` only; `stripeCustomerId` and any other internal Stripe IDs MUST be removed.
- **FR-019**: No controller method MAY declare a JPA entity class as its return type; all returns MUST be DTO records or `Page<DTO>`.

**Webhook Secret**

- **FR-020**: `GitHubWebhookSignatureVerifier` MUST contain a `@PostConstruct` method that throws `IllegalStateException` if `GITHUB_WEBHOOK_SECRET` is blank.

### Key Entities

- **User**: `{id (UUID), email (unique), password_hash, display_name, email_verified, created_at, updated_at}`.
- **RefreshToken**: `{id (UUID), user_id (FK → users), hashed_token (unique), expires_at, revoked (bool), replaced_at (nullable), bound_user_agent, bound_ip, created_at}`.
- **ProjectDto** (response-only): `{id, name, accessControl, createdAt, updatedAt}`.

### Database Migrations Required

| Migration | Description |
|-----------|-------------|
| `V4__add_users_table.sql` | `users` table with email unique index |
| `V5__add_refresh_tokens_table.sql` | `refresh_tokens` table with `replaced_at`, `bound_user_agent`, `bound_ip` columns plus indexes |
| `V6__add_user_fk_constraints.sql` | FK from `user_subscriptions.user_id` and `projects.owner_id` → `users.id` |

---

## Success Criteria

| ID | Criterion | Measurement |
|----|-----------|-------------|
| SC-P1-1 | All `/api/v1/**` routes return `401` without a valid JWT | Automated route-matrix test covers every registered endpoint |
| SC-P1-2 | Auth endpoints enforce 10 req/min rate limit | Load test fires 15 req/min from one IP; requests 11–15 return `429` |
| SC-P1-3 | All API error responses match `ErrorResponse` schema | `GlobalExceptionHandlerTest` asserts schema on every exception type |
| SC-P1-4 | No JPA entity class appears in any controller return type | `ProjectControllerDtoTest` + `SubscriptionControllerDtoTest` |
| SC-P1-5 | Token refresh works without user re-authentication | `AuthControllerIntegrationTest`: login → refresh → access protected endpoint |
| SC-P1-6 | Application fails fast if `GITHUB_WEBHOOK_SECRET` is blank | `ApplicationStartupTest`: blank secret → context fails to load |
| SC-P1-7 | Revoked token replay triggers family revocation | `RefreshTokenFamilyRevocationTest`: replay returns `401`, all user tokens deleted |
| SC-P1-8 | Concurrent refresh within grace period succeeds | `RefreshTokenConcurrencyTest`: two calls within 2 s both return `200` |

---

## Constitution Alignment

| Rule | How This Phase Satisfies It |
|------|-----------------------------|
| Rule 01 | JWT validation fires on every request; no unauthenticated path to business logic |
| Rule 02 | GitHub HMAC still verified inside `GitHubWebhookSignatureVerifier`; webhook route exempt from JWT, not from signature check |
| Rule 03 | `ProjectDto` and cleaned `SubscriptionTierResponse` — no entity in API contract |
| Rule 04 | Feature gating in Phase 2 can now read real `userId` from `SecurityContext` |
| Rule 07 | Webhook handler route exempt from JWT; `@PostConstruct` validates secret at startup |
| Rule 09 | All new request DTOs carry `@Valid` + Bean Validation annotations |
| Section 5.1 | JWT 15-min access + 7-day HttpOnly refresh + rotation implemented |
| Section 5.1 | Rate limiting on auth endpoints active |

---

## Assumptions

- `JWT_SECRET` and `GITHUB_WEBHOOK_SECRET` are injected via environment variables; no fallback default is acceptable in any profile.
- IP-binding for refresh tokens uses the value from `X-Forwarded-For` (first entry) when behind a reverse proxy; raw remote address otherwise.
- IP binding is a hard fail (same as User-Agent binding); both must match.
- Password hashing uses BCrypt with default strength (10 rounds); no other algorithm.
- `email_verified` flag is stored but email verification flow is out of scope for Phase 1.

---

## Dependencies & Out of Scope

**Depends on**: Nothing — this is Phase 1 (baseline).

**Out of scope for Phase 1** (addressed in later phases):
- `StripeClientImpl`, `BillingController`, Stripe webhook handler (Phase 2)
- Redis subscription cache (Phase 2)
- Login/Register UI pages (Phase 3)
- CI pipeline and coverage enforcement (Phase 3)

