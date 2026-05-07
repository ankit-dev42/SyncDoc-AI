import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { SuccessPage } from '../features/billing/components/SuccessPage';

function renderSuccessPage(search = '') {
  return render(
    <MemoryRouter initialEntries={[`/success${search}`]}>
      <Routes>
        <Route path="/success" element={<SuccessPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('SuccessPage', () => {
  it('renders a payment success message', () => {
    renderSuccessPage();
    // The page heading says "Payment successful"
    expect(screen.getByRole('heading', { name: /payment successful/i })).toBeInTheDocument();
  });

  it('shows a return to dashboard link', () => {
    renderSuccessPage();
    expect(screen.getByRole('link', { name: /dashboard|home|return/i })).toBeInTheDocument();
  });

  it('reads session_id from query params without crashing', () => {
    expect(() => renderSuccessPage('?session_id=cs_test_abc123')).not.toThrow();
  });
});
