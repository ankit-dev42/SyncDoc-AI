import { defineConfig, devices } from '@playwright/test';

const nodeEnv = (globalThis as { process?: { env?: Record<string, string | undefined> } }).process?.env ?? {};

export default defineConfig({
  testDir: './tests/integration',
  timeout: 30_000,
  expect: {
    timeout: 5_000,
  },
  fullyParallel: true,
  retries: nodeEnv.CI ? 2 : 0,
  reporter: nodeEnv.CI ? [['github'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: nodeEnv.E2E_BASE_URL ?? 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: nodeEnv.CI
    ? undefined
    : {
        command: 'npm run dev -- --host 127.0.0.1 --port 5173',
        port: 5173,
        reuseExistingServer: true,
        timeout: 120_000,
      },
});