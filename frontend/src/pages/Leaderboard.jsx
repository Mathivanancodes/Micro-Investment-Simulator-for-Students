import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { Award, Trophy, Medal, RefreshCw, AlertCircle } from 'lucide-react';
import './Leaderboard.css';

const Leaderboard = () => {
  const [standings, setStandings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const currentUserId = parseInt(localStorage.getItem('userId')) || 1;

  const fetchStandings = () => {
    setLoading(true);
    api.getLeaderboard()
      .then((data) => {
        setStandings(data);
        setError(null);
      })
      .catch((err) => {
        console.error("Error fetching standings:", err);
        setError("Failed to load standings. Make sure the backend database is running.");
      })
      .finally(() => {
        setLoading(false);
      });
  };

  useEffect(() => {
    fetchStandings();
  }, []);

  const formatCurrency = (val) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(val);
  };

  const getRankBadge = (rank) => {
    switch (rank) {
      case 1:
        return <Trophy className="rank-badge gold" size={20} />;
      case 2:
        return <Medal className="rank-badge silver" size={20} />;
      case 3:
        return <Medal className="rank-badge bronze" size={20} />;
      default:
        return <span className="rank-number">{rank}</span>;
    }
  };

  const getInitials = (name) => {
    if (!name) return '??';
    const parts = name.split(' ');
    if (parts.length >= 2) {
      return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  };

  if (loading) {
    return (
      <div className="center-state">
        <RefreshCw className="spinner" size={40} />
        <p>Calculating student rankings...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="center-state error-state glass-card">
        <AlertCircle size={48} className="error-icon" />
        <h3>Connection Failed</h3>
        <p>{error}</p>
        <button className="btn btn-primary" onClick={fetchStandings}>
          Try Again
        </button>
      </div>
    );
  }

  // Get top 3 users if available
  const top1 = standings.length > 0 ? standings[0] : null;
  const top2 = standings.length > 1 ? standings[1] : null;
  const top3 = standings.length > 2 ? standings[2] : null;

  return (
    <div className="leaderboard-page animate-fade-in">
      <header className="page-header">
        <div>
          <h1>Classroom Leaderboard</h1>
          <p className="subtitle">Compete with your classmates in real-time portfolio performance</p>
        </div>
        <button className="btn btn-primary btn-refresh" onClick={fetchStandings}>
          <RefreshCw size={16} />
          <span>Refresh</span>
        </button>
      </header>

      {/* Top 3 podium highlights */}
      {standings.length > 0 && (
        <section className="podium-section">
          {/* Rank 2 Podium Spot */}
          {top2 ? (
            <div className="podium-spot spot-2 glass-card">
              <Medal className="podium-badge silver" size={32} />
              <div className="podium-avatar">{getInitials(top2.username)}</div>
              <h3>{top2.username}</h3>
              <p className="podium-value">{formatCurrency(top2.totalValue)}</p>
              <span className={`podium-return ${parseFloat(top2.returnRate) >= 0 ? 'positive' : 'negative'}`}>
                {parseFloat(top2.returnRate) >= 0 ? '+' : ''}{parseFloat(top2.returnRate).toFixed(2)}%
              </span>
            </div>
          ) : (
            <div className="podium-spot spot-2 glass-card empty">
              <span className="empty-text">No rank yet</span>
            </div>
          )}

          {/* Rank 1 Podium Spot */}
          {top1 ? (
            <div className="podium-spot spot-1 glass-card">
              <Trophy className="podium-badge gold" size={40} />
              <div className="podium-avatar">{getInitials(top1.username)}</div>
              <h3>{top1.username}</h3>
              <p className="podium-value">{formatCurrency(top1.totalValue)}</p>
              <span className={`podium-return ${parseFloat(top1.returnRate) >= 0 ? 'positive' : 'negative'}`}>
                {parseFloat(top1.returnRate) >= 0 ? '+' : ''}{parseFloat(top1.returnRate).toFixed(2)}%
              </span>
            </div>
          ) : (
            <div className="podium-spot spot-1 glass-card empty">
              <span className="empty-text">No users registered</span>
            </div>
          )}

          {/* Rank 3 Podium Spot */}
          {top3 ? (
            <div className="podium-spot spot-3 glass-card">
              <Medal className="podium-badge bronze" size={32} />
              <div className="podium-avatar">{getInitials(top3.username)}</div>
              <h3>{top3.username}</h3>
              <p className="podium-value">{formatCurrency(top3.totalValue)}</p>
              <span className={`podium-return ${parseFloat(top3.returnRate) >= 0 ? 'positive' : 'negative'}`}>
                {parseFloat(top3.returnRate) >= 0 ? '+' : ''}{parseFloat(top3.returnRate).toFixed(2)}%
              </span>
            </div>
          ) : (
            <div className="podium-spot spot-3 glass-card empty">
              <span className="empty-text">No rank yet</span>
            </div>
          )}
        </section>
      )}

      {/* Leaderboard standings list */}
      <section className="standings-section glass-card">
        <h3>Current Standings</h3>
        <div className="table-wrapper">
          <table className="standings-table">
            <thead>
              <tr>
                <th className="rank-col">Rank</th>
                <th>Student</th>
                <th>Portfolio Value</th>
                <th>Return Rate</th>
                <th>Trades Count</th>
              </tr>
            </thead>
            <tbody>
              {standings.length === 0 ? (
                <tr>
                  <td colSpan="5" className="empty-row">No records found. Register accounts to view rankings.</td>
                </tr>
              ) : (
                standings.map((row) => {
                  const isCurrentUser = row.userId === currentUserId;
                  const rate = parseFloat(row.returnRate);
                  return (
                    <tr 
                      key={row.userId} 
                      className={isCurrentUser ? 'current-user-row' : ''}
                    >
                      <td className="rank-col">{getRankBadge(row.rank)}</td>
                      <td className="student-name-col">
                        {row.username} {isCurrentUser && <span className="you-tag">YOU</span>}
                      </td>
                      <td className="value-col">{formatCurrency(row.totalValue)}</td>
                      <td className={`return-col ${rate >= 0 ? 'positive' : 'negative'}`}>
                        {rate >= 0 ? '+' : ''}{rate.toFixed(2)}%
                      </td>
                      <td>{row.tradesCount}</td>
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

export default Leaderboard;
