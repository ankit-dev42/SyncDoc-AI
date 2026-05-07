import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

export interface AuthUser {
  id: string;
  email: string;
  displayName: string;
}

interface AuthState {
  accessToken: string | null;
  user: AuthUser | null;
  setTokens: (accessToken: string, user: AuthUser) => void;
  clearTokens: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      user: null,

      setTokens: (accessToken, user) => set({ accessToken, user }),

      clearTokens: () => set({ accessToken: null, user: null }),
    }),
    {
      name: 'auth-storage',
      storage: createJSONStorage(() => sessionStorage),
      // Only persist the access token — user is reconstructed from the token on next load
      partialize: (state) => ({ accessToken: state.accessToken }),
    },
  ),
);
