import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { authService } from '../../services/authService';

export default function Header() {
  const clearTokens = useAuthStore((state) => state.clearTokens);
  const userName = useAuthStore((state) => state.userName);
  const setUserName = useAuthStore((state) => state.setUserName);
  const navigate = useNavigate();

  useEffect(() => {
    if (!userName) {
      authService.getMe().then((result) => {
        if (result.success && result.data) {
          setUserName(result.data.name);
        }
      }).catch(() => {});
    }
  }, [userName, setUserName]);

  const handleLogout = () => {
    clearTokens();
    navigate('/login');
  };

  return (
    <header style={{
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      padding: '0 24px',
      height: '56px',
      borderBottom: '1px solid #e0e0e0',
      backgroundColor: '#fff',
    }}>
      <h1 style={{ fontSize: '18px', fontWeight: 'bold' }}>Stock Account</h1>
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
        <span style={{ fontSize: '14px', color: '#555' }}>
          {userName || '사용자'}
        </span>
        <button
          onClick={handleLogout}
          style={{
            padding: '6px 14px',
            cursor: 'pointer',
            border: '1px solid #ccc',
            borderRadius: '4px',
            backgroundColor: '#fff',
            fontSize: '13px',
            color: '#555',
            whiteSpace: 'nowrap',
          }}
        >
          로그아웃
        </button>
      </div>
    </header>
  );
}
