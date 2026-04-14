import React from 'react';
import { PresenceBadge, PresenceStatus } from './PresenceBadge';

export interface PresenceUser {
  userId: string;
  displayName: string;
  status: PresenceStatus;
}

interface PresenceListProps {
  users: PresenceUser[];
  loading?: boolean;
}

export const PresenceList: React.FC<PresenceListProps> = ({ users, loading = false }) => {
  if (loading) {
    return <div className="p-3 text-sm text-slate-500">Loading presence...</div>;
  }

  if (!users.length) {
    return <div className="p-3 text-sm text-slate-500">No active members.</div>;
  }

  return (
    <ul className="space-y-2">
      {users.map((user) => (
        <li key={user.userId} className="flex items-center justify-between rounded-md border border-slate-200 px-3 py-2">
          <span className="text-sm text-slate-800">{user.displayName}</span>
          <PresenceBadge status={user.status} />
        </li>
      ))}
    </ul>
  );
};
