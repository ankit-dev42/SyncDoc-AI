# SyncDoc AI — Project Constitution

**The Non-Negotiable Rules of Engineering**
Version 1.0 · April 2026

> AI-Powered Documentation Synchronization Platform
> Spring Boot 3 · Java 17 · React 18 · PostgreSQL · Stripe · OpenAI

---

## 1. Purpose & Scope

This document is the binding engineering constitution for SyncDoc AI — an AI-powered documentation synchronization platform. It defines the non-negotiable architectural decisions, coding standards, security principles, and quality expectations that govern all engineering work across all five sprints.

Every engineer, AI coding assistant (GitHub Copilot), and external contributor must read and adhere to this document before writing a single line of code. Violations of the Inviolable Rules section are grounds for PR rejection without review.

---

## 2. Architectural Style & Patterns

### 2.1 Overall Architecture

SyncDoc AI follows a layered, modular monolith architecture on the backend and a component-driven SPA on the frontend. The system is structured to be microservice-extractable in future, but is **not** a microservice architecture during the v1 build.

| Layer | Pattern / Responsibility |
|---|---|
| Presentation | React 18 SPA — renders UI, handles user interaction, manages local state |
| API Gateway | Spring Boot REST controllers — validates requests, delegates to services |
| Application | Service layer — orchestrates business logic, no direct DB access |
| Domain | JPA Entities + Repository interfaces — pure domain model |
| Infrastructure | PostgreSQL, Redis, OpenAI, Stripe, GitHub API adapters |

### 2.2 Backend Patterns

- Module-based package structure: `com.syncdoc.{module}.{layer}` (e.g. `com.syncdoc.auth.service`)
- Repository pattern via Spring Data JPA — no raw SQL in service classes
- Event-driven async processing via Spring Events + `@Async` for all webhook handling
- DTO pattern enforced — entities **never** exposed directly via API responses
- Constructor injection only — no `@Autowired` field injection
- Outbox pattern for Stripe webhook idempotency

### 2.3 Frontend Patterns

- Feature-based folder structure: `src/features/{feature}/{components,hooks,api}`
- React Query for all server state — no manual `fetch`/`useEffect` for data
- Zustand for global client state (auth session, UI state)
- Axios instance with interceptors — no raw `fetch()` calls
- Compound component pattern for complex UI (e.g. pricing table, billing portal)
- STOMP over WebSocket for real-time doc update notifications

---

## 3. Technology Stack Decisions

### 3.1 Rationale Summary

| Technology | Version | Rationale |
|---|---|---|
| Java | **17 (LTS)** | Stable LTS with records, sealed classes, text blocks — production proven |
| Spring Boot | 3.x | Auto-configuration, Spring AI integration, native compile-ready |
| Spring Security | 6.x | JWT + OAuth2 resource server out of the box |
| PostgreSQL | 16 | JSONB support for webhook payloads, ACID compliance |
| Redis | 7.x | L2 cache for project metadata, pub/sub for WebSocket scaling |
| Spring AI | 1.x | OpenAI abstraction layer — model-agnostic |
| React | 18 | Concurrent features, Suspense boundaries for data loading |
| TypeScript | 5.x | Strict mode enforced — no `ts-ignore` without justification |
| Tailwind CSS | 3.x | Utility-first, consistent design tokens |
| Vite | 5.x | Sub-second HMR, native ESM, optimal build output |
| Stripe | Latest SDK | PCI-compliant hosted checkout — no card data touches our servers |
| Docker + Compose | Latest | Reproducible local dev environment |
| JUnit 5 + Mockito | Current | Backend unit and integration testing |
| React Testing Library | Current | User-centric frontend tests |

### 3.2 Explicitly Excluded Technologies

The following are **not** permitted without written architectural approval:

- **Lombok** — use Java records and explicit constructors instead
- **MapStruct** — write explicit DTO mappers for clarity
- **`@Transactional` on controller layer** — services own transaction boundaries
- **Redux** — Zustand + React Query covers all state needs
- **Axios in components** — all API calls go through `src/api/` modules

---

## 4. Coding Standards & Conventions

### 4.1 Java / Spring Boot

- Google Java Style Guide enforced via Checkstyle in CI
- Class names: `PascalCase`. Methods/variables: `camelCase`. Constants: `UPPER_SNAKE_CASE`
- Max method length: 40 lines. Max class length: 300 lines. Extract if exceeded.
- All public methods on service classes must have Javadoc
- No magic numbers or strings — define named constants or use `application.yml`
- `@Value` for single config values. `@ConfigurationProperties` for grouped config.
- Never catch generic `Exception` — catch the specific exception type
- Use `Optional<T>` for nullable returns — never return `null` from service methods
- Custom exceptions extend `RuntimeException` with error code and message

### 4.2 TypeScript / React

