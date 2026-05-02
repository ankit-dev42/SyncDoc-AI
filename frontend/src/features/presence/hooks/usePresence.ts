import { useCallback, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { presenceApi, PresenceRecord } from '../api/presenceApi';
import { PresenceStatus } from '../components/PresenceBadge';

export function usePresence(workspaceId: string, currentUserId: string) {
  const result = useQuery<PresenceRecord[]>({
    queryKey: ['presence', workspaceId],
    queryFn: () => presenceApi.list(workspaceId),
    enabled: Boolean(workspaceId && currentUserId),
    staleTime: 30_000,
  });

  const setStatus = useCallback(
    async (status: PresenceStatus) => {
      await presenceApi.setStatus(workspaceId, currentUserId, status);
      await result.refetch();
    },
    [workspaceId, currentUserId, result],
  );

  // Heartbeat runs outside React Query — fire-and-forget, no state
  useEffect(() => {
    if (!workspaceId || !currentUserId) return;

    const id = window.setInterval(() => {
      void presenceApi.heartbeat(workspaceId, currentUserId);
    }, 30_000);

    return () => window.clearInterval(id);
  }, [workspaceId, currentUserId]);

  return { ...result, setStatus };
}
