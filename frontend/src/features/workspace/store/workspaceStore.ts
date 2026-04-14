import { create } from 'zustand';
import { WorkspaceSummary, workspaceApi } from '../api/workspaceApi';

interface WorkspaceState {
  workspaces: WorkspaceSummary[];
  activeWorkspaceId: string;
  currentUserId: string;
  loading: boolean;
  error: string | null;
  initialize: () => Promise<void>;
  switchWorkspace: (workspaceId: string) => void;
}

function getInitialWorkspaceId(): string {
  return localStorage.getItem('workspaceId') ?? 'workspace-1';
}

function getInitialUserId(): string {
  return localStorage.getItem('userId') ?? 'user-1';
}

export const useWorkspaceStore = create<WorkspaceState>((set, get) => ({
  workspaces: [],
  activeWorkspaceId: getInitialWorkspaceId(),
  currentUserId: getInitialUserId(),
  loading: false,
  error: null,

  initialize: async () => {
    const userId = getInitialUserId();
    localStorage.setItem('userId', userId);

    set({ loading: true, error: null, currentUserId: userId });
    try {
      const workspaces = await workspaceApi.list();
      const current = get().activeWorkspaceId;
      const hasCurrent = workspaces.some((workspace) => workspace.id === current);
      const fallback = workspaces[0]?.id ?? current;
      const activeWorkspaceId = hasCurrent ? current : fallback;

      localStorage.setItem('workspaceId', activeWorkspaceId);
      set({ workspaces, activeWorkspaceId, loading: false });
    } catch (_err) {
      set({ loading: false, error: 'Unable to load workspace list' });
    }
  },

  switchWorkspace: (workspaceId: string) => {
    localStorage.setItem('workspaceId', workspaceId);
    set({ activeWorkspaceId: workspaceId, error: null });
  },
}));