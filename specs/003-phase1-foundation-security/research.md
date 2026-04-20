# Research: Phase 1 — Foundation Security

**Generated**: 2026-04-18
**Purpose**: Resolve all technical unknowns identified during Technical Context analysis
**Status**: Complete — no NEEDS CLARIFICATION items remain

---

## R-001 — JWT Library Selection

**Decision**: `io.jsonwebtoken:jjwt` version `0.12.6`

**Rationale**:
- jjwt 0.12.x ships a Builder/Parser API that is type-safe and does not require external
  libraries beyond jjwt-impl (runtime) + jjwt-jackson (runtime for JSON serialization).
- The API `Jwts.builder()` / `Jwts.parser()` is the de-facto standard for minimal-footprint
  stateless JWT in Spring Boot without an OAuth2 provider.
- Spring Security 6 resource-server support (`spring-security-oauth2-resource-server`) is
  designed for systems that delegate to a separate Authorization Server (Keycloak, Auth0,
  Okta). SyncDoc AI IS the authorization server — using oauth2-resource-server without an
  external AS requires implementing the full `JWKSource` + `JwtDecoder` chain (~600 additional
  lines of boilerplate).

**Alternatives Considered**:
- `nimbus-jose-jwt`: Equally valid, lower-level, requires more code for HS256. jjwt preferred
  for its opinionated builder API.
- `spring-security-oauth2-resource-server`: Correct tool for microservices with external IdP.
  Wrong tool for a self-contained monolith.
- `java-jwt` (Auth0): Less actively maintained in the Spring Boot ecosystem.

**Best Practices for jjwt 0.12.x**:
- Materialize `SecretKey` once at construction time from the `JWT_SECRET` env var using
  `Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))`.
- Use `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)` for validation.
- Catch `io.jsonwebtoken.JwtException` (parent of all jjwt exceptions) in the filter layer;
  map to 401 with a structured `ErrorResponse`.
- Never log the raw token or the secret key.

---

## R-002 — Token Storage Architecture

**Decision**: Access token in response body (`application/json`); refresh token in
`HttpOnly; SameSite=Strict; Path=/api/auth/refresh; Secure` cookie.

**Rationale**:
- OWASP recommends HttpOnly cookies for session tokens to prevent XSS token theft.
- `SameSite=Strict` eliminates CSRF on the refresh endpoint since cross-site POSTs will not
  send the cookie.
- `Path=/api/auth/refresh` restricts the cookie to a single path — the browser will not send
  it on `/api/v1/**` requests, reducing exposure surface.
- `Secure` flag ensures the cookie is only transmitted over HTTPS in production; omit for
  `http://localhost` dev profile only.
- Access token in body: the JS client reads it once, stores it in memory (Zustand's
  `authStore` in Phase 3), and attaches it as `Authorization: Bearer <token>` to every
  subsequent request. No localStorage — memory storage limits XSS blast radius to the
  current tab session.

**Refresh token cookie header example**:
```
Set-Cookie: refreshToken=<raw-uuid>; HttpOnly; SameSite=Strict; Path=/api/auth/refresh; Max-Age=604800
```

**Alternatives Considered**:
- Both tokens in body: refresh token readable by JS → XSS exfiltration risk.
- Both in cookies: access token not readable by JS without a cookie-based auth scheme
  (breaks the `Authorization: Bearer` pattern; requires session-like code in Spring Security).
- localStorage: Persists across tabs but is readable by any JS on the domain — rejected.

---

## R-003 — Refresh Token Hashing Strategy

**Decision**: Store only `SHA-256(rawToken)` as lowercase hex (64 characters).

**Rationale**:
- If the `refresh_tokens` table is exfiltrated, raw tokens would enable immediate account
  takeover. A SHA-256 hash is computationally irreversible; the attacker cannot derive the
  raw token from the stored hash.
- SHA-256 is appropriate here because:
  1. The inputs (UUID v4 strings) are already high-entropy (122 random bits) — no need
     for a salt; the randomness of the UUID makes precomputed rainbow tables ineffective.
  2. It is fast (sub-millisecond), while BCrypt/Argon2 are designed for low-entropy
     password hashing and are intentionally slow — inappropriate for a lookup-per-request
     path.
- Standard library only: `MessageDigest.getInstance("SHA-256")` (Java built-in) +
  `HexFormat.of().formatHex(digest)` (Java 17 `java.util.HexFormat`).

