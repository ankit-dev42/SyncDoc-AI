import React, { useEffect, useRef, useState } from 'react';
import { Message } from '../../types/message';
import { MessageItem } from './MessageItem';

interface MessageListProps {
  workspaceId: string;
  channelId: string;
  messages: Message[];
  loading: boolean;
  error: string | null;
  hasMore: boolean;
  onLoadMore: () => void;
  currentUserId: string;
  onEditMessage: (messageId: string, newContent: string) => void;
  onDeleteMessage: (messageId: string) => void;
  onReply: (parentMessageId: string) => void;
}

export const MessageList: React.FC<MessageListProps> = ({
  messages,
  loading,
  error,
  hasMore,
  onLoadMore,
  currentUserId,
  onEditMessage,
  onDeleteMessage,
  onReply,
}) => {
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const [autoScroll, setAutoScroll] = useState(true);

  // Auto-scroll to bottom for new messages
  useEffect(() => {
    if (autoScroll && messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, autoScroll]);

  // Handle scroll to detect if user scrolled up
  const handleScroll = (e: React.UIEvent<HTMLDivElement>) => {
    const { scrollTop, scrollHeight, clientHeight } = e.currentTarget;
    const isAtBottom = scrollTop + clientHeight >= scrollHeight - 10;
    setAutoScroll(isAtBottom);
  };

  if (error) {
    return <div className="p-4 text-sm text-red-600">{error}</div>;
  }

  return (
    <div
      className="flex-1 overflow-y-auto p-4 space-y-4"
      onScroll={handleScroll}
    >
      {loading && messages.length === 0 && (
        <div className="flex justify-center py-8 text-sm text-gray-500">Loading messages...</div>
      )}

      {hasMore && !loading && (
        <button
          onClick={onLoadMore}
          className="w-full py-2 text-sm text-gray-500 hover:text-gray-700 border border-gray-200 rounded-md hover:bg-gray-50"
        >
          Load earlier messages
        </button>
      )}

      {messages.map((message) => (
        <MessageItem
          key={message.id}
          message={message}
          currentUserId={currentUserId}
          onEdit={onEditMessage}
          onDelete={onDeleteMessage}
          onReply={onReply}
        />
      ))}

      {loading && messages.length > 0 && (
        <div className="flex justify-center py-4 text-xs text-gray-500">Loading more...</div>
      )}

      <div ref={messagesEndRef} />
    </div>
  );
};