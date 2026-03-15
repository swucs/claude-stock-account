import api from './api';
import type { ApiResponse } from '../types';

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface UserResponse {
  id: number;
  email: string;
  name: string;
  createdAt: string;
}

export const authService = {
  signup: async (email: string, password: string, name: string) => {
    const response = await api.post<ApiResponse<UserResponse>>('/auth/signup', {
      email,
      password,
      name,
    });
    return response.data;
  },

  login: async (email: string, password: string) => {
    const response = await api.post<ApiResponse<TokenResponse>>('/auth/login', {
      email,
      password,
    });
    return response.data;
  },

  refresh: async (refreshToken: string) => {
    const response = await api.post<ApiResponse<TokenResponse>>('/auth/refresh', {
      refreshToken,
    });
    return response.data;
  },

  getMe: async () => {
    const response = await api.get<ApiResponse<UserResponse>>('/users/me');
    return response.data;
  },

  updateMe: async (data: { name?: string; currentPassword?: string; newPassword?: string }) => {
    const response = await api.put<ApiResponse<UserResponse>>('/users/me', data);
    return response.data;
  },
};
