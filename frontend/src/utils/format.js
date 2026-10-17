const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' });

export const formatCurrency = (value) => inr.format(Number(value) || 0);
export const formatQuantity = (value) => (Number(value) || 0).toFixed(4);
export const formatPercent = (value) => `${Number(value) >= 0 ? '+' : ''}${(Number(value) || 0).toFixed(2)}%`;

export const formatDateTime = (value) => new Intl.DateTimeFormat('en-IN', {
  dateStyle: 'medium', timeStyle: 'short',
}).format(new Date(value));
