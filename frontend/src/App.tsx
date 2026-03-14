import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from './components/layout/Layout';
import ProtectedRoute from './components/common/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import DashboardPage from './pages/DashboardPage';
import BalancePage from './pages/BalancePage';
import PricePage from './pages/PricePage';
import AccountsPage from './pages/AccountsPage';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* 공개 라우트 */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />

        {/* 보호 라우트 */}
        <Route element={<ProtectedRoute />}>
          <Route element={<Layout />}>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/balance" element={<BalancePage />} />
            <Route path="/price" element={<PricePage />} />
            <Route path="/accounts" element={<AccountsPage />} />
          </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
