# Tasks: Phase 1 — Foundation Security

**Input**: Design documents from `/specs/003-phase1-foundation-security/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/rest-endpoints.yml ✅, quickstart.md ✅
**Branch**: `003-phase1-foundation-security`
**Generated**: 2026-04-18

**Tests**: Test tasks are MANDATORY (Principle II: Test-First Development). Write tests first, confirm red, then implement.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

---

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no incomplete dependencies)
- **[Story]**: Maps to user story from spec.md (US1–US4)
- Exact file paths included in every task description

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add dependencies and create DB migrations. MUST complete before any entity creation.

**⚠️ CRITICAL**: Flyway migrations must apply cleanly before Phase 2 entities compile.

- [X] T001 Add jjwt-api, jjwt-impl (runtime), jjwt-jackson (runtime) v0.12.6 to `backend/pom.xml`
- [X] T002 Create Flyway migration `backend/src/main/resources/db/migration/V4__add_users_table.sql` — `users` table with `id UUID PK`, `email UNIQUE`, `password_hash`, `display_name`, `email_verified`, `created_at`, `updated_at` and `idx_users_email` unique index
- [X] T003 Create Flyway migration `backend/src/main/resources/db/migration/V5__add_refresh_tokens_table.sql` — `refresh_tokens` table with `id UUID PK`, `user_id FK→users(id) ON DELETE CASCADE`, `hashed_token VARCHAR(64) UNIQUE`, `expires_at`, `revoked DEFAULT FALSE`, `replaced_at` (nullable), `bound_user_agent VARCHAR(500)`, `bound_ip VARCHAR(45)`, `created_at`; indexes `idx_refresh_tokens_user` and `idx_refresh_tokens_hashed`
- [X] T004 Create Flyway migration `backend/src/main/resources/db/migration/V6__add_user_fk_constraints.sql` — add FK `fk_user_subscriptions_user` on `user_subscriptions.user_id → users.id` and `fk_projects_owner` on `projects.owner_id → users.id`

**Checkpoint**: Run `mvn flyway:validate -pl backend` — must exit 0 before Phase 2

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Domain entities, repositories, auth DTOs, and test helper that ALL user stories depend on.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T005 Create `User` JPA entity at `backend/src/main/java/com/syncdoc/collaboration/auth/model/User.java` — fields: `id UUID`, `email`, `passwordHash`, `displayName`, `emailVerified`; `@Table("users")`; extends `BaseEntity` for `createdAt`/`updatedAt`; `@Column(unique=true)` on `email`
- [X] T006 Create `UserRepository` at `backend/src/main/java/com/syncdoc/collaboration/auth/repository/UserRepository.java` — `extends JpaRepository<User, UUID>` with `Optional<User> findByEmail(String email)`
- [X] T007 Create `RefreshToken` JPA entity at `backend/src/main/java/com/syncdoc/collaboration/auth/model/RefreshToken.java` — fields: `id UUID`, `userId UUID`, `hashedToken`, `expiresAt Instant`, `revoked boolean`, `replacedAt Instant` (nullable), `boundUserAgent`, `boundIp`; `@Table("refresh_tokens")`
- [X] T008 Create `RefreshTokenRepository` at `backend/src/main/java/com/syncdoc/collaboration/auth/repository/RefreshTokenRepository.java` — `extends JpaRepository<RefreshToken, UUID>` with `Optional<RefreshToken> findByHashedToken(String hash)` and `void deleteAllByUserId(UUID userId)`
- [X] T009 [P] Create Auth DTOs: `RegisterRequest` (`@Email @NotBlank email`, `@NotBlank @Size(min=8,max=128) password`, `@Size(max=100) displayName`), `LoginRequest` (`email`, `password`), `TokenResponse` (`String accessToken`) in `backend/src/main/java/com/syncdoc/collaboration/auth/dto/`
- [X] T010 Create `JwtTestTokenHelper` in `backend/src/test/java/com/syncdoc/collaboration/security/JwtTestTokenHelper.java` — `public static String signedToken(UUID userId)` that generates a valid HS256 JWT using the test `JWT_SECRET`; all integration tests will call this to replace `X-User-Id` header usage

**Checkpoint**: All entities compile; `JwtTestTokenHelper.signedToken()` returns a non-null JWT

---

## Phase 3: User Story 1 — Authenticated Access (Priority: P1) 🎯 MVP

**Goal**: Users can register, log in, receive JWT + HttpOnly refresh token, rotate tokens, and log out.
Revoked-token replay triggers family revocation. Concurrent requests within 2s grace window both succeed.

**Independent Test**: Register an account → log in → call `GET /api/v1/projects` with access token → expect 200;
call same endpoint without token → expect 401.

### Tests for User Story 1 (MANDATORY — write first, confirm red, then implement)

- [X] T011 [P] [US1] Create `JwtTokenServiceTest` at `backend/src/test/java/com/syncdoc/collaboration/auth/service/JwtTokenServiceTest.java` — unit tests (no Spring): valid-token generation, 15-min expiry, tampered-signature rejection, SHA-256 hash determinism, `@PostConstruct` rejects secret shorter than 32 chars
- [X] T012 [P] [US1] Create `JwtAuthenticationFilterTest` at `backend/src/test/java/com/syncdoc/collaboration/security/JwtAuthenticationFilterTest.java` — `@WebMvcTest` + MockMvc: valid token → 200, expired token → 401 `{"error":"TOKEN_EXPIRED"}`, missing header → 401, malformed JWT → 401
- [X] T013 [P] [US1] Create `AuthControllerIntegrationTest` at `backend/src/test/java/com/syncdoc/collaboration/auth/controller/AuthControllerIntegrationTest.java` — `@SpringBootTest`: register → 201 no `password_hash`; login → 200 + `Set-Cookie: refreshToken HttpOnly`; refresh → new access token + new cookie; logout → 200 + cookie cleared; invalid password → 401; duplicate email → 409 (covers SC-P1-5)
- [X] T014 [P] [US1] Create `RefreshTokenFamilyRevocationTest` at `backend/src/test/java/com/syncdoc/collaboration/auth/RefreshTokenFamilyRevocationTest.java` — `@SpringBootTest`: replay revoked token after 2s → 401; verify all `refresh_tokens` rows for that user deleted (SC-P1-7); also add binding-mismatch scenario — User-Agent B presents token bound to User-Agent A → family revocation + 401 (covers FR-007)
- [X] T015 [P] [US1] Create `RefreshTokenConcurrencyTest` at `backend/src/test/java/com/syncdoc/collaboration/auth/RefreshTokenConcurrencyTest.java` — `@SpringBootTest`: two simultaneous `/api/auth/refresh` calls with same token within 2s → both return 200 with same new access token (SC-P1-8)

### Implementation for User Story 1

- [X] T016 [US1] Create `JwtTokenService` at `backend/src/main/java/com/syncdoc/collaboration/auth/service/JwtTokenService.java` — methods: `generateAccessToken(String userId)` (HS256, subject=userId, 15-min expiry), `generateRefreshTokenRaw()` (UUID), `hashToken(String raw)` (SHA-256 hex, 64 chars), `validateAccessToken(String token): Claims` (throws `JwtException` on failure), `extractUserId(Claims)`. `@PostConstruct` validates `JWT_SECRET` ≥ 32 characters
- [X] T017 [US1] Create `JwtAuthenticationFilter` at `backend/src/main/java/com/syncdoc/collaboration/security/JwtAuthenticationFilter.java` — `extends OncePerRequestFilter`: extract `Authorization: Bearer <token>`; if absent → pass through; `validateAccessToken()` on `JwtException` → write 401 `ErrorResponse` JSON to response; on success → `SecurityContextHolder.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, []))`
- [X] T018 [US1] Create `AuthService` at `backend/src/main/java/com/syncdoc/collaboration/auth/service/AuthService.java` — implement `register()` (BCryptPasswordEncoder, save User, return `{id, email, displayName}`) and `login()` (verify password, generate access token, generate refresh token, bind `User-Agent`+`clientIp`, persist hash, set `HttpOnly; SameSite=Strict; Path=/api/auth/refresh` cookie); Note: depends on T027 for `BCryptPasswordEncoder @Bean` — if implementation order shifts, `BCryptPasswordEncoder` declaration must precede this task
- [X] T019 [US1] Implement `AuthService.refresh()` in `backend/src/main/java/com/syncdoc/collaboration/auth/service/AuthService.java` — hash cookie value → look up by `hashedToken`; if `revoked=true` AND (`replacedAt` null OR `NOW()-replacedAt >= 2s`) → `deleteAllByUserId()` → throw 401; if `revoked=true` AND `NOW()-replacedAt < 2s` → re-issue same new-rotation token (grace window); check `boundUserAgent`+`boundIp` mismatch → family revocation → 401; mark old revoked + set `replacedAt=NOW()` → issue new raw token → hash → persist → return new access token + new cookie
- [X] T020 [US1] Implement `AuthService.logout()` in `backend/src/main/java/com/syncdoc/collaboration/auth/service/AuthService.java` — hash cookie value → find token → set `revoked=true` → save; write `Set-Cookie: refreshToken=; Max-Age=0; HttpOnly; Path=/api/auth/refresh` to clear cookie
- [X] T021 [US1] Create `AuthController` at `backend/src/main/java/com/syncdoc/collaboration/auth/controller/AuthController.java` — `@RestController @RequestMapping("/api/auth")`: `POST /register` → 201, `POST /login` → 200, `POST /refresh` → 200, `POST /logout` → 200; all endpoints `@PermitAll` (no JWT required); inject `AuthService`
- [X] T022 [US1] Update `WorkspaceMembershipFilter` at `backend/src/main/java/com/syncdoc/collaboration/tenancy/security/WorkspaceMembershipFilter.java` — replace any `(String) authentication.getPrincipal()` or `X-User-Id` header read with `authentication.getName()`; must compile and pass existing tests before next task
- [X] T023 [cleanup] Delete `HeaderAuthenticationFilter.java` at `backend/src/main/java/com/syncdoc/collaboration/tenancy/security/HeaderAuthenticationFilter.java` — remove the source file and any `@Import` or explicit filter registration *outside* `WebSecurityConfig.filterChain()`; do NOT touch `filterChain()` — that is owned by T027

**Checkpoint**: `POST /api/auth/register` → 201; `POST /api/auth/login` → 200 + cookie; `GET /api/v1/projects` with valid JWT → 200; replay revoked token → 401

---

## Phase 4: User Story 2 — Secure Route Configuration (Priority: P1)

**Goal**: All `/api/v1/**` routes require authentication by default. Webhooks, health, and auth routes are explicitly exempt. Rate limiter enforces 10 req/min on auth endpoints, 240 req/min elsewhere. CORS configured from env var.

**Independent Test**: `curl` with no token to `GET /api/v1/projects` → 401; `POST /api/v1/webhooks/github` (valid HMAC) → 202; `GET /actuator/health` → 200.

### Tests for User Story 2 (MANDATORY — write first, confirm red, then implement)

- [X] T024 [P] [US2] Create `WebSecurityConfigIntegrationTest` at `backend/src/test/java/com/syncdoc/collaboration/security/WebSecurityConfigIntegrationTest.java` — `@SpringBootTest`: no-token requests to all FR-009/FR-010 routes; protected routes → 401; exempt routes (`/api/auth/**`, `/api/v1/webhooks/**`, `/api/v1/billing/stripe-webhook`, `/actuator/health`, `/actuator/info`) → not 401; expired token → 401 `{"error":"TOKEN_EXPIRED"}`; also assert CORS response headers: `Access-Control-Allow-Credentials: true` present; `Access-Control-Allow-Origin` matches `FRONTEND_ORIGIN` env var value, never `*`
- [X] T025 [P] [US2] Create `RateLimitFilterIntegrationTest` at `backend/src/test/java/com/syncdoc/collaboration/ratelimit/RateLimitFilterIntegrationTest.java` — `@SpringBootTest`: send 11 requests to `/api/auth/login` from same IP within 60s → requests 11+ return 429; send 241 requests to any `/api/v1/**` endpoint → 429 on 241st; also verify filter registration order in `WebSecurityConfig`: assert `RateLimitFilter` is registered before `UsernamePasswordAuthenticationFilter` (inspect filterChain bean or use `@TestPropertySource`); for the 241-req scenario, use a mock/test-clock `RateLimiter` rather than sending 241 real requests (prevents slow/flaky CI)
- [X] T026 [P] [US2] Create `ApplicationStartupTest` at `backend/src/test/java/com/syncdoc/collaboration/ApplicationStartupTest.java` — `@SpringBootTest`: confirm blank `GITHUB_WEBHOOK_SECRET` causes context load failure with `IllegalStateException("GITHUB_WEBHOOK_SECRET must not be blank")`; confirm blank `JWT_SECRET` (< 32 chars) causes `IllegalArgumentException` (SC-P1-6)

### Implementation for User Story 2

- [X] T027 [US2] Rewrite `WebSecurityConfig.filterChain()` in `backend/src/main/java/com/syncdoc/collaboration/config/WebSecurityConfig.java` — set `authorizeHttpRequests` with `permitAll` on `/api/auth/**`, `/api/v1/webhooks/**`, `/api/v1/billing/stripe-webhook`, `/actuator/health`, `/actuator/info`, `/ws/**`; `anyRequest().authenticated()`; `addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)`; `addFilterAt(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)`; disable `sessionManagement` (stateless); disable `csrf` (JWT-only API); add `BCryptPasswordEncoder @Bean`
- [X] T028 [US2] Add `CorsConfigurationSource @Bean` to `backend/src/main/java/com/syncdoc/collaboration/config/WebSecurityConfig.java` — read `FRONTEND_ORIGIN` env var, default `http://localhost:5173`; never wildcard `*`; `allowCredentials = true`; allowed methods GET/POST/PUT/DELETE/OPTIONS; register on `/**`
- [X] T029 [US2] Add `@PostConstruct validateConfiguration()` to `backend/src/main/java/com/syncdoc/collaboration/webhook/security/GitHubWebhookSignatureVerifier.java` — throw `IllegalStateException("GITHUB_WEBHOOK_SECRET must not be blank. Set it via the GITHUB_WEBHOOK_SECRET environment variable.")` if `webhookSecret == null || webhookSecret.isBlank()`

**Checkpoint**: All exempt routes return non-401; all other routes return 401 without JWT; 11th auth req in 60s → 429; blank webhook secret → startup failure

---

## Phase 5: User Story 3 — Consistent Error Responses (Priority: P2)

**Goal**: Every error response (4xx, 5xx) in the system uses a single `ErrorResponse` record. One exception handler exists; no ambiguity between conflicting advice classes.

**Independent Test**: Trigger a 400, 401, 403, 404, and 422 — all return `{error, message, details, timestamp, path}` with the same schema.

### Tests for User Story 3 (MANDATORY — write first, confirm red, then implement)

- [X] T030 [P] [US3] Create `GlobalExceptionHandlerTest` at `backend/src/test/java/com/syncdoc/collaboration/exception/GlobalExceptionHandlerTest.java` — MockMvc: `MethodArgumentNotValidException` → 400 with `details[]` per-field; `BusinessValidationException` → 422; `AccessDeniedException` → 403; `ResourceNotFoundException` → 404; `ConstraintViolationException` → 400; all responses have `error`, `message`, `timestamp`, `path`

### Implementation for User Story 3

- [X] T031 [US3] Create `ErrorResponse` record at `backend/src/main/java/com/syncdoc/collaboration/exception/ErrorResponse.java` — `record ErrorResponse(String error, String message, List<FieldError> details, Instant timestamp, String path)` with nested `record FieldError(String field, Object rejectedValue, String message)`
- [X] T032 [US3] Rewrite `GlobalExceptionHandler` at `backend/src/main/java/com/syncdoc/collaboration/exception/GlobalExceptionHandler.java` — add `@Order(1)`; add `@ExceptionHandler` for: `MethodArgumentNotValidException` → 400 (with per-field `details`), `ConstraintViolationException` → 400, `BusinessValidationException` → 422, `AccessDeniedException` → 403, `ResourceNotFoundException` → 404, `Exception` (fallback) → 500; all return `ErrorResponse`; remove any `ApiResponse<Map>` return types inherited from before
- [X] T033 [US3] Delete `BusinessValidationExceptionHandler.java` at `backend/src/main/java/com/syncdoc/collaboration/exception/BusinessValidationExceptionHandler.java`

**Checkpoint**: Only `GlobalExceptionHandler` exists; all 5 exception types return uniform `ErrorResponse` JSON

---

## Phase 6: User Story 4 — Safe API Responses (Priority: P2)

**Goal**: No JPA entity is returned from any controller. `ProjectDto` exposes only safe fields. `stripeCustomerId` is removed from subscription responses.

**Independent Test**: `GET /api/v1/projects/{id}` → response has only `{id, name, accessControl, createdAt, updatedAt}` — no `owner`, no `deletedAt`, no entity relations. Subscription endpoint → no `stripeCustomerId`.

### Tests for User Story 4 (MANDATORY — write first, confirm red, then implement)

- [X] T034 [P] [US4] Create `ProjectControllerDtoTest` at `backend/src/test/java/com/syncdoc/collaboration/project/ProjectControllerDtoTest.java` — MockMvc: `GET /api/v1/projects/{id}` response body matches exactly `{id, name, accessControl, createdAt, updatedAt}`; assert `owner`, `deletedAt`, and any `@Entity`-level fields are absent; also use reflection/classpath scan to assert ALL registered controllers in `backend/src/main/java/com/syncdoc/collaboration/` have no JPA entity class as a controller method return type (covers SC-P1-4 fully)
- [X] T035 [P] [US4] Create `SubscriptionControllerDtoTest` at `backend/src/test/java/com/syncdoc/collaboration/subscription/SubscriptionControllerDtoTest.java` — MockMvc: subscription tier response body contains exactly `{tier, status, expiresAt}`; assert `stripeCustomerId` is absent

### Implementation for User Story 4

- [X] T036 [US4] Create `ProjectDto` record at `backend/src/main/java/com/syncdoc/collaboration/project/dto/ProjectDto.java` — `record ProjectDto(UUID id, String name, String accessControl, Instant createdAt, Instant updatedAt)` with a static factory `ProjectDto.from(Project p)`
- [X] T037 [US4] Update `ProjectController.getProject()` at `backend/src/main/java/com/syncdoc/collaboration/project/controller/ProjectController.java` — change return type from `ApiResponse<Project>` (or bare `Project`) to `ApiResponse<ProjectDto>`; call `ProjectDto.from(project)`
- [X] T038 [US4] Update `SubscriptionTierResponse` record in `backend/src/main/java/com/syncdoc/collaboration/subscription/controller/SubscriptionController.java` — remove `stripeCustomerId` field; keep only `{tier, status, expiresAt}` exposed; update any `new SubscriptionTierResponse(...)` call sites to remove the argument

**Checkpoint**: Zero JPA entity types in any controller return type; no sensitive fields in any response body

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Test migration, final quality gate, constitution re-check.

- [X] T039 Migrate all existing integration test files that send `X-User-Id` header to use `JwtTestTokenHelper.signedToken(userId)` — search `backend/src/test/` for `.header("X-User-Id"` and replace with `Authorization: Bearer {JwtTestTokenHelper.signedToken(uuid)}`; `X-User-Id` must not appear in any remaining test; after migration, run `grep -r 'X-User-Id' backend/src/test/` and assert zero matches — this grep gate is a hard requirement in T041's checkpoint; the phase gate does NOT close until this grep returns exit code 1 (no matches = migration complete)
- [X] T040 Run full test suite — `mvn test -pl backend` — confirm all 11 test classes pass; fix any failures before proceeding
- [X] T041 Run `mvn verify -pl backend` — confirm build, compile, and all tests green; this is the phase completion gate

**Checkpoint**: `mvn verify` exits 0; all 11 test classes green; no `X-User-Id` header in any test file

---

## Dependency Graph

```
Phase 1 (Setup)
    └── Phase 2 (Foundational: entities + DTOs + JwtTestTokenHelper)
            ├── Phase 3 (US1: JWT + auth flow)  ← must complete before US2
            │       └── Phase 4 (US2: route lockdown, filter wiring, CORS)
            │
            ├── Phase 5 (US3: error handling)   ← independent after Phase 2
            │
            └── Phase 6 (US4: DTO boundaries)   ← independent after Phase 2
                        
All Phases → Phase 7 (Poland: test migration + quality gate)
```

**US1 must precede US2** because `JwtAuthenticationFilter` must exist before it can be registered in the filter chain.

**US3 and US4 are independent** of US1/US2 and of each other — they can be worked in parallel after Phase 2.

---

## Parallel Execution Examples

### Within Phase 3 (US1) — write all test stubs before any implementation
```
Developer A:   T011  →  T013  →  T016  →  T018  →  T020
Developer B:   T012  →  T014  →  T015  →  T017  →  T019  →  T021
```
Tests (T011–T015) are all [P] and can be created simultaneously.

### After Phase 2 — start US3 and US4 in parallel while US1 is in progress
```
Track A:  T011–T023 (US1: JWT core + auth controller)
Track B:  T030–T033 (US3: exception handler)
Track C:  T034–T038 (US4: DTO cleanup)
```
Then Track A flows into Phase 4 (US2) once done.

---

## Implementation Strategy

**MVP Scope** = Phase 1 + Phase 2 + Phase 3 (US1) only
→ Delivers: real JWT authentication, register/login/refresh/logout, token rotation, family revocation, grace window
→ Independently testable and deployable for downstream team unblocking

**Increment 2** = Phase 4 (US2: route lockdown) — converts open API to locked API
**Increment 3** = Phase 5 + Phase 6 in parallel (US3: errors, US4: DTOs)
**Increment 4** = Phase 7 (polish + quality gate)

---

## Test Class Inventory (11 total)

| # | Test Class | Phase | Type | Story |
|---|---|---|---|---|
| 1 | `JwtTokenServiceTest` | Phase 3 T011 | Unit (no Spring) | US1 |
| 2 | `JwtAuthenticationFilterTest` | Phase 3 T012 | `@WebMvcTest` | US1 |
| 3 | `AuthControllerIntegrationTest` | Phase 3 T013 | `@SpringBootTest` | US1 |
| 4 | `RefreshTokenFamilyRevocationTest` | Phase 3 T014 | `@SpringBootTest` | US1 |
| 5 | `RefreshTokenConcurrencyTest` | Phase 3 T015 | `@SpringBootTest` | US1 |
| 6 | `WebSecurityConfigIntegrationTest` | Phase 4 T024 | `@SpringBootTest` | US2 |
| 7 | `RateLimitFilterIntegrationTest` | Phase 4 T025 | `@SpringBootTest` | US2 |
| 8 | `ApplicationStartupTest` | Phase 4 T026 | `@SpringBootTest` | US2 |
| 9 | `GlobalExceptionHandlerTest` | Phase 5 T030 | MockMvc | US3 |
| 10 | `ProjectControllerDtoTest` | Phase 6 T034 | MockMvc | US4 |
| 11 | `SubscriptionControllerDtoTest` | Phase 6 T035 | MockMvc | US4 |

**Test helper**: `JwtTestTokenHelper` (Phase 2 T010) — shared across all integration tests; replaces all X-User-Id header usage.

---

## Constitution Compliance Checklist

- [ ] **Principle I (Code Quality)**: All service methods ≤40 lines; `AuthService.refresh()` extracts `validateRefreshToken()` helper; new engineer can trace request → filter → controller → service in ≤10 minutes
- [ ] **Principle II (Testing Standards)**: All 11 test classes exist and pass before phase closes; `JwtTestTokenHelper` replaces all `X-User-Id` header usages; no test uses `X-User-Id`
- [ ] **Principle III (UX Consistency)**: All error responses use `ErrorResponse` record; no mixed `ApiResponse<Map>` vs `ErrorResponse` schemas
- [ ] **Principle IV (Performance)**: BCrypt strength=10 in application.yml; strength=4 in test profile via `@TestPropertySource`; JWT validation sub-millisecond; `idx_refresh_tokens_hashed` unique index verified in V5 migration
