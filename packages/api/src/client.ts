import axios, {AxiosInstance, InternalAxiosRequestConfig, AxiosResponse} from 'axios';

const BASE_URL = (typeof import.meta !== 'undefined' && import.meta.env?.VITE_API_BASE_URL) || 'http://localhost:1000';

export const apiClient: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  return config;
});

apiClient.interceptors.response.use(
  (response: AxiosResponse) => response.data,
  error => {
    return Promise.reject(error);
  },
);
