import axios from 'axios';

export function toWorkspaceAccessError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && error.response?.status === 403) {
    return 'You do not have access to this workspace. Select another workspace to continue.';
  }

  return fallback;
}