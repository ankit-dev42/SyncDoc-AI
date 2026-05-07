import { useForm } from 'react-hook-form';
import { useNavigate, useLocation } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { useAuthStore } from '../store/authStore';
import en from '../../../i18n/en.json';

interface LoginFormValues {
  email: string;
  password: string;
}

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const setTokens = useAuthStore((s) => s.setTokens);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>();

  const from: string = (location.state as { from?: string })?.from ?? '/';

  const onSubmit = async (values: LoginFormValues) => {
    try {
      const { accessToken, user } = await authApi.login(values);
      setTokens(accessToken, user);
      navigate(from, { replace: true });
    } catch {
      setError('root', { message: ((en as unknown) as Record<string, Record<string, string>>).auth['login.error'] });
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50">
      <div className="w-full max-w-sm rounded-xl border border-slate-200 bg-white p-8 shadow-sm">
        <h1 className="mb-6 text-2xl font-bold text-slate-900">Sign in</h1>

        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          {errors.root ? (
            <div role="alert" className="mb-4 rounded bg-red-50 p-3 text-sm text-red-700">
              {errors.root.message}
            </div>
          ) : null}

          <div className="mb-4">
            <label htmlFor="email" className="mb-1 block text-sm font-medium text-slate-700">
              Email
            </label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              className="w-full rounded border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              {...register('email', { required: 'Email is required' })}
            />
            {errors.email ? (
              <p className="mt-1 text-xs text-red-600">{errors.email.message}</p>
            ) : null}
          </div>

          <div className="mb-6">
            <label htmlFor="password" className="mb-1 block text-sm font-medium text-slate-700">
              Password
            </label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              className="w-full rounded border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              {...register('password', { required: 'Password is required' })}
            />
            {errors.password ? (
              <p className="mt-1 text-xs text-red-600">{errors.password.message}</p>
            ) : null}
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-lg bg-indigo-600 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-60"
          >
            {isSubmitting ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <p className="mt-4 text-center text-sm text-slate-600">
          No account?{' '}
          <a href="/register" className="text-indigo-600 hover:underline">
            Create one
          </a>
        </p>
      </div>
    </div>
  );
}
