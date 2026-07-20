import React from 'react';
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import './PortfolioAllocationChart.css';

const COLORS = [
  '#3B82F6', // Blue
  '#10B981', // Emerald Green
  '#F59E0B', // Amber Yellow
  '#8B5CF6', // Purple
  '#EC4899', // Pink
  '#06B6D4', // Cyan
  '#14B8A6', // Teal
];

const PortfolioAllocationChart = ({ holdings }) => {
  if (!holdings || holdings.length === 0) {
    return (
      <div className="chart-placeholder">
        <span>No holdings to display allocation</span>
      </div>
    );
  }

  // Format data for Recharts Pie
  const data = holdings.map((h) => ({
    name: h.symbol,
    value: parseFloat(h.marketValue),
  }));

  const formatCurrency = (val) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(val);
  };

  return (
    <div className="portfolio-allocation-chart">
      <ResponsiveContainer width="100%" height={260}>
        <PieChart>
          <Pie
            data={data}
            cx="50%"
            cy="45%"
            innerRadius={60}
            outerRadius={85}
            paddingAngle={3}
            dataKey="value"
          >
            {data.map((entry, index) => (
              <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} stroke="rgba(17, 24, 39, 0.8)" strokeWidth={2} />
            ))}
          </Pie>
          <Tooltip
            contentStyle={{
              backgroundColor: 'rgba(17, 24, 39, 0.95)',
              borderColor: 'rgba(255, 255, 255, 0.1)',
              borderRadius: '8px',
              color: 'var(--text-primary)',
              fontSize: '12px',
              boxShadow: '0 8px 16px rgba(0,0,0,0.5)',
            }}
            formatter={(value) => [formatCurrency(value), 'Value']}
          />
          <Legend 
            verticalAlign="bottom" 
            height={36}
            iconType="circle"
            iconSize={8}
            formatter={(value) => <span className="legend-label">{value}</span>}
          />
        </PieChart>
      </ResponsiveContainer>
    </div>
  );
};

export default PortfolioAllocationChart;
