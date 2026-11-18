import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor: Inject Bearer token & Correlation ID
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('finguard_token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }

    const correlationId = 'ui-' + Math.random().toString(36).substring(2, 10);
    config.headers['X-Correlation-Id'] = correlationId;

    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: Global error handling
api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('finguard_token');
      localStorage.removeItem('finguard_user');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error.response?.data || error.message);
  }
);

export default api;
