import apiClient from '../../../api/client';
import { PresenceStatus } from '../components/PresenceBadge';

export interface PresenceRecord {
  userId: string;
  workspaceId: string;
  status: PresenceStatus;
  lastSeenAt: string;
}

export const presenceApi = {
  async list(workspaceId: string): Promise<PresenceRecord[]> {
    const response = await apiClient.get(`/v1/workspaces/${workspaceId}/presence`);
    return response.data.data;
  },
  async heartbeat(workspaceId: string, userId: string): Promise<PresenceRecord> {
    const response = await apiClient.post(`/v1/workspaces/${workspaceId}/presence/heartbeat`, null, {
      params: { userId },
    });
    return response.data.data;
  },
  async setStatus(workspaceId: string, userId: string, status: PresenceStatus): Promise<PresenceRecord> {
    const response = await apiClient.post(`/v1/workspaces/${workspaceId}/presence/status`, { userId, status });
    return response.data.data;
  },
};
