import axios from 'axios';
import { Message, SendMessageRequest, EditMessageRequest, MessageSearchParams } from '../types/message';
import { attachWorkspaceContextHeaders } from './contextHeaders';

const API_BASE_URL = 'http://localhost:8080/api/v1';

class MessageApiClient {
  private client = axios.create({
    baseURL: API_BASE_URL,
    headers: {
      'Content-Type': 'application/json',
    },
  });

  constructor() {
    this.client.interceptors.request.use((config) => attachWorkspaceContextHeaders(config));
  }

  // Set auth token
  setAuthToken(token: string) {
    this.client.defaults.headers.common['Authorization'] = `Bearer ${token}`;
  }

  // Send a message
  async sendMessage(
    workspaceId: string,
    channelId: string,
    request: SendMessageRequest
  ): Promise<Message> {
    const response = await this.client.post(
      `/workspaces/${workspaceId}/channels/${channelId}/messages`,
      request
    );
    return response.data.data;
  }

  // Get messages with pagination
  async getMessages(
    workspaceId: string,
    channelId: string,
    page = 0,
    size = 50
  ): Promise<{
    content: Message[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
    first: boolean;
    last: boolean;
  }> {
    const response = await this.client.get(
      `/workspaces/${workspaceId}/channels/${channelId}/messages`,
      { params: { page, size } }
    );
    return response.data.data;
  }

  // Get messages after a sequence number
  async getMessagesAfterSequence(
    workspaceId: string,
    channelId: string,
    sequenceNumber: number
  ): Promise<Message[]> {
    const response = await this.client.get(
      `/workspaces/${workspaceId}/channels/${channelId}/messages/after/${sequenceNumber}`
    );
    return response.data.data;
  }

  // Get messages before a sequence number
  async getMessagesBeforeSequence(
    workspaceId: string,
    channelId: string,
    sequenceNumber: number,
    limit = 50
  ): Promise<{
    content: Message[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
  }> {
    const response = await this.client.get(
      `/workspaces/${workspaceId}/channels/${channelId}/messages/before/${sequenceNumber}`,
      { params: { limit } }
    );
    return response.data.data;
  }

  // Edit a message
  async editMessage(
    workspaceId: string,
    channelId: string,
    messageId: string,
    request: EditMessageRequest
  ): Promise<Message> {
    const response = await this.client.put(
      `/workspaces/${workspaceId}/channels/${channelId}/messages/${messageId}`,
      request
    );
    return response.data.data;
  }

  // Delete a message
  async deleteMessage(
    workspaceId: string,
    channelId: string,
    messageId: string,
    deleterId: string
  ): Promise<void> {
    await this.client.delete(
      `/workspaces/${workspaceId}/channels/${channelId}/messages/${messageId}`,
      { params: { deleterId } }
    );
  }

  // Search messages
  async searchMessages(
    params: MessageSearchParams
  ): Promise<{
    content: Message[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
  }> {
    const response = await this.client.get(
      `/workspaces/${params.workspaceId}/channels/${params.channelId}/messages/search`,
      {
        params: {
          query: params.query,
          page: params.page || 0,
          size: params.size || 20,
        },
      }
    );
    return response.data.data;
  }

  // Get thread replies
  async getThreadReplies(
    workspaceId: string,
    channelId: string,
    parentMessageId: string
  ): Promise<Message[]> {
    const response = await this.client.get(
      `/workspaces/${workspaceId}/channels/${channelId}/messages/thread/${parentMessageId}`
    );
    return response.data.data;
  }

  // Get message count
  async getMessageCount(workspaceId: string, channelId: string): Promise<number> {
    const response = await this.client.get(
      `/workspaces/${workspaceId}/channels/${channelId}/messages/count`
    );
    return response.data.data;
  }
}

export const messageApi = new MessageApiClient();