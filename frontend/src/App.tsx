import { useEffect } from 'react';
import './App.css';
import { Thread } from './features/messaging/components/Thread';
import { ThreadList } from './features/messaging/components/ThreadList';
import { useThreads } from './features/messaging/hooks/useThreads';
import { PresenceBadge } from './features/presence/components/PresenceBadge';
import { usePresence } from './features/presence/hooks/usePresence';
import { SearchBox } from './features/search/components/SearchBox';
import { SearchResults } from './features/search/components/SearchResults';
import { useSearch } from './features/search/hooks/useSearch';
import { WorkspaceList } from './features/workspace/components/WorkspaceList';
import { useWorkspaceStore } from './features/workspace/store/workspaceStore';

const DEFAULT_CHANNEL_ID = 'general';

function App() {
  const {
    workspaces,
    activeWorkspaceId,
    currentUserId,
    loading,
    error,
    initialize,
    switchWorkspace,
  } = useWorkspaceStore();

  const {
    users,
    loading: loadingPresence,
    error: presenceError,
    refresh: refreshPresence,
  } = usePresence(activeWorkspaceId, currentUserId);

  const {
    threads,
    activeReplies,
    activeThreadId,
    loadingThreads,
    loadingReplies,
    error: threadError,
    openThread,
  } = useThreads(activeWorkspaceId, DEFAULT_CHANNEL_ID);

  const {
    results,
    loading: searching,
    error: searchError,
    runSearch,
    navigateToResult,
  } = useSearch(activeWorkspaceId, DEFAULT_CHANNEL_ID);

  useEffect(() => {
    void initialize();
  }, [initialize]);

  return (
    <div className="mx-auto max-w-6xl px-4 py-6 text-slate-900">
      <header className="mb-6 rounded-xl border border-slate-200 bg-white p-4">
        <h1 className="text-2xl font-bold">SyncDoc Collaboration Platform</h1>
        <p className="mt-1 text-sm text-slate-600">
          Active workspace: <strong>{activeWorkspaceId}</strong> • User: <strong>{currentUserId}</strong>
        </p>
      </header>

      <div className="grid gap-4 lg:grid-cols-3">
        <section className="space-y-4 lg:col-span-1">
          <WorkspaceList
            workspaces={workspaces}
            activeWorkspaceId={activeWorkspaceId}
            loading={loading}
            error={error}
            onSelectWorkspace={switchWorkspace}
          />

          <div className="rounded-md border border-slate-200 bg-white p-3">
            <div className="mb-2 flex items-center justify-between">
              <h2 className="text-sm font-semibold text-slate-700">Presence</h2>
              <button
                onClick={() => void refreshPresence()}
                className="rounded border border-slate-300 px-2 py-1 text-xs text-slate-700 hover:bg-slate-50"
              >
                Refresh
              </button>
            </div>
            {loadingPresence ? <p className="text-sm text-slate-500">Loading presence...</p> : null}
            {presenceError ? <p className="text-sm text-red-700">{presenceError}</p> : null}
            <ul className="space-y-2">
              {users.map((user) => (
                <li key={user.userId} className="flex items-center justify-between rounded border border-slate-200 p-2">
                  <span className="text-sm text-slate-700">{user.userId}</span>
                  <PresenceBadge status={user.status} />
                </li>
              ))}
            </ul>
          </div>
        </section>

        <section className="space-y-4 lg:col-span-2">
          <div className="rounded-md border border-slate-200 bg-white p-3">
            <h2 className="mb-2 text-sm font-semibold text-slate-700">Search</h2>
            <SearchBox onSearch={(query, filters) => void runSearch(query, filters)} isLoading={searching} />
            <div className="mt-3">
              <SearchResults results={results} loading={searching} error={searchError} onSelect={navigateToResult} />
            </div>
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <div className="rounded-md border border-slate-200 bg-white p-3">
              <h2 className="mb-2 text-sm font-semibold text-slate-700">Threads</h2>
              <ThreadList threads={threads} isLoading={loadingThreads} onOpenThread={(id) => void openThread(id)} />
              {threadError ? <p className="mt-2 text-sm text-red-700">{threadError}</p> : null}
            </div>

            <div className="rounded-md border border-slate-200 bg-white p-3">
              <Thread
                rootMessageId={activeThreadId ?? 'Select a thread'}
                replies={activeReplies}
                isLoading={loadingReplies}
                error={threadError}
              />
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}

export default App;