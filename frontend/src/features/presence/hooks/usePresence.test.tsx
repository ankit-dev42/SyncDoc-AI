import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { usePresence } from './usePresence';

vi.mock('../api/presenceApi', () => ({
  presenceApi: {
    list: vi.fn(),
    heartbeat: vi.fn(),
    setStatus: vi.fn(),
  },
}));

import { presenceApi } from '../api/presenceApi';

const mockPresence = [{ userId: 'u1', workspaceId: 'ws1', status: 'ONLINE' as const, lastSeenAt: '2026-04-30T00:00:00Z' }];

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe('usePresence', () => {
  it('returns data using useQuery (not useEffect)', async () => {
    vi.mocked(presenceApi.list).mockResolvedValue(mockPresence);
    const { result } = renderHook(() => usePresence('ws1', 'u1'), { wrapper });

    // Initially isPending should be true
    expect(result.current.isPending).toBe(true);

    await waitFor(() => {
      expect(result.current.isPending).toBe(false);
    });

    expect(result.current.data).toEqual(mockPresence);
  });

  it('isPending is true before resolution', () => {
    vi.mocked(presenceApi.list).mockImplementation(() => new Promise(() => {}));
    const { result } = renderHook(() => usePresence('ws1', 'u1'), { wrapper });
    expect(result.current.isPending).toBe(true);
  });
});
