import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useSearch } from './useSearch';

vi.mock('../api/searchApi', () => ({
  searchApi: {
    search: vi.fn(),
  },
}));

import { searchApi } from '../api/searchApi';

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}

beforeEach(() => {
  vi.clearAllMocks();
  vi.useFakeTimers();
});

afterEach(() => {
  vi.useRealTimers();
});

describe('useSearch', () => {
  it('does not fire API call when query is fewer than 2 characters', async () => {
    const { result } = renderHook(() => useSearch('ws1', 'general', ''), { wrapper });
    act(() => { vi.advanceTimersByTime(300); });
    expect(searchApi.search).not.toHaveBeenCalled();
    expect(result.current.data).toBeUndefined();
  });

  it('fires exactly one API call after 300ms debounce with 2+ character query', async () => {
    vi.mocked(searchApi.search).mockResolvedValue([]);

    renderHook(
      ({ query }: { query: string }) => useSearch('ws1', 'general', query),
      { wrapper, initialProps: { query: 'ab' } },
    );

    await act(async () => { vi.advanceTimersByTime(300); });

    expect(searchApi.search).toHaveBeenCalledOnce();
  });

  it('debounce prevents multiple API calls on rapid input changes', async () => {
    vi.mocked(searchApi.search).mockResolvedValue([]);

    const { rerender } = renderHook(
      ({ query }: { query: string }) => useSearch('ws1', 'general', query),
      { wrapper, initialProps: { query: 'a' } },
    );

    rerender({ query: 'ab' });
    rerender({ query: 'abc' });
    await act(async () => { vi.advanceTimersByTime(100); });
    rerender({ query: 'abcd' });
    await act(async () => { vi.advanceTimersByTime(300); });

    expect(searchApi.search).toHaveBeenCalledOnce();
  });
});
