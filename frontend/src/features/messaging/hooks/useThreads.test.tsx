import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useThreads } from './useThreads';

vi.mock('../api/threadApi', () => ({
  threadApi: {
    listThreads: vi.fn(),
    getReplies: vi.fn(),
    createThread: vi.fn(),
  },
}));

import { threadApi } from '../api/threadApi';

const mockThreads = [{ id: 't1', workspaceId: 'ws1', channelId: 'general', rootMessageId: 'm1', replyCount: 2, lastActivityAt: '2026-04-30T00:00:00Z' }];

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe('useThreads', () => {
  it('returns threads list using useQuery', async () => {
    vi.mocked(threadApi.listThreads).mockResolvedValue(mockThreads);
    const { result } = renderHook(() => useThreads('ws1', 'general'), { wrapper });

    expect(result.current.isPending).toBe(true);

    await waitFor(() => {
      expect(result.current.isPending).toBe(false);
    });

    expect(result.current.data).toEqual(mockThreads);
  });

  it('createThread mutation returns { mutate, isPending }', () => {
    vi.mocked(threadApi.listThreads).mockResolvedValue([]);
    const { result } = renderHook(() => useThreads('ws1', 'general'), { wrapper });
    expect(typeof result.current.createThread.mutate).toBe('function');
    expect(result.current.createThread.isPending).toBe(false);
  });
});
