import { useState, useEffect, useRef, useCallback } from 'react';
import { useAccountStore } from '../store/accountStore';
import { balanceService } from '../services/balanceService';
import type { BrokerType, BalanceResponse } from '../types';

export default function BalancePage() {
  const { accounts, brokers, fetchAccounts, fetchBrokers } = useAccountStore();
  const [selectedBroker, setSelectedBroker] = useState<BrokerType | ''>('');
  const [selectedAccountId, setSelectedAccountId] = useState<number | null>(null);
  const [balance, setBalance] = useState<BalanceResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [autoRefresh, setAutoRefresh] = useState(false);
  const [lastUpdated, setLastUpdated] = useState<string | null>(null);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    fetchBrokers();
    fetchAccounts();
  }, [fetchBrokers, fetchAccounts]);

  const filteredAccounts = selectedBroker
    ? accounts.filter((a) => a.brokerType === selectedBroker)
    : accounts;

  const fetchBalance = useCallback(async () => {
    if (!selectedAccountId) return;
    setLoading(true);
    setError(null);
    try {
      const result = await balanceService.getBalance(selectedAccountId);
      if (result.success && result.data) {
        setBalance(result.data);
        setLastUpdated(new Date().toLocaleTimeString());
      } else {
        setError(result.error?.message || '잔고 조회에 실패했습니다.');
      }
    } catch {
      setError('잔고 조회에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  }, [selectedAccountId]);

  useEffect(() => {
    if (selectedAccountId) {
      fetchBalance();
    } else {
      setBalance(null);
    }
  }, [selectedAccountId, fetchBalance]);

  // 30초 자동갱신
  useEffect(() => {
    if (autoRefresh && selectedAccountId) {
      intervalRef.current = setInterval(fetchBalance, 30000);
    }
    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    };
  }, [autoRefresh, selectedAccountId, fetchBalance]);

  const handleBrokerChange = (broker: BrokerType | '') => {
    setSelectedBroker(broker);
    setSelectedAccountId(null);
    setBalance(null);
    fetchAccounts(broker || undefined);
  };

  const formatNumber = (num: number) => num.toLocaleString('ko-KR');
  const formatRate = (rate: number) => `${rate >= 0 ? '+' : ''}${rate.toFixed(2)}%`;

  const cardStyle = {
    padding: '16px 20px',
    borderRadius: '8px',
    border: '1px solid #e0e0e0',
    textAlign: 'center' as const,
  };

  const selectStyle = {
    padding: '8px 12px',
    border: '1px solid #ccc',
    borderRadius: '4px',
    fontSize: '14px',
    minWidth: '160px',
  };

  return (
    <div style={{ padding: '24px' }}>
      <h2 style={{ margin: '0 0 24px 0' }}>잔고 조회</h2>

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
            onChange={(e) => setSelectedAccountId(e.target.value ? Number(e.target.value) : null)}
          >
            <option value="">선택하세요</option>
            {filteredAccounts.map((a) => (
              <option key={a.id} value={a.id}>
                {a.brokerName} - {a.accountNumber} {a.accountName ? `(${a.accountName})` : ''}
              </option>
            ))}
          </select>
        </div>

        {selectedAccountId && (
          <>
            <button
              style={{
                padding: '8px 16px',
                backgroundColor: '#1976d2',
                color: '#fff',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer',
                fontSize: '14px',
              }}
              onClick={fetchBalance}
              disabled={loading}
            >
              {loading ? '조회 중...' : '새로고침'}
            </button>
            <label style={{ display: 'flex', alignItems: 'center', gap: '6px', cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={autoRefresh}
                onChange={(e) => setAutoRefresh(e.target.checked)}
              />
              <span style={{ fontSize: '14px' }}>30초 자동갱신</span>
            </label>
            {lastUpdated && (
              <span style={{ fontSize: '12px', color: '#999' }}>마지막 갱신: {lastUpdated}</span>
            )}
          </>
        )}
      </div>

      {error && (
        <div style={{ padding: '12px', backgroundColor: '#ffebee', color: '#c62828', borderRadius: '4px', marginBottom: '16px' }}>
          {error}
        </div>
      )}

      {/* 잔고 요약 */}
      {balance && (
        <>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '16px', marginBottom: '24px' }}>
            <div style={cardStyle}>
              <div style={{ fontSize: '13px', color: '#666', marginBottom: '4px' }}>총 평가금액</div>
              <div style={{ fontSize: '24px', fontWeight: 'bold' }}>
                {formatNumber(balance.totalEvaluationAmount)}
                <span style={{ fontSize: '14px', color: '#666' }}> 원</span>
              </div>
            </div>
            <div style={cardStyle}>
              <div style={{ fontSize: '13px', color: '#666', marginBottom: '4px' }}>총 매입금액</div>
              <div style={{ fontSize: '24px', fontWeight: 'bold' }}>
                {formatNumber(balance.totalPurchaseAmount)}
                <span style={{ fontSize: '14px', color: '#666' }}> 원</span>
              </div>
            </div>
            <div style={cardStyle}>
              <div style={{ fontSize: '13px', color: '#666', marginBottom: '4px' }}>총 평가손익</div>
              <div style={{
                fontSize: '24px',
                fontWeight: 'bold',
                color: balance.totalProfitLossAmount >= 0 ? '#d32f2f' : '#1976d2',
              }}>
                {balance.totalProfitLossAmount >= 0 ? '+' : ''}{formatNumber(balance.totalProfitLossAmount)}
                <span style={{ fontSize: '14px' }}> 원</span>
              </div>
            </div>
          </div>

          {/* 보유종목 테이블 */}
          {balance.holdings.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
              보유 종목이 없습니다.
            </div>
          ) : (
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ borderBottom: '2px solid #e0e0e0' }}>
                  <th style={{ padding: '12px', textAlign: 'left' }}>종목코드</th>
                  <th style={{ padding: '12px', textAlign: 'left' }}>종목명</th>
                  <th style={{ padding: '12px', textAlign: 'right' }}>수량</th>
                  <th style={{ padding: '12px', textAlign: 'right' }}>평균매입가</th>
                  <th style={{ padding: '12px', textAlign: 'right' }}>현재가</th>
                  <th style={{ padding: '12px', textAlign: 'right' }}>평가금액</th>
                  <th style={{ padding: '12px', textAlign: 'right' }}>손익금액</th>
                  <th style={{ padding: '12px', textAlign: 'right' }}>수익률</th>
                </tr>
              </thead>
              <tbody>
                {balance.holdings.map((h) => (
                  <tr key={h.stockCode} style={{ borderBottom: '1px solid #eee' }}>
                    <td style={{ padding: '12px', fontFamily: 'monospace' }}>{h.stockCode}</td>
                    <td style={{ padding: '12px', fontWeight: 'bold' }}>{h.stockName}</td>
                    <td style={{ padding: '12px', textAlign: 'right' }}>{formatNumber(h.quantity)}</td>
                    <td style={{ padding: '12px', textAlign: 'right' }}>{formatNumber(h.avgPurchasePrice)}</td>
                    <td style={{ padding: '12px', textAlign: 'right' }}>{formatNumber(h.currentPrice)}</td>
                    <td style={{ padding: '12px', textAlign: 'right' }}>{formatNumber(h.evaluationAmount)}</td>
                    <td style={{
                      padding: '12px',
                      textAlign: 'right',
                      color: h.profitLossAmount >= 0 ? '#d32f2f' : '#1976d2',
                      fontWeight: 'bold',
                    }}>
                      {h.profitLossAmount >= 0 ? '+' : ''}{formatNumber(h.profitLossAmount)}
                    </td>
                    <td style={{
                      padding: '12px',
                      textAlign: 'right',
                      color: h.profitLossRate >= 0 ? '#d32f2f' : '#1976d2',
                      fontWeight: 'bold',
                    }}>
                      {formatRate(h.profitLossRate)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}

      {!selectedAccountId && !loading && (
        <div style={{ textAlign: 'center', padding: '60px', color: '#999' }}>
          <p style={{ fontSize: '16px' }}>증권사와 계좌를 선택하면 잔고를 조회합니다.</p>
          <p style={{ fontSize: '14px' }}>30초 자동갱신을 켜면 실시간으로 잔고가 업데이트됩니다.</p>
        </div>
      )}
    </div>
  );
}
