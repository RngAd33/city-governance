import axios, { AxiosError, type AxiosInstance } from 'axios';
import { message } from 'antd';

declare module 'axios' {
  export interface AxiosRequestConfig<D = any> {
    /** 为 true 时跳过响应拦截器的通用错误提示（调用方自行解析错误体并展示） */
    skipErrorToast?: boolean;
  }
}

const axiosInstance: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor
axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error: AxiosError) => {
    return Promise.reject(error);
  }
);

// Response interceptor
axiosInstance.interceptors.response.use(
  (response) => {
    return response;
  },
  (error: AxiosError) => {
    const skipErrorToast = error.config?.skipErrorToast;
    if (error.response) {
      const { status } = error.response;
      switch (status) {
        case 401:
          message.error('未授权，请重新登录');
          localStorage.removeItem('token');
          window.location.href = '/login';
          break;
        case 403:
          if (!skipErrorToast) message.error('拒绝访问');
          break;
        case 404:
          if (!skipErrorToast) message.error('请求的资源不存在');
          break;
        case 500:
          if (!skipErrorToast) message.error('服务器内部错误');
          break;
        default:
          if (!skipErrorToast) message.error('网络请求失败');
      }
    } else if (error.request) {
      if (!skipErrorToast) message.error('网络连接失败，请检查网络设置');
    } else {
      if (!skipErrorToast) message.error('请求配置错误');
    }
    return Promise.reject(error);
  }
);

export default axiosInstance;
