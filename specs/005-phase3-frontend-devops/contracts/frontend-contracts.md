# Component Contracts: Phase 3 — Frontend & DevOps

**Date**: 2026-04-30  
**Convention**: All non-page components use named exports (Constitution Principle III)

---

## Auth Store Contract

**File**: `src/features/auth/store/authStore.ts`

```ts
// Public API — what consumers import
export { useAuthStore } from './authStore';

// Store shape
interface AuthUser {
  id: string;
  email: string;
  displayName: string;
}

interface AuthStore {
  accessToken: string | null;
  user: AuthUser | null;
  setTokens: (accessToken: string, user: AuthUser) => void;
  clearTokens: () => void;
}
```

**Usage contract**:
- `setTokens()` writes `accessToken` to sessionStorage and sets `user` in memory
- `clearTokens()` removes `accessToken` from sessionStorage, sets both fields to `null`
- Consumers MUST NOT write to sessionStorage directly — all token lifecycle goes through this store

---

## Axios Client Contract

**File**: `src/api/client.ts`

```ts
// Exported singleton
export default apiClient;  // AxiosInstance

// Navigation handler registration — must be called from main.tsx after router creation
export function setNavigationHandler(fn: (path: string) => void): void;

// Base URL source
// import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api'
```

**Interceptor guarantees**:
- Every outbound request with a non-null `accessToken` carries `Authorization: Bearer {token}`
- Concurrent 401 responses trigger exactly **one** `POST /api/auth/refresh` call
- On refresh failure: `clearTokens()` + navigate to `/login`
- On refresh success: original request retried automatically

---

## ProtectedRoute Contract

**File**: `src/components/ProtectedRoute.tsx`

```ts
// Named export — no props
export function ProtectedRoute(): JSX.Element;

// Behaviour
// - If authStore.accessToken is null → <Navigate to="/login" state={{ from: location }} replace />
// - If authenticated → <Outlet />
```

**Guarantees**:
- Sets `location.state.from` so `LoginPage` can redirect back after login
- Uses `useLocation()` from React Router to capture current path

---

## PricingTable Contract

**File**: `src/features/billing/components/PricingTable.tsx`

```ts
export interface PricingTableProps {
  onUpgradeClick: () => void;        // called when Free-tier upgrade CTA clicked
  isUpgrading: boolean;              // controls loading state on upgrade button
  hasActiveSubscription: boolean;    // when true, renders ManageSubscriptionButton instead
  onManageClick: () => void;         // called when manage subscription CTA clicked
}

export function PricingTable(props: PricingTableProps): JSX.Element;
```

---

## UpgradeButton Contract

**File**: `src/features/billing/components/UpgradeButton.tsx`

```ts
export interface UpgradeButtonProps {
  onClick: () => void;
  isLoading: boolean;
}

export function UpgradeButton(props: UpgradeButtonProps): JSX.Element;
```

---

## ManageSubscriptionButton Contract

**File**: `src/features/billing/components/ManageSubscriptionButton.tsx`

```ts
export interface ManageSubscriptionButtonProps {
  onClick: () => void;
  isLoading: boolean;
}

export function ManageSubscriptionButton(props: ManageSubscriptionButtonProps): JSX.Element;
```

---

## Hook Contracts (after React Query migration)

### `usePresence`

```ts
// Input
function usePresence(workspaceId: string, currentUserId: string): UsePresenceResult;

// Output
interface UsePresenceResult {
  users: PresenceRecord[];
  isPending: boolean;
  isError: boolean;
  setStatus: (status: PresenceStatus) => Promise<void>;
  refetch: () => void;
}
```

### `useThreads`

```ts
function useThreads(workspaceId: string, channelId: string): UseThreadsResult;

interface UseThreadsResult {
  threads: ThreadSummary[];
  isPending: boolean;
  isError: boolean;
  openThread: (rootMessageId: string) => void;
  activeReplies: ThreadReply[];
  isLoadingReplies: boolean;
}
```

### `useSearch`

```ts
function useSearch(workspaceId: string, defaultChannelId: string): UseSearchResult;

interface UseSearchResult {
  results: SearchResult[];
  isPending: boolean;
  isError: boolean;
  query: string;
  setQuery: (q: string) => void;       // triggers debounce
  navigateToResult: (result: SearchResult) => void;
}
```

**Debounce**: 300ms delay before query key updates. `enabled: debouncedQuery.length >= 2`.

### `useUpgradeFlow`

```ts
function useUpgradeFlow(): UseUpgradeFlowResult;

interface UseUpgradeFlowResult {
  startUpgrade: () => void;  // calls mutate()
  isPending: boolean;
  isError: boolean;
  error: Error | null;
}
```

**Note**: `userId` argument removed from public interface — resolved from `authStore` inside the hook.

---

## LoginPage Contract

**File**: `src/features/auth/components/LoginPage.tsx`

```ts
// Named export (page component — may also be default export per spec)
export function LoginPage(): JSX.Element;

// Behaviour
// - On submit: POST /api/auth/login
// - On success: calls setTokens(), navigates to location.state?.from ?? '/'
// - On error: displays i18n error key from en.json
```

---

## RegisterPage Contract

**File**: `src/features/auth/components/RegisterPage.tsx`

```ts
export function RegisterPage(): JSX.Element;

// Behaviour
// - Fields: email, password, displayName
// - On submit: POST /api/auth/register
// - On success: navigate to /login
```

---

## i18n Keys Required (en.json additions)

| Key | English value |
|-----|---------------|
| `auth.login.emailLabel` | `Email address` |
| `auth.login.passwordLabel` | `Password` |
| `auth.login.submitButton` | `Sign in` |
| `auth.login.errorInvalidCredentials` | `Invalid email or password` |
| `auth.login.errorGeneric` | `Sign in failed. Please try again.` |
| `auth.register.displayNameLabel` | `Display name` |
| `auth.register.submitButton` | `Create account` |
| `auth.register.errorEmailTaken` | `An account with this email already exists` |
| `auth.register.errorGeneric` | `Registration failed. Please try again.` |
| `billing.upgrade.errorGeneric` | `Could not start checkout. Please try again.` |
