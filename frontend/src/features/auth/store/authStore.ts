import { create } from 'zustand';
import { authApi } from '../api/authApi';

interface AuthState {
  token: string | null;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, displayName?: string) => Promise<void>;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: localStorage.getItem('accessToken'),

  login: async (email, password) => {
    const accessToken = await authApi.login({ email, password });
    localStorage.setItem('accessToken', accessToken);
    set({ token: accessToken });
  },

  register: async (email, password, displayName) => {
    await authApi.register({ email, password, displayName });
    // Auto-login after register
    const accessToken = await authApi.login({ email, password });
    localStorage.setItem('accessToken', accessToken);
    set({ token: accessToken });
  },

  logout: () => {
    localStorage.removeItem('accessToken');
    set({ token: null });
  },
}));
