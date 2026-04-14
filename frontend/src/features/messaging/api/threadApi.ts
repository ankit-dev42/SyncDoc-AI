import axios from 'axios';
import { attachWorkspaceContextHeaders } from '../../../api/contextHeaders';

const api = axios.create({ baseURL: 'http://localhost:8080/api/v1' });
api.interceptors.request.use((config) => attachWorkspaceContextHeaders(config));

export interface ThreadSummary {
  id: string;
  workspaceId: string;
  channelId: string;
  rootMessageId: string;
  replyCount: number;
  lastActivityAt: string;
}

export interface ThreadReply {
  id: string;
  parentMessageId?: string;
  senderId: string;
  content: string;
  createdAt: string;
}

export const threadApi = {
  async listThreads(workspaceId: string, channelId: string): Promise<ThreadSummary[]> {
    const response = await api.get(`/workspaces/${workspaceId}/channels/${channelId}/messages/threads`);
    return response.data.data;
  },

  async getReplies(workspaceId: string, channelId: string, rootMessageId: string): Promise<ThreadReply[]> {
    const response = await api.get(`/workspaces/${workspaceId}/channels/${channelId}/messages/thread/${rootMessageId}`);
    return response.data.data;
  },
};
