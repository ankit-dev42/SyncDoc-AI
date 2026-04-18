import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { PaymentSuccessBanner } from './PaymentSuccessBanner';

/**
 * T032 — US4: PaymentSuccessBanner component tests
 *
 * Validates i18n string rendering for the post-payment confirmation banner.
 * These tests are intentionally written before the component exists (TDD).
 */
describe('PaymentSuccessBanner', () => {
  it('renders the payment success heading', () => {
    render(<PaymentSuccessBanner />);

    expect(
      screen.getByRole('heading', { name: 'Payment successful' }),
    ).toBeInTheDocument();
  });

  it('renders the payment success body text', () => {
    render(<PaymentSuccessBanner />);

    expect(
      screen.getByText('Your subscription has been updated successfully.'),
    ).toBeInTheDocument();
  });

  it('renders with accessible status role for screen readers', () => {
    render(<PaymentSuccessBanner />);

    expect(screen.getByRole('status')).toBeInTheDocument();
  });
});
