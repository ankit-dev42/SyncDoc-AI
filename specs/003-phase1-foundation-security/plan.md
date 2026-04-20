# Implementation Plan: Phase 1 — Foundation Security

**Branch**: `003-phase1-foundation-security` | **Date**: 2026-04-18 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/003-phase1-foundation-security/spec.md`

## Summary

Replace every foundational security stub with a production-quality implementation.
The current codebase trusts an unauthenticated `X-User-Id` header, leaves all routes
open via `anyRequest().permitAll()`, and has two conflicting `@RestControllerAdvice`
handlers. This plan implements real JWT authentication (jjwt / HS256), strict refresh
token rotation with family revocation and a 2-second concurrency grace period,
full route lockdown, unified exception handling, DTO boundary enforcement, and startup
validation for critical secrets. All ten implementation steps are ordered to minimise
blocked work: migrations first, then entities, then the JWT core, then the filter chain,
then the controller layer, then cleanup.

## Technical Context

**Language/Version**: Java 17 (LTS), Spring Boot 3.2.x  
**Primary Dependencies**: Spring Security 6, jjwt 0.12.x (jjwt-api / jjwt-impl / jjwt-jackson), Spring Data JPA, Flyway 10, BCrypt (via Spring Security)  
**Storage**: PostgreSQL 16 (primary), no cache layer required for Phase 1  
**Testing**: JUnit 5, Mockito, Spring Boot Test (`@SpringBootTest`), MockMvc  
**Target Platform**: Linux server (Docker Compose local; production TBD)  
**Project Type**: REST web-service (stateless JWT, no session)  
**Performance Goals**: API responses ≤200ms p95 (Constitution Rule IV); auth endpoints ≤100ms excluding BCrypt  
**Constraints**: BCrypt rounds = 10 (startup target ≤200ms); JWT secret ≥256 bits enforced at startup; no raw refresh token persisted  
**Scale/Scope**: Phase 1 provides the identity primitive for all subsequent phases; zero new external service calls introduced

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**For this feature, verify compliance with:**

- ✅ **Principle I (Code Quality)**: Module structure follows existing package conventions (`security`, `auth`, `exception`). `JwtTokenService` and `AuthController` are ≤40 lines per method; helper methods extracted where BCrypt/HS256 logic is dense. New engineer can trace request → filter → controller → service in ≤10 minutes via standard Spring filter chain ordering.
- ✅ **Principle II (Testing Standards)**: Seven test classes agreed before implementation begins (see Testing Strategy below). Integration tests cover every auth endpoint (`AuthControllerIntegrationTest`), every filter combination (`JwtAuthenticationFilterTest`, `WebSecurityConfigIntegrationTest`), and the rate limiter (`RateLimitFilterIntegrationTest`). Unit tests cover the pure JWT logic in isolation (`JwtTokenServiceTest`). Test-first order is mandatory: write → observe red → implement → green → refactor.
- ✅ **Principle III (UX Consistency)**: All error responses conform to `ErrorResponse` record (single schema). No UI impact in Phase 1; API error contract is the client-facing UX. i18n keys are deferred to Phase 3 (Login/Register pages not in this phase).
- ✅ **Principle IV (Performance)**: BCrypt cost factor 10 target ≈120ms on 2-GHz server — within the 200ms p95 budget for auth endpoints. HS256 signature verification is sub-millisecond. `hashed_token` index (`idx_refresh_tokens_hashed`) ensures `O(log n)` lookup for refresh rotation.

**Post-Phase 1 design re-check**: All four principles remain satisfied; no violations detected. See Complexity Tracking for the grace-period concurrency pattern (justified exception to simple rotation).

## Project Structure

### Documentation (this feature)

```text
specs/003-phase1-foundation-security/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   └── rest-endpoints.yml   # Phase 1 output — auth REST contract
└── tasks.md             # Phase 2 output (speckit.tasks command)
```

### Source Code Changes

```text
backend/
├── pom.xml                               # ADD: jjwt-api, jjwt-impl, jjwt-jackson (3 deps)
│
├── src/main/resources/db/migration/
│   ├── V4__add_users_table.sql           # NEW
│   ├── V5__add_refresh_tokens_table.sql  # NEW (+ replaced_at, bound_user_agent, bound_ip)
│   └── V6__add_user_fk_constraints.sql  # NEW
│
└── src/main/java/com/syncdoc/collaboration/
    │
    ├── auth/                             # NEW package
    │   ├── controller/
    │   │   └── AuthController.java       # NEW — /api/auth/{register,login,refresh,logout}
    │   ├── dto/
    │   │   ├── RegisterRequest.java      # NEW
    │   │   ├── LoginRequest.java         # NEW
    │   │   └── TokenResponse.java        # NEW — access token body response
    │   ├── model/
    │   │   ├── User.java                 # NEW — JPA entity
    │   │   └── RefreshToken.java         # NEW — JPA entity
    │   ├── repository/
    │   │   ├── UserRepository.java       # NEW
    │   │   └── RefreshTokenRepository.java # NEW
    │   └── service/
    │       ├── JwtTokenService.java      # NEW — generate / validate / parse
    │       └── AuthService.java          # NEW — register / login / refresh / logout
    │
    ├── security/                         # EXISTING package
    │   ├── JwtAuthenticationFilter.java  # NEW — replaces HeaderAuthenticationFilter
    │   ├── JwtTestTokenHelper.java       # NEW — test-only helper (src/test tree)
    │   └── RateLimitFilter.java          # EXISTING — no changes needed
    │
    ├── config/
    │   └── WebSecurityConfig.java        # REWRITE — route lockdown, CORS bean, filter order
    │
    ├── exception/
    │   ├── GlobalExceptionHandler.java   # REWRITE — merge + new ErrorResponse schema
    │   └── BusinessValidationExceptionHandler.java  # DELETE
    │
    ├── project/
    │   ├── controller/
    │   │   └── ProjectController.java    # UPDATE — return ProjectDto, not Project entity
    │   └── dto/
    │       └── ProjectDto.java           # NEW
    │
    ├── subscription/
    │   └── controller/
    │       └── SubscriptionController.java  # UPDATE — remove stripeCustomerId
    │
    ├── webhook/
    │   └── security/
    │       └── GitHubWebhookSignatureVerifier.java  # UPDATE — @PostConstruct startup check
    │
    └── tenancy/
        └── security/
            └── HeaderAuthenticationFilter.java  # DELETE
