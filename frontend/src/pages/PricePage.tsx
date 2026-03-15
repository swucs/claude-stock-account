import { useState, useEffect, useRef, useCallback } from 'react';
import { useAccountStore } from '../store/accountStore';
import { balanceService } from '../services/balanceService';
import { priceService } from '../services/priceService';
import type { BrokerType, StockPriceResponse } from '../types';

type ConnectionStatus = 'disconnected' | 'connecting' | 'connected' | 'error';

export default function PricePage() {
  const { accounts, brokers, fetchAccounts, fetchBrokers } = useAccountStore();
  const [selectedBroker, setSelectedBroker] = useState<BrokerType | ''>('');
  const [selectedAccountId, setSelectedAccountId] = useState<number | null>(null);
  const [stockCodes, setStockCodes] = useState<string[]>([]);
  const [prices, setPrices] = useState<Map<string, StockPriceResponse>>(new Map());
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('disconnected');
  const [error, setError] = useState<string | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    fetchBrokers();
    fetchAccounts();
  }, [fetchBrokers, fetchAccounts]);

  const filteredAccounts = selectedBroker
    ? accounts.filter((a) => a.brokerType === selectedBroker)
    : accounts;

  // 계좌 선택 시 보유종목 코드 조회
  const loadStockCodes = useCallback(async () => {
    if (!selectedAccountId) return;
    try {
      const result = await balanceService.getBalance(selectedAccountId);
      if (result.success && result.data) {
        const codes = result.data.holdings.map((h) => h.stockCode);
        setStockCodes(codes);
      }
    } catch {
      setError('보유종목 조회에 실패했습니다.');
    }
  }, [selectedAccountId]);

  useEffect(() => {
    if (selectedAccountId) {
      loadStockCodes();
    } else {
      setStockCodes([]);
      setPrices(new Map());
    }
  }, [selectedAccountId, loadStockCodes]);

  // SSE 스트림 연결
  const startStream = useCallback(async () => {
    if (!selectedAccountId || stockCodes.length === 0) return;

    // 기존 연결 종료
    if (abortRef.current) {
      abortRef.current.abort();
    }

    setConnectionStatus('connecting');
    setError(null);

    const controller = new AbortController();
    abortRef.current = controller;

    try {
      const response = await priceService.createPriceStream(selectedAccountId, stockCodes);

      if (!response.ok || !response.body) {
        setConnectionStatus('error');
        setError('시세 스트림 연결에 실패했습니다.');
        return;
      }

      setConnectionStatus('connected');
      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        if (controller.signal.aborted) break;

        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        for (const line of lines) {
          if (line.startsWith('data:')) {
            try {
              const data: StockPriceResponse = JSON.parse(line.substring(5).trim());
              setPrices((prev) => {
                const next = new Map(prev);
                next.set(data.stockCode, data);
                return next;
              });
            } catch {
              // JSON 파싱 실패 무시
            }
          }
        }
      }

      setConnectionStatus('disconnected');
    } catch (err) {
      if (!controller.signal.aborted) {
        setConnectionStatus('error');
        setError('시세 스트림 연결이 끊어졌습니다.');
      }
    }
  }, [selectedAccountId, stockCodes]);

  // 스트림 중지
  const stopStream = () => {
    if (abortRef.current) {
      abortRef.current.abort();
      abortRef.current = null;
    }
    setConnectionStatus('disconnected');
  };

  // 컴포넌트 언마운트 시 정리
  useEffect(() => {
    return () => {
      if (abortRef.current) {
        abortRef.current.abort();
      }
    };
  }, []);

  const handleBrokerChange = (broker: BrokerType | '') => {
    setSelectedBroker(broker);
    setSelectedAccountId(null);
    stopStream();
    setPrices(new Map());
    fetchAccounts(broker || undefined);
  };

  const handleAccountChange = (accountId: number | null) => {
    setSelectedAccountId(accountId);
    stopStream();
    setPrices(new Map());
  };

  const formatNumber = (num: number) => num.toLocaleString('ko-KR');
  const formatRate = (rate: number) => `${rate >= 0 ? '+' : ''}${rate.toFixed(2)}%`;

  const statusColor: Record<ConnectionStatus, string> = {
    disconnected: '#999',
    connecting: '#ff9800',
    connected: '#4caf50',
    error: '#d32f2f',
  };

  const statusText: Record<ConnectionStatus, string> = {
    disconnected: '연결 안됨',
    connecting: '연결 중...',
    connected: '실시간 연결됨',
    error: '연결 오류',
  };

  const selectStyle = {
    padding: '8px 12px',
    border: '1px solid #ccc',
    borderRadius: '4px',
    fontSize: '14px',
    minWidth: '160px',
  };

  const btnStyle = (active: boolean) => ({
    padding: '8px 16px',
    backgroundColor: active ? '#4caf50' : '#1976d2',
    color: '#fff',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '14px',
  });

  return (
    <div style={{ padding: '24px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <h2 style={{ margin: 0 }}>실시간 시세</h2>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{
            display: 'inline-block',
            width: '10px',
            height: '10px',
            borderRadius: '50%',
            backgroundColor: statusColor[connectionStatus],
          }} />
          <span style={{ fontSize: '13px', color: statusColor[connectionStatus] }}>
            {statusText[connectionStatus]}
          </span>
        </div>
      </div>

      {/* 증권사 & 계좌 선택 */}
      <div style={{ display: 'flex', gap: '12px', alignItems: 'center', marginBottom: '20px', flexWrap: 'wrap' }}>
        <div>
          <label style={{ fontSize: '13px', color: '#666', marginRight: '8px' }}>증권사</label>
          <select
            style={selectStyle}
            value={selectedBroker}
            onChange={(e) => handleBrokerChange(e.target.value as BrokerType | '')}
          >
            <option value="">전체</option>
            {brokers.map((b) => (
              <option key={b.code} value={b.code}>{b.name}</option>
            ))}
          </select>
        </div>
        <div>
          <label style={{ fontSize: '13px', color: '#666', marginRight: '8px' }}>계좌</label>
          <select
            style={selectStyle}
            value={selectedAccountId ?? ''}
            onChange={(e) => handleAccountChange(e.target.value ? Number(e.target.value) : null)}
          >
            <option value="">선택하세요</option>
            {filteredAccounts.map((a) => (
              <option key={a.id} value={a.id}>
                {a.brokerName} - {a.accountNumber} {a.accountName ? `(${a.accountName})` : ''}
              </option>
            ))}
          </select>
        </div>

        {selectedAccountId && stockCodes.length > 0 && (
          <>
            {connectionStatus === 'disconnected' || connectionStatus === 'error' ? (
              <button style={btnStyle(false)} onClick={startStream}>
                시세 연결
              </button>
            ) : (
              <button
                style={{ ...btnStyle(false), backgroundColor: '#d32f2f' }}
                onClick={stopStream}
              >
                연결 해제
              </button>
            )}
          </>
        )}
      </div>

      {error && (
        <div style={{ padding: '12px', backgroundColor: '#ffebee', color: '#c62828', borderRadius: '4px', marginBottom: '16px' }}>
          {error}
        </div>
      )}

      {/* 시세 테이블 */}
      {prices.size > 0 ? (
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ borderBottom: '2px solid #e0e0e0' }}>
              <th style={{ padding: '12px', textAlign: 'left' }}>종목코드</th>
              <th style={{ padding: '12px', textAlign: 'left' }}>종목명</th>
              <th style={{ padding: '12px', textAlign: 'right' }}>현재가</th>
              <th style={{ padding: '12px', textAlign: 'right' }}>전일대비</th>
              <th style={{ padding: '12px', textAlign: 'right' }}>등락률</th>
              <th style={{ padding: '12px', textAlign: 'right' }}>거래량</th>
              <th style={{ padding: '12px', textAlign: 'right' }}>시가</th>
              <th style={{ padding: '12px', textAlign: 'right' }}>고가</th>
              <th style={{ padding: '12px', textAlign: 'right' }}>저가</th>
            </tr>
          </thead>
          <tbody>
            {Array.from(prices.values()).map((p) => (
              <tr key={p.stockCode} style={{ borderBottom: '1px solid #eee' }}>
                <td style={{ padding: '12px', fontFamily: 'monospace' }}>{p.stockCode}</td>
                <td style={{ padding: '12px', fontWeight: 'bold' }}>{p.stockName}</td>
                <td style={{ padding: '12px', textAlign: 'right', fontWeight: 'bold' }}>
                  {formatNumber(p.currentPrice)}
                </td>
                <td style={{
                  padding: '12px',
                  textAlign: 'right',
                  color: p.changePrice >= 0 ? '#d32f2f' : '#1976d2',
                  fontWeight: 'bold',
                }}>
                  {p.changePrice >= 0 ? '+' : ''}{formatNumber(p.changePrice)}
                </td>
                <td style={{
                  padding: '12px',
                  textAlign: 'right',
                  color: p.changeRate >= 0 ? '#d32f2f' : '#1976d2',
                  fontWeight: 'bold',
                }}>
                  {formatRate(p.changeRate)}
                </td>
                <td style={{ padding: '12px', textAlign: 'right' }}>{formatNumber(p.volume)}</td>
                <td style={{ padding: '12px', textAlign: 'right' }}>{formatNumber(p.openPrice)}</td>
                <td style={{ padding: '12px', textAlign: 'right' }}>{formatNumber(p.highPrice)}</td>
                <td style={{ padding: '12px', textAlign: 'right' }}>{formatNumber(p.lowPrice)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : selectedAccountId && stockCodes.length > 0 ? (
        <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
          <p>"시세 연결" 버튼을 누르면 실시간 시세를 확인할 수 있습니다.</p>
        </div>
      ) : selectedAccountId && stockCodes.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
          <p>보유 종목이 없어 시세를 조회할 수 없습니다.</p>
        </div>
      ) : (
        <div style={{ textAlign: 'center', padding: '60px', color: '#999' }}>
          <p style={{ fontSize: '16px' }}>증권사와 계좌를 선택하면 보유종목의 실시간 시세를 확인합니다.</p>
          <p style={{ fontSize: '14px' }}>SSE(Server-Sent Events)를 통해 3초 간격으로 시세가 업데이트됩니다.</p>
        </div>
      )}
    </div>
  );
}
