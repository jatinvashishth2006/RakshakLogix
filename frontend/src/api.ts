import axios from 'axios';
export const api = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api' });
api.interceptors.request.use(config => { const token = localStorage.getItem('rakshak-token'); if (token) config.headers.Authorization = `Bearer ${token}`; return config; });
api.interceptors.response.use(r => r, error => { if (error.response?.status === 401) localStorage.removeItem('rakshak-token'); return Promise.reject(error); });
