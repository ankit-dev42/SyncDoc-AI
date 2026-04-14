export interface Message {
  id: string;
  workspaceId: string;
  channelId: string;
  senderId: string;
  senderName?: string;
  senderAvatar?: string;
  content: string;
  sequenceNumber: number;
  idempotencyKey?: string;
  messageType: MessageType;
  parentMessageId?: string;
  createdAt: string;
  updatedAt: string;
  editedAt?: string;
  deletedAt?: string;
  isEdited: boolean;
  isDeleted: boolean;
}

export enum MessageType {
  TEXT = 'TEXT',
  FILE = 'FILE',
  SYSTEM = 'SYSTEM',
}

export interface SendMessageRequest {
  content: string;
  idempotencyKey?: string;
  parentMessageId?: string;
}

export interface EditMessageRequest {
  content: string;
  editorId: string;
}

export interface MessageSearchParams {
  workspaceId: string;
  channelId: string;
  query: string;
  page?: number;
  size?: number;
}

export interface MessageThread {
  parentMessage: Message;
  replies: Message[];
}