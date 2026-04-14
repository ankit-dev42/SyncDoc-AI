import axios from 'axios';
import { attachWorkspaceContextHeaders } from '../../../api/contextHeaders';

const api = axios.create({ baseURL: 'http://localhost:8080/api/v1' });
api.interceptors.request.use((config) => attachWorkspaceContextHeaders(config));

export interface SearchFilters {
  from?: string;
  in?: string;
  before?: string;
  after?: string;
}

export interface SearchResult {
  messageId: string;
  workspaceId: string;
  channelId: string;
  senderId: string;
  snippet: string;
  createdAt: string;
  sequenceNumber: number;
}

export const searchApi = {
  async search(workspaceId: string, query: string, channelId: string, filters: SearchFilters): Promise<SearchResult[]> {
    const response = await api.get(`/workspaces/${workspaceId}/search`, {
      params: {
        query,
        channelId,
        from: filters.from,
        before: filters.before,
        after: filters.after,
      },
    });
    return response.data.data;
  },
};
