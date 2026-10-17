import { useCallback } from 'react';
import { Link } from 'react-router-dom';
import { Wallet, Briefcase, TrendingUp, RefreshCw } from 'lucide-react';
import { api } from '../services/api';
import useResource from '../hooks/useResource';
import { formatCurrency, formatPercent, formatQuantity } from '../utils/format';
import StatCard from '../components/StatCard';
import PortfolioValueChart from '../components/PortfolioValueChart';
import PortfolioAllocationChart from '../components/PortfolioAllocationChart';
import './Dashboard.css';

export default function Dashboard() {
  const userId = localStorage.getItem('userId');
  const load = useCallback((config) => api.getPortfolio(userId, config), [userId]);
  const { data: portfolio, loading, refreshing, error, refresh } = useResource(load, 30000);

  if (loading) return <div className="center-state" role="status"><RefreshCw className="spinner" size={40} /><p>Loading your portfolio…</p></div>;
  if (!portfolio) return <div className="center-state error-state glass-card"><h1>Unable to load your portfolio</h1><p role="alert">{error}</p><button className="btn btn-primary" onClick={refresh}>Try again</button></div>;

  return (
    <div className="dashboard-page animate-fade-in">
      <header className="page-header">
        <div><h1>Your Investment Dashboard</h1><p className="subtitle">Virtual INR investments. Simulated prices refresh every 30 seconds.</p></div>
        <button className="btn btn-refresh" onClick={refresh} disabled={refreshing}><RefreshCw size={16} className={refreshing ? 'spinner' : ''} aria-hidden="true" /> Refresh</button>
      </header>
      {error && <div className="alert alert-danger" role="alert">{error} Showing the last loaded portfolio.</div>}
      <section className="stat-cards-grid" aria-label="Portfolio summary">
        <StatCard title="Total Portfolio Value" value={formatCurrency(portfolio.totalValue)} icon={<TrendingUp size={24} />} trendText="Unrealised return" trendValue={`${formatCurrency(portfolio.pnl)} (${formatPercent(portfolio.pnlPercentage)})`} isPositive={Number(portfolio.pnl) >= 0} />
        <StatCard title="Available Cash (Wallet)" value={formatCurrency(portfolio.walletBalance)} icon={<Wallet size={24} />} />
        <StatCard title="Holdings Market Value" value={formatCurrency(portfolio.holdingsValue)} icon={<Briefcase size={24} />} />
      </section>
      <div className="dashboard-actions"><Link className="btn btn-primary" to="/trade">Start investing</Link><Link className="btn btn-refresh" to="/wallet">Add virtual funds</Link></div>
      <section className="charts-grid" aria-label="Portfolio charts">
        <div className="glass-card chart-card"><h2>Performance History</h2><PortfolioValueChart data={portfolio.historicalValue} /></div>
        <div className="glass-card chart-card"><h2>Asset Allocation</h2><PortfolioAllocationChart holdings={portfolio.holdings} /></div>
      </section>
      <section className="holdings-section glass-card">
        <h2>Your Assets &amp; Holdings</h2>
        <div className="table-wrapper" tabIndex={0} role="region" aria-label="Your holdings table">
          <table className="holdings-table">
            <thead><tr><th scope="col">Stock</th><th scope="col">Quantity</th><th scope="col">Average Price</th><th scope="col">Current Price</th><th scope="col">Market Value</th><th scope="col">Gain / Loss</th></tr></thead>
            <tbody>{portfolio.holdings.length === 0
              ? <tr><td colSpan={6} className="empty-row">Your first investment can be just ₹10. <Link to="/trade">Explore stocks</Link>.</td></tr>
              : portfolio.holdings.map((holding) => <tr key={holding.stockId}>
                <td><strong className="ticker-cell">{holding.symbol}</strong><span className="stock-subtitle">{holding.name}</span></td>
                <td>{formatQuantity(holding.quantity)}</td><td>{formatCurrency(holding.averagePrice)}</td><td>{formatCurrency(holding.currentPrice)}</td><td>{formatCurrency(holding.marketValue)}</td>
                <td className={`pnl-cell ${Number(holding.pnl) >= 0 ? 'positive' : 'negative'}`}>{formatCurrency(holding.pnl)} ({formatPercent(holding.pnlPercentage)})</td>
              </tr>)}</tbody>
          </table>
        </div>
      </section>
      <p className="simulation-note">Portfolio value includes cash and current holdings. Gain/loss compares your remaining holdings with their cost. Deposits are not investment gains.</p>
    </div>
  );
}
