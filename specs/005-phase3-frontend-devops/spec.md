# Feature Specification: Phase 3 — Frontend & DevOps

**Feature Branch**: `005-frontend-devops`  
**Created**: 2026-04-30  
**Status**: Draft  
**Input**: Transform the SyncDoc AI frontend from a prototype into a production SPA and automate quality enforcement via CI.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Authenticated SPA Navigation (Priority: P1)

As a user, I want to navigate between pages protected by authentication and be seamlessly redirected to Login when my session expires, so I can resume exactly where I left off without losing context.

**Why this priority**: Nothing else in the frontend is accessible without authentication. All other user stories depend on a working auth boundary.

**Independent Test**: Can be fully tested by navigating to a protected route while unauthenticated, verifying redirect to `/login`, logging in, and confirming return to the original route.

**Acceptance Scenarios**:

1. **Given** an unauthenticated user, **When** they visit `/billing`, **Then** they are redirected to `/login` with the intended destination preserved via React Router `location.state`
2. **Given** a user on the login page (redirected from `/billing`), **When** they successfully authenticate, **Then** they are sent back to `/billing` (not `/`)
3. **Given** no `location.state.from` is set (direct `/login` visit), **When** login succeeds, **Then** the user lands at `/`
4. **Given** a valid access token in sessionStorage, **When** the token expires mid-session and an API call returns 401, **Then** the axios interceptor silently refreshes the token and retries the original request without the user noticing
5. **Given** the refresh token is also expired, **When** a 401 is encountered, **Then** `clearTokens()` is called and the user is redirected to `/login`
6. **Given** N concurrent requests all receive a 401 simultaneously, **When** the interceptor handles them, **Then** exactly one call to `/api/auth/refresh` is made (Promise singleton lock); all N requests retry with the new token
7. **Given** a user visits `/success?session_id=abc123`, **When** the page loads, **Then** `useSearchParams()` reads `session_id` and the success banner renders with that value

---

### User Story 2 — Billing UI (Priority: P2)

As a paying (or prospective) user, I want to see a clear pricing comparison, upgrade to Pro, and manage my subscription, so I can make an informed purchase decision without leaving the app.

**Why this priority**: Billing is the primary revenue path. The backend billing endpoints exist (Phase 2); this adds the UI surface to drive conversions.

**Independent Test**: Can be fully tested by rendering `/billing`, verifying plan details, clicking "Upgrade to Pro" and confirming a checkout mutation fires, and clicking "Manage Subscription" and confirming portal API is called.

**Acceptance Scenarios**:

1. **Given** a user on `/billing`, **When** the page loads, **Then** `PricingTable` renders both Free and Pro tiers with feature comparisons and prices
2. **Given** a Free-tier user clicks "Upgrade to Pro", **When** the `useUpgradeFlow` mutation fires, **Then** a loading spinner appears and the user is redirected to the Stripe Checkout URL on success
3. **Given** a Pro-tier user clicks "Manage Subscription", **When** the billing portal API responds, **Then** the user is redirected to the Stripe billing portal URL
4. **Given** the checkout mutation is in-flight, **When** the user inspects the button, **Then** it is disabled and shows a loading indicator
5. **Given** the checkout mutation fails, **When** the error is returned, **Then** an error message is displayed using an i18n key from `en.json`

---

### User Story 3 — Server State via React Query (Priority: P2)

As a developer, I want all server-derived data in the frontend fetched exclusively via React Query, so there are no stale-cache issues, no manual loading/error state management, and the codebase is consistent with the constitution.

**Why this priority**: React Query is constitutionally mandated. Leaving `useEffect`-based fetching in place blocks all other UI work from being correctly tested and maintained.

**Independent Test**: Can be verified by searching `src/features/*/hooks/` for any `useEffect` that calls an API — the count must be zero after this phase.

**Acceptance Scenarios**:

1. **Given** `usePresence` is called, **When** the component mounts, **Then** it uses `useQuery` with a stable query key — no `useEffect` with an API call
2. **Given** `useThreads` is called, **When** the component mounts, **Then** list fetching uses `useQuery` and thread creation uses `useMutation`
3. **Given** `useSearch` is called with a query shorter than 2 characters, **When** evaluated, **Then** the query is disabled and no API call fires
4. **Given** `useSearch` is called with ≥2 characters, **When** the debounce delay elapses, **Then** exactly one API call fires with the debounced value
5. **Given** `useUpgradeFlow` is called, **When** the upgrade action is triggered, **Then** it uses `useMutation` — not a `useEffect` watching state

