interface PricingTableProps {
  onUpgrade: () => void;
  onManage: () => void;
  isUpgradePending: boolean;
}

const FEATURES = [
  { label: 'Real-time collaboration', free: true, pro: true },
  { label: 'Document history (7 days)', free: true, pro: false },
  { label: 'Document history (unlimited)', free: false, pro: true },
  { label: 'Workspaces', free: '1', pro: 'Unlimited' },
  { label: 'AI extraction', free: false, pro: true },
  { label: 'Priority support', free: false, pro: true },
];

const Check = () => <span aria-hidden="true" className="text-green-600">✓</span>;
const Cross = () => <span aria-hidden="true" className="text-slate-300">—</span>;

function Cell({ value }: { value: boolean | string }) {
  if (typeof value === 'string') return <span className="text-sm text-slate-700">{value}</span>;
  return value ? <Check /> : <Cross />;
}

export function PricingTable({ onUpgrade, onManage, isUpgradePending }: PricingTableProps) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse text-left text-sm">
        <thead>
          <tr className="border-b border-slate-200">
            <th className="py-3 pr-6 font-semibold text-slate-700">Feature</th>
            <th className="px-4 py-3 text-center font-semibold text-slate-700">Free</th>
            <th className="px-4 py-3 text-center font-semibold text-indigo-600">Pro</th>
          </tr>
        </thead>
        <tbody>
          {FEATURES.map((f) => (
            <tr key={f.label} className="border-b border-slate-100">
              <td className="py-2 pr-6 text-slate-600">{f.label}</td>
              <td className="px-4 py-2 text-center">
                <Cell value={f.free} />
              </td>
              <td className="px-4 py-2 text-center">
                <Cell value={f.pro} />
              </td>
            </tr>
          ))}
          <tr>
            <td className="py-4 pr-6" />
            <td className="px-4 py-4 text-center">
              <button
                onClick={onManage}
                className="rounded border border-slate-300 px-3 py-1 text-xs font-medium text-slate-600 hover:bg-slate-50"
              >
                Manage
              </button>
            </td>
            <td className="px-4 py-4 text-center">
              <button
                onClick={onUpgrade}
                disabled={isUpgradePending}
                aria-label="Upgrade to Pro"
                className="rounded-lg bg-indigo-600 px-4 py-1.5 text-xs font-semibold text-white hover:bg-indigo-700 disabled:opacity-60"
              >
                {isUpgradePending ? 'Redirecting…' : 'Upgrade to Pro'}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  );
}
