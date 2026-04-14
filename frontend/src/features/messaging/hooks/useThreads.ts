import { useCallback, useEffect, useMemo, useState } from 'react';
import { threadApi, ThreadReply, ThreadSummary } from '../api/threadApi';
import { toWorkspaceAccessError } from '../../workspace/utils/workspaceAccessError';

export function useThreads(workspaceId: string, channelId: string) {
  const [threads, setThreads] = useState<ThreadSummary[]>([]);
  const [activeReplies, setActiveReplies] = useState<ThreadReply[]>([]);
  const [activeThreadId, setActiveThreadId] = useState<string | null>(null);
  const [loadingThreads, setLoadingThreads] = useState(false);
  const [loadingReplies, setLoadingReplies] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refreshThreads = useCallback(async () => {
    if (!workspaceId || !channelId) {
      return;
    }

    setLoadingThreads(true);
    setError(null);
    try {
      const data = await threadApi.listThreads(workspaceId, channelId);
      setThreads(data);
    } catch (err) {
      setError(toWorkspaceAccessError(err, 'Failed to load thread list'));
    } finally {
      setLoadingThreads(false);
    }
  }, [workspaceId, channelId]);

  const openThread = useCallback(async (rootMessageId: string) => {
    if (!workspaceId || !channelId) {
      return;
    }

    setLoadingReplies(true);
    setError(null);
    setActiveThreadId(rootMessageId);
    try {
      const replies = await threadApi.getReplies(workspaceId, channelId, rootMessageId);
      setActiveReplies(replies);
    } catch (err) {
      setError(toWorkspaceAccessError(err, 'Failed to load thread replies'));
      setActiveReplies([]);
    } finally {
      setLoadingReplies(false);
    }
  }, [workspaceId, channelId]);

  useEffect(() => {
    void refreshThreads();
  }, [refreshThreads]);

  return useMemo(() => ({
    threads,
    activeReplies,
    activeThreadId,
    loadingThreads,
    loadingReplies,
    error,
    refreshThreads,
    openThread,
  }), [threads, activeReplies, activeThreadId, loadingThreads, loadingReplies, error, refreshThreads, openThread]);
}