---

### User Story 4 — Coverage Gates (Priority: P3)

As a team, we want CI to automatically enforce minimum test coverage thresholds so coverage regressions are caught before they reach `develop`.

**Why this priority**: Coverage gates are a quality infrastructure concern — important but non-blocking for users. Depends on tests being written first.

**Independent Test**: Can be verified by introducing a deliberate uncovered code path, running `mvn verify -Pci` (backend) or `npm run test:coverage` (frontend), and confirming the build fails.

**Acceptance Scenarios**:

1. **Given** backend line coverage is below 70%, **When** `mvn verify -Pci` runs, **Then** the build fails with a JaCoCo coverage violation message
2. **Given** frontend statement/branch/function/line coverage is below 60%, **When** `npm run test:coverage` runs, **Then** the command exits non-zero with a threshold violation report
3. **Given** a PR is opened, **When** CI runs on GitHub Actions, **Then** both coverage checks are enforced and a failing check blocks the merge

---

### User Story 5 — Continuous Integration Pipeline (Priority: P3)

As a team, we want every push to automatically run tests, lint, build, and coverage checks, so broken code is caught before it reaches shared branches.

**Why this priority**: CI is infrastructure. It multiplies the value of all other work in this phase but does not deliver user-visible functionality.

**Independent Test**: Can be verified by pushing a commit that breaks a test and confirming GitHub Actions marks the PR status check as failed.

**Acceptance Scenarios**:

1. **Given** a push to any branch, **When** CI triggers, **Then** the backend job runs `mvn verify -Pci` with Postgres 16 and Redis 7 services
2. **Given** a push to any branch, **When** CI triggers, **Then** the frontend job runs `npm run lint`, `npm run test:coverage`, and `npm run build` in sequence
3. **Given** a push to `main` or `develop`, **When** CI triggers, **Then** the E2E job runs Playwright tests against a full `docker-compose up` stack
4. **Given** a PR is opened, **When** the PR checks workflow runs, **Then** Checkstyle (backend) and ESLint (frontend) results are reported on the PR
5. **Given** any CI job fails, **When** the status is reported, **Then** the PR merge button is blocked

---

### Edge Cases

- What happens when the refresh token expires between the interceptor receiving 401 and completing the refresh call? → Refresh API returns 401, interceptor calls `clearTokens()` and navigates to `/login`
- What happens when `sessionStorage` is cleared (e.g., tab closed and reopened)? → `accessToken` is `null`, `ProtectedRoute` redirects to `/login` immediately
- What happens when `VITE_API_BASE_URL` is not set in the environment? → `import.meta.env.VITE_API_BASE_URL` is `undefined`; the API client will fail with a clear network error rather than hitting localhost silently
- What happens when `${STRIPE_API_KEY}` (env-var reference) appears in a config file? → `SecretExposureAuditTest` must NOT flag this as a secret leak; only bare literal values like `sk_live_xxx` must trigger failure
- What happens when two tabs are open and one refreshes the token? → Both tabs use sessionStorage independently (tab-scoped); each manages its own token lifecycle

---

## Requirements *(mandatory)*

### Functional Requirements

**Routing**

- **FR-001**: The frontend MUST use React Router v6 `createBrowserRouter` with routes for `/`, `/login`, `/register`, `/success`, `/billing`, `/projects/:projectId`, and a catch-all `*` (404)
- **FR-002**: A `ProtectedRoute` component MUST read `authStore` and redirect unauthenticated users to `/login` using `<Navigate to="/login" state={{ from: location }} replace />`
- **FR-003**: The `/success` route MUST use `useSearchParams()` to read `session_id` — not `window.location.search`
- **FR-004**: The `window.location.pathname === '/success'` check in `App.tsx` MUST be removed

**Auth Store**

- **FR-005**: `authStore.ts` MUST implement `{ accessToken: string | null, user: { id: string, email: string, displayName: string } | null, setTokens(accessToken, user): void, clearTokens(): void }` using Zustand
- **FR-006**: `accessToken` MUST be persisted to `sessionStorage` (tab-scoped) — NOT `localStorage`
- **FR-007**: The `user` object MUST hold identity only (`id`, `email`, `displayName`) — subscription tier MUST NOT be stored in the auth store (fetched via React Query instead)

