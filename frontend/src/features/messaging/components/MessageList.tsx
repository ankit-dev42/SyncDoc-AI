import React from 'react';

export interface MessageItem {
  id: string;
  senderId: string;
  content: string;
  createdAt: string;
  threadReplyCount?: number;
  threadUnreadCount?: number;
}

interface MessageListProps {
  messages: MessageItem[];
  isInitialLoading: boolean;
  isLoadingMore: boolean;
  hasMore: boolean;
  error?: string | null;
  onLoadMore: () => void;
  onOpenThread?: (messageId: string) => void;
}

export const MessageList: React.FC<MessageListProps> = ({
  messages,
  isInitialLoading,
  isLoadingMore,
  hasMore,
  error,
  onLoadMore,
  onOpenThread,
}) => {
  if (isInitialLoading) {
    return <div className="p-4 text-sm text-slate-500">Loading message history...</div>;
  }

  return (
    <section className="flex h-full flex-col gap-3">
      {error ? <div className="rounded-md bg-red-50 p-3 text-sm text-red-700">{error}</div> : null}

      {hasMore ? (
        <button
          onClick={onLoadMore}
          disabled={isLoadingMore}
          className="rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-700 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {isLoadingMore ? 'Loading earlier messages...' : 'Load earlier messages'}
        </button>
      ) : null}

      <ul className="space-y-2 overflow-y-auto">
        {messages.map((message) => (
          <li key={message.id} className="rounded-md border border-slate-200 p-3">
            <div className="mb-1 text-xs text-slate-500">{message.senderId} • {new Date(message.createdAt).toLocaleString()}</div>
            <div className="text-sm text-slate-800">{message.content}</div>
            {(message.threadReplyCount ?? 0) > 0 ? (
              <button
                onClick={() => onOpenThread?.(message.id)}
                className="mt-2 inline-flex items-center gap-2 rounded-full bg-slate-100 px-2 py-1 text-xs text-slate-700 hover:bg-slate-200"
              >
                <span>{message.threadReplyCount} replies</span>
                {(message.threadUnreadCount ?? 0) > 0 ? (
                  <span className="rounded-full bg-amber-200 px-1.5 py-0.5 text-[10px] text-amber-800">
                    {message.threadUnreadCount} new
                  </span>
                ) : null}
              </button>
            ) : null}
          </li>
        ))}
      </ul>
    </section>
  );
};
