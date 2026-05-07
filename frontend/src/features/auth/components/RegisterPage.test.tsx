import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { RegisterPage } from './RegisterPage';
import * as authApiModule from '../api/authApi';

beforeEach(() => {
  vi.restoreAllMocks();
});

function renderRegisterPage() {
  return render(
    <MemoryRouter initialEntries={['/register']}>
      <Routes>
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/login" element={<div>Login Page</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('RegisterPage', () => {
  it('renders email, password, and displayName fields', () => {
    renderRegisterPage();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/display name/i)).toBeInTheDocument();
  });

  it('navigates to /login after successful registration', async () => {
    vi.spyOn(authApiModule.authApi, 'register').mockResolvedValue(undefined);

    renderRegisterPage();
    await userEvent.type(screen.getByLabelText(/email/i), 'new@user.com');
    await userEvent.type(screen.getByLabelText(/password/i), 'password123');
    await userEvent.type(screen.getByLabelText(/display name/i), 'New User');
    await userEvent.click(screen.getByRole('button', { name: /create account/i }));

    await waitFor(() => {
      expect(screen.getByText('Login Page')).toBeInTheDocument();
    });
  });

  it('displays auth.register.error message on API failure', async () => {
    vi.spyOn(authApiModule.authApi, 'register').mockRejectedValue(new Error('Email taken'));

    renderRegisterPage();
    await userEvent.type(screen.getByLabelText(/email/i), 'taken@user.com');
    await userEvent.type(screen.getByLabelText(/password/i), 'password123');
    await userEvent.type(screen.getByLabelText(/display name/i), 'Taken User');
    await userEvent.click(screen.getByRole('button', { name: /create account/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });
  });
});
