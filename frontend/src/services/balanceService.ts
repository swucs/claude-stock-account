import api from './api';
import type { ApiResponse, BalanceResponse } from '../types';

export const balanceService = {
  getBalance: async (accountId: number) => {
    const response = await api.get<ApiResponse<BalanceResponse>>(
      `/accounts/${accountId}/balance`
    );
    return response.data;
  },
};
