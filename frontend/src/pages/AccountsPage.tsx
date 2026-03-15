import { useState, useEffect } from 'react';
import { useAccountStore } from '../store/accountStore';
import { accountService } from '../services/accountService';
import type { BrokerType, AccountCreateRequest, AccountUpdateRequest, Account } from '../types';

export default function AccountsPage() {
  const { accounts, brokers, loading, error, fetchAccounts, fetchBrokers, clearError } = useAccountStore();
  const [filterBroker, setFilterBroker] = useState<BrokerType | ''>('');
  const [showForm, setShowForm] = useState(false);
  const [editingAccount, setEditingAccount] = useState<Account | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null);

  // 등록 폼 상태
  const [formData, setFormData] = useState({
    brokerType: '' as BrokerType | '',
    accountNumber: '',
    accountName: '',
    appKey: '',
    secretKey: '',
  });

  // 수정 폼 상태
  const [editData, setEditData] = useState({
    accountName: '',
    appKey: '',
    secretKey: '',
  });

  useEffect(() => {
    fetchBrokers();
    fetchAccounts();
  }, [fetchAccounts, fetchBrokers]);

  const handleFilterChange = (broker: BrokerType | '') => {
    setFilterBroker(broker);
    fetchAccounts(broker || undefined);
  };

  const resetForm = () => {
    setFormData({ brokerType: '', accountNumber: '', accountName: '', appKey: '', secretKey: '' });
    setFormError(null);
    setShowForm(false);
  };

  const handleCreate = async () => {
    if (!formData.brokerType || !formData.accountNumber || !formData.appKey || !formData.secretKey) {
      setFormError('증권사, 계좌번호, App Key, Secret Key는 필수입니다.');
      return;
    }
    setFormError(null);
    try {
      const result = await accountService.createAccount(formData as AccountCreateRequest);
      if (result.success) {
        resetForm();
        fetchAccounts(filterBroker || undefined);
      } else {
        setFormError(result.error?.message || '계좌 등록에 실패했습니다.');
      }
    } catch {
      setFormError('계좌 등록에 실패했습니다.');
    }
  };

  const handleEditStart = (account: Account) => {
    setEditingAccount(account);
    setEditData({ accountName: account.accountName || '', appKey: '', secretKey: '' });
    setFormError(null);
  };

  const handleEditSave = async () => {
    if (!editingAccount) return;
    setFormError(null);
    const updateData: AccountUpdateRequest = {};
    if (editData.accountName) updateData.accountName = editData.accountName;
    if (editData.appKey) updateData.appKey = editData.appKey;
    if (editData.secretKey) updateData.secretKey = editData.secretKey;

    try {
      const result = await accountService.updateAccount(editingAccount.id, updateData);
      if (result.success) {
        setEditingAccount(null);
        fetchAccounts(filterBroker || undefined);
      } else {
        setFormError(result.error?.message || '계좌 수정에 실패했습니다.');
      }
    } catch {
      setFormError('계좌 수정에 실패했습니다.');
    }
  };

  const handleDelete = async (accountId: number) => {
    try {
      const result = await accountService.deleteAccount(accountId);
      if (result.success) {
        setDeleteConfirmId(null);
        fetchAccounts(filterBroker || undefined);
      }
    } catch {
      // 삭제 실패
    }
  };

  const inputStyle = {
    padding: '8px 12px',
    border: '1px solid #ccc',
    borderRadius: '4px',
    fontSize: '14px',
    width: '100%',
    boxSizing: 'border-box' as const,
  };

  const btnPrimary = {
    padding: '8px 20px',
    backgroundColor: '#1976d2',
    color: '#fff',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '14px',
  };

  const btnSecondary = {
    padding: '8px 20px',
    backgroundColor: '#fff',
    color: '#333',
    border: '1px solid #ccc',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '14px',
  };

  const btnDanger = {
    padding: '6px 14px',
    backgroundColor: '#d32f2f',
    color: '#fff',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '13px',
  };

  return (
    <div style={{ padding: '24px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <h2 style={{ margin: 0 }}>계좌 관리</h2>
        <button style={btnPrimary} onClick={() => { setShowForm(true); setEditingAccount(null); }}>
          계좌 등록
        </button>
      </div>

      {error && (
        <div style={{ padding: '12px', backgroundColor: '#ffebee', color: '#c62828', borderRadius: '4px', marginBottom: '16px' }}>
          {error}
          <button onClick={clearError} style={{ marginLeft: '12px', border: 'none', background: 'none', cursor: 'pointer', color: '#c62828' }}>✕</button>
        </div>
      )}

      {/* 증권사 필터 */}
      <div style={{ marginBottom: '20px', display: 'flex', gap: '8px' }}>
        <button
          style={filterBroker === '' ? btnPrimary : btnSecondary}
          onClick={() => handleFilterChange('')}
        >
          전체
        </button>
        {brokers.map((broker) => (
          <button
            key={broker.code}
            style={filterBroker === broker.code ? btnPrimary : btnSecondary}
            onClick={() => handleFilterChange(broker.code)}
          >
            {broker.name}
          </button>
        ))}
      </div>

      {/* 계좌 등록 폼 */}
      {showForm && (
        <div style={{ border: '1px solid #e0e0e0', borderRadius: '8px', padding: '20px', marginBottom: '24px', backgroundColor: '#fafafa' }}>
          <h3 style={{ margin: '0 0 16px 0' }}>계좌 등록</h3>
          {formError && (
            <div style={{ padding: '8px 12px', backgroundColor: '#ffebee', color: '#c62828', borderRadius: '4px', marginBottom: '12px', fontSize: '14px' }}>
              {formError}
            </div>
          )}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
            <div>
              <label style={{ display: 'block', marginBottom: '4px', fontSize: '13px', color: '#666' }}>증권사 *</label>
              <select
                value={formData.brokerType}
                onChange={(e) => setFormData({ ...formData, brokerType: e.target.value as BrokerType })}
                style={inputStyle}
              >
                <option value="">선택하세요</option>
                {brokers.map((b) => (
                  <option key={b.code} value={b.code}>{b.name}</option>
                ))}
              </select>
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: '4px', fontSize: '13px', color: '#666' }}>계좌번호 *</label>
              <input
                style={inputStyle}
                placeholder="예: 50123456-01"
                value={formData.accountNumber}
                onChange={(e) => setFormData({ ...formData, accountNumber: e.target.value })}
              />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: '4px', fontSize: '13px', color: '#666' }}>계좌명</label>
              <input
                style={inputStyle}
                placeholder="예: 한투 주식계좌"
                value={formData.accountName}
                onChange={(e) => setFormData({ ...formData, accountName: e.target.value })}
              />
            </div>
            <div />
            <div>
              <label style={{ display: 'block', marginBottom: '4px', fontSize: '13px', color: '#666' }}>App Key *</label>
              <input
                style={inputStyle}
                type="password"
                placeholder="증권사에서 발급받은 App Key"
                value={formData.appKey}
                onChange={(e) => setFormData({ ...formData, appKey: e.target.value })}
              />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: '4px', fontSize: '13px', color: '#666' }}>Secret Key *</label>
              <input
                style={inputStyle}
                type="password"
                placeholder="증권사에서 발급받은 Secret Key"
                value={formData.secretKey}
                onChange={(e) => setFormData({ ...formData, secretKey: e.target.value })}
              />
            </div>
          </div>
          <div style={{ marginTop: '16px', display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
            <button style={btnSecondary} onClick={resetForm}>취소</button>
            <button style={btnPrimary} onClick={handleCreate}>등록</button>
          </div>
        </div>
      )}

      {/* 수정 폼 */}
      {editingAccount && (
        <div style={{ border: '1px solid #e0e0e0', borderRadius: '8px', padding: '20px', marginBottom: '24px', backgroundColor: '#fff8e1' }}>
          <h3 style={{ margin: '0 0 16px 0' }}>계좌 수정 - {editingAccount.brokerName} ({editingAccount.accountNumber})</h3>
          {formError && (
            <div style={{ padding: '8px 12px', backgroundColor: '#ffebee', color: '#c62828', borderRadius: '4px', marginBottom: '12px', fontSize: '14px' }}>
              {formError}
            </div>
          )}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
            <div>
              <label style={{ display: 'block', marginBottom: '4px', fontSize: '13px', color: '#666' }}>계좌명</label>
              <input
                style={inputStyle}
                value={editData.accountName}
                onChange={(e) => setEditData({ ...editData, accountName: e.target.value })}
              />
            </div>
            <div />
            <div>
              <label style={{ display: 'block', marginBottom: '4px', fontSize: '13px', color: '#666' }}>새 App Key (변경 시에만 입력)</label>
              <input
                style={inputStyle}
                type="password"
                placeholder="변경할 App Key"
                value={editData.appKey}
                onChange={(e) => setEditData({ ...editData, appKey: e.target.value })}
              />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: '4px', fontSize: '13px', color: '#666' }}>새 Secret Key (변경 시에만 입력)</label>
              <input
                style={inputStyle}
                type="password"
                placeholder="변경할 Secret Key"
                value={editData.secretKey}
                onChange={(e) => setEditData({ ...editData, secretKey: e.target.value })}
              />
            </div>
          </div>
          <div style={{ marginTop: '16px', display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
            <button style={btnSecondary} onClick={() => { setEditingAccount(null); setFormError(null); }}>취소</button>
            <button style={btnPrimary} onClick={handleEditSave}>저장</button>
          </div>
        </div>
      )}

      {/* 계좌 목록 */}
      {loading ? (
        <p style={{ color: '#999' }}>불러오는 중...</p>
      ) : accounts.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
          <p>등록된 계좌가 없습니다.</p>
          <p style={{ fontSize: '14px' }}>상단의 "계좌 등록" 버튼을 눌러 계좌를 추가해보세요.</p>
        </div>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ borderBottom: '2px solid #e0e0e0' }}>
              <th style={{ padding: '12px', textAlign: 'left' }}>증권사</th>
              <th style={{ padding: '12px', textAlign: 'left' }}>계좌명</th>
              <th style={{ padding: '12px', textAlign: 'left' }}>계좌번호</th>
              <th style={{ padding: '12px', textAlign: 'left' }}>App Key</th>
              <th style={{ padding: '12px', textAlign: 'left' }}>Secret Key</th>
              <th style={{ padding: '12px', textAlign: 'center' }}>관리</th>
            </tr>
          </thead>
          <tbody>
            {accounts.map((account) => (
              <tr key={account.id} style={{ borderBottom: '1px solid #eee' }}>
                <td style={{ padding: '12px' }}>
                  <span style={{
                    padding: '4px 8px',
                    backgroundColor: '#e3f2fd',
                    borderRadius: '4px',
                    fontSize: '13px',
                    fontWeight: 'bold',
                  }}>
                    {account.brokerName}
                  </span>
                </td>
                <td style={{ padding: '12px' }}>{account.accountName || '-'}</td>
                <td style={{ padding: '12px', fontFamily: 'monospace' }}>{account.accountNumber}</td>
                <td style={{ padding: '12px', fontFamily: 'monospace', color: '#999' }}>{account.appKey}</td>
                <td style={{ padding: '12px', fontFamily: 'monospace', color: '#999' }}>{account.secretKey}</td>
                <td style={{ padding: '12px', textAlign: 'center' }}>
                  {deleteConfirmId === account.id ? (
                    <div style={{ display: 'flex', gap: '4px', justifyContent: 'center' }}>
                      <button style={btnDanger} onClick={() => handleDelete(account.id)}>확인</button>
                      <button style={btnSecondary} onClick={() => setDeleteConfirmId(null)}>취소</button>
                    </div>
                  ) : (
                    <div style={{ display: 'flex', gap: '4px', justifyContent: 'center' }}>
                      <button
                        style={{ ...btnSecondary, padding: '6px 14px', fontSize: '13px' }}
                        onClick={() => handleEditStart(account)}
                      >
                        수정
                      </button>
                      <button
                        style={{ ...btnDanger, backgroundColor: '#fff', color: '#d32f2f', border: '1px solid #d32f2f' }}
                        onClick={() => setDeleteConfirmId(account.id)}
                      >
                        삭제
                      </button>
                    </div>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