**Column type**: `VARCHAR(64)` — SHA-256 produces a 32-byte digest; hex-encoded = 64 chars.

**Alternatives Considered**:
- BCrypt (rejected: ~120ms per hash; unacceptable on 60 req/min rotation frequency).
- Raw UUID (rejected: DB breach = instant account takeover).
- Argon2 (rejected: same performance problem as BCrypt for this use case).

---

## R-004 — Filter Chain Order

**Decision**: `RateLimitFilter → JwtAuthenticationFilter → WorkspaceMembershipFilter`
— all registered before the authorization decision point.

**Rationale**:
- Rate limiting MUST precede token parsing. A malicious actor can craft arbitrarily long,
  syntactically valid JWTs to trigger long signature verification loops. Placing the rate
  limiter first protects CPU from this class of DoS.
- `JwtAuthenticationFilter` registers at `UsernamePasswordAuthenticationFilter.class` —
  the Spring Security idiom for stateless JWT. This position ensures `SecurityContext` is
  populated before `FilterSecurityInterceptor` evaluates authorization rules.
- `WorkspaceMembershipFilter` reads `SecurityContextHolder`; it must fire AFTER
  `JwtAuthenticationFilter` so it can resolve `authentication.getName()` to a real userId.
- `HeaderAuthenticationFilter` is deleted — its registration in the old `WebSecurityConfig`
  (`.addFilterBefore(..., AnonymousAuthenticationFilter.class)`) is removed.

**Spring Security registration**:
```java
.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
.addFilterAt(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
.addFilterAfter(workspaceMembershipFilter, JwtAuthenticationFilter.class)
```

---

## R-005 — Exception Handler Merge

**Decision**: Delete `BusinessValidationExceptionHandler.java`; merge all handlers into
`GlobalExceptionHandler.java` annotated `@RestControllerAdvice @Order(1)`.

**Problem Statement**:
Both `GlobalExceptionHandler` and `BusinessValidationExceptionHandler` declare:
- `@ExceptionHandler(MethodArgumentNotValidException.class)`
- `@ExceptionHandler(ConstraintViolationException.class)`

When two `@RestControllerAdvice` beans handle the same exception, Spring resolves which
fires using `@Order` or bean-definition order. Without explicit ordering this is
non-deterministic and has changed behaviour between Spring Framework minor versions.

**Difference analysis**:
- `GlobalExceptionHandler.handleValidation()` → returns `ApiResponse<Void>` string summary
- `BusinessValidationExceptionHandler.handleMethodArgumentNotValid()` → returns
  `ApiResponse<Map<String,String>>` with `field`, `error`, `path` keys

The new `ErrorResponse` record replaces both patterns with a single canonical schema.

**New `BusinessValidationException` mapping**: HTTP 422 (Unprocessable Entity) — this is
the correct semantic for failed domain validations. The current `BusinessValidationExceptionHandler`
resolves the status dynamically from `ex.getStatusCode()` which can produce 400, 409, or
422 inconsistently.

**`@Order(1)` justification**: Ensures `GlobalExceptionHandler` is always the highest-priority
advice bean, preventing any framework advice (e.g., `ResponseEntityExceptionHandler`) from
intercepting exceptions first.

---

## R-006 — CORS Configuration

**Decision**: `CorsConfigurationSource` bean; origin from `FRONTEND_ORIGIN` env var;
no wildcard; `allowCredentials = true`.

**Rationale**:
- `allowCredentials = true` is required for the `Set-Cookie` refresh token to be sent and
  received by the browser. The browser will reject `Set-Cookie` on a credentialed cross-origin
  request if `allowOrigins` contains `*` (CORS spec prohibits `* + credentials`).
- `FRONTEND_ORIGIN` env var enables environment-specific origins without code changes:
  - Local dev: `http://localhost:5173`
  - Staging: `https://staging.syncdocai.com`
  - Production: `https://app.syncdocai.com`
- `allowedMethods`: GET, POST, PUT, DELETE, OPTIONS. OPTIONS is mandatory for preflight.
- `allowedHeaders: ["*"]` — permits Authorization, Content-Type, and any custom headers.

**Startup guard**: If `FRONTEND_ORIGIN` is blank in production profile, the server should
log a WARN and fallback to the Spring Security default (which will block all cross-origin
requests) — this is acceptable behaviour; the application does not halt.

---

## R-007 — X-User-Id Migration Strategy

