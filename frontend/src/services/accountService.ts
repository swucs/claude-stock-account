import api from './api';
import type {
  ApiResponse,
  Account,
  AccountCreateRequest,
  AccountUpdateRequest,
  BrokerInfo,
  BrokerType,
} from '../types';

export const accountService = {
  getAccounts: async (brokerType?: BrokerType) => {
    const params = brokerType ? { brokerType } : {};
    const response = await api.get<ApiResponse<Account[]>>('/accounts', { params });
    return response.data;
  },

  getAccount: async (accountId: number) => {
    const response = await api.get<ApiResponse<Account>>(`/accounts/${accountId}`);
    return response.data;
  },

  createAccount: async (data: AccountCreateRequest) => {
    const response = await api.post<ApiResponse<Account>>('/accounts', data);
    return response.data;
  },

  updateAccount: async (accountId: number, data: AccountUpdateRequest) => {
    const response = await api.put<ApiResponse<Account>>(`/accounts/${accountId}`, data);
    return response.data;
  },

  deleteAccount: async (accountId: number) => {
    const response = await api.delete<ApiResponse<void>>(`/accounts/${accountId}`);
    return response.data;
  },

  getBrokers: async () => {
    const response = await api.get<ApiResponse<BrokerInfo[]>>('/brokers');
    return response.data;
  },
};
