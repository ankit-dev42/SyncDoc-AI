/**
 * App Test Fixtures
 *
 * Extends Playwright's base test with:
 *   - `freePage`  — a Page pre-authenticated as the FREE user
 *   - `proPage`   — a Page pre-authenticated as the PRO user
 *   - `freeUser`  — the free user's credentials/id
 *   - `proUser`   — the pro user's credentials/id
 *
 * Authentication is injected by writing the accessToken into sessionStorage
 * via addInitScript (runs before any page script), mirroring what authStore does.
 */

import { test as base, type Page, type BrowserContext } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const USERS_FILE = path.join(__dirname, '.test-users.json');

export interface TestUser {
  email: string;
  password: string;
  id: string;
  accessToken: string;
}

interface TestUsers {
  free: TestUser;
  pro: TestUser;
}

function loadUsers(): TestUsers {
  if (!fs.existsSync(USERS_FILE)) {
    throw new Error(
      `[app.fixture] ${USERS_FILE} not found.\n` +
      'Run the setup scripts first:\n' +
      '  ./scripts/create-free-user.sh\n' +
      '  ./scripts/create-master-player.sh\n' +
      '  ./scripts/setup-e2e-users.sh',
    );
  }
  return JSON.parse(fs.readFileSync(USERS_FILE, 'utf-8')) as TestUsers;
}

type AppFixtures = {
  freeUser: TestUser;
  proUser: TestUser;
  freePage: Page;
  proPage: Page;
};

/**
 * Injects accessToken into sessionStorage before any page scripts run,
 * so the Zustand authStore hydrates with the token on first render.
 */
async function injectAuth(context: BrowserContext, user: TestUser) {
  // The key used by Zustand persist middleware for the authStore
  const storageKey = 'auth-storage';
  const storageValue = JSON.stringify({
    state: { accessToken: user.accessToken, user: { id: user.id, email: user.email } },
    version: 0,
  });

  await context.addInitScript(
    ({ key, value }: { key: string; value: string }) => {
      try {
        sessionStorage.setItem(key, value);
      } catch {
        // sessionStorage may not be available on about:blank — safe to ignore
      }
    },
    { key: storageKey, value: storageValue },
  );
}

export const test = base.extend<AppFixtures>({
  freeUser: async ({}, use) => {
    await use(loadUsers().free);
  },

  proUser: async ({}, use) => {
    await use(loadUsers().pro);
  },

  freePage: async ({ browser, freeUser }, use) => {
    const context = await browser.newContext();
    await injectAuth(context, freeUser);
    const page = await context.newPage();
    await use(page);
    await context.close();
  },

  proPage: async ({ browser, proUser }, use) => {
    const context = await browser.newContext();
    await injectAuth(context, proUser);
    const page = await context.newPage();
    await use(page);
    await context.close();
  },
});

export { expect } from '@playwright/test';
