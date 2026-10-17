import { useState } from 'react';
import { GraduationCap, Lock, Mail, User, RefreshCw, AlertTriangle } from 'lucide-react';
import { api, errorMessage, saveSession } from '../services/api';
import './Auth.css';

export default function Auth({ onLoginSuccess }) {
  const [isLogin, setIsLogin] = useState(true);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError('');
    try {
      const data = isLogin
        ? await api.login(email.trim(), password)
        : await api.register(name.trim(), email.trim(), password);
      saveSession(data);
      onLoginSuccess();
    } catch (requestError) {
      setError(requestError.response?.data?.message || errorMessage(requestError));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page animate-fade-in">
      <div className="auth-container glass-card">
        <header className="auth-header">
          <GraduationCap className="auth-logo-icon" size={48} aria-hidden="true" />
          <h1>ApexTrade</h1>
          <p>Micro-Investment Simulator for Students</p>
          <p className="simulation-note">Start with ₹1,000 in virtual cash. Learn with simulated prices.</p>
        </header>
        <div className="auth-tabs" aria-label="Account access">
          <button type="button" className={`tab-btn ${isLogin ? 'active' : ''}`} aria-pressed={isLogin} disabled={loading} onClick={() => { setIsLogin(true); setError(''); }}>Sign In</button>
          <button type="button" className={`tab-btn ${!isLogin ? 'active' : ''}`} aria-pressed={!isLogin} disabled={loading} onClick={() => { setIsLogin(false); setError(''); }}>Create Account</button>
        </div>
        {error && <div className="auth-error alert alert-danger" role="alert"><AlertTriangle size={18} aria-hidden="true" /><span>{error}</span></div>}
        <form className="auth-form" onSubmit={handleSubmit}>
          {!isLogin && <div className="form-group">
            <label htmlFor="auth-name">Name</label>
            <div className="input-with-icon"><User size={18} className="input-icon" aria-hidden="true" />
              <input id="auth-name" type="text" autoComplete="name" placeholder="Your name" value={name} onChange={(event) => setName(event.target.value)} required minLength={2} maxLength={80} disabled={loading} />
            </div>
          </div>}
          <div className="form-group">
            <label htmlFor="auth-email">Email address</label>
            <div className="input-with-icon"><Mail size={18} className="input-icon" aria-hidden="true" />
              <input id="auth-email" type="email" autoComplete="email" placeholder="student@example.com" value={email} onChange={(event) => setEmail(event.target.value)} required maxLength={100} disabled={loading} />
            </div>
          </div>
          <div className="form-group">
            <label htmlFor="auth-password">Password</label>
            <div className="input-with-icon"><Lock size={18} className="input-icon" aria-hidden="true" />
              <input id="auth-password" type="password" autoComplete={isLogin ? 'current-password' : 'new-password'} placeholder={isLogin ? 'Your password' : 'At least 8 characters'} value={password} onChange={(event) => setPassword(event.target.value)} required minLength={isLogin ? 1 : 8} maxLength={72} disabled={loading} />
            </div>
            {!isLogin && <small>Use 8–72 characters.</small>}
          </div>
          <button type="submit" className="btn btn-primary btn-auth-submit" disabled={loading}>{loading ? <><RefreshCw className="spinner" size={18} aria-hidden="true" /> Please wait…</> : (isLogin ? 'Sign In' : 'Create Wallet')}</button>
        </form>
        <p className="auth-disclaimer">For learning only. No real money or market orders.</p>
      </div>
    </div>
  );
}
