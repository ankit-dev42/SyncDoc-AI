import axios from 'axios';
import { PresenceStatus } from '../components/PresenceBadge';
import { attachWorkspaceContextHeaders } from '../../../api/contextHeaders';

const api = axios.create({ baseURL: 'http://localhost:8080/api/v1' });
api.interceptors.request.use((config) => attachWorkspaceContextHeaders(config));

export interface PresenceRecord {
  userId: string;
  workspaceId: string;
  status: PresenceStatus;
  lastSeenAt: string;
}

export const presenceApi = {
  async list(workspaceId: string): Promise<PresenceRecord[]> {
    const response = await api.get(`/workspaces/${workspaceId}/presence`);
    return response.data.data;
  },
  async heartbeat(workspaceId: string, userId: string): Promise<PresenceRecord> {
    const response = await api.post(`/workspaces/${workspaceId}/presence/heartbeat`, null, {
      params: { userId },
    });
    return response.data.data;
  },
  async setStatus(workspaceId: string, userId: string, status: PresenceStatus): Promise<PresenceRecord> {
    const response = await api.post(`/workspaces/${workspaceId}/presence/status`, { userId, status });
    return response.data.data;
  },
};
