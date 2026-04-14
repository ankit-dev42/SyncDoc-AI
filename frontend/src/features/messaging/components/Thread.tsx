import React from 'react';
import { ThreadReply } from '../api/threadApi';

interface ThreadProps {
  rootMessageId: string;
  replies: ThreadReply[];
  isLoading: boolean;
  error?: string | null;
}

export const Thread: React.FC<ThreadProps> = ({ rootMessageId, replies, isLoading, error }) => {
  if (isLoading) {
    return <div className="p-4 text-sm text-slate-500">Loading thread context...</div>;
  }

  if (error) {
    return <div className="rounded-md bg-red-50 p-3 text-sm text-red-700">{error}</div>;
  }

  return (
    <section className="flex h-full flex-col gap-3 rounded-md border border-slate-200 p-3">
      <header className="text-xs font-semibold uppercase tracking-wide text-slate-500">
        Thread: {rootMessageId}
      </header>

      {replies.length === 0 ? (
        <div className="text-sm text-slate-500">No replies yet.</div>
      ) : (
        <ul className="space-y-2 overflow-y-auto">
          {replies.map((reply) => (
            <li key={reply.id} className="rounded-md border border-slate-200 p-3">
              <div className="mb-1 text-xs text-slate-500">
                {reply.senderId} • {new Date(reply.createdAt).toLocaleString()}
              </div>
              <div className="text-sm text-slate-800">{reply.content}</div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
};
