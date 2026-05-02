import { useMutation } from '@tanstack/react-query';
import { billingApi } from '../api/billingApi';
import { useAuthStore } from '../../auth/store/authStore';

export type UpgradeStatus = 'idle' | 'redirecting' | 'error';

/**
 * T232 — Migrated to useMutation.
 * Exposes the raw mutation object so tests can call .mutate() directly.
 * Convenience aliases `startUpgrade` and `status` kept for UI components.
 */
export function useUpgradeFlow() {
  const userId = useAuthStore((s) => s.user?.id ?? '');

  const mutation = useMutation({
    mutationFn: () => billingApi.createCheckoutSession(userId),
    onSuccess: ({ checkoutUrl }) => {
      window.location.href = checkoutUrl;
    },
  });

  const startUpgrade = () => mutation.mutate();

  const status: UpgradeStatus = mutation.isPending
    ? 'redirecting'
    : mutation.isError
      ? 'error'
      : 'idle';

  const error = mutation.error instanceof Error ? mutation.error.message : null;

  return { ...mutation, status, error, startUpgrade };
}
