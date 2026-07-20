import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import PortfolioValueChart from '../components/PortfolioValueChart';
import { RefreshCw, Search, ArrowUpRight, ArrowDownRight, CheckCircle, AlertTriangle, Lightbulb } from 'lucide-react';
import tipsData from '../assets/tips.json';
import './Trade.css';

const Trade = () => {
  const [stocks, setStocks] = useState([]);
  const [selectedStock, setSelectedStock] = useState(null);
  const [stockHistory, setStockHistory] = useState([]);
  const [walletBalance, setWalletBalance] = useState(0);
  const [quantity, setQuantity] = useState('');
  
  const [loading, setLoading] = useState(true);
  const [chartLoading, setChartLoading] = useState(false);
  const [orderLoading, setOrderLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  
  // Status Messages
  const [successMsg, setSuccessMsg] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [eduTip, setEduTip] = useState(null);

  const userId = localStorage.getItem('userId') || 1; // Dynamic userId

  const loadData = async () => {
    setLoading(true);
    try {
      // 1. Fetch available stocks
      const stockList = await api.getStocks();
      setStocks(stockList);
      
      // 2. Fetch user wallet balance from portfolio endpoint
      const portfolioData = await api.getPortfolio(userId);
      setWalletBalance(portfolioData.walletBalance);

      // Select first stock by default if nothing is selected
      if (stockList.length > 0 && !selectedStock) {
        handleSelectStock(stockList[0]);
      } else if (selectedStock) {
        // Refresh currently selected stock data
        const updated = stockList.find(s => s.symbol === selectedStock.symbol);
        if (updated) setSelectedStock(updated);
      }
      
      setErrorMsg('');
    } catch (err) {
      console.error("Error loading trade data:", err);
      setErrorMsg("Failed to connect to the backend server. Verify your application is running.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleSelectStock = async (stock) => {
    setSelectedStock(stock);
    setChartLoading(true);
    setErrorMsg('');
    setSuccessMsg('');
    setEduTip(null);
    setQuantity('');

    try {
      const history = await api.getStockHistory(stock.symbol, 7);
      
      // Map PriceHistory to expected format for PortfolioValueChart
      const formattedHistory = history.map(item => {
        const dateObj = new Date(item.timestamp);
        return {
          date: dateObj.toLocaleDateString('en-US', { month: 'short', day: '2-digit' }),
          value: parseFloat(item.price)
        };
      });

      setStockHistory(formattedHistory);
    } catch (err) {
      console.error("Error loading stock history:", err);
      setStockHistory([]);
    } finally {
      setChartLoading(false);
    }
  };

  const handleTrade = async (type) => {
    if (!selectedStock || !quantity || parseFloat(quantity) <= 0) {
      setErrorMsg("Please enter a valid stock quantity.");
      return;
    }

    setOrderLoading(true);
    setErrorMsg('');
    setSuccessMsg('');
    setEduTip(null);

    try {
      // Pre-evaluate educational conditions BEFORE execution state transitions
      const pastTx = await api.getTransactions(userId);
      const isFirstBuy = type === 'BUY' && pastTx.length === 0;

      const portfolioBefore = await api.getPortfolio(userId);
      const currentHolding = portfolioBefore.holdings.find(h => h.symbol === selectedStock.symbol);
      const isLossSale = type === 'SELL' && currentHolding && (parseFloat(selectedStock.currentPrice) < parseFloat(currentHolding.averagePrice));

      let response;
      const qtyVal = parseFloat(quantity);

      if (type === 'BUY') {
        response = await api.buyStock(userId, selectedStock.symbol, qtyVal);
        setSuccessMsg(`Successfully bought ${qtyVal.toFixed(4)} shares of ${selectedStock.symbol}!`);
      } else {
        response = await api.sellStock(userId, selectedStock.symbol, qtyVal);
        setSuccessMsg(`Successfully sold ${qtyVal.toFixed(4)} shares of ${selectedStock.symbol}!`);
      }

      setQuantity('');
      
      // Reload wallet, stocks list and evaluate new diversification status
      const updatedPortfolio = await api.getPortfolio(userId);
      setWalletBalance(updatedPortfolio.walletBalance);
      
      const stockList = await api.getStocks();
      setStocks(stockList);
      const updatedStock = stockList.find(s => s.symbol === selectedStock.symbol);
      if (updatedStock) setSelectedStock(updatedStock);

      // Check diversification (3+ sectors)
      const sectors = new Set();
      const tech = ['AAPL', 'MSFT', 'GOOGL', 'NVDA'];
      const consumer = ['AMZN', 'TSLA', 'DIS', 'WMT'];
      const finance = ['JPM', 'BAC', 'V', 'KO'];
      
      updatedPortfolio.holdings.forEach(h => {
        if (tech.includes(h.symbol)) sectors.add('Tech');
        else if (consumer.includes(h.symbol)) sectors.add('Consumer');
        else if (finance.includes(h.symbol)) sectors.add('Finance');
      });
      const isDiversified = sectors.size >= 3;

      // Set corresponding learning alert tip
      if (isFirstBuy) {
        setEduTip(tipsData.FIRST_BUY);
      } else if (isLossSale) {
        setEduTip(tipsData.LOSS_SALE);
      } else if (isDiversified && updatedPortfolio.holdings.length >= 3) {
        setEduTip(tipsData.DIVERSIFICATION);
      }

    } catch (err) {
      console.error("Trade execution failed:", err);
      if (err.response && err.response.data && err.response.data.message) {
        setErrorMsg(err.response.data.message);
      } else {
        setErrorMsg("Order execution failed. Please verify your holdings or balance.");
      }
    } finally {
      setOrderLoading(false);
    }
  };

  const formatCurrency = (val) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(val || 0);
  };

  // Filter stocks based on query
  const filteredStocks = stocks.filter(
    s => s.symbol.toLowerCase().includes(searchQuery.toLowerCase()) || 
         s.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const calculatedTotal = selectedStock && quantity 
    ? parseFloat(selectedStock.currentPrice) * parseFloat(quantity) 
    : 0;

  if (loading) {
    return (
      <div className="center-state">
        <RefreshCw className="spinner" size={40} />
        <p>Loading market securities...</p>
      </div>
    );
  }

  return (
    <div className="trade-page animate-fade-in">
      <header className="page-header">
        <div>
          <h1>Stock Market Board</h1>
          <p className="subtitle">Execute buy and sell trades using real-time mock asset data</p>
        </div>
        <div className="wallet-pill glass-card">
          <span>Available Cash:</span>
          <strong>{formatCurrency(walletBalance)}</strong>
        </div>
      </header>

      {/* Main trading columns */}
      <div className="trade-grid">
        {/* Left Side: Stocks List */}
        <div className="glass-card stocks-list-card">
          <div className="search-bar">
            <Search size={18} className="search-icon" />
            <input 
              type="text" 
              placeholder="Search tickers (e.g. AAPL, MSFT)" 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          <div className="stocks-list">
            {filteredStocks.length === 0 ? (
              <p className="empty-search">No matching symbols found.</p>
            ) : (
              filteredStocks.map((stock) => {
                const isSelected = selectedStock && selectedStock.symbol === stock.symbol;
                return (
                  <div 
                    key={stock.id} 
                    className={`stock-item ${isSelected ? 'selected' : ''}`}
                    onClick={() => handleSelectStock(stock)}
                  >
                    <div className="stock-info">
                      <span className="stock-symbol">{stock.symbol}</span>
                      <span className="stock-name">{stock.name}</span>
                    </div>
                    <div className="stock-pricing">
                      <span className="stock-price">{formatCurrency(stock.currentPrice)}</span>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* Right Side: Order Desk */}
        <div className="order-desk">
          {selectedStock ? (
            <div className="glass-card order-card">
              {/* Selected Stock Overview */}
              <div className="selected-stock-header">
                <div>
                  <h2>{selectedStock.symbol}</h2>
                  <p>{selectedStock.name}</p>
                </div>
                <div className="selected-stock-price">
                  <span>Current Price</span>
                  <h3>{formatCurrency(selectedStock.currentPrice)}</h3>
                </div>
              </div>

              {/* Chart of Selected Stock */}
              <div className="stock-chart-section">
                <h4>7-Day Price History</h4>
                {chartLoading ? (
                  <div className="chart-placeholder">
                    <RefreshCw className="spinner" size={24} />
                  </div>
                ) : (
                  <PortfolioValueChart data={stockHistory} />
                )}
              </div>

              {/* Trade status notifications */}
              {successMsg && (
                <div className="alert alert-success animate-fade-in">
                  <CheckCircle size={18} />
                  <span>{successMsg}</span>
                </div>
              )}
              {errorMsg && (
                <div className="alert alert-danger animate-fade-in">
                  <AlertTriangle size={18} />
                  <span>{errorMsg}</span>
                </div>
              )}
              {eduTip && (
                <div className="alert alert-info edu-tip-alert animate-fade-in">
                  <Lightbulb className="edu-icon" size={28} />
                  <div className="edu-text-container">
                    <strong>{eduTip.title}</strong>
                    <p>{eduTip.text}</p>
                  </div>
                </div>
              )}

              {/* Action Form */}
              <div className="trade-form">
                <div className="form-group">
                  <label htmlFor="quantity">Quantity (Shares)</label>
                  <input
                    id="quantity"
                    type="number"
                    step="0.0001"
                    min="0.0001"
                    placeholder="0.00"
                    value={quantity}
                    onChange={(e) => {
                      setQuantity(e.target.value);
                      setErrorMsg('');
                      setSuccessMsg('');
                      setEduTip(null);
                    }}
                    disabled={orderLoading}
                  />
                </div>

                <div className="trade-calculations">
                  <div className="calc-row">
                    <span>Subtotal:</span>
                    <span>{formatCurrency(calculatedTotal)}</span>
                  </div>
                  <div className="calc-row total">
                    <span>Estimated Total:</span>
                    <strong>{formatCurrency(calculatedTotal)}</strong>
                  </div>
                </div>

                <div className="trade-buttons">
                  <button 
                    className="btn btn-success" 
                    onClick={() => handleTrade('BUY')}
                    disabled={orderLoading || calculatedTotal === 0}
                  >
                    {orderLoading ? <RefreshCw className="spinner" size={16} /> : 'BUY'}
                  </button>
                  <button 
                    className="btn btn-danger" 
                    onClick={() => handleTrade('SELL')}
                    disabled={orderLoading || calculatedTotal === 0}
                  >
                    {orderLoading ? <RefreshCw className="spinner" size={16} /> : 'SELL'}
                  </button>
                </div>
              </div>
            </div>
          ) : (
            <div className="glass-card no-selection">
              <p>Select a stock from the market panel on the left to start trading.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Trade;
