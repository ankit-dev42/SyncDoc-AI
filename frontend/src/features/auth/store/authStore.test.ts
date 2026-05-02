import { describe, it, expect, beforeEach } from 'vitest';
import { useAuthStore } from './authStore';

const mockUser = { id: 'user-1', email: 'test@example.com', displayName: 'Test User' };

beforeEach(() => {
  useAuthStore.getState().clearTokens();
  sessionStorage.clear();
});

describe('authStore', () => {
  it('setTokens persists accessToken to sessionStorage', () => {
    useAuthStore.getState().setTokens('tok_123', mockUser);
    const stored = sessionStorage.getItem('auth-storage');
    expect(stored).not.toBeNull();
    const parsed = JSON.parse(stored!);
    expect(parsed.state.accessToken).toBe('tok_123');
  });

  it('setTokens stores user in memory but NOT in sessionStorage', () => {
    useAuthStore.getState().setTokens('tok_123', mockUser);
    expect(useAuthStore.getState().user).toEqual(mockUser);
    const stored = JSON.parse(sessionStorage.getItem('auth-storage') ?? '{}');
    expect(stored.state?.user).toBeUndefined();
  });

  it('clearTokens removes accessToken from sessionStorage', () => {
    useAuthStore.getState().setTokens('tok_123', mockUser);
    useAuthStore.getState().clearTokens();
    const stored = sessionStorage.getItem('auth-storage');
    if (stored) {
      const parsed = JSON.parse(stored);
      expect(parsed.state?.accessToken).toBeNull();
    } else {
      expect(stored).toBeNull();
    }
  });

  it('clearTokens sets both accessToken and user to null', () => {
    useAuthStore.getState().setTokens('tok_123', mockUser);
    useAuthStore.getState().clearTokens();
    expect(useAuthStore.getState().accessToken).toBeNull();
    expect(useAuthStore.getState().user).toBeNull();
  });
});
