import { Link } from 'react-router-dom';

export default function SignupPage() {
  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
      <div style={{ textAlign: 'center' }}>
        <h1>회원가입</h1>
        <p>회원가입 폼이 여기에 구현됩니다.</p>
        <Link to="/login">로그인으로 돌아가기</Link>
      </div>
    </div>
  );
}
