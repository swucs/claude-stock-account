import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';

export default function Header() {
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const clearTokens = useAuthStore((state) => state.clearTokens);
  const navigate = useNavigate();

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
      <div style={{ position: 'relative' }}>
        <button
          onClick={() => setDropdownOpen(!dropdownOpen)}
          style={{
            padding: '8px 16px',
            cursor: 'pointer',
            border: '1px solid #ccc',
            borderRadius: '4px',
            backgroundColor: '#fff',
          }}
        >
          내 정보
        </button>
        {dropdownOpen && (
          <div style={{
            position: 'absolute',
            right: 0,
            top: '40px',
            border: '1px solid #ccc',
            borderRadius: '4px',
            backgroundColor: '#fff',
            boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
            zIndex: 10,
          }}>
            <button
              onClick={handleLogout}
              style={{
                padding: '8px 24px',
                cursor: 'pointer',
                border: 'none',
                backgroundColor: 'transparent',
                width: '100%',
              }}
            >
              로그아웃
            </button>
          </div>
        )}
      </div>
    </header>
  );
}