- ESLint + Prettier enforced — CI fails on lint errors
- Strict TypeScript: no `any`, no `as Type` assertions without a comment explaining why
- Named exports only — no default exports except for route-level page components
- Component props typed with `interface`, not `type` alias
- Custom hooks prefixed with `use` — must live in their feature's `hooks/` folder
- No inline styles — use Tailwind classes. Exception: dynamic values via `style` prop
- All user-visible strings extracted to constants for future i18n
- `console.log` not permitted in committed code — use a logger utility

### 4.3 Git Workflow

- Branch naming: `feature/SD-{ticket}-short-description`, `fix/SD-{ticket}-description`
- Conventional Commits enforced: `feat:`, `fix:`, `chore:`, `docs:`, `test:`, `refactor:`, `ci:`
- PR requires 1 approval minimum + all CI checks green before merge
- Squash merge to main — preserve linear history
- No force-push to `main` or `develop` branches under any circumstance

---

## 5. Security Principles

### 5.1 Authentication & Authorization

- JWT access tokens: 15-minute expiry. Refresh tokens: 7-day expiry, stored `HttpOnly` cookie
- Refresh token rotation on every use — invalidate previous token
- GitHub OAuth2 tokens stored encrypted at rest (AES-256) — never logged
- Role-based access: `USER` and `ADMIN`. Every endpoint annotated with `@PreAuthorize`
- Rate limiting on all auth endpoints: 10 requests/minute per IP via Spring rate limiter

### 5.2 API & Data Security

- All GitHub webhook payloads verified via HMAC-SHA256 signature before processing
- Stripe webhook payloads verified via `Stripe-Signature` header — reject if invalid
- Input validation on ALL DTO fields using Jakarta Bean Validation (`@Valid` on controllers)
- SQL injection prevented by JPA parameterised queries — no native queries with string concat
- CORS: explicit allowlist of frontend origin only — no wildcard `*` in production
- Sensitive fields (tokens, secrets) annotated `@JsonIgnore` and excluded from logs
- HTTPS enforced in all environments above local dev

### 5.3 Secrets Management

- No secrets in code or git history — use environment variables or `.env` (gitignored)
- `.env.example` committed with placeholder values to document required variables
- Production secrets managed via CI/CD environment variables (GitHub Actions secrets)
- Database passwords rotated on every environment provisioning

---

## 6. Testing Philosophy & Coverage

### 6.1 Testing Pyramid

We follow the testing pyramid: many unit tests, fewer integration tests, minimal E2E tests. Speed and confidence are both required.

| Test Type | Scope & Tools |
|---|---|
| Unit (Backend) | JUnit 5 + Mockito — test service/domain logic in isolation, mock all dependencies |
| Integration (Backend) | Spring Boot Test + Testcontainers (PostgreSQL) — test real DB interactions |
| Unit (Frontend) | React Testing Library — test components in isolation with mocked API |
| Contract | OpenAPI schema validation — ensure Frontend/Backend contract |
| E2E | Playwright (Sprint 5 only) — critical user journeys only |

### 6.2 Coverage Requirements

- Backend: **70% line coverage minimum** — enforced by JaCoCo in CI; build fails below threshold
- Frontend: **60% statement coverage minimum** — enforced by Vitest coverage in CI
- All new service-layer methods must have at least one unit test and one negative test
- All security-critical paths (auth, webhook validation, feature gating) require integration tests
- Tests are not optional — PRs without tests for new logic will be rejected

### 6.3 Test Naming Convention

- Backend: `methodName_whenCondition_thenExpectedBehavior`
- Frontend: `renders [component] when [condition]` / `calls [handler] when [action]`

---

## 7. Performance Benchmarks & SLAs

### 7.1 Response Time Targets

| Endpoint Category | Target (p95) | Enforcement |
|---|---|---|
| Auth endpoints (login, refresh) | < 300ms | Monitored via Actuator metrics |
| Project dashboard load | < 500ms | Redis cache required for project metadata |
| Webhook ingestion (GitHub) | < 200ms | Async — return 200 immediately, process via `@Async` |
| AI doc generation | < 30s | WebSocket notification when complete — not blocking |
| Stripe checkout session | < 1s | Stripe SDK call, cached customer ID |
| Frontend initial load (LCP) | < 2.5s | Vite code splitting, lazy routes |

### 7.2 Caching Strategy

- Redis L2 cache for: project metadata, user subscription tier, GitHub repo list
- Cache TTL: project metadata 5 min, subscription tier 15 min, repo list 10 min
- Cache invalidated on: project update, Stripe webhook received, GitHub push
- Spring `@Cacheable` on service methods — never cache at controller level
- Never cache mutable user-specific data without user-scoped cache keys

### 7.3 Database Performance

