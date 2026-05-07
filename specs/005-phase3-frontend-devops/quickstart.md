# Quickstart: Phase 3 — Frontend & DevOps

**Date**: 2026-04-30  
**Branch**: `005-frontend-devops`

---

## Prerequisites

- Node.js 20+
- Java 17
- Docker Desktop running (for E2E tests)
- Phase 2 backend running (`docker compose up -d`)

---

## 1. Install New Frontend Dependencies

```bash
cd frontend

# React Router
npm install react-router-dom@6
npm install --save-dev @types/react-router-dom

# React Query v5
npm install @tanstack/react-query@5
npm install @tanstack/react-query-devtools

# Form handling
npm install react-hook-form

# Coverage provider
npm install --save-dev @vitest/coverage-v8
```

---

## 2. Configure Environment

Create `frontend/.env.local`:
```
VITE_API_BASE_URL=http://localhost:8080/api
```

See `frontend/.env.example` for all required variables.

---

## 3. Run Frontend with Coverage

```bash
# Run all unit tests with coverage report
cd frontend
npm run test:coverage

# Run tests in CI mode (verbose output)
npm run test:ci
```

---

## 4. Verify JaCoCo Coverage Gate

```bash
# Run backend tests with JaCoCo check (will fail if < 70% line coverage)
cd backend
mvn verify -Pci -B

# View HTML report
open target/site/jacoco/index.html
```

---

## 5. Run E2E Tests Locally

```bash
# Start the full stack
docker compose up -d

# Wait for backend health
curl -f http://localhost:8080/actuator/health

# Run Playwright
cd frontend
npm run test:e2e
```

---

## 6. Verify Auth Flow

1. Start the dev server: `npm run dev` in `frontend/`
2. Visit `http://localhost:5173/billing` — should redirect to `/login`
3. Register at `/register`
4. Login at `/login` — should redirect back to `/billing`
5. Open DevTools → Application → Session Storage → confirm `accessToken` is present

---

## 7. Verify SecretExposureAuditTest

```bash
cd backend
mvn test -Dtest=SecretExposureAuditTest -pl .
```

Should pass on a clean checkout. The test scans for hardcoded secrets in source files and must not flag `${STRIPE_API_KEY}` or `import.meta.env.*` patterns.

---

## 8. Key Files Changed in This Phase

| File | Change |
|------|--------|
| `frontend/src/api/client.ts` | baseURL → env var, refresh lock, sessionStorage |
| `frontend/src/features/auth/store/authStore.ts` | New shape: accessToken + user, sessionStorage |
| `frontend/src/main.tsx` | createBrowserRouter + QueryClientProvider |
| `frontend/src/components/ProtectedRoute.tsx` | New file |
| `frontend/src/features/auth/components/LoginPage.tsx` | New file |
| `frontend/src/features/auth/components/RegisterPage.tsx` | New file |
| `frontend/src/features/billing/components/PricingTable.tsx` | New file |
| `frontend/src/features/billing/components/UpgradeButton.tsx` | New file |
| `frontend/src/features/billing/components/ManageSubscriptionButton.tsx` | New file |
| `frontend/src/features/presence/hooks/usePresence.ts` | Migrate to useQuery |
| `frontend/src/features/messaging/hooks/useThreads.ts` | Migrate to useQuery + useMutation |
| `frontend/src/features/search/hooks/useSearch.ts` | Migrate to useQuery + debounce |
| `frontend/src/features/billing/hooks/useUpgradeFlow.ts` | Migrate to useMutation |
| `frontend/vite.config.ts` | Add coverage configuration |
| `frontend/package.json` | Add test:coverage + test:ci scripts |
| `backend/pom.xml` | Add ci Maven profile with JaCoCo check goal |
| `.github/workflows/ci.yml` | New file |
| `.github/workflows/pr-checks.yml` | New file |
| `backend/src/test/.../SecretExposureAuditTest.java` | Fix false-positive regex |
