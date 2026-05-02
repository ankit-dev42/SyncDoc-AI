import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from './ProtectedRoute';
import { useAuthStore } from '../features/auth/store/authStore';

beforeEach(() => {
  useAuthStore.getState().clearTokens();
});

function renderProtectedRoute(initialPath: string) {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <Routes>
        <Route path="/login" element={<div>Login Page</div>} />
        <Route element={<ProtectedRoute />}>
          <Route path="/billing" element={<div>Billing Page</div>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

describe('ProtectedRoute', () => {
  it('redirects to /login when accessToken is null', () => {
    renderProtectedRoute('/billing');
    expect(screen.getByText('Login Page')).toBeInTheDocument();
    expect(screen.queryByText('Billing Page')).not.toBeInTheDocument();
  });

  it('renders Outlet when accessToken is present', () => {
    useAuthStore
      .getState()
      .setTokens('valid-token', { id: '1', email: 'a@b.com', displayName: 'A' });
    renderProtectedRoute('/billing');
    expect(screen.getByText('Billing Page')).toBeInTheDocument();
    expect(screen.queryByText('Login Page')).not.toBeInTheDocument();
  });
});