- All foreign keys indexed — verify with `EXPLAIN ANALYZE` before PR merge
- Pagination required on all list endpoints — no unbounded queries
- N+1 queries forbidden — use `JOIN FETCH` or `@EntityGraph` for associations
- Database migrations via Flyway — versioned, reversible where possible

---

## 8. Accessibility Standards

SyncDoc AI targets WCAG 2.1 Level AA compliance for all user-facing interfaces.

- All interactive elements reachable and operable via keyboard alone
- All images have descriptive `alt` text; decorative images use `alt=""`
- Color contrast ratio: 4.5:1 minimum for normal text, 3:1 for large text
- All form inputs have associated `<label>` elements — no placeholder-only labels
- Error messages associated with inputs via `aria-describedby`
- Loading states communicated via `aria-live` regions for screen readers
- Focus indicators visible and not overridden with `outline: none` without alternative
- STOMP/WebSocket notifications announced to screen readers via `aria-live`
- Pricing table keyboard-navigable with clear ARIA roles for plan comparison
- Axe-core automated accessibility checks run in CI on frontend build

---

## 9. Inviolable Rules

> These rules cannot be broken under any circumstances. A PR that violates any of these will be closed without review. There are no exceptions, regardless of deadline pressure.

**RULE 01: No Secrets in Git** — API keys, JWT secrets, database passwords, Stripe keys, and GitHub App credentials must never be committed to the repository. If this happens, the secret must be rotated immediately and the commit purged from history.

**RULE 02: Webhook Signatures Must Be Verified** — GitHub and Stripe webhook endpoints must validate cryptographic signatures on every request before processing any payload. Unauthenticated webhooks are a critical attack surface.

**RULE 03: DTOs at API Boundary** — JPA entities must never be serialized directly in controller responses. All API responses go through explicitly defined DTO classes to prevent accidental data exposure.

**RULE 04: Feature Gating Is Enforced Server-Side** — Subscription plan checks (Free vs. Pro repo limits) must be enforced in the backend service layer. Frontend visibility controls are UX only and not a security boundary.

**RULE 05: No Direct Commits to Main** — All changes to `main` and `develop` branches go through Pull Requests with at least one approval. Direct commits bypass code review and CI enforcement.

**RULE 06: Stripe — No Raw Card Data** — The application must never collect, transmit, or store raw payment card data. All payment flows go through Stripe Checkout or Stripe.js hosted fields exclusively.

**RULE 07: Async Webhook Processing** — Webhook endpoints (GitHub, Stripe) must return HTTP 200 within 5 seconds to avoid retries. All processing logic runs in `@Async` threads — never inline in the handler.

**RULE 08: Test Coverage Cannot Decrease** — The CI pipeline enforces minimum coverage thresholds. A PR that causes coverage to drop below 70% (backend) or 60% (frontend) will fail CI and must not be merged.

**RULE 09: All Inputs Must Be Validated** — Every DTO accepted by a controller must have `@Valid` and corresponding Bean Validation annotations. Unvalidated input is an injection and abuse vector.

**RULE 10: No Lombok** — Lombok is explicitly excluded. Use Java 17 records for immutable data, explicit constructors for entities, and manual builders where needed. Clarity over brevity.

---

## 10. Sprint-by-Sprint Compliance Checkpoints

At the end of each sprint, the following checks must pass before the sprint is considered complete.

### Sprint 1 — Walking Skeleton

1. JWT auth flow end-to-end tested with refresh token rotation
2. Module-based package structure enforced and code-reviewed
3. Database schema reviewed — all foreign keys indexed
4. ESLint + Checkstyle pass with zero errors in CI

### Sprint 2 — Plumbing

1. GitHub webhook signature verification integration test passing
2. `@Async` processing confirmed — webhook handler returns < 200ms
3. OAuth2 GitHub tokens stored encrypted, verified via DB inspection
4. `SyncLogs` table populated correctly for all event types

### Sprint 3 — Monetization

1. Stripe webhook idempotency tested — duplicate events handled gracefully
2. Feature gating enforced server-side — bypass attempt via API confirmed blocked
3. No card data logged — verified via log audit
4. Stripe Checkout session tested on both Free and Pro plan paths

### Sprint 4 — Intelligence

1. OpenAI API key loaded from environment only — not hardcoded
2. WebSocket STOMP connection tested from React client
3. AI prompt template reviewed for PII leakage before production
4. Diff extraction tested with merge commits, empty commits, and large diffs

### Sprint 5 — Polishing

1. JaCoCo report confirms >= 70% backend coverage
2. Vitest coverage report confirms >= 60% frontend coverage
3. Docker Compose brings up all services cleanly from scratch
4. Redis cache hit rate > 80% under simulated load
5. Axe-core reports zero critical or serious accessibility violations
6. All 10 Inviolable Rules audited and signed off

---

*This constitution is a living document. Amendments require team consensus and must be version-controlled in this file.*

*SyncDoc AI Engineering Team · April 2026*