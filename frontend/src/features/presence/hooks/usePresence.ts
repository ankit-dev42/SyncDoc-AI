import { useCallback, useEffect, useMemo, useState } from 'react';
import { presenceApi, PresenceRecord } from '../api/presenceApi';
import { PresenceStatus } from '../components/PresenceBadge';
import { toWorkspaceAccessError } from '../../workspace/utils/workspaceAccessError';

export function usePresence(workspaceId: string, currentUserId: string) {
  const [users, setUsers] = useState<PresenceRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await presenceApi.list(workspaceId);
      setUsers(data);
    } catch (err) {
      setError(toWorkspaceAccessError(err, 'Failed to load presence state'));
    } finally {
      setLoading(false);
    }
  }, [workspaceId]);

  const setStatus = useCallback(async (status: PresenceStatus) => {
    await presenceApi.setStatus(workspaceId, currentUserId, status);
    await refresh();
  }, [workspaceId, currentUserId, refresh]);

  useEffect(() => {
    if (!workspaceId || !currentUserId) {
      return;
    }
    void refresh();

    const id = window.setInterval(() => {
      void presenceApi.heartbeat(workspaceId, currentUserId);
    }, 30000);

    return () => window.clearInterval(id);
  }, [workspaceId, currentUserId, refresh]);

  return useMemo(() => ({ users, loading, error, refresh, setStatus }), [users, loading, error, refresh, setStatus]);
}
