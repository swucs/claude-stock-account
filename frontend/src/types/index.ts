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

// 잔고 관련
export interface StockHolding {
  stockCode: string;
  stockName: string;
  quantity: number;
  avgPurchasePrice: number;
  currentPrice: number;
  evaluationAmount: number;
  profitLossAmount: number;
  profitLossRate: number;
}

export interface BalanceResponse {
  accountId: number;
  accountNumber: string;
  brokerName: string;
  totalEvaluationAmount: number;
  totalPurchaseAmount: number;
  totalProfitLossAmount: number;
  holdings: StockHolding[];
}

// 시세 관련
export interface StockPriceResponse {
  stockCode: string;
  stockName: string;
  currentPrice: number;
  changePrice: number;
  changeRate: number;
  volume: number;
  highPrice: number;
  lowPrice: number;
  openPrice: number;
}
