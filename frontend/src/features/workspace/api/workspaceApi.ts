import axios from 'axios';
import { attachWorkspaceContextHeaders } from '../../../api/contextHeaders';

const api = axios.create({ baseURL: 'http://localhost:8080/api/v1' });
api.interceptors.request.use((config) => attachWorkspaceContextHeaders(config));

export interface WorkspaceSummary {
  id: string;
  name: string;
  memberCount: number;
}

export const workspaceApi = {
  async list(): Promise<WorkspaceSummary[]> {
    const response = await api.get('/workspaces');
    return response.data.data;
  },
};