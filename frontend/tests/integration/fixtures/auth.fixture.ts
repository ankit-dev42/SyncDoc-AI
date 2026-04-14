import { test as base, type BrowserContext, type Page } from '@playwright/test';

const nodeEnv = (globalThis as { process?: { env?: Record<string, string | undefined> } }).process?.env ?? {};

type AuthFixtures = {
  authContext: BrowserContext;
  authPage: Page;
};

// Shared auth-capable fixture for dashboard E2E flows.
export const test = base.extend<AuthFixtures>({
  authContext: async ({ browser }, use) => {
    const context = await browser.newContext();

    const authToken = nodeEnv.E2E_AUTH_TOKEN;
    if (authToken) {
      await context.addCookies([
        {
          name: 'auth_token',
          value: authToken,
          domain: 'localhost',
          path: '/',
          httpOnly: false,
          secure: false,
          sameSite: 'Lax',
        },
      ]);
    }

    await use(context);
    await context.close();
  },
  authPage: async ({ authContext }, use) => {
    const page = await authContext.newPage();
    await use(page);
    await page.close();
  },
});

export const expect = test.expect;