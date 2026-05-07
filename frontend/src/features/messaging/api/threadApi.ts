import apiClient from '../../../api/client';

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
    const response = await apiClient.get(`/v1/workspaces/${workspaceId}/channels/${channelId}/messages/threads`);
    return response.data.data;
  },

  async getReplies(workspaceId: string, channelId: string, rootMessageId: string): Promise<ThreadReply[]> {
    const response = await apiClient.get(`/v1/workspaces/${workspaceId}/channels/${channelId}/messages/thread/${rootMessageId}`);
    return response.data.data;
  },

  async createThread(workspaceId: string, channelId: string, message: string): Promise<ThreadSummary> {
    const response = await apiClient.post(`/v1/workspaces/${workspaceId}/channels/${channelId}/messages/threads`, { message });
    return response.data.data;
  },
};
