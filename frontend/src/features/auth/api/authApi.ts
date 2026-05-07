import apiClient from '../../../api/client';
import type { AuthUser } from '../store/authStore';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName?: string;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

interface LoginResponse {
  accessToken: string;
  user: AuthUser;
}

export const authApi = {
  login: async (req: LoginRequest): Promise<LoginResponse> => {
    const res = await apiClient.post<ApiResponse<LoginResponse>>('/auth/login', req);
    return res.data.data;
  },

  register: async (req: RegisterRequest): Promise<void> => {
    await apiClient.post('/auth/register', req);
  },
};
