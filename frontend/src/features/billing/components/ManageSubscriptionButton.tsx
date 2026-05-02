import { useState } from 'react';
import { billingApi } from '../api/billingApi';
import { useAuthStore } from '../../auth/store/authStore';

export function ManageSubscriptionButton() {
  const userId = useAuthStore((s) => s.user?.id ?? '');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleClick = async () => {
    setLoading(true);
    setError(null);
    try {
      const { portalUrl } = await billingApi.getBillingPortal(userId);
      window.location.href = portalUrl;
    } catch {
      setError('Failed to open billing portal. Please try again.');
      setLoading(false);
    }
  };

  return (
    <>
      <button
        onClick={handleClick}
        disabled={loading}
        className="rounded border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-600 hover:bg-slate-50 disabled:opacity-60"
      >
        {loading ? 'Opening portal…' : 'Manage subscription'}
      </button>
      {error ? <p className="mt-1 text-xs text-red-600">{error}</p> : null}
    </>
  );
}
