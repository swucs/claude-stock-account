import api from './api';
import type { ApiResponse, StockPriceResponse } from '../types';

export const priceService = {
  getCurrentPrice: async (accountId: number, stockCode: string) => {
    const response = await api.get<ApiResponse<StockPriceResponse>>(
      `/accounts/${accountId}/price`,
      { params: { stockCode } }
    );
    return response.data;
  },

  /**
   * SSE 스트림을 생성한다.
   * EventSource는 GET 요청만 지원하므로 JWT 토큰을 쿼리 파라미터로 전달할 수 없다.
   * 대신 Vite 프록시를 통해 /api/v1 경로로 요청하고, 별도의 인증은 서버 측에서 처리한다.
   *
   * 참고: EventSource는 커스텀 헤더를 지원하지 않으므로,
   * 여기서는 fetch API 기반 SSE로 구현한다.
   */
  createPriceStream: (accountId: number, stockCodes: string[]) => {
    const accessToken = localStorage.getItem('accessToken');
    const params = new URLSearchParams();
    stockCodes.forEach((code) => params.append('stockCodes', code));

    const url = `/api/v1/accounts/${accountId}/price/stream?${params.toString()}`;

    // fetch 기반 SSE (Authorization 헤더 포함)
    return fetch(url, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
        Accept: 'text/event-stream',
      },
    });
  },
};