```

**Test tree additions** (`src/test/java/com/syncdoc/collaboration/`):

```text
├── auth/
│   ├── service/JwtTokenServiceTest.java          # Unit — pure JWT logic
│   └── controller/AuthControllerIntegrationTest.java  # @SpringBootTest end-to-end
├── security/
│   ├── JwtAuthenticationFilterTest.java          # MockMvc — valid/expired/missing/malformed
│   └── WebSecurityConfigIntegrationTest.java     # Route access matrix
├── ratelimit/
│   └── RateLimitFilterIntegrationTest.java       # Burst test
├── exception/
│   └── GlobalExceptionHandlerTest.java           # All exception types
├── project/
│   └── ProjectControllerDtoTest.java             # No entity in response
└── subscription/
    └── SubscriptionControllerDtoTest.java         # stripeCustomerId absent
```

**Structure Decision**: Web-application layout — `backend/` (Spring Boot REST API) with existing package-by-feature organisation. The new `auth` package follows the same pattern as `messaging`, `presence`, and `search`.

## Complexity Tracking

| Justification | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| Grace-period concurrency window (`replaced_at` + ≤2s acceptance) | Mobile clients fire concurrent requests on connection resume; without grace, legitimate users get 429 or 401 | Simple single-use rotation causes ~2% session-loss rate on mobile network switches |
| IP + User-Agent binding on refresh tokens | Prevents stolen-cookie replay from a different device/browser | Token rotation alone does not detect cross-device theft |
| Family revocation on replay detection | Limits blast radius when a refresh token is exfiltrated | Single-token revocation still leaves sibling tokens active |

---

## Architecture Decisions

### AD-001 — JWT Library: jjwt over spring-security-oauth2-resource-server

| Attribute | Detail |
|---|---|
| **Decision** | Use `io.jsonwebtoken:jjwt-api:0.12.6` (+ impl + jackson) |
| **Rationale** | SyncDoc AI does not have an external OAuth2 provider. Spring Security's resource-server support is optimised for Keycloak / Auth0 / Okta. Without an actual authorization server it adds ~600 lines of configuration with no benefit. jjwt is ≤30 lines for generation + ≤20 lines for validation. |
| **Alternatives Considered** | `spring-security-oauth2-resource-server` (over-engineered for internal auth); `nimbus-jose-jwt` (valid choice but larger API surface) |
| **pom.xml additions** | `jjwt-api`, `jjwt-impl` (runtime), `jjwt-jackson` (runtime) |

### AD-002 — Token Storage: Body + HttpOnly SameSite=Strict Cookie

| Attribute | Detail |
|---|---|
| **Decision** | Access token in JSON response body; refresh token in `HttpOnly; SameSite=Strict; Path=/api/auth/refresh` cookie |
| **Rationale** | Access token in body is readable by JS to attach to `Authorization: Bearer` headers. Refresh token in HttpOnly cookie is inaccessible to JS, protecting against XSS exfiltration. `SameSite=Strict` prevents CSRF on the refresh endpoint. `Path=/api/auth/refresh` restricts cookie transmission to the single rotation endpoint. |
| **Alternatives Considered** | Both in body (refresh token JS-readable → XSS risk); both in cookies (JS cannot read access token); localStorage (XSS risk) |

### AD-003 — Refresh Token Storage: SHA-256 Hash Only

| Attribute | Detail |
|---|---|
| **Decision** | Generate a random UUID refresh token; store only `SHA-256(token)` hex in `refresh_tokens.hashed_token` |
| **Rationale** | If the database is compromised, raw tokens allow immediate session hijacking. SHA-256 is non-reversible — an attacker with the hash cannot reconstruct the raw token. |
| **Alternatives Considered** | BCrypt hash (≥120ms per check — too slow for rotation); raw token (unacceptable under DB-breach threat model) |
| **Implementation note** | `MessageDigest.getInstance("SHA-256")` + `HexFormat.of().formatHex(digest)` — no extra library needed |

### AD-004 — Filter Chain Order: RateLimitFilter → JwtAuthenticationFilter → Authorization

| Attribute | Detail |
|---|---|
| **Decision** | `RateLimitFilter` before `UsernamePasswordAuthenticationFilter`; `JwtAuthenticationFilter` at `UsernamePasswordAuthenticationFilter.class` position |
| **Rationale** | Rate limiting must fire before any token parsing to prevent CPU-exhaustion via crafted-JWT floods. Placing `JwtAuthenticationFilter` at the `UsernamePasswordAuthenticationFilter` position is the Spring Security idiom for stateless JWT — it populates `SecurityContext` before authorization decisions. |
| **WebSecurityConfig** | `addFilterBefore(rateLimitFilter, ...)` then `addFilterAt(jwtAuthenticationFilter, ...)` |

### AD-005 — Exception Handler Merge Strategy

| Attribute | Detail |
|---|---|
| **Decision** | Keep `GlobalExceptionHandler.java`, delete `BusinessValidationExceptionHandler.java`, add `@Order(1)`, new `ErrorResponse` record |
| **Rationale** | Both handlers declare `@ExceptionHandler(MethodArgumentNotValidException.class)` and `@ExceptionHandler(ConstraintViolationException.class)`. Spring's resolution is non-deterministic across JVM upgrades. Merging eliminates the ambiguity. `BusinessValidationException → 422` (currently returns dynamic status). |
| **New ErrorResponse** | `record ErrorResponse(String error, String message, List<FieldError> details, Instant timestamp, String path)` replaces `ApiResponse<Void>` for all error cases |
| **Migration risk** | `BusinessValidationExceptionHandler` returns `ApiResponse<Map<String,String>>` with a `data` field; after merge it returns `ErrorResponse`. No frontend client exists yet — acceptable breakage. |

---

## Implementation Order

### Step 1 — Database Migrations (unblocks entity creation)

**Files to create** in `backend/src/main/resources/db/migration/`:

```sql
-- V4__add_users_table.sql
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(100),
  email_verified BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_users_email ON users(email);
