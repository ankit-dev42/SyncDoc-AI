# Tasks: Phase 3 — Frontend & DevOps

**Input**: `specs/005-phase3-frontend-devops/` (spec.md, plan.md, data-model.md, contracts/frontend-contracts.md, research.md, quickstart.md)  
**Branch**: `005-frontend-devops`  
**Date**: 2026-04-30  
**Task Range**: T200–T242

**Tests**: Test tasks are MANDATORY (per Principle II: Test-First Development). All `[TEST]` tasks must be written red before any implementation in their phase begins.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

---

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[US#]**: User story label — US1–US5 match spec.md priority order (see mapping below)
- No label = Setup or Foundational phase task

**User Story → Phase Mapping**

| Label | Story | Priority |
|-------|-------|----------|
| [US1] | Authenticated SPA Navigation | P1 |
| [US2] | Billing UI | P2 |
| [US3] | Server State via React Query | P2 |
| [US4] | Coverage Gates | P3 |
| [US5] | Continuous Integration Pipeline | P3 |

---

## Phase 1: Setup — Backend Coverage Configuration

**Purpose**: Wire JaCoCo enforcement and fix the SecretExposureAuditTest false-positive before frontend work begins. These changes must land first so the CI configuration in Phase 2 has a working backend gate to call.

**⚠️ CRITICAL**: Without the `ci` Maven profile, `mvn verify -Pci` in CI will succeed even at 0% coverage. Do not proceed to Phase 2 until T200–T202 are complete.

- [X] T200 Add `<profile><id>ci</id>` block to `backend/pom.xml` that activates the `check` goal on the existing `jacoco-maven-plugin` entry; set `LINE` coverage minimum to `0.70`; exclude `**/config/**` and `**/model/**` class patterns; note: `prepare-agent` and `report` goals already exist in `<build><plugins>` per codebase state — the `ci` profile is the only addition needed in backend/pom.xml
- [X] T202 [P] Fix `SecretExposureAuditTest` — replace whole-line `line.contains("${")` exclusion with a per-match check: a pattern match is only flagged if the matched region is NOT preceded by `${` and NOT followed by `}` in backend/src/test/java/com/syncdoc/collaboration/quality/security/SecretExposureAuditTest.java

**Checkpoint**: `mvn test -pl backend -Dtest=SecretExposureAuditTest` must pass with `${STRIPE_API_KEY}` present in source; the `ci` profile must exist in `mvn help:all-profiles -pl backend` output.

---

## Phase 2: Foundational — CI Pipeline & Frontend Configuration

**Purpose**: Create the GitHub Actions pipeline so CI runs on every push, then install all frontend dependencies and configure coverage thresholds. All user story work depends on the router, auth store, and React Query packages being present.

**⚠️ CRITICAL**: T205–T209 are sequential — T205→T206→T207→T208→T209 all modify `package.json` or depend on the prior install completing first. T203, T204, and T210 can proceed in parallel with T205–T209 since they touch different file trees (`.github/workflows/` and `.env.example` respectively).

- [X] T203 [P] [US5] Create `.github/workflows/ci.yml` with three jobs: `backend` (postgres:16 + redis:7 services, runs `mvn verify -Pci`, uploads JaCoCo report artifact), `frontend` (runs `npm ci`, `npm run lint`, `npm run test:coverage`, `npm run build`), `e2e` (main/develop branches only — `docker-compose up -d`, Playwright run, supply required secrets via job `env` block: `POSTGRES_URL`, `JWT_SECRET`, `STRIPE_API_KEY`, `STRIPE_WEBHOOK_SECRET`, `OPENAI_API_KEY`, `FRONTEND_ORIGIN`) in .github/workflows/ci.yml
- [X] T204 [P] [US5] Create `.github/workflows/pr-checks.yml` that runs on every PR: `backend-lint` step runs Maven Checkstyle plugin; `frontend-lint` step runs `npm run lint`; both report results on the PR in .github/workflows/pr-checks.yml
- [X] T205 Install `react-router-dom@6` and `@types/react-router-dom` as dependencies — run `npm install react-router-dom@6 @types/react-router-dom` in frontend/ in frontend/package.json
- [X] T206 Install `@tanstack/react-query@5` and `@tanstack/react-query-devtools` — run `npm install @tanstack/react-query @tanstack/react-query-devtools` in frontend/ in frontend/package.json
- [X] T207 Install `react-hook-form` — run `npm install react-hook-form` in frontend/ in frontend/package.json (zustand is already installed — no reinstall needed)
- [X] T208 Install `@vitest/coverage-v8` as a dev dependency — run `npm install -D @vitest/coverage-v8` in frontend/ in frontend/package.json
- [X] T209 Add `coverage` block to the `test` config in `vite.config.ts`: `provider: 'v8'`, `thresholds: { statements: 60, branches: 60, functions: 60, lines: 60 }`, `exclude` standard patterns (`node_modules`, `dist`, `*.config.*`) in frontend/vite.config.ts
- [X] T210 [P] Add `VITE_API_BASE_URL=http://localhost:8080/api/v1` to `.env.example`; add `"test:coverage": "vitest run --coverage"` and `"test:ci": "vitest run --coverage --reporter=junit"` scripts to `package.json` in frontend/.env.example and frontend/package.json

**Checkpoint**: `npm run test:coverage` exits non-zero if coverage falls below 60%; `ci.yml` and `pr-checks.yml` are valid YAML confirmed by `yamllint` or push.

---

## Phase 3: User Story 1 — Authenticated SPA Navigation (Priority: P1) 🎯 MVP

**Goal**: Replace the prototype's direct rendering with React Router v6 protected routes, a sessionStorage-backed Zustand auth store, and an axios interceptor with a Promise-singleton refresh lock. This is the P1 story — nothing else in the frontend is usable without a working auth boundary.

**Independent Test**: Navigate to `/billing` while unauthenticated → verify redirect to `/login` with `location.state.from = '/billing'`; log in → verify redirect back to `/billing`. Fire 5 concurrent requests that all return 401 → verify exactly one `/api/auth/refresh` call is made and all 5 requests retry successfully.

### Tests for User Story 1 — Write first, verify red, then implement

> **REQUIRED**: All five test files must be written and confirmed to fail (imports resolve but assertions fail) before T222 is started.

- [X] T211 [P] [US1] Write `authStore.test.ts` — `setTokens(accessToken, user)` persists `accessToken` to sessionStorage and stores `user` in memory; `clearTokens()` removes the sessionStorage key and sets both fields to `null`; `user` is NOT written to sessionStorage in frontend/src/features/auth/store/authStore.test.ts
- [X] T212 [P] [US1] Write `axiosInterceptor.test.ts` — mock axios adapter to reject with 401 on the first call and resolve on retry; fire 5 concurrent requests; assert `POST /api/auth/refresh` is called exactly once; assert all 5 responses resolve with the retried data; assert that when refresh itself returns 401, `clearTokens()` is called in frontend/src/api/axiosInterceptor.test.ts
- [X] T213 [P] [US1] Write `ProtectedRoute.test.tsx` — render `<MemoryRouter initialEntries={['/billing']}>`; when `authStore.accessToken` is `null`, assert `<Navigate to="/login" state={{ from: '/billing' }} replace />` is rendered; when `accessToken` is a non-null string, assert `<Outlet />` is rendered in frontend/src/components/ProtectedRoute.test.tsx
- [X] T214 [P] [US1] Write `LoginPage.test.tsx` — render `<LoginPage />`; submit with empty fields and assert validation error i18n keys are shown; mock `POST /api/auth/login` success and assert `setTokens()` is called with returned data; assert navigation to `location.state.from` if set; mock API failure and assert error message from `en.json` is displayed in frontend/src/features/auth/components/LoginPage.test.tsx
- [X] T215 [P] [US1] Write `RegisterPage.test.tsx` — render `<RegisterPage />`; assert `email`, `password`, and `displayName` fields are present; mock `POST /api/auth/register` success and assert navigation to `/login`; mock API failure and assert error message from `en.json` is displayed in frontend/src/features/auth/components/RegisterPage.test.tsx

### Implementation for User Story 1

- [X] T222 [US1] Rewrite `authStore.ts` with Zustand `persist` middleware using `createJSONStorage(() => sessionStorage)`; store shape: `{ accessToken: string | null, user: AuthUser | null, setTokens(accessToken, user): void, clearTokens(): void }`; persist only `accessToken` (exclude `user` from serialization) in frontend/src/features/auth/store/authStore.ts
- [X] T223 [US1] Rewrite `client.ts`: set `baseURL` from `import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api'`; read token from `sessionStorage` (via `useAuthStore.getState()`); add request interceptor attaching `Authorization: Bearer {token}`; add response interceptor with `let refreshPromise: Promise<string> | null = null` singleton lock (creates promise on first 401, queues subsequent 401s on same promise, retries all on resolve, calls `clearTokens()` + `navigateTo('/login')` on rejection); export `setNavigationHandler(fn)` function in frontend/src/api/client.ts
- [X] T224a [P] [US1] Create `NotFoundPage.tsx` — named export; renders a 404 not-found message; no auth dependency; must exist before T224 can import it as the `*` catch-all route target in frontend/src/components/NotFoundPage.tsx
- [X] T224 [US1] Rewrite `main.tsx`: call `createBrowserRouter` with full route tree (`/`, `/login`, `/register`, `/success`, `/billing`, `/projects/:projectId`, `*` → NotFoundPage); depends on T224a; wrap with `<RouterProvider>`, `<QueryClientProvider>`, `<ReactQueryDevtools>`; call `setNavigationHandler(router.navigate)` after router creation in frontend/src/main.tsx
- [X] T225 [P] [US1] Create `ProtectedRoute.tsx` — reads `useAuthStore().accessToken`; renders `<Navigate to="/login" state={{ from: location }} replace />` if `null`; renders `<Outlet />` otherwise; uses named export in frontend/src/components/ProtectedRoute.tsx
- [X] T226 [P] [US1] Remove the `window.location.pathname === '/success'` conditional block from `App.tsx`; remove any direct route-based rendering that duplicates router logic; keep remaining non-routing app-level setup in frontend/src/App.tsx
- [X] T227 [US1] Add i18n error keys to `frontend/src/i18n/en.json` (create the file if absent): `auth.login.error`, `auth.register.error`, `billing.upgrade.error`; then create `LoginPage.tsx` — React Hook Form with `email` and `password` fields; on submit calls `POST /api/auth/login`, calls `setTokens()` with response data, navigates to `location.state?.from ?? '/'`; displays error using i18n key `auth.login.error` from `en.json` on failure; named export in frontend/src/i18n/en.json and frontend/src/features/auth/components/LoginPage.tsx
- [X] T228 [P] [US1] Create `RegisterPage.tsx` — React Hook Form with `email`, `password`, `displayName` fields; on submit calls `POST /api/auth/register`, navigates to `/login`; displays error using i18n key `auth.register.error` from `en.json` on failure; named export in frontend/src/features/auth/components/RegisterPage.tsx

*Run T211–T213 tests after T222–T225: all should now pass green. Run T214–T215 tests after T227–T228.*

---

## Phase 4: User Story 2 — Billing UI (Priority: P2)

**Goal**: Add a PricingTable with Free vs Pro comparison, an UpgradeButton that triggers the checkout mutation, a ManageSubscriptionButton for the portal, and fix the billingApi.ts baseURL to use the environment variable.

**Independent Test**: Render `/billing`; verify Free and Pro tiers are visible; click "Upgrade to Pro" and assert `useUpgradeFlow` mutation fires; click "Manage Subscription" and assert portal API is called. Verify no `localhost` string appears anywhere in `src/`.

### Tests for User Story 2 — Write first, verify red, then implement

> **REQUIRED**: All three test files must be written and confirmed to fail before T234 is started.

- [X] T216 [P] [US2] Write `billingApi.test.ts` — mock `import.meta.env.VITE_API_BASE_URL`; assert `POST /api/v1/billing/checkout` is called with correct payload; assert `GET /api/v1/billing/portal` is called; assert no URL in the module contains `localhost` as a hardcoded string in frontend/src/features/billing/api/billingApi.test.ts
- [X] T217 [P] [US2] Write `PricingTable.test.tsx` — render `<PricingTable onUpgrade={mockFn} onManage={mockFn} />`; assert Free and Pro tier names are visible; assert feature comparison rows are rendered; click Upgrade button and assert `onUpgrade` fires; assert `UpgradeButton` shows loading state when `isPending` prop is `true` in frontend/src/features/billing/components/PricingTable.test.tsx
- [X] T218 [P] [US2] Write `useUpgradeFlow.test.ts` — wrap with `QueryClientProvider`; assert the mutation calls `billingApi.createCheckoutSession` with the `userId` from `authStore`; assert `isPending` is `true` during mutation; assert `window.location.href` is set to the returned `checkoutUrl` on success; assert error is surfaced on mutation failure in frontend/src/features/billing/hooks/useUpgradeFlow.test.ts

### Implementation for User Story 2

- [X] T234 [US2] Audit `billingApi.ts`: if it contains a standalone `axios.create(...)` call, remove it and replace with the shared `apiClient` from `src/api/client.ts`; expected post-condition: `billingApi.ts` MUST use the shared `apiClient` and contain no hardcoded URL strings — confirmed by `grep -r "localhost" src/` returning zero results in frontend/src/features/billing/api/billingApi.ts
- [X] T235 [P] [US2] Create `PricingTable.tsx` — named export; renders Free vs Pro feature comparison table with prices; accepts `onUpgrade: () => void` and `onManage: () => void` and `isUpgradePending: boolean` props; renders `UpgradeButton` for the Pro column in frontend/src/features/billing/components/PricingTable.tsx
- [X] T236 [P] [US2] Create `ManageSubscriptionButton.tsx` — named export; on click calls `GET /api/v1/billing/portal` and redirects user to returned `portalUrl`; shows loading state during API call in frontend/src/features/billing/components/ManageSubscriptionButton.tsx
- [X] T237a [P] [US2] Create `UpgradeButton.tsx` (FR-035) — named export; invokes `useUpgradeFlow` mutation on click; shows loading spinner and disabled state when `isPending` is true; redirects to Stripe Checkout URL on mutation success; displays i18n key `billing.upgrade.error` on failure in frontend/src/features/billing/components/UpgradeButton.tsx
- [X] T237b [US2] Create `BillingPage.tsx` as route component for `/billing` — renders `PricingTable` with `UpgradeButton` and `ManageSubscriptionButton`; reads current subscription tier via `useSubscriptionTier()` (T232a); depends on T237a, T232a, T235, T236 in frontend/src/features/billing/components/BillingPage.tsx

*Run T216–T218 tests after T234–T237: all should now pass green.*

---

## Phase 5: User Story 3 — Server State via React Query (Priority: P2)

**Goal**: Migrate all four data-fetching hooks from manual `useEffect`/`useState` to React Query v5 `useQuery`/`useMutation`. After this phase, `grep -r "useEffect" src/features/*/hooks/` must return zero data-fetching usages.

**Independent Test**: Run the grep — zero `useEffect` calls in `src/features/*/hooks/`. Run each hook's test in isolation; all four must pass using `renderHook` wrapped with `QueryClientProvider`.

### Tests for User Story 3 — Write first, verify red, then implement

> **REQUIRED**: All four test files must be written and confirmed to fail before T229 is started.

- [X] T219 [P] [US3] Write `usePresence.test.ts` — wrap with `QueryClientProvider`; mock `presenceApi.list(workspaceId)`; assert `useQuery` is used (hook returns `{ data, isPending }`); assert data is populated on resolved mock; assert `isPending` is `true` before resolution; heartbeat `setInterval` is NOT replaced by React Query in frontend/src/features/presence/hooks/usePresence.test.ts
- [X] T220 [P] [US3] Write `useSearch.test.ts` — wrap with `QueryClientProvider`; assert no API call fires when query is fewer than 2 characters (`enabled: false`); advance fake timers by 300ms with a ≥2-character query and assert exactly one API call fires; assert debounce prevents multiple calls on rapid input changes in frontend/src/features/search/hooks/useSearch.test.ts
- [X] T221 [P] [US3] Write `useThreads.test.ts` — wrap with `QueryClientProvider`; mock `threadsApi.list(workspaceId)`; assert list uses `useQuery` and returns `{ data, isPending }`; mock `threadsApi.create()` and assert thread creation uses `useMutation` returning `{ mutate, isPending }` in frontend/src/features/messaging/hooks/useThreads.test.ts
- [X] T221a [P] [US3] Write `useSubscriptionTier.test.ts` — wrap with `QueryClientProvider`; mock `GET /api/v1/subscriptions/me`; assert `useQuery` returns `{ data: { tier }, isPending }`; assert `isPending` is `true` before resolution; assert `userId` is sourced from `authStore` in frontend/src/features/billing/hooks/useSubscriptionTier.test.ts

- [X] T229 [P] [US3] Migrate `usePresence.ts` to `useQuery({ queryKey: ['presence', workspaceId], queryFn: () => presenceApi.list(workspaceId) })`; remove `useState`/`useEffect` data-fetching pattern; retain heartbeat `setInterval` outside React Query (timing side-effect, not server state) in frontend/src/features/presence/hooks/usePresence.ts
- [X] T230 [P] [US3] Migrate `useThreads.ts` to `useQuery({ queryKey: ['threads', workspaceId], queryFn: () => threadsApi.list(workspaceId) })` for listing and `useMutation({ mutationFn: threadsApi.create })` for creation; remove all `useEffect`/`useState` data-fetching in frontend/src/features/messaging/hooks/useThreads.ts
- [X] T231 [P] [US3] Migrate `useSearch.ts` to `useQuery({ queryKey: ['search', workspaceId, debouncedQuery], queryFn: () => searchApi.run(...), enabled: debouncedQuery.length >= 2 })`; add or extract `useDebounce(value, 300)` utility hook; remove manual `runSearch` callback pattern in frontend/src/features/search/hooks/useSearch.ts
- [X] T232 [P] [US3] Migrate `useUpgradeFlow.ts` to `useMutation({ mutationFn: () => billingApi.createCheckoutSession(userId) })`; resolve `userId` from `useAuthStore()` inside the hook; remove `useState`/`useCallback`/`useEffect` data-fetching patterns in frontend/src/features/billing/hooks/useUpgradeFlow.ts
- [X] T232a [P] [US3] Create `useSubscriptionTier.ts` — `useQuery({ queryKey: ['subscriptionTier', userId], queryFn: () => billingApi.getSubscriptionTier(userId) })`; resolve `userId` from `useAuthStore()`; return `{ tier, isPending, isError }`; named export; consumed by `BillingPage.tsx` (T237b) in frontend/src/features/billing/hooks/useSubscriptionTier.ts
- [X] T233 [US3] Verification checkpoint: confirm `main.tsx` has `QueryClient` configured with `defaultOptions: { queries: { staleTime: 60_000, retry: 2 } }` and that `<QueryClientProvider client={queryClient}>` and `<ReactQueryDevtools initialIsOpen={false} />` wrap the router tree; if `defaultOptions` is absent (T224 may have omitted it), add it now — this task always produces a code change (the `defaultOptions` config) in frontend/src/main.tsx

*Run T219–T221 tests after T229–T232: all should now pass green.*

---

## Phase 6: User Story 4 — Coverage Gates & Additional Tests (Priority: P3)

**Goal**: Expand test coverage to meet the 60% frontend thresholds, write the two remaining test files (ProtectedRoute authenticated pass-through + SuccessPage search-params), then verify both coverage gates pass in CI.

**Independent Test (US4)**: Intentionally remove a tested code path, run `npm run test:coverage` — confirm it exits non-zero with a threshold violation. Restore the code path — confirm it exits zero.

- [X] T238 [US4] Write `ProtectedRoute.test.tsx` (authenticated branch) — render with a non-null `accessToken` in the store; assert `<Outlet />` is rendered and no redirect occurs; appends to the test file started in T213; covers the pass-through path not tested there in frontend/src/components/ProtectedRoute.test.tsx
- [X] T239 [P] [US4] Write `SuccessPage.test.tsx` — render `/success?session_id=abc123` inside a `MemoryRouter`; assert `useSearchParams()` reads `session_id` value `abc123`; assert success banner renders the value; assert NO access to `window.location.search` is made in frontend/src/features/billing/components/SuccessPage.test.tsx
- [X] T239a [US4] Create `SuccessPage.tsx` — named export; uses `useSearchParams()` to read `session_id`; renders a payment success banner displaying the session ID value; no `window.location` access of any kind; this is the `/success` route component used by T224's route tree in frontend/src/features/billing/components/SuccessPage.tsx
- [X] T240 [US4] Run `cd backend && mvn verify -Pci`; JaCoCo `check` goal MUST pass at ≥70% line coverage; if it fails, identify uncovered paths and add targeted tests — do NOT lower the threshold in backend/ (checkpoint — do not proceed to T241 if this fails)
- [X] T241 [US4] Run `cd frontend && npm run test:coverage`; all four dimensions (statements, branches, functions, lines) MUST report ≥60%; if any dimension fails, add targeted tests for the uncovered areas — do NOT lower the threshold in frontend/ (checkpoint — do not proceed to T242 if this fails)

---

## Final Phase: Checkpoint — Build Verification

**Purpose**: Confirm the full frontend TypeScript build is clean with zero errors — the last gate before `speckit.analyze`.

- [X] T242 Run `cd frontend && npm run build`; zero TypeScript compilation errors required; address any type errors found — do NOT use `@ts-ignore` or `any` suppressions to pass in frontend/ (checkpoint)

---

## Dependencies

```
US5 (CI — T203–T204)
  └─ No story dependencies — can be written the moment Phase 1 is complete

US1 (Auth — T211–T228)
  ├─ depends on: Phase 1 (T200–T202), Phase 2 deps (T205–T210)
  ├─ blocks: US2 (PricingTable uses auth store for userId)
  └─ blocks: US3 (useUpgradeFlow reads userId from authStore)

US2 (Billing UI — T216–T218, T234, T235, T236, T237a, T237b)
  ├─ depends on: US1 complete (T222–T228), US3 useMutation (T232), US3 useSubscriptionTier (T232a)
  └─ blocks: none

US3 (React Query — T219–T221a, T229–T233)
  ├─ depends on: Phase 2 deps installed (T206), US1 client.ts (T223)
  └─ blocks: US2 (BillingPage uses migrated useUpgradeFlow + useSubscriptionTier)

US4 (Coverage Gates — T238–T239a, T240–T241)
  ├─ depends on: all prior test tasks (T211–T221a, T238–T239) and implementations
  └─ blocks: T242 (final build check)
```

**Story Completion Order**: US5 → US1 → US3 → US2 → US4 → Checkpoint

---

## Parallel Execution Examples

### Within Phase 1 (can parallelize T202 with T200)
```
Agent A: T200 pom.xml ci profile + JaCoCo check goal
Agent B: T202 SecretExposureAuditTest regex fix        ← different file
```

### Within Phase 2 (CI files and .env.example parallel with sequential npm installs)
```
Agent A: T203 ci.yml                                   ← .github/workflows/
Agent B: T204 pr-checks.yml                            ← .github/workflows/
Agent C: T205 → T206 → T207 → T208 → T209 (sequential, same package.json / vite.config.ts)
Agent D: T210 .env.example                             ← different file
(T203, T204, T210 can run in parallel with Agent C's sequential chain)
```

### Within Phase 3 Tests — all 5 test files are parallel
```
Agent A: T211 authStore.test.ts
Agent B: T212 axiosInterceptor.test.ts
Agent C: T213 ProtectedRoute.test.tsx (unauthenticated)
Agent D: T214 LoginPage.test.tsx
Agent E: T215 RegisterPage.test.tsx
```

### Within Phase 3 Implementation — after T222–T223 complete
```
Agent A: T225 ProtectedRoute.tsx (different file from main.tsx)
Agent B: T226 App.tsx cleanup     (different file from main.tsx)
Agent C: T224 main.tsx (router + QueryClientProvider)
(T227 and T228 after T224; they can run in parallel with each other)
```

### Within Phase 5 — all 5 hook migrations/additions are parallel
```
Agent A: T229 usePresence.ts
Agent B: T230 useThreads.ts
Agent C: T231 useSearch.ts
Agent D: T232 useUpgradeFlow.ts
Agent E: T232a useSubscriptionTier.ts    ← different file
(T233 main.tsx defaultOptions config after T229–T232a)
```

### Within Phase 6 — T238 and T239/T239a are independent
```
Agent A: T238 ProtectedRoute.test.tsx (authenticated branch) ← sequential, appends to Phase 3 file
Agent B: T239 SuccessPage.test.tsx (write test red)
         T239a SuccessPage.tsx     (implement to make test green) ← sequential pair
```

---

## Implementation Strategy — MVP First

**Suggested MVP Scope: User Story 1 only (T200–T228)**

Delivering US1 in isolation produces a working auth boundary:
- Protected routes redirect unauthenticated users
- Login and Register pages function end-to-end
- JWT token refresh is race-condition-safe
- sessionStorage persists the access token across page refreshes (within the tab)
- The billing and React Query stories can be shipped independently after US1 merges

**Incremental Delivery Order**:
1. **MVP**: Phase 1 + Phase 2 + US1 (T200–T228) — auth-complete SPA
2. **Increment 2**: US3 React Query migration (T219–T221, T229–T233) — all hooks migrated, SC-P3-3 met
3. **Increment 3**: US2 Billing UI (T216–T218, T234, T235, T236, T237a, T237b) — Pricing and checkout live
4. **Increment 4**: US4 Coverage Gates + Final Checkpoint (T238–T239a, T240–T242) — all thresholds enforced

---

## Format Validation

All tasks follow the required checklist format:
```
- [X] [TaskID] [P?] [Story?] Description with file path
```

- ✅ All tasks begin with `- [X]`
- ✅ All tasks have base sequential Task IDs T200–T242; inserted tasks T224a, T221a, T232a, T237a, T237b, T239a fill coverage gaps identified in `speckit.analyze`
- ✅ [P] marker present only on tasks operating on different files with no incomplete-task dependencies
- ✅ [US#] marker present on all User Story phase tasks; absent on Setup/Foundational/Polish tasks
- ✅ All tasks include exact file paths
- ✅ Test tasks are listed before corresponding implementation tasks within each story phase
