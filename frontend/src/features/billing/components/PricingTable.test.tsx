import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { PricingTable } from './PricingTable';

describe('PricingTable', () => {
  it('renders Free and Pro tier names', () => {
    render(<PricingTable onUpgrade={vi.fn()} onManage={vi.fn()} isUpgradePending={false} />);
    // Column headers identify the two tiers
    expect(screen.getByRole('columnheader', { name: /free/i })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /pro/i })).toBeInTheDocument();
  });

  it('renders feature comparison rows', () => {
    render(<PricingTable onUpgrade={vi.fn()} onManage={vi.fn()} isUpgradePending={false} />);
    // At least one feature row should be present
    const rows = screen.getAllByRole('row');
    expect(rows.length).toBeGreaterThan(0);
  });

  it('calls onUpgrade when the upgrade button is clicked', async () => {
    const onUpgrade = vi.fn();
    render(<PricingTable onUpgrade={onUpgrade} onManage={vi.fn()} isUpgradePending={false} />);
    await userEvent.click(screen.getByRole('button', { name: /upgrade/i }));
    expect(onUpgrade).toHaveBeenCalledOnce();
  });

  it('shows loading state on UpgradeButton when isUpgradePending is true', () => {
    render(<PricingTable onUpgrade={vi.fn()} onManage={vi.fn()} isUpgradePending={true} />);
    const upgradeBtn = screen.getByRole('button', { name: /upgrade/i });
    expect(upgradeBtn).toBeDisabled();
  });
});
