import { useCallback, useState } from 'react';
import { RefreshCw, History, ArrowUpRight, ArrowDownRight, Wallet } from 'lucide-react';
import { api } from '../services/api';
import useResource from '../hooks/useResource';
import { formatCurrency, formatQuantity, formatDateTime } from '../utils/format';
import './TransactionHistory.css';

export default function TransactionHistory() {
  const userId = localStorage.getItem('userId');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const load = useCallback((config) => api.getTransactions(userId, page, size, config), [userId, page, size]);
  const { data, loading, refreshing, error, refresh } = useResource(load);

  if (loading) return <div className="center-state" role="status"><RefreshCw className="spinner" size={40} /><p>Loading your transactions…</p></div>;
  if (!data) return <div className="center-state error-state glass-card"><h1>Unable to load your history</h1><p role="alert">{error}</p><button className="btn btn-primary" onClick={refresh}>Try again</button></div>;

  return <div className="history-page animate-fade-in">
    <header className="page-header"><div><h1>Transaction History</h1><p className="subtitle">Your complete record of virtual deposits, purchases, and sales.</p></div><button className="btn btn-refresh" onClick={refresh} disabled={refreshing}><RefreshCw size={16} className={refreshing ? 'spinner' : ''} aria-hidden="true" /> Refresh</button></header>
    {error && <div className="alert alert-danger" role="alert">{error}</div>}
    <section className="history-section glass-card" aria-busy={refreshing}>
      <div className="history-section-header"><History className="header-icon" size={20} aria-hidden="true" /><h2>Wallet &amp; Trade Activity</h2><span className="transaction-count">{data.totalElements} transactions</span></div>
      <div className="table-wrapper" tabIndex={0} role="region" aria-label="Transaction history table">
        <table className="history-table"><thead><tr><th scope="col">ID</th><th scope="col">Date</th><th scope="col">Stock</th><th scope="col">Action</th><th scope="col">Quantity</th><th scope="col">Price / Share</th><th scope="col">Amount</th></tr></thead>
          <tbody>{data.content.length === 0 ? <tr><td colSpan={7} className="empty-row">No transactions yet. Add virtual funds or make your first investment.</td></tr> : data.content.map((transaction) => {
            const deposit = transaction.type === 'DEPOSIT';
            const Icon = deposit ? Wallet : transaction.type === 'BUY' ? ArrowUpRight : ArrowDownRight;
            return <tr key={transaction.id}><td className="tx-id-col">#{transaction.id}</td><td>{formatDateTime(transaction.createdAt)}</td><td className="ticker-cell">{transaction.symbol || 'Wallet'}</td><td><span className={`action-badge ${transaction.type.toLowerCase()}`}><Icon size={14} aria-hidden="true" />{transaction.type}</span></td><td>{deposit ? '—' : formatQuantity(transaction.quantity)}</td><td>{deposit ? '—' : formatCurrency(transaction.price)}</td><td className="total-col">{formatCurrency(transaction.amount)}</td></tr>;
          })}</tbody>
        </table>
      </div>
      <nav className="pagination" aria-label="Transaction pages"><label htmlFor="page-size">Rows per page <select id="page-size" value={size} onChange={(event) => { setSize(Number(event.target.value)); setPage(0); }} disabled={refreshing}><option value={5}>5</option><option value={10}>10</option><option value={20}>20</option></select></label><span role="status">Page {data.totalPages ? data.page + 1 : 1} of {Math.max(data.totalPages, 1)}</span><div className="pagination-buttons"><button className="btn btn-refresh" disabled={refreshing || data.page === 0} onClick={() => setPage(data.page - 1)}>Previous</button><button className="btn btn-refresh" disabled={refreshing || data.page + 1 >= data.totalPages} onClick={() => setPage(data.page + 1)}>Next</button></div></nav>
    </section>
  </div>;
}
