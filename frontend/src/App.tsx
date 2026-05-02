import { lazy, Suspense, useEffect } from 'react';
import './App.css';
import { SkeletonLoader } from './components/SkeletonLoader';
import { PresenceBadge } from './features/presence/components/PresenceBadge';
import { usePresence } from './features/presence/hooks/usePresence';
import { useWorkspaceStore } from './features/workspace/store/workspaceStore';
import { useAuthStore } from './features/auth/store/authStore';
import { useSearch } from './features/search/hooks/useSearch';
import { useThreads } from './features/messaging/hooks/useThreads';

// Lazy-load heavy feature modules to reduce initial bundle size (T101)
const WorkspaceList = lazy(() => import('./features/workspace/components/WorkspaceList').then(m => ({ default: m.WorkspaceList })));
const SearchBox     = lazy(() => import('./features/search/components/SearchBox').then(m => ({ default: m.SearchBox })));
const SearchResults = lazy(() => import('./features/search/components/SearchResults').then(m => ({ default: m.SearchResults })));
const ThreadList    = lazy(() => import('./features/messaging/components/ThreadList').then(m => ({ default: m.ThreadList })));
const Thread        = lazy(() => import('./features/messaging/components/Thread').then(m => ({ default: m.Thread })));

const DEFAULT_CHANNEL_ID = 'general';

// All hooks live here — no conditional returns before hooks
export function Dashboard() {
  const { clearTokens } = useAuthStore();

  const {
    workspaces,
    activeWorkspaceId,
    currentUserId,
    loading,
    error,
    initialize,
    switchWorkspace,
  } = useWorkspaceStore();

  const presenceResult = usePresence(activeWorkspaceId, currentUserId);
  const users = presenceResult.data ?? [];
  const loadingPresence = presenceResult.isPending;
  const presenceError = presenceResult.isError ? 'Failed to load presence state' : null;

  const threadResult = useThreads(activeWorkspaceId, DEFAULT_CHANNEL_ID);
  const threads = threadResult.data ?? [];
  const loadingThreads = threadResult.isPending;
  const threadError = threadResult.isError ? 'Failed to load threads' : null;

  const searchResult = useSearch(activeWorkspaceId, DEFAULT_CHANNEL_ID, '');
  const results = searchResult.data ?? [];
  const searching = searchResult.isPending;
  const searchError = searchResult.isError ? 'Failed to search' : null;
  const { runSearch, navigateToResult } = searchResult;


  useEffect(() => {
    void initialize();
  }, [initialize]);

  return (
    <div className="mx-auto max-w-6xl px-4 py-6 text-slate-900">
      <header className="mb-6 rounded-xl border border-slate-200 bg-white p-4">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold">SyncDoc Collaboration Platform</h1>
          <button
            onClick={clearTokens}
            className="rounded border border-slate-300 px-3 py-1 text-sm text-slate-600 hover:bg-slate-50"
          >
            Sign out
          </button>
        </div>
        <p className="mt-1 text-sm text-slate-600">
          Active workspace: <strong>{activeWorkspaceId}</strong> • User: <strong>{currentUserId}</strong>
        </p>
      </header>

      <div className="grid gap-4 lg:grid-cols-3">
        <section className="space-y-4 lg:col-span-1">
          <Suspense fallback={<SkeletonLoader rows={4} label="Loading workspaces…" />}>
            <WorkspaceList
              workspaces={workspaces}
              activeWorkspaceId={activeWorkspaceId}
              loading={loading}
              error={error}
              onSelectWorkspace={switchWorkspace}
            />
          </Suspense>

          <div className="rounded-md border border-slate-200 bg-white p-3">
            <div className="mb-2 flex items-center justify-between">
              <h2 className="text-sm font-semibold text-slate-700">Presence</h2>
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
            <Suspense fallback={<SkeletonLoader rows={1} label="Loading search…" />}>
              <SearchBox onSearch={(query, filters) => void runSearch(query, filters)} isLoading={searching} />
              <div className="mt-3">
                <SearchResults results={results} loading={searching} error={searchError} onSelect={navigateToResult} />
              </div>
            </Suspense>
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <div className="rounded-md border border-slate-200 bg-white p-3">
              <h2 className="mb-2 text-sm font-semibold text-slate-700">Threads</h2>
              <Suspense fallback={<SkeletonLoader rows={3} label="Loading threads…" />}>
                <ThreadList threads={threads} isLoading={loadingThreads} onOpenThread={() => {}} />
              </Suspense>
              {threadError ? <p className="mt-2 text-sm text-red-700">{threadError}</p> : null}
            </div>

            <div className="rounded-md border border-slate-200 bg-white p-3">
              <Suspense fallback={<SkeletonLoader rows={5} label="Loading thread…" />}>
                <Thread
                  rootMessageId="Select a thread"
                  replies={[]}
                  isLoading={false}
                  error={null}
                />
              </Suspense>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}

export default function App() {
  return <Dashboard />;
}