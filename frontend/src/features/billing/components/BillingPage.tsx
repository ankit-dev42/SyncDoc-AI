import { useSubscriptionTier } from '../hooks/useSubscriptionTier';
import { useUpgradeFlow } from '../hooks/useUpgradeFlow';
import { PricingTable } from './PricingTable';
import { ManageSubscriptionButton } from './ManageSubscriptionButton';

export function BillingPage() {
  const { data: tier, isPending: tierLoading } = useSubscriptionTier();
  const { status, error, startUpgrade } = useUpgradeFlow();

  const isPro = tier?.tier?.toLowerCase() === 'pro';

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <h1 className="mb-6 text-2xl font-bold text-slate-900">Billing &amp; Subscription</h1>

      {tierLoading ? (
        <p className="text-sm text-slate-500">Loading subscription info…</p>
      ) : (
        <div className="mb-6 rounded-xl border border-slate-200 bg-white p-4">
          <p className="text-sm text-slate-600">
            Current plan:{' '}
            <strong className="capitalize text-slate-900">{tier?.tier ?? 'Free'}</strong>
          </p>
        </div>
      )}

      {status === 'error' && error ? (
        <div role="alert" className="mb-4 rounded bg-red-50 p-3 text-sm text-red-700">
          {error}
        </div>
      ) : null}

      {isPro ? (
        <div className="flex items-center gap-4">
          <p className="text-sm text-slate-600">You are on the Pro plan.</p>
          <ManageSubscriptionButton />
        </div>
      ) : (
        <PricingTable
          onUpgrade={startUpgrade}
          onManage={() => {/* no-op on free plan */}}
          isUpgradePending={status === 'redirecting'}
        />
      )}
    </div>
  );
}