```

```sql
-- V5__add_refresh_tokens_table.sql
CREATE TABLE refresh_tokens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  hashed_token VARCHAR(64) NOT NULL UNIQUE,
  expires_at TIMESTAMP NOT NULL,
  revoked BOOLEAN NOT NULL DEFAULT FALSE,
  replaced_at TIMESTAMP,
  bound_user_agent VARCHAR(500),
  bound_ip VARCHAR(45),
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE UNIQUE INDEX idx_refresh_tokens_hashed ON refresh_tokens(hashed_token);
```

```sql
-- V6__add_user_fk_constraints.sql
ALTER TABLE user_subscriptions
  ADD CONSTRAINT fk_user_subscriptions_user
  FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE projects
  ADD CONSTRAINT fk_projects_owner
  FOREIGN KEY (owner_id) REFERENCES users(id);
```

**Validation gate**: `mvn flyway:validate -pl backend` must pass before Step 3.

---

### Step 2 — pom.xml: jjwt Dependencies

Add inside `<dependencies>`:

```xml
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-api</artifactId>
  <version>0.12.6</version>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-impl</artifactId>
  <version>0.12.6</version>
  <scope>runtime</scope>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-jackson</artifactId>
  <version>0.12.6</version>
  <scope>runtime</scope>
</dependency>
```

Add `BCryptPasswordEncoder @Bean` to `WebSecurityConfig`.

---

### Step 3 — Entities + Repositories

**`User` entity** — `com.syncdoc.collaboration.auth.model.User`
- Fields: `id (UUID)`, `email`, `passwordHash`, `displayName`, `emailVerified`
- `@Table(name = "users")`, `@Column(unique = true)` on `email`
- Extends `BaseEntity` for `createdAt`/`updatedAt`

**`RefreshToken` entity** — `com.syncdoc.collaboration.auth.model.RefreshToken`
- Fields: `id`, `userId (UUID)`, `hashedToken`, `expiresAt`, `revoked`, `replacedAt`, `boundUserAgent`, `boundIp`
- `@Table(name = "refresh_tokens")`

**`UserRepository`** — `findByEmail(String email): Optional<User>`

**`RefreshTokenRepository`**:
- `findByHashedToken(String hash): Optional<RefreshToken>`
- `deleteAllByUserId(UUID userId): void`

---

### Step 4 — JwtTokenService (tested in isolation)

**Class**: `com.syncdoc.collaboration.auth.service.JwtTokenService`

| Method | Description |
|---|---|
| `generateAccessToken(String userId): String` | HS256, 15-min expiry, `sub = userId` |
| `generateRefreshTokenRaw(): String` | `UUID.randomUUID().toString()` |
| `hashToken(String raw): String` | SHA-256 hex — stored in DB |
| `validateAccessToken(String token): Claims` | Verify signature + expiry; throw on failure |
| `extractUserId(String token): String` | `claims.getSubject()` |

`@PostConstruct` validates `JWT_SECRET` is ≥32 characters.

---

### Step 5 — JwtAuthenticationFilter

**Class**: `com.syncdoc.collaboration.security.JwtAuthenticationFilter` extends `OncePerRequestFilter`

1. Extract `Authorization: Bearer <token>`; if absent → `chain.doFilter()` and return
2. `jwtTokenService.validateAccessToken(token)` — on `JwtException` → write 401 `ErrorResponse`
3. On success → set `SecurityContextHolder` with `UsernamePasswordAuthenticationToken(userId, null, [])`
4. `chain.doFilter()`

---

### Step 6 — AuthController + AuthService

| Route | Method | Auth | Description |
|---|---|---|---|
| `POST /api/auth/register` | none | `RegisterRequest {email, password, displayName}` | Create user; return `201 {id, email, displayName}` |
| `POST /api/auth/login` | none | `LoginRequest {email, password}` | Return `{accessToken}` + `Set-Cookie: refreshToken` |
| `POST /api/auth/refresh` | none | Cookie `refreshToken` | Rotate tokens; apply grace-period + binding checks |
| `POST /api/auth/logout` | none | Cookie `refreshToken` | Revoke token; clear cookie |

**Refresh logic** (condensed):
1. Hash incoming cookie value → look up by `hashedToken`
2. If `revoked = true` AND `replaced_at` is null OR `NOW() - replaced_at > 2s` → family revocation → 401
3. If `revoked = true` AND `NOW() - replaced_at ≤ 2s` → grace window → re-issue same-rotation token
4. Check `boundUserAgent` + `boundIp` → mismatch → family revocation → 401
5. Mark old token revoked, set `replaced_at = NOW()` → issue new raw token → hash → persist

---

### Step 7 — WebSecurityConfig Overhaul

Complete rewrite of `filterChain()`:

```java
.authorizeHttpRequests(authz -> authz
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers("/api/v1/webhooks/**").permitAll()
    .requestMatchers("/api/v1/billing/stripe-webhook").permitAll()
    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
    .requestMatchers("/ws/**").permitAll()
    .anyRequest().authenticated()
)
.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
.addFilterAt(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
```

`CorsConfigurationSource` bean reads `FRONTEND_ORIGIN` env var (default `http://localhost:5173`).
No wildcard `*`. `allowCredentials = true`.

---

### Step 8 — GlobalExceptionHandler Merge + ErrorResponse

New `ErrorResponse` record:
```java
record ErrorResponse(String error, String message, List<FieldError> details,
                     Instant timestamp, String path) {
    record FieldError(String field, Object rejectedValue, String message) {}
}
```

Merge `BusinessValidationException → 422` from `BusinessValidationExceptionHandler`.
Add `@Order(1)`. All handlers return `ErrorResponse`. Delete `BusinessValidationExceptionHandler.java`.

---

### Step 9 — ProjectDto + SubscriptionTierResponse Cleanup + Delete HeaderAuthenticationFilter

- Create `ProjectDto(UUID id, String name, String accessControl, Instant createdAt, Instant updatedAt)`
- Update `ProjectController.getProject()` to return `ApiResponse<ProjectDto>`
- Update `SubscriptionController.SubscriptionTierResponse` record: remove `stripeCustomerId`
- **Delete** `HeaderAuthenticationFilter.java`

---

### Step 10 — GitHubWebhookSignatureVerifier @PostConstruct

```java
@PostConstruct
public void validateConfiguration() {
    if (webhookSecret == null || webhookSecret.isBlank()) {
        throw new IllegalStateException(
            "GITHUB_WEBHOOK_SECRET must not be blank. " +
            "Set it via the GITHUB_WEBHOOK_SECRET environment variable."
        );
    }
}
```

---

## Testing Strategy

| Test Class | Type | Scope | Success Criterion |
|---|---|---|---|
| `JwtTokenServiceTest` | Unit (no Spring) | `JwtTokenService` | Token generation, expiry, tampered signature, hash determinism |
| `AuthControllerIntegrationTest` | `@SpringBootTest` | Full auth flow | register → login → access → refresh → logout; replay → family revocation; grace window; binding mismatch |
| `JwtAuthenticationFilterTest` | `@WebMvcTest` + MockMvc | Filter | Valid/expired/missing/malformed → correct HTTP status + `error` field |
| `WebSecurityConfigIntegrationTest` | `@SpringBootTest` | Route access matrix | All FR-009/FR-010 routes tested with/without token |
| `RateLimitFilterIntegrationTest` | `@SpringBootTest` | Rate limiter | 11 req in 60s from one IP → requests 11+ return 429 |
| `GlobalExceptionHandlerTest` | MockMvc | Handler | 6 exception types → correct status + `ErrorResponse` schema |
| `ProjectControllerDtoTest` | MockMvc | Controller response | No entity field in response; shape matches `ProjectDto` exactly |
| `SubscriptionControllerDtoTest` | MockMvc | Controller response | `stripeCustomerId` absent |
| `RefreshTokenFamilyRevocationTest` | `@SpringBootTest` | Family revocation | Replay → 401 + all user tokens deleted (SC-P1-7) |
| `RefreshTokenConcurrencyTest` | `@SpringBootTest` | Grace window | Two calls within 2s → both 200 (SC-P1-8) |
| `ApplicationStartupTest` | `@SpringBootTest` | Startup validation | Blank `GITHUB_WEBHOOK_SECRET` → context load failure (SC-P1-6) |

**Test helper**: `JwtTestTokenHelper.signedToken(UUID userId)` in `src/test/java` — creates a signed JWT using the test `JWT_SECRET`. All existing `X-User-Id` header usages in tests must be migrated to this helper.

---

## Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| V6 FK constraints fail on existing orphaned rows | Medium | High | Run `V6` only after verifying seed data is clean; add `IF EXISTS` guard |
| BCrypt cost 10 exceeds 200ms on slow CI | Low | Low | `@TestPropertySource(properties = "security.bcrypt.strength=4")` in test profile |
| Grace-period boundary race at exactly 2s | Low | Medium | Use `<` (strict less-than) on comparator; `replaced_at` set atomically in same transaction |
| `WorkspaceMembershipFilter` reads `SecurityContext` — must call `authentication.getName()` not cast to `String` | High | High | Update `WorkspaceMembershipFilter` before deleting `HeaderAuthenticationFilter` |

---

## Post-Phase Constitution Re-Check

| Principle | Status after design |
|---|---|
| I — Code Quality | All service methods ≤40 lines. `AuthService.refresh()` extracts `validateRefreshToken()` helper. |
| II — Testing Standards | 11 test classes defined before implementation. Critical paths (auth, rotation, family revocation, startup) at 100% coverage target. |
| III — UX Consistency | All error responses use `ErrorResponse` record. No UI work in Phase 1. |
| IV — Performance | BCrypt cost 10 ≈120ms. JWT validation sub-millisecond. Refresh lookup via `UNIQUE INDEX` on `hashed_token`: O(log n). |
