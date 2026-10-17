import { useCallback, useState } from 'react';
import { Link } from 'react-router-dom';
import { Wallet as WalletIcon, RefreshCw } from 'lucide-react';
import { api, errorMessage } from '../services/api';
import useResource from '../hooks/useResource';
import { formatCurrency } from '../utils/format';
import './Wallet.css';

export default function Wallet() {
  const userId = localStorage.getItem('userId');
  const load = useCallback((config) => api.getWallet(userId, config), [userId]);
  const { data: wallet, replaceData, loading, refreshing, error, refresh } = useResource(load);
  const [amount, setAmount] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [depositError, setDepositError] = useState('');
  const [success, setSuccess] = useState('');

  const deposit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setDepositError('');
    setSuccess('');
    try {
      const updated = await api.deposit(userId, Number(amount));
      replaceData(updated);
      setSuccess(`${formatCurrency(amount)} in virtual funds added to your wallet.`);
      setAmount('');
    } catch (requestError) {
      setDepositError(errorMessage(requestError));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <div className="center-state" role="status"><RefreshCw className="spinner" size={40} /><p>Loading your wallet…</p></div>;
  if (!wallet) return <div className="center-state error-state glass-card"><h1>Unable to load your wallet</h1><p role="alert">{error}</p><button className="btn btn-primary" onClick={refresh}>Try again</button></div>;

  return <div className="wallet-page animate-fade-in">
    <header className="page-header"><div><h1>Your Virtual Wallet</h1><p className="subtitle">Practice with virtual rupees. No payment details needed.</p></div><button className="btn btn-refresh" onClick={refresh} disabled={refreshing || submitting}><RefreshCw size={16} className={refreshing ? 'spinner' : ''} aria-hidden="true" /> Refresh</button></header>
    {error && <div className="alert alert-danger" role="alert">{error}</div>}
    <section className="wallet-grid">
      <div className="glass-card wallet-summary"><WalletIcon size={36} aria-hidden="true" /><h2>Available balance</h2><p className="wallet-balance" aria-label="Available balance">{formatCurrency(wallet.balance)}</p><p>Your account starts with ₹1,000. Buy fractional shares from ₹10 to ₹500 per order.</p><Link to="/trade" className="btn btn-primary">Explore stocks</Link></div>
      <div className="glass-card wallet-deposit"><h2>Add virtual funds</h2><p>Deposits are recorded in your transaction history.</p>
        {success && <div className="alert alert-success" role="status">{success}</div>}
        {depositError && <div className="alert alert-danger" role="alert">{depositError}</div>}
        <form onSubmit={deposit} className="deposit-form"><div className="form-group"><label htmlFor="deposit-amount">Deposit amount (INR)</label><input id="deposit-amount" type="number" min="0.01" max="9999999999999.99" step="0.01" required inputMode="decimal" placeholder="100.00" value={amount} onChange={(event) => { setAmount(event.target.value); setSuccess(''); setDepositError(''); }} disabled={submitting} /><small>Enter a positive amount with up to two decimal places.</small></div><button className="btn btn-success" type="submit" disabled={submitting}>{submitting ? 'Adding funds…' : 'Add virtual funds'}</button></form>
        <Link to="/transactions">View transaction history</Link>
      </div>
    </section>
  </div>;
}
