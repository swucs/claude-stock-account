import { Link } from 'react-router-dom';

export default function LoginPage() {
  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
      <div style={{ textAlign: 'center' }}>
        <h1>로그인</h1>
        <p>로그인 폼이 여기에 구현됩니다.</p>
        <Link to="/signup">회원가입</Link>
      </div>
    </div>
  );
}
