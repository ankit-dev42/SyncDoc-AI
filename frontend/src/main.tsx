import React from 'react';
import ReactDOM from 'react-dom/client';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { setNavigationHandler } from './api/client';
import { ProtectedRoute } from './components/ProtectedRoute';
import { NotFoundPage } from './components/NotFoundPage';
import { LoginPage } from './features/auth/components/LoginPage';
import { RegisterPage } from './features/auth/components/RegisterPage';
import { Dashboard } from './App';
import './index.css';

// Lazy-load route components that are not needed on initial load
const BillingPage = React.lazy(() =>
  import('./features/billing/components/BillingPage').then((m) => ({ default: m.BillingPage })),
);
const SuccessPage = React.lazy(() =>
  import('./features/billing/components/SuccessPage').then((m) => ({ default: m.SuccessPage })),
);

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60_000,
      retry: 2,
    },
  },
});

const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/register',
    element: <RegisterPage />,
  },
  {
    path: '/success',
    element: (
      <React.Suspense fallback={null}>
        <SuccessPage />
      </React.Suspense>
    ),
  },
  {
    element: <ProtectedRoute />,
    children: [
      {
        path: '/',
        element: <Dashboard />,
      },
      {
        path: '/billing',
        element: (
          <React.Suspense fallback={null}>
            <BillingPage />
          </React.Suspense>
        ),
      },
      {
        path: '/projects/:projectId',
        element: <Dashboard />,
      },
    ],
  },
  {
    path: '*',
    element: <NotFoundPage />,
  },
]);

// Register the router's navigate function so client.ts can redirect on auth failure
setNavigationHandler((path) => router.navigate(path));

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  </React.StrictMode>,
);
