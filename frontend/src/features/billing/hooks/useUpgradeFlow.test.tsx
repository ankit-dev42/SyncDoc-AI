import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuthStore } from '../../auth/store/authStore';
import { useUpgradeFlow } from './useUpgradeFlow';

vi.mock('../api/billingApi', () => ({
  billingApi: {
    createCheckoutSession: vi.fn(),
    getSubscriptionTier: vi.fn(),
  },
}));

import { billingApi } from '../api/billingApi';

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}

beforeEach(() => {
  useAuthStore.getState().setTokens('tok_test', { id: 'user-1', email: 'a@b.com', displayName: 'A' });
  vi.clearAllMocks();
});

describe('useUpgradeFlow', () => {
  it('calls billingApi.createCheckoutSession with userId from authStore', async () => {
    vi.mocked(billingApi.createCheckoutSession).mockResolvedValue({
      checkoutUrl: 'https://stripe.com/pay/test',
      sessionId: 'sess_test',
    });

    const { result } = renderHook(() => useUpgradeFlow(), { wrapper });

    result.current.mutate();

    await waitFor(() => {
      expect(billingApi.createCheckoutSession).toHaveBeenCalledWith('user-1');
    });
  });

  it('isPending is true during mutation', async () => {
    // Never resolves — keeps the mutation in pending state for the whole test
    vi.mocked(billingApi.createCheckoutSession).mockImplementation(() => new Promise(() => {}));

    const { result } = renderHook(() => useUpgradeFlow(), { wrapper });
    result.current.mutate();

    await waitFor(() => {
      expect(result.current.isPending).toBe(true);
    });
  });

  it('surfaces error on mutation failure', async () => {
    vi.mocked(billingApi.createCheckoutSession).mockRejectedValue(new Error('Checkout failed'));

    const { result } = renderHook(() => useUpgradeFlow(), { wrapper });
    result.current.mutate();

    await waitFor(() => {
      expect(result.current.error).not.toBeNull();
    });
  });
});
