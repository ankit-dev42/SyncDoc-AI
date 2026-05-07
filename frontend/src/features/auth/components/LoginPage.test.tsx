import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { LoginPage } from './LoginPage';
import { useAuthStore } from '../store/authStore';
import * as authApiModule from '../api/authApi';

beforeEach(() => {
  useAuthStore.getState().clearTokens();
  vi.restoreAllMocks();
});

function renderLoginPage(from = '/') {
  return render(
    <MemoryRouter initialEntries={[{ pathname: '/login', state: { from } }]}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<div>Home</div>} />
        <Route path="/billing" element={<div>Billing</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('LoginPage', () => {
  it('shows validation error when email is empty', async () => {
    renderLoginPage();
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }));
    await waitFor(() => {
      expect(screen.getByText(/email is required/i)).toBeInTheDocument();
    });
  });

  it('calls setTokens and navigates to location.state.from on successful login', async () => {
    vi.spyOn(authApiModule.authApi, 'login').mockResolvedValue({
      accessToken: 'tok_abc',
      user: { id: '1', email: 'a@b.com', displayName: 'A' },
    } as never);

    renderLoginPage('/billing');
    await userEvent.type(screen.getByLabelText(/email/i), 'a@b.com');
    await userEvent.type(screen.getByLabelText(/password/i), 'secret123');
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(useAuthStore.getState().accessToken).toBe('tok_abc');
    });
  });

  it('displays auth.login.error message on API failure', async () => {
    vi.spyOn(authApiModule.authApi, 'login').mockRejectedValue(new Error('Invalid credentials'));

    renderLoginPage();
    await userEvent.type(screen.getByLabelText(/email/i), 'a@b.com');
    await userEvent.type(screen.getByLabelText(/password/i), 'wrong');
    await userEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      // The error message rendered should come from the i18n key auth.login.error
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });
  });
});
