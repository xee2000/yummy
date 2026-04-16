import axios, {AxiosInstance, InternalAxiosRequestConfig, AxiosResponse} from 'axios';

const BASE_URL = process.env.API_BASE_URL ?? 'https://api.yummy.app';

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
