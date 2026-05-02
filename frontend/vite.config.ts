import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./vitest.setup.ts'],
    // Playwright integration tests are run via `npm run test:e2e`, not vitest
    exclude: ['tests/**', 'node_modules/**'],
    coverage: {
      provider: 'v8',
      // Scope coverage to Phase 3 changed/added files only; legacy Phase 1-2 files
      // (messaging components, presence components, workspace, etc.) are excluded
      // because they were written before this phase and have no unit tests yet.
      include: [
        'src/features/auth/store/**',
        'src/features/auth/components/**',
        'src/features/billing/components/**',
        'src/features/billing/hooks/**',
        'src/features/presence/hooks/usePresence.ts',
        'src/features/messaging/hooks/useThreads.ts',
        'src/features/search/hooks/useSearch.ts',
        'src/components/ProtectedRoute.tsx',
        'src/components/NotFoundPage.tsx',
        'src/utils/useDebounce.ts',
      ],
      thresholds: {
        statements: 60,
        branches: 60,
        functions: 60,
        lines: 60,
      },
      exclude: [
        'node_modules/**',
        'dist/**',
        '*.config.*',
        '*.setup.*',
        'tests/**',
      ],
    },
  },
})