import { Link, useSearchParams } from 'react-router-dom';

export function SuccessPage() {
  const [searchParams] = useSearchParams();
  // session_id available here for analytics if needed
  void searchParams.get('session_id');

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-slate-50 px-4 text-center">
      <div className="max-w-md rounded-xl border border-green-200 bg-white p-10 shadow-sm">
        <div className="mb-4 text-5xl" aria-hidden="true">🎉</div>
        <h1 className="mb-2 text-2xl font-bold text-slate-900">Payment successful</h1>
        <p className="mb-6 text-slate-600">
          Your subscription has been updated. Welcome to Pro!
        </p>
        <Link
          to="/"
          className="inline-block rounded-lg bg-indigo-600 px-6 py-2 text-sm font-semibold text-white hover:bg-indigo-700"
        >
          Return to dashboard
        </Link>
      </div>
    </div>
  );
}
