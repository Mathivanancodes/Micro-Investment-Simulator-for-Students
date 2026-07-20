import React from 'react';
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts';
import './PortfolioValueChart.css';

const PortfolioValueChart = ({ data }) => {
  if (!data || data.length === 0) {
    return (
      <div className="chart-placeholder">
        <span>No historical data available</span>
      </div>
    );
  }

  // Format currency in tooltips
  const formatCurrency = (val) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(val);
  };

  return (
    <div className="portfolio-value-chart">
      <ResponsiveContainer width="100%" height={300}>
        <AreaChart data={data} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
          <defs>
            <linearGradient id="colorValue" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#3B82F6" stopOpacity={0.25}/>
              <stop offset="95%" stopColor="#3B82F6" stopOpacity={0}/>
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" stroke="rgba(255, 255, 255, 0.04)" />
          <XAxis 
            dataKey="date" 
            stroke="var(--text-secondary)" 
            fontSize={12}
            tickLine={false}
            axisLine={false}
            dy={10}
          />
          <YAxis 
            stroke="var(--text-secondary)" 
            fontSize={12}
            tickLine={false}
            axisLine={false}
            tickFormatter={(val) => `$${val}`}
            dx={-5}
          />
          <Tooltip 
            contentStyle={{ 
              backgroundColor: 'rgba(17, 24, 39, 0.95)', 
              borderColor: 'rgba(255, 255, 255, 0.1)', 
              borderRadius: '8px',
              boxShadow: '0 8px 16px rgba(0,0,0,0.5)',
              color: 'var(--text-primary)'
            }}
            formatter={(value) => [formatCurrency(value), 'Portfolio Value']}
            labelStyle={{ fontWeight: 600, color: 'var(--text-secondary)' }}
          />
          <Area 
            type="monotone" 
            dataKey="value" 
            stroke="#3B82F6" 
            strokeWidth={3} 
            fillOpacity={1} 
            fill="url(#colorValue)" 
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
};

export default PortfolioValueChart;
