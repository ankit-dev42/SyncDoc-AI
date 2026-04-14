import React from 'react';

export type PresenceStatus = 'ONLINE' | 'AWAY' | 'OFFLINE';

interface PresenceBadgeProps {
  status: PresenceStatus;
  label?: string;
}

const statusClass: Record<PresenceStatus, string> = {
  ONLINE: 'bg-emerald-500',
  AWAY: 'bg-amber-500',
  OFFLINE: 'bg-slate-400',
};

export const PresenceBadge: React.FC<PresenceBadgeProps> = ({ status, label }) => {
  return (
    <span className="inline-flex items-center gap-2 rounded-full px-2 py-1 text-xs font-medium text-slate-700 bg-slate-100">
      <span className={`h-2.5 w-2.5 rounded-full ${statusClass[status]}`} />
      <span>{label ?? status.toLowerCase()}</span>
    </span>
  );
};
