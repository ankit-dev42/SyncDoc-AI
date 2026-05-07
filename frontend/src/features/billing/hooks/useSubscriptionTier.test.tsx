import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuthStore } from '../../auth/store/authStore';
import { useSubscriptionTier } from './useSubscriptionTier';

vi.mock('../api/billingApi', () => ({
  billingApi: {
    getSubscriptionTier: vi.fn(),
    createCheckoutSession: vi.fn(),
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

describe('useSubscriptionTier', () => {
  it('returns { data: { tier }, isPending } via useQuery', async () => {
    vi.mocked(billingApi.getSubscriptionTier).mockResolvedValue({
      userId: 'user-1',
      tier: 'PRO',
      status: 'ACTIVE',
    });

    const { result } = renderHook(() => useSubscriptionTier(), { wrapper });
    expect(result.current.isPending).toBe(true);

    await waitFor(() => {
      expect(result.current.isPending).toBe(false);
    });

    expect(result.current.tier).toBe('PRO');
  });

  it('isPending is true before resolution', () => {
    vi.mocked(billingApi.getSubscriptionTier).mockImplementation(() => new Promise(() => {}));
    const { result } = renderHook(() => useSubscriptionTier(), { wrapper });
    expect(result.current.isPending).toBe(true);
  });

  it('sources userId from authStore', async () => {
    vi.mocked(billingApi.getSubscriptionTier).mockResolvedValue({
      userId: 'user-1',
      tier: 'FREE',
      status: 'ACTIVE',
    });

    renderHook(() => useSubscriptionTier(), { wrapper });

    await waitFor(() => {
      expect(billingApi.getSubscriptionTier).toHaveBeenCalledWith('user-1');
    });
  });
});
