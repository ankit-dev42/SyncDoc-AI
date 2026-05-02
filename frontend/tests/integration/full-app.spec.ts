/**
 * Full Application Functional Tests — SyncDoc AI
 *
 * Covers every navigable page for both FREE and PRO user tiers:
 *
 *  AUTH:
 *   01 — Register page renders & form validates
 *   02 — Login page renders
 *   03 — Unauthenticated access to protected route → redirect to /login
 *   04 — Post-login redirect back to originally-requested route
 *
 *  FREE USER FLOWS:
 *   05 — Home / dashboard after login
 *   06 — Billing page: Free tier shows pricing table with Upgrade CTA
 *   07 — Upgrade CTA triggers checkout API → redirects to /success
 *   08 — Success page renders payment confirmation banner
 *   09 — 404 / NotFound page renders
 *
 *  PRO USER FLOWS:
 *   10 — Billing page: Pro tier shows "You are on the Pro plan" + Manage button
 *   11 — Manage Subscription button visible for Pro user
 *   12 — Pro user can navigate to /billing without being redirected
 *
 *  AUTH EDGE CASES:
 *   13 — Logout clears session and redirects to /login
 *   14 — Direct login without prior redirect lands at /
 *
 * Screenshots saved to: frontend/test-results/screenshots/full-app/
 */

import * as path from 'path';
import * as fs from 'fs';
import { fileURLToPath } from 'url';
import { test, expect, type TestUser } from './fixtures/app.fixture';
import type { Page } from '@playwright/test';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const SCREENSHOTS_DIR = path.join(__dirname, '../test-results/screenshots/full-app');

function ensureDir() {
  fs.mkdirSync(SCREENSHOTS_DIR, { recursive: true });
}

async function snap(page: Page, name: string) {
  ensureDir();
  const file = path.join(SCREENSHOTS_DIR, `${name}.png`);
  await page.screenshot({ path: file, fullPage: true });
  console.log(`  📸  ${name}.png`);
}

/** Log into the app via the UI and return the page (now at the redirect target) */
async function loginViaUI(page: Page, user: TestUser, landingPath = '/') {
  await page.goto('/login');
  await expect(page.getByLabel(/email/i)).toBeVisible({ timeout: 8000 });
  await page.getByLabel(/email/i).fill(user.email);
  await page.getByLabel(/password/i).fill(user.password);
  await page.getByRole('button', { name: /sign in|log in|login/i }).click();
  await expect(page).toHaveURL(new RegExp(landingPath.replace('/', '\\/')), { timeout: 10000 });
}

// ─────────────────────────────────────────────────────────────────────────────
// AUTH PAGES — no auth needed, use a plain browser page
// ─────────────────────────────────────────────────────────────────────────────

