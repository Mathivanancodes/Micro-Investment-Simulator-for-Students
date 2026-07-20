import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import StatCard from '../components/StatCard';
import PortfolioValueChart from '../components/PortfolioValueChart';
import PortfolioAllocationChart from '../components/PortfolioAllocationChart';
import { Wallet, Briefcase, TrendingUp, AlertCircle, RefreshCw } from 'lucide-react';
import './Dashboard.css';

const Dashboard = () => {
  const [portfolio, setPortfolio] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const userId = localStorage.getItem('userId') || 1; // Load dynamic userId from localStorage

  const fetchPortfolioData = () => {
    setLoading(true);
    api.getPortfolio(userId)
      .then((data) => {
        setPortfolio(data);
        setError(null);
      })
      .catch((err) => {
        console.error("Error fetching portfolio:", err);
        setError("Failed to load portfolio summary. Make sure the backend and database are running.");
      })
      .finally(() => {
        setLoading(false);
      });
  };

  useEffect(() => {
    fetchPortfolioData();
  }, []);

  const formatCurrency = (val) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(val || 0);
  };

  if (loading) {
    return (
      <div className="center-state">
        <RefreshCw className="spinner" size={40} />
        <p>Loading your portfolio dashboard...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="center-state error-state glass-card">
        <AlertCircle size={48} className="error-icon" />
        <h3>Connection Failed</h3>
        <p>{error}</p>
        <button className="btn btn-primary" onClick={fetchPortfolioData}>
          Try Again
        </button>
      </div>
    );
  }

  // Calculate total gains / loss
  const totalCost = portfolio.holdings.reduce((sum, h) => sum + (parseFloat(h.quantity) * parseFloat(h.averagePrice)), 0);
  const totalPnL = parseFloat(portfolio.holdingsValue) - totalCost;
  const totalPnLPct = totalCost > 0 ? (totalPnL / totalCost) * 100 : 0;
  const isPositivePnL = totalPnL >= 0;

  return (
    <div className="dashboard-page animate-fade-in">
      <header className="page-header">
        <div>
          <h1>Welcome Back, Student Trader</h1>
          <p className="subtitle">Track your virtual investments and portfolio performance</p>
        </div>
        <button className="btn btn-primary btn-refresh" onClick={fetchPortfolioData}>
          <RefreshCw size={16} />
          <span>Refresh</span>
        </button>
      </header>

      {/* Grid of Stat Cards */}
      <section className="stat-cards-grid">
        <StatCard
          title="Total Portfolio Value"
          value={formatCurrency(portfolio.totalValue)}
          icon={<TrendingUp size={24} />}
          trendText="Total Return"
          trendValue={`${formatCurrency(totalPnL)} (${totalPnLPct.toFixed(2)}%)`}
          isPositive={isPositivePnL}
        />
        <StatCard
          title="Available Cash (Wallet)"
          value={formatCurrency(portfolio.walletBalance)}
          icon={<Wallet size={24} />}
        />
        <StatCard
          title="Holdings Market Value"
          value={formatCurrency(portfolio.holdingsValue)}
          icon={<Briefcase size={24} />}
        />
      </section>

      {/* Charts Section */}
      <section className="charts-grid">
        <div className="glass-card chart-card">
          <h3>Performance History</h3>
          <PortfolioValueChart data={portfolio.historicalValue} />
        </div>
        <div className="glass-card chart-card">
          <h3>Asset Allocation</h3>
          <PortfolioAllocationChart holdings={portfolio.holdings} />
        </div>
      </section>

      {/* Holdings Table Section */}
      <section className="holdings-section glass-card">
        <h3>Your Assets & Holdings</h3>
        <div className="table-wrapper">
          <table className="holdings-table">
            <thead>
              <tr>
                <th>Ticker</th>
                <th>Quantity</th>
                <th>Avg Price</th>
                <th>Current Price</th>
                <th>Market Value</th>
                <th>Gain/Loss</th>
              </tr>
            </thead>
            <tbody>
              {portfolio.holdings.length === 0 ? (
                <tr>
                  <td colSpan="6" className="empty-row">
                    You don't hold any stocks yet. Go to the <strong>Trade</strong> page to start investing!
                  </td>
                </tr>
              ) : (
                portfolio.holdings.map((h, i) => {
                  const pnlVal = parseFloat(h.pnl);
                  const isPos = pnlVal >= 0;
                  return (
                    <tr key={i}>
                      <td className="ticker-cell">{h.symbol}</td>
                      <td>{parseFloat(h.quantity).toFixed(4)}</td>
                      <td>{formatCurrency(h.averagePrice)}</td>
                      <td>{formatCurrency(h.currentPrice)}</td>
                      <td>{formatCurrency(h.marketValue)}</td>
                      <td className={`pnl-cell ${isPos ? 'positive' : 'negative'}`}>
                        {isPos ? '+' : ''}{formatCurrency(h.pnl)} ({parseFloat(h.pnlPercentage).toFixed(2)}%)
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
};

export default Dashboard;
