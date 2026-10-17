import { useState, useEffect, useCallback, lazy, Suspense } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { RefreshCw } from 'lucide-react';
import Navbar from './components/Navbar';
import Auth from './pages/Auth';
import { api, AUTH_EXPIRED_EVENT, clearSession, saveSession, errorMessage } from './services/api';

const Dashboard = lazy(() => import('./pages/Dashboard'));
const Trade = lazy(() => import('./pages/Trade'));
const Leaderboard = lazy(() => import('./pages/Leaderboard'));
const TransactionHistory = lazy(() => import('./pages/TransactionHistory'));
const Wallet = lazy(() => import('./pages/Wallet'));

function App() {
  const [authenticated, setAuthenticated] = useState(false);
  const [loading, setLoading] = useState(true);
  const [sessionError, setSessionError] = useState('');

  const checkAuth = useCallback(async () => {
    setLoading(true);
    setSessionError('');
    if (!localStorage.getItem('token')) {
      setAuthenticated(false);
      setLoading(false);
      return;
    }
    try {
      const profile = await api.getMe();
      saveSession(profile);
      setAuthenticated(true);
    } catch (error) {
      if (error.response?.status === 401) {
        clearSession();
        setAuthenticated(false);
      } else {
        // Keep credentials through a temporary connection or server failure.
        setSessionError(errorMessage(error));
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    checkAuth();
    const handleExpired = () => {
      setAuthenticated(false);
      setSessionError('');
    };
    window.addEventListener(AUTH_EXPIRED_EVENT, handleExpired);
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, handleExpired);
  }, [checkAuth]);

  const handleLogout = () => {
    clearSession();
    setAuthenticated(false);
    setSessionError('');
  };

  if (loading) return <div className="center-state full-screen" role="status"><RefreshCw className="spinner" size={40} /><p>Verifying your session…</p></div>;
  if (sessionError) return (
    <div className="center-state full-screen">
      <h1>Unable to check your session</h1><p role="alert">{sessionError}</p>
      <button className="btn btn-primary" onClick={checkAuth}>Try again</button>
      <button className="btn btn-refresh" onClick={handleLogout}>Back to sign in</button>
    </div>
  );
  if (!authenticated) return <Auth onLoginSuccess={() => setAuthenticated(true)} />;

  return (
    <div className="app-container">
      <a className="skip-link" href="#main-content">Skip to content</a>
      <Navbar onLogout={handleLogout} />
      <main className="main-content" id="main-content">
        <Suspense fallback={<div className="center-state" role="status"><RefreshCw className="spinner" size={32} /><p>Loading page…</p></div>}>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/wallet" element={<Wallet />} />
          <Route path="/trade" element={<Trade />} />
          <Route path="/leaderboard" element={<Leaderboard />} />
          <Route path="/transactions" element={<TransactionHistory />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
        </Suspense>
      </main>
    </div>
  );
}

export default App;
