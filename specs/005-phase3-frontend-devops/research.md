# Research: Phase 3 — Frontend & DevOps

**Date**: 2026-04-30  
**Branch**: `005-frontend-devops`

---

## R1 — React Router v6 `createBrowserRouter` Pattern

**Decision**: Use `createBrowserRouter` + `RouterProvider` in `main.tsx` (not the legacy `<BrowserRouter>` wrapper).  
**Rationale**: `createBrowserRouter` is the recommended API from React Router v6.4+. It enables future adoption of data loaders without a breaking refactor. `RouterProvider` replaces the wrapper-style setup.  
**Alternatives considered**:
- `BrowserRouter` (legacy wrapper) — rejected because it blocks future loader adoption and doesn't support `useBlocker`.
- Hash router — rejected: SEO-unfriendly, no server-side routing support.

**ProtectedRoute pattern**: A layout route component that reads `useAuthStore` and returns `<Navigate to="/login" state={{ from: location }} replace />` when `accessToken` is null, or `<Outlet />` when authenticated. Nested under the protected route in the route config.

**Key API note**: `/success` must read `session_id` from `useSearchParams()`, not `window.location.search`. The current `window.location.pathname === '/success'` check in `App.tsx` must be deleted.

---

## R2 — Zustand v4 `persist` with `sessionStorage`

**Decision**: Use Zustand `persist` middleware with `createJSONStorage(() => sessionStorage)`, persisting only `accessToken`.  
**Rationale**: Zustand v4.4.7 is already installed. The `persist` middleware supports custom storage adapters. `sessionStorage` is tab-scoped — it clears on tab close (unlike localStorage) which limits XSS exposure window. The `user` object (id, email, displayName) stays in memory only, so it is wiped on tab close / page refresh, forcing re-fetch from `/api/auth/refresh`.  
**Alternatives considered**:
- `localStorage` — rejected: tokens survive across tabs and browser restarts, wider XSS exposure window (current code uses this and it must change).
- In-memory only — rejected: token is lost on page refresh, creating a bad UX where the user must log in again on every F5.

**Store shape migration**:
- Current: `{ token: string | null }` persisted in `localStorage`
- Target: `{ accessToken: string | null, user: { id, email, displayName } | null, setTokens(accessToken, user): void, clearTokens(): void }` with `accessToken` persisted in `sessionStorage`, `user` in memory.

---

## R3 — Axios Refresh Lock (Promise Singleton)

**Decision**: Module-level `let refreshPromise: Promise<string> | null = null` in `client.ts`.  
**Rationale**: When N concurrent requests all receive a 401, the first one creates the refresh promise; all subsequent ones attach `.then()` to the same promise. Only one actual `POST /api/auth/refresh` call hits the network. On resolution, all N original requests are retried with the new token.  
**Alternatives considered**:
- `isRefreshing: boolean` flag + queue — more lines of code, functionally equivalent.
- Separate refresh service class — over-engineered for a module-level concern.

**Navigation-from-interceptor problem**: `client.ts` is a plain module; it has no access to React Router's `useNavigate`. Solution: export a `setNavigationHandler(fn: (path: string) => void)` function from `client.ts` that `main.tsx` calls with the router's `navigate` after creating the router. This avoids circular imports.

**Exact lock flow**:
```
401 received →
  if (refreshPromise == null) {
    refreshPromise = authApi.refresh()
      .then(({ accessToken, user }) => {
        setTokens(accessToken, user);
        return accessToken;
      })
      .catch(err => { clearTokens(); navigateTo('/login'); throw err; })
      .finally(() => { refreshPromise = null; });
  }
  return refreshPromise.then(token => {
    originalRequest.headers.Authorization = `Bearer ${token}`;
    return apiClient(originalRequest);
  });
```

---

## R4 — React Query v5 Migration

**Decision**: Install `@tanstack/react-query@5` and use object-syntax API exclusively.  
**Rationale**: v5 is the current stable release. v4 positional args are removed. The codebase has zero React Query usage today so there is no migration compatibility concern.  
**Alternatives considered**:
- SWR — rejected: constitution explicitly mandates React Query.
- RTK Query — rejected: adds Redux dependency, constitution mandates Zustand for client state.

**v5 breaking changes vs v4 to be aware of**:
- `useQuery(key, fn)` → `useQuery({ queryKey, queryFn })`
- `isLoading` → `isPending` (for cache-miss state)
- `cacheTime` → `gcTime`
- `QueryClient` default: `new QueryClient({ defaultOptions: { queries: { staleTime: 60_000, retry: 2 } } })`
- Devtools: `import { ReactQueryDevtools } from '@tanstack/react-query-devtools'`

**Hook migration map**:
| Hook | From | To |
|---|---|---|
| `usePresence` | `useEffect` + manual state | `useQuery({ queryKey: ['presence', workspaceId], queryFn: ... })` |
| `useThreads` | `useEffect` + manual state | `useQuery` for list + `useMutation` for create |
| `useSearch` | manual `runSearch` callback | `useQuery({ queryKey: ['search', workspaceId, debouncedQuery], enabled: debouncedQuery.length >= 2 })` |
| `useUpgradeFlow` | `useState` + `useCallback` | `useMutation({ mutationFn: () => billingApi.createCheckoutSession(userId) })` |

**Debounce for `useSearch`**: Implement with a lightweight `useDebounce` hook using `useEffect` + `setTimeout` — this is a timing side-effect, not a data-fetching side-effect, so it does not violate the "zero data-fetching useEffect" rule.

