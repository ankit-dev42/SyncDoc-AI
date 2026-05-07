import axios from 'axios';
import { attachWorkspaceContextHeaders } from './contextHeaders';
// Top-level import is safe: authStore does NOT import from client.ts (no circular dep)
import { useAuthStore } from '../features/auth/store/authStore';

type NavigateFn = (path: string) => void;
let navigateTo: NavigateFn | null = null;

/** Register the router's navigate function from main.tsx after router creation. */
export function setNavigationHandler(fn: NavigateFn): void {
  navigateTo = fn;
}

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor: attach JWT Bearer token + workspace context headers
apiClient.interceptors.request.use((config) => {
  const token: string | null = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return attachWorkspaceContextHeaders(config);
});

// Singleton refresh promise — prevents N concurrent 401s from firing N refresh calls
let refreshPromise: Promise<string> | null = null;

// Response interceptor: handle 401 with refresh-lock mechanism
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error?.config;
    if (error?.response?.status !== 401 || originalRequest?._retry) {
      return Promise.reject(error);
    }

    // Skip refresh on auth endpoints themselves (avoid infinite loop)
    const url: string = originalRequest?.url ?? '';
    if (url.includes('/auth/')) {
      return Promise.reject(error);
    }

    originalRequest._retry = true;

    if (!refreshPromise) {
      refreshPromise = apiClient
        .post<{ data: { accessToken: string } }>('/auth/refresh', {}, { withCredentials: true })
        .then((res) => {
          const newToken = res.data.data.accessToken;
          const currentUser = useAuthStore.getState().user;
          useAuthStore.getState().setTokens(newToken, currentUser!);
          return newToken;
        })
        .catch((refreshError) => {
          useAuthStore.getState().clearTokens();
          if (navigateTo) navigateTo('/login');
          return Promise.reject(refreshError);
        })
        .finally(() => {
          refreshPromise = null;
        });
    }

    try {
      const newToken = await refreshPromise;
      originalRequest.headers.Authorization = `Bearer ${newToken}`;
      return apiClient(originalRequest);
    } catch {
      return Promise.reject(error);
    }
  },
);

export default apiClient;