import { useQuery } from '@tanstack/react-query';
import { billingApi, SubscriptionTier } from '../api/billingApi';
import { useAuthStore } from '../../auth/store/authStore';

export function useSubscriptionTier() {
  const userId = useAuthStore((s) => s.user?.id ?? '');

  const result = useQuery<SubscriptionTier>({
    queryKey: ['subscriptionTier', userId],
    queryFn: () => billingApi.getSubscriptionTier(userId),
    enabled: Boolean(userId),
    staleTime: 60_000,
  });

  // Expose `.tier` directly so callers don't need to do `result.data?.tier`
  return { ...result, tier: result.data?.tier ?? null };
}
