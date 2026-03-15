export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  error: ErrorResponse | null;
}

export interface ErrorResponse {
  code: string;
  message: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

export interface User {
  id: number;
  email: string;
  name: string;
}

export type BrokerType = 'KIS' | 'KIWOOM' | 'LS';

export interface BrokerInfo {
  code: BrokerType;
  name: string;
}

export interface Account {
  id: number;
  brokerType: BrokerType;
  brokerName: string;
  accountNumber: string;
  accountName: string;
  appKey: string;
  secretKey: string;
  createdAt: string;
  updatedAt: string;
}

export interface AccountCreateRequest {
  brokerType: BrokerType;
  accountNumber: string;
  accountName: string;
  appKey: string;
  secretKey: string;
  additionalInfo?: string;
}

export interface AccountUpdateRequest {
  accountNumber?: string;
  accountName?: string;
  appKey?: string;
  secretKey?: string;
  additionalInfo?: string;
}
