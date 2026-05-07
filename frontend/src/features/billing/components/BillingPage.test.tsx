import { render, screen } from '@testing-library/react';
import { vi, describe, it, expect } from 'vitest';
import { BillingPage } from './BillingPage';

// Mock child components to isolate BillingPage logic
vi.mock('./PricingTable', () => ({
  PricingTable: ({ onUpgrade }: { onUpgrade: () => void; onManage: () => void; isUpgradePending: boolean }) => (
    <div data-testid="pricing-table">
      <button onClick={onUpgrade}>Upgrade to Pro</button>
    </div>
  ),
}));

vi.mock('./ManageSubscriptionButton', () => ({
  ManageSubscriptionButton: () => <div data-testid="manage-sub-btn">Manage subscription</div>,
}));

vi.mock('../hooks/useSubscriptionTier', () => ({
  useSubscriptionTier: vi.fn(),
}));

vi.mock('../hooks/useUpgradeFlow', () => ({
  useUpgradeFlow: vi.fn(),
}));

import { useSubscriptionTier } from '../hooks/useSubscriptionTier';
import { useUpgradeFlow } from '../hooks/useUpgradeFlow';

const mockUseTier = vi.mocked(useSubscriptionTier);
const mockUseUpgrade = vi.mocked(useUpgradeFlow);

function makeUpgradeFlow(overrides = {}) {
  return { status: 'idle', error: null, startUpgrade: vi.fn(), ...overrides } as unknown as ReturnType<typeof useUpgradeFlow>;
}

describe('BillingPage', () => {
  it('shows loading indicator while subscription tier is loading', () => {
    mockUseTier.mockReturnValue({ data: undefined, isPending: true, isError: false } as unknown as ReturnType<typeof useSubscriptionTier>);
    mockUseUpgrade.mockReturnValue(makeUpgradeFlow());
    render(<BillingPage />);
    expect(screen.getByText(/loading subscription info/i)).toBeInTheDocument();
  });

  it('shows PricingTable for a Free-tier user', () => {
    mockUseTier.mockReturnValue({ data: { tier: 'free', userId: 'u1', status: 'active' }, isPending: false, isError: false } as unknown as ReturnType<typeof useSubscriptionTier>);
    mockUseUpgrade.mockReturnValue(makeUpgradeFlow());
    render(<BillingPage />);
    expect(screen.getByTestId('pricing-table')).toBeInTheDocument();
    expect(screen.queryByTestId('manage-sub-btn')).not.toBeInTheDocument();
  });

  it('shows ManageSubscriptionButton for a Pro-tier user', () => {
    mockUseTier.mockReturnValue({ data: { tier: 'pro', userId: 'u1', status: 'active' }, isPending: false, isError: false } as unknown as ReturnType<typeof useSubscriptionTier>);
    mockUseUpgrade.mockReturnValue(makeUpgradeFlow());
    render(<BillingPage />);
    expect(screen.getByTestId('manage-sub-btn')).toBeInTheDocument();
    expect(screen.queryByTestId('pricing-table')).not.toBeInTheDocument();
  });

  it('displays upgrade error alert when status is error', () => {
    mockUseTier.mockReturnValue({ data: { tier: 'free', userId: 'u1', status: 'active' }, isPending: false, isError: false } as unknown as ReturnType<typeof useSubscriptionTier>);
    mockUseUpgrade.mockReturnValue(makeUpgradeFlow({ status: 'error', error: 'Checkout failed' }));
    render(<BillingPage />);
    expect(screen.getByRole('alert')).toHaveTextContent('Checkout failed');
  });

  it('displays the current plan tier', () => {
    mockUseTier.mockReturnValue({ data: { tier: 'free', userId: 'u1', status: 'active' }, isPending: false, isError: false } as unknown as ReturnType<typeof useSubscriptionTier>);
    mockUseUpgrade.mockReturnValue(makeUpgradeFlow());
    render(<BillingPage />);
    expect(screen.getByText(/current plan/i)).toBeInTheDocument();
  });
});
