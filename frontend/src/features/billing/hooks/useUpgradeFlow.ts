import { useCallback, useState } from 'react';
import { billingApi } from '../api/billingApi';

export type UpgradeStatus = 'idle' | 'redirecting' | 'error';

export interface UpgradeFlowState {
  status: UpgradeStatus;
  error: string | null;
  startUpgrade: () => Promise<void>;
}

/**
 * T034 — US4: Upgrade Flow Hook
 *
 * Manages the dashboard "Upgrade to Pro" CTA lifecycle:
 *   idle → redirecting → (browser navigates to Stripe checkout)
 *
 * On success the browser is redirected to the Stripe-hosted checkout page.
 * After payment Stripe redirects back to /success?session_id=<id>.
 *
 * On API failure the hook transitions to `error` so the caller can surface
 * a user-facing message rather than a silent no-op.
 */
export function useUpgradeFlow(userId: string): UpgradeFlowState {
  const [status, setStatus] = useState<UpgradeStatus>('idle');
  const [error, setError] = useState<string | null>(null);

  const startUpgrade = useCallback(async () => {
    setStatus('redirecting');
    setError(null);
    try {
      const { checkoutUrl } = await billingApi.createCheckoutSession(userId);
      // Full-page redirect to Stripe hosted checkout
      window.location.href = checkoutUrl;
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : 'Failed to start checkout. Please try again.',
      );
      setStatus('error');
    }
  }, [userId]);

  return { status, error, startUpgrade };
}
