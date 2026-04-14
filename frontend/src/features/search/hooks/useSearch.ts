import { useCallback, useMemo, useState } from 'react';
import { SearchFilters, SearchResult, searchApi } from '../api/searchApi';
import { toWorkspaceAccessError } from '../../workspace/utils/workspaceAccessError';

export function useSearch(workspaceId: string, defaultChannelId: string) {
  const [results, setResults] = useState<SearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const runSearch = useCallback(async (query: string, filters: SearchFilters) => {
    if (!query.trim()) {
      setResults([]);
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const channelId = filters.in || defaultChannelId;
      const data = await searchApi.search(workspaceId, query, channelId, filters);
      setResults(data);
    } catch (err) {
      setError(toWorkspaceAccessError(err, 'Search request failed'));
      setResults([]);
    } finally {
      setLoading(false);
    }
  }, [workspaceId, defaultChannelId]);

  const navigateToResult = useCallback((result: SearchResult) => {
    window.location.hash = `#workspace=${result.workspaceId}&channel=${result.channelId}&seq=${result.sequenceNumber}`;
  }, []);

  return useMemo(() => ({
    results,
    loading,
    error,
    runSearch,
    navigateToResult,
  }), [results, loading, error, runSearch, navigateToResult]);
}