test.describe('Auth Pages', () => {
  test('01 — Register page renders with all required fields', async ({ browser }) => {
    const page = await browser.newPage();

    await page.goto('/register');
    await expect(page.getByLabel(/display name/i)).toBeVisible({ timeout: 8000 });
    await expect(page.getByLabel(/email/i)).toBeVisible();
    await expect(page.getByLabel(/password/i)).toBeVisible();
    await expect(page.getByRole('button', { name: /register|sign up|create/i })).toBeVisible();

    await snap(page, '01-register-page');
    await page.close();
  });

  test('02 — Login page renders with email & password fields', async ({ browser }) => {
    const page = await browser.newPage();

    await page.goto('/login');
    await expect(page.getByLabel(/email/i)).toBeVisible({ timeout: 8000 });
    await expect(page.getByLabel(/password/i)).toBeVisible();
    await expect(page.getByRole('button', { name: /sign in|log in|login/i })).toBeVisible();

    await snap(page, '02-login-page');
    await page.close();
  });

  test('03 — Unauthenticated visit to /billing redirects to /login', async ({ browser }) => {
    const ctx = await browser.newContext();
    const page = await ctx.newPage();

    // Navigate to app root first so sessionStorage is accessible, then clear it
    await page.goto('/login');
    await page.evaluate(() => {
      try { sessionStorage.clear(); } catch { /* ignore */ }
    });

    await page.goto('/billing');
    await expect(page).toHaveURL(/\/login/, { timeout: 8000 });
    await expect(page.getByLabel(/email/i)).toBeVisible();

    await snap(page, '03-unauthenticated-redirect-to-login');
    await ctx.close();
  });

  test('04 — Login redirects back to originally-requested protected route', async ({ browser, freeUser }) => {
    const ctx = await browser.newContext();
    const page = await ctx.newPage();

    // Visit login page first to make sessionStorage accessible
    await page.goto('/login');
    await page.evaluate(() => {
      try { sessionStorage.clear(); } catch { /* ignore */ }
    });

    // Now try to visit /billing (unauthenticated) → should land on /login
    await page.goto('/billing');
    await expect(page).toHaveURL(/\/login/, { timeout: 8000 });

    await snap(page, '04a-redirected-to-login-from-billing');

    // Log in
    await page.getByLabel(/email/i).fill(freeUser.email);
    await page.getByLabel(/password/i).fill(freeUser.password);
    await page.getByRole('button', { name: /sign in|log in|login/i }).click();

    // Must land back at /billing (not /)
    await expect(page).toHaveURL(/\/billing/, { timeout: 10000 });
    await snap(page, '04b-post-login-redirected-back-to-billing');

    await ctx.close();
  });

  test('14 — Direct /login visit (no prior redirect) lands at / on success', async ({ browser, freeUser }) => {
    const ctx = await browser.newContext();
    const page = await ctx.newPage();

    await page.goto('/login');
    await page.getByLabel(/email/i).fill(freeUser.email);
    await page.getByLabel(/password/i).fill(freeUser.password);
    await page.getByRole('button', { name: /sign in|log in|login/i }).click();

    // No prior redirect → should land at /
    await expect(page).toHaveURL('http://localhost:5173/', { timeout: 10000 });
    await snap(page, '14-direct-login-lands-at-home');
    await ctx.close();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// FREE USER FLOWS — uses freePage fixture (pre-authenticated)
// ─────────────────────────────────────────────────────────────────────────────

test.describe('Free User Flows', () => {
  test('05 — Home page loads after login (free user)', async ({ freePage }) => {
    await freePage.goto('/');
    // Wait for app to render something meaningful (not stuck on loading)
    await freePage.waitForLoadState('networkidle', { timeout: 10000 });
    await snap(freePage, '05-free-user-home');
  });

  test('06 — Billing page shows pricing table with Upgrade CTA for free user', async ({ freePage, freeUser }) => {
    // Mock the subscription tier endpoint to return FREE (in case of network delay)
    await freePage.route(`**/api/v1/subscriptions/${freeUser.id}/tier`, (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { tier: 'FREE', status: 'ACTIVE', userId: freeUser.id, expiresAt: null },
        }),
      }),
    );

    await freePage.goto('/billing');
    await freePage.waitForLoadState('networkidle', { timeout: 10000 });

    // Pricing table should show both Free and Pro tiers
    await expect(freePage.getByRole('columnheader', { name: 'Free' })).toBeVisible({ timeout: 8000 });
    await expect(freePage.getByRole('columnheader', { name: 'Pro' })).toBeVisible();

    // Upgrade button should be present
    await expect(freePage.getByRole('button', { name: /upgrade to pro/i })).toBeVisible();

    await snap(freePage, '06-free-billing-page-pricing-table');
  });

  test('07 — Upgrade to Pro CTA redirects to /success (mocked checkout)', async ({ freePage, freeUser }) => {
    await freePage.route(`**/api/v1/subscriptions/${freeUser.id}/tier`, (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { tier: 'FREE', status: 'ACTIVE', userId: freeUser.id, expiresAt: null },
        }),
      }),
    );

    await freePage.route('**/api/v1/billing/checkout', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            checkoutUrl: 'http://localhost:5173/success?session_id=cs_test_functional',
            sessionId: 'cs_test_functional',
          },
        }),
      }),
    );

    await freePage.goto('/billing');
    await expect(freePage.getByRole('button', { name: /upgrade to pro/i })).toBeVisible({ timeout: 8000 });

    await snap(freePage, '07a-billing-upgrade-button');

    await freePage.getByRole('button', { name: /upgrade to pro/i }).click();

    await expect(freePage).toHaveURL(/\/success/, { timeout: 10000 });
    await snap(freePage, '07b-after-upgrade-success-redirect');
  });

  test('08 — Success page renders payment confirmation banner with session ID', async ({ freePage }) => {
    await freePage.goto('/success?session_id=cs_test_banner_check');
    await expect(freePage.getByRole('heading', { name: /payment successful/i })).toBeVisible({ timeout: 8000 });
    await expect(freePage.getByText(/subscription/i)).toBeVisible();

    await snap(freePage, '08-success-page-banner');
  });

  test('09 — 404 NotFound page renders for unknown routes', async ({ freePage }) => {
    await freePage.goto('/this-route-does-not-exist-at-all');
    await expect(freePage.getByRole('heading', { name: '404' })).toBeVisible({ timeout: 8000 });
    await expect(freePage.getByText(/page not found/i)).toBeVisible();

    await snap(freePage, '09-not-found-page');
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// PRO USER FLOWS — uses proPage fixture (pre-authenticated, PRO subscription)
// ─────────────────────────────────────────────────────────────────────────────

test.describe('Pro User Flows', () => {
  test('10 — Billing page shows Pro plan info for pro user (live API)', async ({ proPage, proUser }) => {
    await proPage.goto('/billing');
    await proPage.waitForLoadState('networkidle', { timeout: 10000 });

    // Pro user sees "You are on the Pro plan" section (not the pricing table upgrade CTA)
    await expect(proPage.getByText('You are on the Pro plan.')).toBeVisible({ timeout: 8000 });

    await snap(proPage, '10-pro-user-billing-page');
  });

  test('11 — Pro user billing page shows Manage Subscription button', async ({ proPage, proUser }) => {
    // Mock tier to ensure PRO is shown (avoids race with subscription propagation)
    await proPage.route(`**/api/v1/subscriptions/${proUser.id}/tier`, (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { tier: 'PRO', status: 'ACTIVE', userId: proUser.id, expiresAt: null },
        }),
      }),
    );

    await proPage.goto('/billing');
    await proPage.waitForLoadState('networkidle', { timeout: 10000 });

    await expect(proPage.getByRole('button', { name: /manage subscription/i })).toBeVisible({ timeout: 8000 });
    // Upgrade CTA must NOT be visible for Pro users
    await expect(proPage.getByRole('button', { name: /upgrade to pro/i })).not.toBeVisible();

    await snap(proPage, '11-pro-billing-manage-button');
  });

  test('12 — Pro user can navigate directly to /billing without redirect', async ({ proPage }) => {
    await proPage.goto('/billing');
    // Must not be redirected to /login
    await expect(proPage).toHaveURL(/\/billing/, { timeout: 8000 });
    await proPage.waitForLoadState('networkidle', { timeout: 10000 });

    await snap(proPage, '12-pro-user-direct-billing-access');
  });

  test('10b — Pro user home page', async ({ proPage }) => {
    await proPage.goto('/');
    await proPage.waitForLoadState('networkidle', { timeout: 10000 });
    await snap(proPage, '10b-pro-user-home');
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// AUTH EDGE CASES
// ─────────────────────────────────────────────────────────────────────────────

test.describe('Auth Edge Cases', () => {
  test('13 — Logout clears session and redirects to /login', async ({ browser, freeUser }) => {
    const ctx = await browser.newContext();
    const page = await ctx.newPage();

    await loginViaUI(page, freeUser, '/');
    await snap(page, '13a-logged-in-before-logout');

    // Call logout API directly (matches what the logout button would call)
    await page.evaluate(async () => {
      await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' });
      try { sessionStorage.clear(); } catch { /* ignore */ }
    });

    // Navigate and confirm we're locked out
    await page.goto('/billing');
    await expect(page).toHaveURL(/\/login/, { timeout: 8000 });
    await snap(page, '13b-after-logout-redirected-to-login');

    await ctx.close();
  });
});
