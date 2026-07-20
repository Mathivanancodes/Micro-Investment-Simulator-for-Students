import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Axios Request Interceptor to automatically append JWT Token from localStorage
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export const api = {
  // Authentication & Session
  login: (username, password) => 
    apiClient.post('/api/auth/login', { username, password }).then((res) => res.data),

  register: (username, email, password) => 
    apiClient.post('/api/auth/register', { username, email, password }).then((res) => res.data),

  getMe: () => apiClient.get('/api/auth/me').then((res) => res.data),

  // Portfolio Summary
  getPortfolio: (userId) => apiClient.get(`/api/portfolio/${userId}`).then((res) => res.data),

  // Transaction History
  getTransactions: (userId) => apiClient.get(`/api/transactions/${userId}`).then((res) => res.data),

  // Stocks Operations
  getStocks: () => apiClient.get('/api/stocks').then((res) => res.data),
  
  getStockHistory: (symbol, days = 7) => 
    apiClient.get(`/api/stocks/${symbol}/history`, { params: { days } }).then((res) => res.data),

  // Trade Execution
  buyStock: (userId, symbol, quantity) => 
    apiClient.post('/api/trade/buy', { userId, symbol, quantity }).then((res) => res.data),
    
  sellStock: (userId, symbol, quantity) => 
    apiClient.post('/api/trade/sell', { userId, symbol, quantity }).then((res) => res.data),

  // Leaderboard Standing
  getLeaderboard: () => apiClient.get('/api/leaderboard').then((res) => res.data),
  
  resetLeaderboard: () => apiClient.post('/api/leaderboard/reset').then((res) => res.data),
};

export default api;