**Axios JWT Interceptor**

- **FR-008**: The axios request interceptor MUST attach `Authorization: Bearer {accessToken}` to every outbound request when a token is present
- **FR-009**: The axios response interceptor MUST handle 401 responses using a Promise singleton lock (`let refreshPromise: Promise<string> | null = null`) so that N concurrent 401s result in exactly one call to `POST /api/auth/refresh`
- **FR-010**: On successful refresh, all queued requests MUST retry with the new token
- **FR-011**: On refresh failure, `clearTokens()` MUST be called and the user navigated to `/login`
- **FR-012**: `billingApi.ts` MUST use `import.meta.env.VITE_API_BASE_URL` as the base URL — no hardcoded `localhost` strings anywhere in `src/`
- **FR-036**: `VITE_API_BASE_URL` MUST be documented in `.env.example` with a default value of `http://localhost:8080/api/v1` so developers can onboard without consulting external documentation

**Login & Register Pages**

- **FR-013**: `LoginPage` MUST use React Hook Form, POST to `POST /api/auth/login`, update `authStore` on success, then navigate to `location.state?.from ?? '/'`
- **FR-014**: `RegisterPage` MUST use React Hook Form with fields: `email`, `password`, `displayName`, POST to `POST /api/auth/register`, then navigate to `/login` on success
- **FR-015**: All user-visible error strings MUST use i18n keys from `en.json` — no hardcoded error text

**React Query Migration**

- **FR-016**: `@tanstack/react-query` v5 MUST be used for ALL server state — `useQuery` object syntax only (no v4 positional argument syntax)
- **FR-017**: `QueryClient` MUST be configured with `staleTime: 60_000` and `retry: 2` in `main.tsx`
- **FR-018**: `QueryClientProvider` and `ReactQueryDevtools` MUST wrap the entire component tree
- **FR-019**: `usePresence`, `useThreads`, `useSearch`, and `useUpgradeFlow` MUST be migrated to `useQuery` / `useMutation` — no data-fetching `useEffect` may remain in `src/features/*/hooks/`
- **FR-020**: `useSearch` MUST have `enabled: query.length >= 2` and use a debounce of ≥300ms before the query key changes

**Billing UI**

- **FR-021**: `PricingTable` MUST be a named export and render a Free vs Pro feature comparison table with prices
- **FR-022**: `ManageSubscriptionButton` MUST be a named export
- **FR-023**: `BillingPage` MUST be the route component for `/billing`
- **FR-024**: No component file in `src/` (except page-level route components) MAY use a default export
- **FR-035**: `UpgradeButton` MUST be a named export; it MUST invoke the `useUpgradeFlow` mutation on click and redirect the user to the Stripe Checkout URL on success

**JaCoCo Backend Coverage**

- **FR-025**: `jacoco-maven-plugin` 0.8.11 MUST be configured with `prepare-agent`, `report`, and `check` goals
- **FR-026**: A `ci` Maven profile MUST activate the JaCoCo `check` goal with a line coverage threshold of 70%
- **FR-027**: Excluded packages: `**/config/**` and `**/model/**`

**Vitest Frontend Coverage**

- **FR-028**: `@vitest/coverage-v8` MUST be installed and configured in `vitest.config.ts` with `provider: 'v8'` and thresholds of 60% for `statements`, `branches`, `functions`, and `lines`
- **FR-029**: `package.json` MUST include `"test:coverage"` and `"test:ci"` scripts

**GitHub Actions CI**

- **FR-030**: `.github/workflows/ci.yml` MUST define three jobs: `backend` (postgres:16 + redis:7), `frontend`, and `e2e` (main/develop only)
- **FR-031**: The E2E job MUST supply required secrets via the job `env` block mapped from GitHub Actions secrets: `POSTGRES_URL`, `JWT_SECRET`, `STRIPE_API_KEY`, `STRIPE_WEBHOOK_SECRET`, `OPENAI_API_KEY`, `FRONTEND_ORIGIN`
- **FR-032**: `.github/workflows/pr-checks.yml` MUST run Checkstyle (backend) and ESLint (frontend) on every PR
- **FR-033**: The JaCoCo report artifact MUST be uploaded in the backend CI job

