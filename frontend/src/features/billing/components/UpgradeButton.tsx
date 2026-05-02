import { useUpgradeFlow } from '../hooks/useUpgradeFlow';

export function UpgradeButton() {
  const { status, error, startUpgrade } = useUpgradeFlow();

  return (
    <div>
      <button
        onClick={startUpgrade}
        disabled={status === 'redirecting'}
        className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-60"
      >
        {status === 'redirecting' ? 'Redirecting to checkout…' : 'Upgrade to Pro'}
      </button>
      {status === 'error' && error ? (
        <p className="mt-2 text-sm text-red-600">{error}</p>
      ) : null}
    </div>
  );
}
