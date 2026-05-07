import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { ManageSubscriptionButton } from './ManageSubscriptionButton';

vi.mock('../api/billingApi', () => ({
  billingApi: {
    getBillingPortal: vi.fn(),
  },
}));

vi.mock('../../auth/store/authStore', () => ({
  useAuthStore: vi.fn((selector: (s: { user: { id: string } }) => unknown) =>
    selector({ user: { id: 'user-123' } }),
  ),
}));

import { billingApi } from '../api/billingApi';
const mockGetBillingPortal = vi.mocked(billingApi.getBillingPortal);

describe('ManageSubscriptionButton', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Reset location href before each test
    Object.defineProperty(window, 'location', {
      value: { href: '' },
      writable: true,
    });
  });

  it('renders "Manage subscription" button', () => {
    render(<ManageSubscriptionButton />);
    expect(screen.getByRole('button', { name: /manage subscription/i })).toBeInTheDocument();
  });

  it('disables button and shows loading text while fetching portal URL', async () => {
    // Never resolves during this test
    mockGetBillingPortal.mockImplementation(() => new Promise(() => {}));
    render(<ManageSubscriptionButton />);
    fireEvent.click(screen.getByRole('button'));
    expect(screen.getByRole('button')).toBeDisabled();
    expect(screen.getByRole('button')).toHaveTextContent(/opening portal/i);
  });

  it('redirects to portal URL on success', async () => {
    mockGetBillingPortal.mockResolvedValueOnce({ portalUrl: 'https://billing.stripe.com/portal/abc' });
    render(<ManageSubscriptionButton />);
    fireEvent.click(screen.getByRole('button'));
    await waitFor(() => {
      expect(window.location.href).toBe('https://billing.stripe.com/portal/abc');
    });
  });

  it('displays error message and re-enables button on API failure', async () => {
    mockGetBillingPortal.mockRejectedValueOnce(new Error('Network error'));
    render(<ManageSubscriptionButton />);
    fireEvent.click(screen.getByRole('button'));
    await waitFor(() => {
      expect(screen.getByText(/failed to open billing portal/i)).toBeInTheDocument();
    });
    expect(screen.getByRole('button')).not.toBeDisabled();
  });
});