**SecretExposureAuditTest**

- **FR-034**: The `SecretExposureAuditTest` regex MUST be updated to exclude `${...}` env-var expression patterns — a match MUST require the key NOT to be preceded by `${` and NOT followed by `}`

---

### Key Entities

- **AuthStore**: Zustand store holding `{ accessToken, user: { id, email, displayName } }` — persisted to sessionStorage for access token only
- **ProtectedRoute**: React component that gates route rendering behind auth check; preserves intended destination via `location.state`
- **QueryClient**: Singleton React Query client configured at app root with `staleTime` and `retry` policy
- **RefreshLock**: Module-level `let refreshPromise: Promise<string> | null` in the axios interceptor file — prevents concurrent refresh races

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-P3-1**: All protected routes (`/billing`, `/projects/:projectId`) redirect unauthenticated users to `/login` — verified by Playwright navigation tests
- **SC-P3-2**: N concurrent 401 responses from the API trigger exactly one call to `/api/auth/refresh` — verified by unit test mocking axios with simultaneous failing requests
- **SC-P3-3**: Zero data-fetching `useEffect` calls exist in `src/features/*/hooks/` — verified by static grep in CI
- **SC-P3-4**: `mvn verify -Pci` exits non-zero when backend line coverage falls below 70% — verified by a deliberate coverage-drop test
- **SC-P3-5**: `npm run test:coverage` exits non-zero when any frontend threshold falls below 60% — verified against actual coverage report
- **SC-P3-6**: `SecretExposureAuditTest` passes on a clean repo containing `${STRIPE_API_KEY}` in config files and fails when `STRIPE_API_KEY=sk_live_xxx` appears as a literal value

---

## Constitution Alignment *(mandatory)*

### Code Quality (Principle I)
- [x] No single component exceeds 300 lines; pages delegate to sub-components
- [x] No magic strings — all API base URLs via `import.meta.env`, all i18n keys from `en.json`
- [x] Named exports enforced for all non-page components (Principle III)

### Testing Standards (Principle II)
- [x] All [TEST] tasks in tasks.md written and observed failing BEFORE implementation begins
- [x] Unit test coverage target: ≥60% frontend (statements/branches/functions/lines), ≥70% backend (lines)
- [x] Integration tests cover: axios interceptor refresh race, ProtectedRoute redirect, React Query hook states
- [x] TDD enforced: tests must fail before implementation of each task group

### User Experience Consistency (Principle III)
- [x] All components use named exports
- [x] Loading states handled via React Query `isLoading` / `isPending` — no manual state
- [x] All error messages sourced from `en.json` i18n keys — no hardcoded UI text
- [x] `useSearchParams()` used for URL param reading — no `window.location` string parsing

### Performance Requirements (Principle IV)
- [x] axios interceptor adds zero latency to successful requests (header attach is synchronous)
- [x] React Query `staleTime: 60s` prevents redundant refetches on navigation
- [x] Frontend bundle size monitored via `npm run build` output in CI

---

## Assumptions

- Phase 2 backend is merged and the following endpoints exist: `POST /api/auth/login`, `POST /api/auth/register`, `POST /api/auth/refresh`, `POST /api/v1/billing/checkout`, `GET /api/v1/billing/portal`
- `en.json` already exists in `src/i18n/` or will be created as part of `LoginPage` implementation (task T227)
- The existing `PaymentSuccessBanner.test.tsx` is the sole test file; all other test files listed in tasks.md are new
- `console.log` is prohibited in committed source code per Constitution Section 4.2
- `sessionStorage` is the correct scope for `accessToken` — it provides tab isolation (safer than `localStorage` for XSS exposure) while surviving page refreshes within the same tab
- Non-page components (`PricingTable`, `ManageSubscriptionButton`, `ProtectedRoute`) use named exports; page-level route components (`LoginPage`, `RegisterPage`, `BillingPage`) may use either export style but named is preferred
- `docker-compose.yml` in the repo root is the correct file to use for the E2E CI job
- GitHub repository secrets (`POSTGRES_URL`, `JWT_SECRET`, etc.) are configured by the team before the E2E job is enabled
