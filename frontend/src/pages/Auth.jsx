import React, { useState } from 'react';
import { api } from '../services/api';
import { GraduationCap, Lock, Mail, User, RefreshCw, AlertTriangle } from 'lucide-react';
import './Auth.css';

const Auth = ({ onLoginSuccess }) => {
  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!username || !password || (!isLogin && !email)) {
      setError("Please fill out all required fields.");
      return;
    }

    setLoading(true);
    setError('');

    try {
      let data;
      if (isLogin) {
        data = await api.login(username, password);
      } else {
        data = await api.register(username, email, password);
      }

      // Store auth credentials
      localStorage.setItem('token', data.token);
      localStorage.setItem('userId', data.userId);
      localStorage.setItem('username', data.username);

      if (onLoginSuccess) {
        onLoginSuccess(data);
      }
    } catch (err) {
      console.error("Authentication error:", err);
      if (err.response && err.response.data && err.response.data.error) {
        setError(err.response.data.error);
      } else {
        setError("Connection failed. Make sure the Spring Boot backend is active.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page animate-fade-in">
      <div className="auth-container glass-card">
        <div className="auth-header">
          <GraduationCap className="auth-logo-icon" size={48} />
          <h2>ApexTrade</h2>
          <p>Micro-Investment Simulator for Students</p>
        </div>

        <div className="auth-tabs">
          <button 
            type="button" 
            className={`tab-btn ${isLogin ? 'active' : ''}`}
            onClick={() => { setIsLogin(true); setError(''); }}
          >
            Sign In
          </button>
          <button 
            type="button" 
            className={`tab-btn ${!isLogin ? 'active' : ''}`}
            onClick={() => { setIsLogin(false); setError(''); }}
          >
            Create Account
          </button>
        </div>

        {error && (
          <div className="auth-error alert alert-danger animate-fade-in">
            <AlertTriangle size={18} />
            <span>{error}</span>
          </div>
        )}

        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="auth-username">Username</label>
            <div className="input-with-icon">
              <User size={18} className="input-icon" />
              <input
                id="auth-username"
                type="text"
                placeholder="Enter username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                disabled={loading}
              />
            </div>
          </div>

          {!isLogin && (
            <div className="form-group">
              <label htmlFor="auth-email">Email Address</label>
              <div className="input-with-icon">
                <Mail size={18} className="input-icon" />
                <input
                  id="auth-email"
                  type="email"
                  placeholder="student@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  disabled={loading}
                />
              </div>
            </div>
          )}

          <div className="form-group">
            <label htmlFor="auth-password">Password</label>
            <div className="input-with-icon">
              <Lock size={18} className="input-icon" />
              <input
                id="auth-password"
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={loading}
              />
            </div>
          </div>

          <button type="submit" className="btn btn-primary btn-auth-submit" disabled={loading}>
            {loading ? <RefreshCw className="spinner" size={18} /> : (isLogin ? 'Sign In' : 'Create Wallet')}
          </button>
        </form>
      </div>
    </div>
  );
};

export default Auth;
