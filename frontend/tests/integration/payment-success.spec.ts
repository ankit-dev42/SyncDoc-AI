import { test, expect } from './fixtures/auth.fixture';

/**
 * T031 — US4: Payment Success Flow E2E
 *
 * Validates the end-to-end upgrade → checkout → success path.
 * The entire flow MUST complete within 30 seconds (hard build gate).
 *
 * Requires:
 *  - A running frontend dev server (port 5173 via playwright.config.ts webServer)
 *  - App renders <PaymentSuccessBanner> at /success?session_id=*
 *  - App renders an "Upgrade to Pro" CTA that calls POST /api/v1/billing/checkout
 */

test.describe('Payment Success Flow (US4)', () => {
  /**
   * Core gate: navigating directly to the post-payment redirect URL must render
   * the success banner with correct i18n strings within the latency budget.
   */
  test('success page renders confirmation banner within 30 seconds', async ({ authPage }) => {
    const startMs = Date.now();

    await authPage.goto('/success?session_id=cs_test_abc8675309');

    await expect(
      authPage.getByRole('heading', { name: 'Payment successful' }),
    ).toBeVisible();

    await expect(
      authPage.getByText('Your subscription has been updated successfully.'),
    ).toBeVisible();

    const durationSeconds = (Date.now() - startMs) / 1000;
    expect(
      durationSeconds,
      `Flow took ${durationSeconds.toFixed(2)}s — must be under 30s`,
    ).toBeLessThan(30);
  });

  /**
   * Upgrade path: clicking the "Upgrade to Pro" CTA calls the billing API,
   * which resolves to a local success URL (mocked via Playwright route intercept).
   * After mock redirect the success banner must be visible.
   */
  test('upgrade CTA redirects to success page when checkout API succeeds', async ({
    authPage,
  }) => {
    // Intercept the backend checkout session call and return a local success URL
    await authPage.route('**/api/v1/billing/checkout', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            checkoutUrl: 'http://localhost:5173/success?session_id=cs_test_mock_e2e',
            sessionId: 'cs_test_mock_e2e',
          },
        }),
      }),
    );

    await authPage.goto('/');

    // The dashboard must expose an Upgrade CTA for this test to pass
    await authPage.getByRole('button', { name: /Upgrade to Pro/i }).click();

    // After redirect to success URL the banner should appear
    await authPage.waitForURL('**/success**', { timeout: 10_000 });

    await expect(
      authPage.getByRole('heading', { name: 'Payment successful' }),
    ).toBeVisible();
  });
});