**Decision**: Hard cutover in Phase 1. No deprecation period.

**Rationale**:
- There is no production traffic against a live system. The codebase is pre-launch.
- A staged migration (supporting both `X-User-Id` and `JWT Bearer` simultaneously) would
  require keeping `HeaderAuthenticationFilter` alive, maintaining two authentication code
  paths, and creating ambiguous integration test coverage.
- Hard cutover is lower risk given zero production users.

**Migration steps**:
1. Create `JwtTestTokenHelper.signedToken(UUID userId)` in `src/test/java` — generates a
   signed JWT with the test `JWT_SECRET` property (`jwt.secret` in `application-test.yml`).
2. Find all test usages of `.header("X-User-Id", ...)` — replace with
   `.header("Authorization", "Bearer " + JwtTestTokenHelper.signedToken(userId))`.
3. Delete `HeaderAuthenticationFilter.java` and its associated test file.
4. Remove `tenancy.security.HeaderAuthenticationFilter` import from `WebSecurityConfig`.

**Estimated test files to migrate**: From codebase scan — `MessageController`, `PresenceRestController`,
`SearchRestController`, `ProjectController`, `SubscriptionController` integration tests
(approximately 5–8 test files).

---

## R-008 — Refresh Token Family Revocation Implementation

**Decision**: On detecting a revoked token replay, call `refreshTokenRepository.deleteAllByUserId(userId)`.

**Threat model**: An attacker has exfiltrated a refresh token from a previous rotation.
They attempt to use it. The server detects `revoked = true`. If only that one token is
revoked and the attacker holds a *sibling* (non-revoked) token from a subsequent rotation,
they retain access. Family revocation eliminates all tokens — the user must re-authenticate.

**Implementation (condensed)**:
```java
if (refreshToken.isRevoked()) {
    // Check grace window
    if (refreshToken.getReplacedAt() != null &&
        Duration.between(refreshToken.getReplacedAt(), Instant.now()).toSeconds() <= 2) {
        // Grace period — accept
        return reissueForGraceWindow(refreshToken);
    }
    // Outside grace period — revoked replay detected
    refreshTokenRepository.deleteAllByUserId(refreshToken.getUserId());
    throw new InvalidRefreshTokenException("TOKEN_REPLAYED");
}
```

**User experience**: The legitimate user is logged out and redirected to the login screen.
This is the correct security response — the session may be compromised.

---

## R-009 — Concurrency Grace Period

**Decision**: Accept a refresh token for ≤2 seconds after it has been rotated (soft expiry
via `replaced_at` column) to handle concurrent mobile/PWA refresh calls.

**Problem**: On mobile, when a user switches from WiFi to cellular, the OS may queue
multiple inflight requests. All arrive at `/api/auth/refresh` within milliseconds of each
other. Without a grace window, the first rotation succeeds and the subsequent ones present
a "revoked" token, triggering family revocation and logging out the user.

**Grace window logic**:
- `replaced_at` is set to `NOW()` at the moment a token is rotated.
- On subsequent requests with the old (revoked) token: if `NOW() - replaced_at <= 2s`, treat
  as an acceptable concurrent request. Return the same new token already issued (lookup the
  new token by `user_id` where `revoked = false` and `created_at = replaced_at`).
- The 2-second boundary is strict (`<` comparator, not `<=`) to avoid boundary ambiguity.

**Alternative**: Idempotency by tracking the `replaced_at` token and making the grace-window
response serve the *same* new token as the successful rotation. This prevents duplicate token
issuance.

---

## Summary of Resolved Unknowns

| Unknown | Resolution |
|---|---|
| JWT library choice | jjwt 0.12.6 — simpler than oauth2-resource-server for self-contained auth |
| Token storage | Body (access) + HttpOnly cookie (refresh) |
| Refresh token DB storage | SHA-256 hex hash only, never raw value |
| Filter chain order | RateLimit → JwtAuth → WorkspaceMembership |
| Exception handler conflict | Delete BusinessValidationExceptionHandler; merge into GlobalExceptionHandler @Order(1) |
| CORS | CorsConfigurationSource bean; FRONTEND_ORIGIN env var; no wildcard |
| X-User-Id migration | Hard cutover; JwtTestTokenHelper for tests |
| Family revocation | deleteAllByUserId on revoked token replay |
| Grace period concurrency | replaced_at column + ≤2s window; return same new token |
