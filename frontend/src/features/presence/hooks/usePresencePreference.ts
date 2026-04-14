import { useCallback, useState } from 'react';
import type { PresenceStatus } from '../components/PresenceBadge';

const STORAGE_KEY = 'syncdoc:presencePreference';

/**
 * Persists the user's manually chosen presence status in localStorage so the
 * preference survives page refreshes and is re-applied on the next session.
 */
export function usePresencePreference() {
  const [savedStatus, setSavedStatus] = useState<PresenceStatus | null>(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return (raw as PresenceStatus) ?? null;
    } catch {
      return null;
    }
  });

  const savePreference = useCallback((status: PresenceStatus) => {
    try {
      localStorage.setItem(STORAGE_KEY, status);
    } catch {
      /* storage may be unavailable in private browsing */
    }
    setSavedStatus(status);
  }, []);

  const clearPreference = useCallback(() => {
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      /* noop */
    }
    setSavedStatus(null);
  }, []);

  return { savedStatus, savePreference, clearPreference };
}
