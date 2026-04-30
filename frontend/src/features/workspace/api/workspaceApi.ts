import apiClient from '../../../api/client';

export interface WorkspaceSummary {
  id: string;
  name: string;
  memberCount: number;
}

export const workspaceApi = {
  async list(): Promise<WorkspaceSummary[]> {
    const response = await apiClient.get('/v1/workspaces');
    return response.data.data;
  },
};