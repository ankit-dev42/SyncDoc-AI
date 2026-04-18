import { useTranslation } from '../../../i18n/useTranslation';

/**
 * T035 — US4: /success route banner component
 *
 * Shown after Stripe redirects the user back to the app following a successful
 * payment.  Uses the project's zero-dependency i18n hook so strings stay in
 * sync with `en.json` (billing.paymentSuccessTitle / billing.paymentSuccessBody).
 *
 * Accessibility:
 *   - role="status" + aria-live="polite" so screen readers announce the
 *     confirmation without interrupting ongoing activity.
 */
export function PaymentSuccessBanner() {
  const { t } = useTranslation();

  return (
    <div
      role="status"
      aria-live="polite"
      className="mx-auto mt-16 max-w-lg rounded-xl border border-green-200 bg-green-50 p-8 text-center shadow-sm"
    >
      {/* Checkmark icon */}
      <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-green-100">
        <svg
          className="h-8 w-8 text-green-600"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M5 13l4 4L19 7"
          />
        </svg>
      </div>

      <h2 className="text-xl font-semibold text-green-800">
        {t('billing.paymentSuccessTitle')}
      </h2>

      <p className="mt-2 text-sm text-green-700">{t('billing.paymentSuccessBody')}</p>

      <a
        href="/"
        className="mt-6 inline-block rounded-lg bg-green-600 px-5 py-2 text-sm font-medium text-white hover:bg-green-700"
      >
        Go to dashboard
      </a>
    </div>
  );
}
