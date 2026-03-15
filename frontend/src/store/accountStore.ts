import { create } from 'zustand';
import type { Account, BrokerInfo, BrokerType } from '../types';
import { accountService } from '../services/accountService';

interface AccountState {
  accounts: Account[];
  brokers: BrokerInfo[];
  loading: boolean;
  error: string | null;
  fetchAccounts: (brokerType?: BrokerType) => Promise<void>;
  fetchBrokers: () => Promise<void>;
  clearError: () => void;
}

export const useAccountStore = create<AccountState>((set) => ({
  accounts: [],
  brokers: [],
  loading: false,
  error: null,

  fetchAccounts: async (brokerType?: BrokerType) => {
    set({ loading: true, error: null });
    try {
      const result = await accountService.getAccounts(brokerType);
      if (result.success && result.data) {
        set({ accounts: result.data, loading: false });
      } else {
        set({ error: result.error?.message || '계좌 목록을 불러오지 못했습니다.', loading: false });
      }
    } catch {
      set({ error: '계좌 목록을 불러오지 못했습니다.', loading: false });
    }
  },

  fetchBrokers: async () => {
    try {
      const result = await accountService.getBrokers();
      if (result.success && result.data) {
        set({ brokers: result.data });
      }
    } catch {
      // 증권사 목록 로드 실패는 조용히 처리
    }
  },

  clearError: () => set({ error: null }),
}));
