import React, { useState, useEffect } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import Navbar from './components/Navbar';
import Dashboard from './pages/Dashboard';
import Trade from './pages/Trade';
import Leaderboard from './pages/Leaderboard';
import TransactionHistory from './pages/TransactionHistory';
import Auth from './pages/Auth';
import { api } from './services/api';
import { RefreshCw } from 'lucide-react';

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [loading, setLoading] = useState(true);

  const checkAuth = () => {
    const token = localStorage.getItem('token');
    if (!token) {
      setIsAuthenticated(false);
      setLoading(false);
      return;
    }

    // Verify token validity by calling profile endpoint
    api.getMe()
      .then(() => {
        setIsAuthenticated(true);
      })
      .catch((err) => {
        console.error("Auth validation failed:", err);
        // Clean up credentials if token expired
        localStorage.clear();
        setIsAuthenticated(false);
      })
      .finally(() => {
        setLoading(false);
      });
  };

  useEffect(() => {
    checkAuth();
  }, []);

  const handleLoginSuccess = (data) => {
    setIsAuthenticated(true);
  };

  const handleLogout = () => {
    localStorage.clear();
    setIsAuthenticated(false);
  };

  if (loading) {
    return (
      <div className="center-state" style={{ minHeight: '100vh' }}>
        <RefreshCw className="spinner" size={40} />
        <p>Verifying secure session...</p>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Auth onLoginSuccess={handleLoginSuccess} />;
  }

  return (
    <div className="app-container">
      {/* Sidebar Navigation */}
      <Navbar onLogout={handleLogout} />
      
      {/* Primary Page Content Wrapper */}
      <main className="main-content">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/trade" element={<Trade />} />
          <Route path="/leaderboard" element={<Leaderboard />} />
          <Route path="/transactions" element={<TransactionHistory />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}

export default App;
