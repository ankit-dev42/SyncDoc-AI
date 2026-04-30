import apiClient from '../../../api/client';

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

interface TokenResponse {
  accessToken: string;
}

export const authApi = {
  login: async (req: LoginRequest): Promise<string> => {
    const res = await apiClient.post<ApiResponse<TokenResponse>>('/auth/login', req);
    return res.data.data.accessToken;
  },

  register: async (req: RegisterRequest): Promise<void> => {
    await apiClient.post('/auth/register', req);
  },
};
