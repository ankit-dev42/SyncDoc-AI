import React from 'react';
import { ThreadSummary } from '../api/threadApi';

interface ThreadListProps {
  threads: ThreadSummary[];
  isLoading: boolean;
  onOpenThread: (rootMessageId: string) => void;
}

export const ThreadList: React.FC<ThreadListProps> = ({ threads, isLoading, onOpenThread }) => {
  if (isLoading) {
    return <div className="p-3 text-sm text-slate-500">Loading threads...</div>;
  }

  if (threads.length === 0) {
    return <div className="p-3 text-sm text-slate-500">No active threads.</div>;
  }

  return (
    <ul className="space-y-2">
      {threads.map((thread) => (
        <li key={thread.id}>
          <button
            onClick={() => onOpenThread(thread.rootMessageId)}
            className="flex w-full items-center justify-between rounded-md border border-slate-200 px-3 py-2 text-left hover:bg-slate-50"
          >
            <span className="text-sm text-slate-800">Root: {thread.rootMessageId}</span>
            <span className="rounded-full bg-slate-200 px-2 py-0.5 text-xs text-slate-700">
              {thread.replyCount} replies
            </span>
          </button>
        </li>
      ))}
    </ul>
  );
};
