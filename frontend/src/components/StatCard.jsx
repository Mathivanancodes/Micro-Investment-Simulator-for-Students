import React from 'react';
import './StatCard.css';

const StatCard = ({ title, value, icon, trendText, trendValue, isPositive }) => {
  const showTrend = trendText || trendValue;
  
  return (
    <div className="stat-card glass-card animate-fade-in">
      <div className="stat-card-header">
        <span className="stat-card-title">{title}</span>
        <div className="stat-card-icon">{icon}</div>
      </div>
      <div className="stat-card-body">
        <h2 className="stat-card-value">{value}</h2>
        {showTrend && (
          <div className={`stat-card-trend ${isPositive ? 'positive' : 'negative'}`}>
            <span>
              {isPositive ? '+' : ''}{trendValue}
            </span>
            <span className="trend-text">{trendText}</span>
          </div>
        )}
      </div>
    </div>
  );
};

export default StatCard;
