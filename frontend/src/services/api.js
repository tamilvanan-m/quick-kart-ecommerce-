import axios from 'axios';

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api';

const api = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('qk_access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshPromise = null;

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url?.includes('/auth/login') &&
      !originalRequest.url?.includes('/auth/refresh')
    ) {
      const refreshToken = localStorage.getItem('qk_refresh_token');
      if (!refreshToken) {
        return Promise.reject(error);
      }

      originalRequest._retry = true;
      try {
        if (!refreshPromise) {
          refreshPromise = axios
            .post(`${baseURL}/auth/refresh`, { refreshToken })
            .finally(() => {
              refreshPromise = null;
            });
        }
        const { data } = await refreshPromise;
        localStorage.setItem('qk_access_token', data.accessToken);
        localStorage.setItem('qk_refresh_token', data.refreshToken);
        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        localStorage.removeItem('qk_access_token');
        localStorage.removeItem('qk_refresh_token');
        localStorage.removeItem('qk_user');
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);

export const getApiErrorMessage = (err, fallback = 'Something went wrong. Please try again.') => {
  if (err?.response?.data?.validationErrors) {
    const vals = Object.values(err.response.data.validationErrors);
    if (vals.length > 0) return vals[0];
  }
  return err?.response?.data?.message || err?.message || fallback;
};

export default api;
