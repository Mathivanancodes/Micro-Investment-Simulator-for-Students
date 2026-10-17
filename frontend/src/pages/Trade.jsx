import { useCallback, useState } from 'react';
import { Link } from 'react-router-dom';
import { RefreshCw, Search, CheckCircle, AlertTriangle, Lightbulb } from 'lucide-react';
import { api, errorMessage } from '../services/api';
import useResource from '../hooks/useResource';
import { formatCurrency, formatQuantity } from '../utils/format';
import PortfolioValueChart from '../components/PortfolioValueChart';
import tips from '../assets/tips.json';
import './Trade.css';

export default function Trade() {
  const userId = localStorage.getItem('userId');
  const load = useCallback(async (config) => {
    const [stocks, portfolio] = await Promise.all([api.getStocks(config), api.getPortfolio(userId, config)]);
    return { stocks, portfolio };
  }, [userId]);
  const { data, loading, refreshing, error, refresh } = useResource(load, 30000);
  const [selectedId, setSelectedId] = useState(null);
  const [side, setSide] = useState('BUY');
  const [input, setInput] = useState('');
  const [search, setSearch] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState('');
  const [tradeError, setTradeError] = useState('');
  const [tip, setTip] = useState(null);

  const selectedStock = data?.stocks.find((stock) => stock.id === selectedId) || data?.stocks[0];
  const stockId = selectedStock?.id;
  const loadHistory = useCallback((config) => stockId ? api.getStockHistory(stockId, 7, config) : Promise.resolve([]), [stockId]);
  const { data: history, refreshing: historyLoading, error: historyError } = useResource(loadHistory, 30000);
  const holding = data?.portfolio.holdings.find((item) => item.stockId === stockId);
  const buying = side === 'BUY';
  const numericInput = Number(input);
  const estimatedQuantity = buying && selectedStock ? Math.round(numericInput / Number(selectedStock.currentPrice) * 10000) / 10000 : numericInput;
  const estimatedAmount = buying ? numericInput : numericInput * Number(selectedStock?.currentPrice || 0);

  const clearFeedback = () => { setSuccess(''); setTradeError(''); setTip(null); };
  const selectStock = (id) => { setSelectedId(id); setInput(''); clearFeedback(); };
  const changeSide = (value) => { setSide(value); setInput(''); clearFeedback(); };

  const submitTrade = async (event) => {
    event.preventDefault();
    if (!selectedStock || !Number.isFinite(numericInput) || numericInput <= 0) {
      setTradeError('Enter a valid positive amount or share quantity.');
      return;
    }
    setSubmitting(true);
    clearFeedback();
    try {
      const trade = buying
        ? await api.buyStock(userId, stockId, numericInput)
        : await api.sellStock(userId, stockId, numericInput);
      setSuccess(`Successfully ${buying ? 'bought' : 'sold'} ${formatQuantity(trade.quantity)} shares of ${trade.symbol} for ${formatCurrency(trade.amount)}. Wallet: ${formatCurrency(trade.remainingBalance)}.`);
      setInput('');
      const updated = await refresh();
      if (!buying && holding && Number(trade.price) < Number(holding.averagePrice)) setTip(tips.LOSS_SALE);
      else if (buying && !data.portfolio.holdings.length) setTip(tips.FIRST_BUY);
      else if (buying && updated?.portfolio.holdings.length >= 3) setTip(tips.DIVERSIFICATION);
    } catch (requestError) {
      setTradeError(errorMessage(requestError, 'The trade could not be completed. Check your balance or holdings.'));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <div className="center-state" role="status"><RefreshCw className="spinner" size={40} /><p>Loading the stock market…</p></div>;
  if (!data) return <div className="center-state error-state glass-card"><h1>Unable to load the market</h1><p role="alert">{error}</p><button className="btn btn-primary" onClick={refresh}>Try again</button></div>;
  const matches = data.stocks.filter((stock) => `${stock.symbol} ${stock.name}`.toLowerCase().includes(search.toLowerCase()));
  const chart = (history || []).map((entry) => ({ date: new Date(entry.recordedAt).toLocaleDateString('en-IN', { month: 'short', day: 'numeric' }), value: Number(entry.price) }));

  return <div className="trade-page animate-fade-in">
    <header className="page-header"><div><h1>Stock Market Board</h1><p className="subtitle">Buy ₹10–₹500 of fractional shares. Prices are simulated.</p></div><Link to="/wallet" className="wallet-pill glass-card"><span>Available Cash:</span><strong>{formatCurrency(data.portfolio.walletBalance)}</strong></Link></header>
    <div className="market-status"><span>Prices refresh every 30 seconds. Orders use the server price at execution.</span><button className="btn btn-refresh" onClick={refresh} disabled={refreshing || submitting}><RefreshCw size={16} className={refreshing ? 'spinner' : ''} aria-hidden="true" /> Refresh prices</button></div>
    {error && <div className="alert alert-danger" role="alert">{error} Displayed prices may be out of date.</div>}
    <div className="trade-grid">
      <section className="glass-card stocks-list-card" aria-label="Available stocks">
        <div className="search-bar"><Search size={18} className="search-icon" aria-hidden="true" /><label className="sr-only" htmlFor="stock-search">Search stocks</label><input id="stock-search" type="search" placeholder="Search stocks" value={search} onChange={(event) => setSearch(event.target.value)} /></div>
        <div className="stocks-list">{matches.length === 0 ? <p className="empty-search">No stocks match your search.</p> : matches.map((stock) => <button type="button" key={stock.id} className={`stock-item ${stock.id === stockId ? 'selected' : ''}`} aria-pressed={stock.id === stockId} disabled={submitting} onClick={() => selectStock(stock.id)}><span className="stock-info"><span className="stock-symbol">{stock.symbol}</span><span className="stock-name">{stock.name}</span></span><span className="stock-price">{formatCurrency(stock.currentPrice)}</span></button>)}</div>
      </section>
      <section className="order-desk" aria-label="Trade order">
        {selectedStock ? <div className="glass-card order-card">
          <header className="selected-stock-header"><div><h2>{selectedStock.symbol}</h2><p>{selectedStock.name}</p></div><div className="selected-stock-price"><span>Price per share</span><h3>{formatCurrency(selectedStock.currentPrice)}</h3></div></header>
          <div className="stock-chart-section"><h3>7-Day Price History</h3>{historyLoading ? <div className="chart-placeholder" role="status"><RefreshCw className="spinner" size={24} /><span>Loading price history…</span></div> : historyError ? <p role="alert" className="simulation-note">Price history is currently unavailable. You can still place an order.</p> : <PortfolioValueChart data={chart} valueLabel="Price per share" />}</div>
          {success && <div className="alert alert-success" role="status"><CheckCircle size={18} aria-hidden="true" /><span>{success}</span></div>}
          {tradeError && <div className="alert alert-danger" role="alert"><AlertTriangle size={18} aria-hidden="true" /><span>{tradeError}</span></div>}
          {tip && <aside className="alert alert-info edu-tip-alert"><Lightbulb className="edu-icon" size={26} aria-hidden="true" /><div className="edu-text-container"><strong>{tip.title}</strong><p>{tip.text}</p></div></aside>}
          <div className="order-tabs" aria-label="Order type"><button className={`tab-btn ${buying ? 'active' : ''}`} type="button" aria-pressed={buying} disabled={submitting} onClick={() => changeSide('BUY')}>Buy</button><button className={`tab-btn ${!buying ? 'active' : ''}`} type="button" aria-pressed={!buying} disabled={submitting} onClick={() => changeSide('SELL')}>Sell</button></div>
          <form className="trade-form" onSubmit={submitTrade}>
            <div className="form-group"><label htmlFor="trade-input">{buying ? 'Investment amount (INR)' : 'Quantity to sell (shares)'}</label><input id="trade-input" type="number" inputMode="decimal" step={buying ? '0.01' : '0.0001'} min={buying ? '10' : '0.0001'} max={buying ? '500' : undefined} placeholder={buying ? '100.00' : '0.1000'} value={input} onChange={(event) => { setInput(event.target.value); clearFeedback(); }} disabled={submitting} required /><small>{buying ? '₹10 to ₹500 per order, up to two decimal places.' : `You own ${formatQuantity(holding?.quantity)} shares. Use up to four decimal places.`}</small></div>
            <div className="trade-calculations"><div className="calc-row"><span>{buying ? 'Estimated shares:' : 'Shares to sell:'}</span><span>{formatQuantity(estimatedQuantity)}</span></div><div className="calc-row total"><span>{buying ? 'Investment amount:' : 'Estimated proceeds:'}</span><strong>{formatCurrency(estimatedAmount)}</strong></div></div>
            <p className="simulation-note">Fractional shares are rounded to the nearest four decimal places (HALF_UP). Your entered investment amount is debited; the executed quantity and price appear in your confirmation.</p>
            <button type="submit" className={`btn ${buying ? 'btn-success' : 'btn-danger'}`} disabled={submitting || !input}>{submitting ? <><RefreshCw className="spinner" size={18} aria-hidden="true" /> Placing order…</> : buying ? 'Buy shares' : 'Sell shares'}</button>
          </form>
        </div> : <div className="glass-card no-selection"><p>No stocks are available yet.</p></div>}
      </section>
    </div>
  </div>;
}