---

## R5 — JaCoCo `ci` Maven Profile

**Decision**: Add a `ci` Maven profile containing only the `check` goal execution — keeping the `prepare-agent` and `report` goals in the base build.  
**Rationale**: The base build always runs `prepare-agent` + `report` so coverage is always measured. The `check` goal (which fails the build) is only activated in CI via `-Pci` to avoid blocking local development.  
**Alternatives considered**:
- Putting `check` in the default build — rejected: fails local builds when coverage is still low during TDD cycle.
- Separate Maven module — rejected: overkill.

**Exclusions**: `**/config/**` (Spring configuration classes — untestable with unit tests) and `**/model/**` (JPA entity POJOs — no logic to test). These are excluded from the LINE coverage denominator so the 70% threshold is meaningful.

---

## R6 — Vitest v8 Coverage

**Decision**: Install `@vitest/coverage-v8` and configure in `vitest.config.ts`.  
**Rationale**: `v8` provider uses Node.js V8's built-in coverage; it's faster and requires no Babel instrumentation. Istanbul (`@vitest/coverage-istanbul`) would be the alternative but v8 is preferred for Vite projects.  
**Configuration**:
```ts
coverage: {
  provider: 'v8',
  thresholds: { statements: 60, branches: 60, functions: 60, lines: 60 },
  exclude: ['tests/**', 'src/main.tsx', 'src/vite-env.d.ts'],
}
```
**Scripts**:
- `test:coverage`: `vitest run --coverage` — runs all tests with coverage, exits non-zero on threshold violation
- `test:ci`: `vitest run --coverage --reporter=verbose` — same but with verbose output for CI logs

---

## R7 — GitHub Actions CI Pipeline

**Decision**: Two workflow files — `ci.yml` (full test suite) and `pr-checks.yml` (lint only).  
**Rationale**: Separating fast lint checks (pr-checks) from slow tests (ci) gives faster PR feedback. E2E tests run only on `main`/`develop` pushes to avoid Playwright overhead on every feature branch commit.

**Backend job requirements**:
- PostgreSQL 16: `postgres:16-alpine` service, health check via `pg_isready`
- Redis 7: `redis:7-alpine` service
- JDK: `actions/setup-java@v4` with `java-version: '17'`
- Command: `mvn verify -Pci -B` from `./backend`
- Artifact: upload `backend/target/site/jacoco/` as `jacoco-report`

**Frontend job requirements**:
- Node 20: `actions/setup-node@v4` with `node-version: '20'`
- Commands: `npm ci`, `npm run lint`, `npm run test:ci`, `npm run build`

**E2E job conditions**: `if: github.ref == 'refs/heads/main' || github.ref == 'refs/heads/develop'`
- `docker compose up -d`
- `npx wait-on http://localhost:8080/actuator/health`  
- `npx playwright install --with-deps`
- `npm run test:e2e`

**Secrets needed in E2E env**: `POSTGRES_URL`, `JWT_SECRET`, `STRIPE_API_KEY`, `STRIPE_WEBHOOK_SECRET`, `OPENAI_API_KEY`, `FRONTEND_ORIGIN`

---

## R8 — SecretExposureAuditTest Regex Fix

**Decision**: Move the `${...}` exclusion from a whole-line check to a per-match check on the extracted literal.  
**Rationale**: The current `line.contains("${")` check skips the entire line when ANY env-var reference appears anywhere in the line. This is overly broad — a line containing both `${ENV_VAR}` and a separate hardcoded secret would be falsely allowed. The correct fix is to check the captured literal group itself.  
**Fix pattern** — add to the match loop in `isHardcodedSecret`:
```java
String literal = matcher.group(1);
// Skip env-var expression placeholders like ${STRIPE_API_KEY}
if (literal.startsWith("${") && literal.endsWith("}")) {
  continue; // not a literal secret
}
```
Or use a negative lookahead/lookbehind in the `QUOTED_LITERAL` regex:
```java
// Match quoted string NOT of the form "${...}"
Pattern.compile("[\"'](?!\\$\\{)([^\"']{6,})[\"'](?<![\"']\\$\\{[^}]*\\}[\"'])")
```
The simpler literal-level check is preferred for readability.

**Also**: The current `line.contains("${")` whole-line check should be REMOVED and replaced with the per-match check, so that lines like:
```yaml
# Use ${STRIPE_API_KEY} in production — hardcoded value "sk_live_abc123" below:
stripe.api-key: "sk_live_abc123"
```
Are still correctly flagged even though `${` appears earlier in a comment on the same line.

---

## Summary of All Resolved Unknowns

| # | Unknown | Resolution |
|---|---------|------------|
| R1 | Router API choice | `createBrowserRouter` + `RouterProvider` |
| R2 | Auth state persistence | Zustand `persist` with `sessionStorage` (accessToken only) |
| R3 | Concurrent 401 handling | Module-level Promise singleton in `client.ts` |
| R4 | React Query v5 migration | Object syntax, `useQuery`/`useMutation`, debounce hook for search |
| R5 | JaCoCo profile strategy | `ci` Maven profile with `check` goal only, 70% LINE threshold |
| R6 | Vitest coverage provider | `@vitest/coverage-v8`, 60% all dimensions |
| R7 | CI pipeline structure | Two workflow files, E2E gated on main/develop |
| R8 | SecretExposureAuditTest fix | Per-match literal check, remove whole-line `${` exclusion |
