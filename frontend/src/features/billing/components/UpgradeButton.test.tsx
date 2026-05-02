import { render, screen, fireEvent } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { UpgradeButton } from './UpgradeButton';
import type { UpgradeStatus } from '../hooks/useUpgradeFlow';

vi.mock('../hooks/useUpgradeFlow', () => ({
  useUpgradeFlow: vi.fn(),
}));

import { useUpgradeFlow } from '../hooks/useUpgradeFlow';
const mockUseUpgradeFlow = vi.mocked(useUpgradeFlow);

function makeFlow(overrides: Partial<{ status: UpgradeStatus; error: string | null; startUpgrade: () => void }> = {}) {
  return {
    status: 'idle' as UpgradeStatus,
    error: null,
    startUpgrade: vi.fn(),
    ...overrides,
  } as ReturnType<typeof useUpgradeFlow>;
}

describe('UpgradeButton', () => {
  beforeEach(() => {
    mockUseUpgradeFlow.mockReturnValue(makeFlow());
  });

  it('renders "Upgrade to Pro" button in idle state', () => {
    render(<UpgradeButton />);
    expect(screen.getByRole('button', { name: /upgrade to pro/i })).toBeInTheDocument();
    expect(screen.getByRole('button')).not.toBeDisabled();
  });

  it('shows redirecting label and disables button when status is redirecting', () => {
    mockUseUpgradeFlow.mockReturnValue(makeFlow({ status: 'redirecting' }));
    render(<UpgradeButton />);
    const btn = screen.getByRole('button');
    expect(btn).toBeDisabled();
    expect(btn).toHaveTextContent(/redirecting/i);
  });

  it('displays error message when status is error', () => {
    mockUseUpgradeFlow.mockReturnValue(makeFlow({ status: 'error', error: 'Payment failed' }));
    render(<UpgradeButton />);
    expect(screen.getByText('Payment failed')).toBeInTheDocument();
  });

  it('calls startUpgrade when button is clicked', () => {
    const startUpgrade = vi.fn();
    mockUseUpgradeFlow.mockReturnValue(makeFlow({ startUpgrade }));
    render(<UpgradeButton />);
    fireEvent.click(screen.getByRole('button'));
    expect(startUpgrade).toHaveBeenCalledOnce();
  });
});
