import React from 'react';
import { WorkspaceSummary } from '../api/workspaceApi';

interface WorkspaceListProps {
  workspaces: WorkspaceSummary[];
  activeWorkspaceId: string;
  loading?: boolean;
  error?: string | null;
  onSelectWorkspace: (workspaceId: string) => void;
}

export const WorkspaceList: React.FC<WorkspaceListProps> = ({
  workspaces,
  activeWorkspaceId,
  loading = false,
  error,
  onSelectWorkspace,
}) => {
  if (loading) {
    return <div className="rounded-md border border-slate-200 bg-white p-3 text-sm text-slate-500">Loading workspaces...</div>;
  }

  if (error) {
    return <div className="rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">{error}</div>;
  }

  if (workspaces.length === 0) {
    return <div className="rounded-md border border-slate-200 bg-white p-3 text-sm text-slate-500">No workspaces available.</div>;
  }

  return (
    <div className="rounded-md border border-slate-200 bg-white p-3">
      <h2 className="mb-2 text-sm font-semibold text-slate-700">Your Workspaces</h2>
      <ul className="space-y-2">
        {workspaces.map((workspace) => {
          const isActive = workspace.id === activeWorkspaceId;
          return (
            <li key={workspace.id}>
              <button
                className={`flex w-full items-center justify-between rounded-md border px-3 py-2 text-left text-sm ${
                  isActive
                    ? 'border-blue-600 bg-blue-50 text-blue-900'
                    : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
                }`}
                onClick={() => onSelectWorkspace(workspace.id)}
              >
                <span className="font-medium">{workspace.name}</span>
                <span className="text-xs text-slate-500">{workspace.memberCount} members</span>
              </button>
            </li>
          );
        })}
      </ul>
    </div>
  );
};