import axios from 'axios';

// Same-origin requests use the Vite proxy in development and a reverse proxy
// in production. Deployments may override the API origin with VITE_API_URL.
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

export const AUTH_EXPIRED_EVENT = 'simulator:auth-expired';
const AUTH_KEYS = ['token', 'userId', 'name', 'email', 'username'];

export function clearSession() {
  AUTH_KEYS.forEach((key) => localStorage.removeItem(key));
}

export function saveSession(data) {
  if (data.token) localStorage.setItem('token', data.token);
  localStorage.setItem('userId', String(data.userId));
  localStorage.setItem('name', data.name);
  localStorage.setItem('email', data.email);
  localStorage.removeItem('username');
}

export function errorMessage(error, fallback = 'The request failed. Please try again.') {
  if (error.response?.status === 401) return 'Your session has expired. Please sign in again.';
  if (error.response?.data?.message) return error.response.data.message;
  if (error.code === 'ECONNABORTED') return 'The server took too long to respond. Please try again.';
  if (!error.response) return 'Unable to reach the server. Check your connection and try again.';
  return fallback;
}

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

apiClient.interceptors.response.use((response) => response, (error) => {
  const isSignIn = /\/auth\/(login|register)$/.test(error.config?.url || '');
  if (error.response?.status === 401 && !isSignIn) {
    clearSession();
    window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT));
  }
  return Promise.reject(error);
});

const unwrap = (response) => response.data;

export const api = {
  login: (email, password) => apiClient.post('/api/auth/login', { email, password }).then(unwrap),
  register: (name, email, password) => apiClient.post('/api/auth/register', { name, email, password }).then(unwrap),
  getMe: (config) => apiClient.get('/api/auth/me', config).then(unwrap),
  getWallet: (userId, config) => apiClient.get(`/api/wallet/${userId}`, config).then(unwrap),
  deposit: (userId, amount) => apiClient.post(`/api/wallet/${userId}/deposit`, { amount }).then(unwrap),
  getPortfolio: (userId, config) => apiClient.get(`/api/portfolio/${userId}`, config).then(unwrap),
  getTransactions: (userId, page = 0, size = 10, config = {}) =>
    apiClient.get(`/api/transactions/${userId}`, { ...config, params: { page, size } }).then(unwrap),
  getStocks: (config) => apiClient.get('/api/stocks', config).then(unwrap),
  getStockHistory: (stockId, days = 7, config = {}) =>
    apiClient.get(`/api/stocks/${stockId}/history`, { ...config, params: { days } }).then(unwrap),
  buyStock: (userId, stockId, amount) => apiClient.post('/api/trade/buy', { userId, stockId, amount }).then(unwrap),
  sellStock: (userId, stockId, quantity) => apiClient.post('/api/trade/sell', { userId, stockId, quantity }).then(unwrap),
  getLeaderboard: (config) => apiClient.get('/api/leaderboard', config).then(unwrap),
};

export default api;
