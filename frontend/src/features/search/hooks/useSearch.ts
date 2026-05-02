import { useCallback } from 'react';
import { useQuery } from '@tanstack/react-query';
import { SearchResult, searchApi } from '../api/searchApi';
import { useDebounce } from '../../../utils/useDebounce';

export function useSearch(workspaceId: string, channelId: string, query: string) {
  const debouncedQuery = useDebounce(query, 300);

  const result = useQuery<SearchResult[]>({
    queryKey: ['search', workspaceId, channelId, debouncedQuery],
    queryFn: () => searchApi.search(workspaceId, debouncedQuery, channelId, {}),
    enabled: debouncedQuery.length >= 2,
    staleTime: 30_000,
  });

  // Keep a thin imperative escape-hatch for components that use programmatic search
  const runSearch = useCallback(
    async (q: string, filters: { in?: string } = {}) => {
      const targetChannel = filters.in ?? channelId;
      await searchApi.search(workspaceId, q, targetChannel, filters);
    },
    [workspaceId, channelId],
  );

  const navigateToResult = useCallback((r: SearchResult) => {
    window.location.hash = `#workspace=${r.workspaceId}&channel=${r.channelId}&seq=${r.sequenceNumber}`;
  }, []);

  return { ...result, runSearch, navigateToResult };
}
