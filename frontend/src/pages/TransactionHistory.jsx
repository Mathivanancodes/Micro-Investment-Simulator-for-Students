import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { RefreshCw, History, AlertCircle, ArrowUpRight, ArrowDownRight } from 'lucide-react';
import './TransactionHistory.css';

const TransactionHistory = () => {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const userId = localStorage.getItem('userId') || 1; // Load dynamic userId from localStorage

  const fetchTransactions = () => {
    setLoading(true);
    api.getTransactions(userId)
      .then((data) => {
        setTransactions(data);
        setError(null);
      })
      .catch((err) => {
        console.error("Error fetching transactions:", err);
        setError("Failed to load transaction logs. Make sure the backend is running.");
      })
      .finally(() => {
        setLoading(false);
      });
  };

  useEffect(() => {
    fetchTransactions();
  }, []);

  const formatCurrency = (val) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(val);
  };

  const formatDate = (dateString) => {
    const dateObj = new Date(dateString);
    return dateObj.toLocaleString('en-US', {
      month: 'short',
      day: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  };

  if (loading) {
    return (
      <div className="center-state">
        <RefreshCw className="spinner" size={40} />
        <p>Loading your trading logs...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="center-state error-state glass-card">
        <AlertCircle size={48} className="error-icon" />
        <h3>Connection Failed</h3>
        <p>{error}</p>
        <button className="btn btn-primary" onClick={fetchTransactions}>
          Try Again
        </button>
      </div>
    );
  }

  return (
    <div className="history-page animate-fade-in">
      <header className="page-header">
        <div>
          <h1>Transaction History</h1>
          <p className="subtitle">Auditable record of all your buy and sell order executions</p>
        </div>
        <button className="btn btn-primary btn-refresh" onClick={fetchTransactions}>
          <RefreshCw size={16} />
          <span>Refresh</span>
        </button>
      </header>

      {/* Standings List Table */}
      <section className="history-section glass-card">
        <div className="history-section-header">
          <History className="header-icon" size={20} />
          <h3>Executed Trades</h3>
        </div>
        <div className="table-wrapper">
          <table className="history-table">
            <thead>
              <tr>
                <th>Tx ID</th>
                <th>Execution Date</th>
                <th>Ticker</th>
                <th>Action</th>
                <th>Quantity</th>
                <th>Price / Share</th>
                <th>Total Value</th>
              </tr>
            </thead>
            <tbody>
              {transactions.length === 0 ? (
                <tr>
                  <td colSpan="7" className="empty-row">
                    No transactions found. Go to the <strong>Trade</strong> page to place your first order!
                  </td>
                </tr>
              ) : (
                transactions.map((tx) => {
                  const isBuy = tx.type === 'BUY';
                  return (
                    <tr key={tx.id}>
                      <td className="tx-id-col">#{tx.id}</td>
                      <td>{formatDate(tx.timestamp)}</td>
                      <td className="ticker-cell">{tx.stockSymbol}</td>
                      <td>
                        <span className={`action-badge ${isBuy ? 'buy' : 'sell'}`}>
                          {isBuy ? <ArrowUpRight size={14} /> : <ArrowDownRight size={14} />}
                          <span>{tx.type}</span>
                        </span>
                      </td>
                      <td>{parseFloat(tx.quantity).toFixed(4)}</td>
                      <td>{formatCurrency(tx.price)}</td>
                      <td className="total-col">{formatCurrency(tx.totalAmount)}</td>
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

export default TransactionHistory;
