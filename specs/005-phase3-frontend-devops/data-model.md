# Data Model: Phase 3 — Frontend & DevOps

**Date**: 2026-04-30  
**Branch**: `005-frontend-devops`

---

## Frontend Entities

### AuthStore (Zustand)

**File**: `src/features/auth/store/authStore.ts`

| Field | Type | Persistence | Notes |
|-------|------|-------------|-------|
| `accessToken` | `string \| null` | `sessionStorage` | JWT access token — tab-scoped |
| `user` | `AuthUser \| null` | in-memory only | Wiped on tab close |

**AuthUser shape** (identity only — no subscription tier):
```ts
interface AuthUser {
  id: string;
  email: string;
  displayName: string;
}
```

**Store actions**:
```ts
interface AuthState {
  accessToken: string | null;
  user: AuthUser | null;
  setTokens: (accessToken: string, user: AuthUser) => void;
  clearTokens: () => void;
}
```

**Migration from current state**:
- Remove `token` field → rename to `accessToken`
- Remove `login()` and `register()` methods (auth side-effects belong in LoginPage/RegisterPage, not the store)
- Remove `logout()` method → replace with `clearTokens()`
- Add `user: AuthUser | null`
- Switch from `localStorage` to `sessionStorage` persistence

---

### RefreshLock

**File**: `src/api/client.ts` (module-level)

| Variable | Type | Scope | Notes |
|----------|------|-------|-------|
| `refreshPromise` | `Promise<string> \| null` | Module singleton | Prevents concurrent refresh calls |

State transitions:
```
null → Promise<string>   (first 401 received, refresh call initiated)
Promise<string> → null   (refresh resolved or rejected, via .finally())
```

---

### QueryClient Configuration

**File**: `src/main.tsx`

```ts
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60_000,   // 60 seconds — prevents redundant refetches on navigation
      retry: 2,            // retry failed queries twice before error state
    },
  },
});
```

Shared as singleton via `QueryClientProvider` wrapping the entire tree.

---

### React Query Key Conventions

| Hook | Query Key | Notes |
|------|-----------|-------|
| `usePresence` | `['presence', workspaceId]` | Invalidated on manual refresh |
| `useThreads` | `['threads', workspaceId, channelId]` | Create mutation invalidates this key |
| `useSearch` | `['search', workspaceId, debouncedQuery]` | `enabled: debouncedQuery.length >= 2` |
| Subscription tier | `['subscription', userId]` | Fetched in BillingPage, not in authStore |

---

### ProtectedRoute Props

**File**: `src/components/ProtectedRoute.tsx`

```ts
interface ProtectedRouteProps {
  // No props — reads authStore directly via useAuthStore()
  // Renders <Outlet /> when authenticated
  // Renders <Navigate to="/login" state={{ from: location }} replace /> when not
}
```

---

### Route Configuration

**File**: `src/main.tsx`

```ts
const router = createBrowserRouter([
  {
    path: '/',
    element: <ProtectedRoute />,     // wraps all authenticated routes
    children: [
      { index: true, element: <DashboardPage /> },
      { path: 'billing', element: <BillingPage /> },
      { path: 'projects/:projectId', element: <ProjectPage /> },
    ],
  },
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  { path: '/success', element: <SuccessPage /> },  // public — Stripe redirect
  { path: '*', element: <NotFoundPage /> },
]);
```

---

## Backend Entities (JaCoCo configuration)

### JaCoCo Maven Profile

**File**: `backend/pom.xml` (new `<profiles>` section)

| Config | Value |
|--------|-------|
| Profile ID | `ci` |
| Goal | `check` bound to `verify` phase |
| Counter | `LINE` |
| Minimum ratio | `0.70` (70%) |
| Excludes | `**/config/**`, `**/model/**` |

---

## CI/CD Entities

### GitHub Actions Workflow Jobs

**File**: `.github/workflows/ci.yml`

| Job | Trigger | Services | Key Steps |
|-----|---------|----------|-----------|
| `backend` | push to any branch | postgres:16-alpine, redis:7-alpine | `mvn verify -Pci -B`, upload jacoco-report |
| `frontend` | push to any branch | none | `npm ci`, `npm run lint`, `npm run test:ci`, `npm run build` |
| `e2e` | push to main/develop only | via docker-compose | `docker compose up -d`, playwright run |

**File**: `.github/workflows/pr-checks.yml`

| Job | Trigger | Steps |
|-----|---------|-------|
| `lint` | PR opened/synchronized | Checkstyle (backend), ESLint (frontend) |

---

## State Transitions

### Auth Token Lifecycle

```
[No token] 
  → login() → setTokens(accessToken, user) → [Authenticated]
  → API 401 → refresh lock → setTokens(newToken, user) → [Authenticated]
  → refresh fails → clearTokens() → navigate('/login') → [No token]
  → tab closed → sessionStorage cleared → [No token on next tab open]
```

### Axios Interceptor States

```
Request outgoing:
  accessToken present → attach Authorization: Bearer {token}
  accessToken null    → request sent without Authorization

Response incoming (401):
  refreshPromise == null → create refresh promise
  refreshPromise != null → attach to existing promise
  Promise resolves → retry original request with new token
  Promise rejects  → clearTokens() + navigate('/login') + propagate error
```
