import { NavLink } from 'react-router-dom';

const menuItems = [
  { path: '/', label: '대시보드' },
  { path: '/balance', label: '잔고 조회' },
  { path: '/price', label: '실시간 시세' },
  { path: '/accounts', label: '계좌 관리' },
];

export default function Sidebar() {
  return (
    <nav style={{
      width: '200px',
      borderRight: '1px solid #e0e0e0',
      padding: '16px 0',
      backgroundColor: '#fafafa',
    }}>
      <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
        {menuItems.map((item) => (
          <li key={item.path}>
            <NavLink
              to={item.path}
              style={({ isActive }) => ({
                display: 'block',
                padding: '12px 24px',
                textDecoration: 'none',
                color: isActive ? '#1976d2' : '#333',
                backgroundColor: isActive ? '#e3f2fd' : 'transparent',
                fontWeight: isActive ? 'bold' : 'normal',
              })}
            >
              {item.label}
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  );
}
