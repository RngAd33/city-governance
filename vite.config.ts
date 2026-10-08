import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

// 后端微服务端口映射
// collector：本地 IDE 直接运行在 8081；docker-compose 将容器 8081 映射为宿主机 18081
const BACKEND = {
  collector: 'http://localhost:8081',
  traffic: 'http://localhost:8082',
  energy: 'http://localhost:8083',
  environment: 'http://localhost:8084',
  water: 'http://localhost:8085',
  govai: 'http://localhost:8086',
  infra: 'http://localhost:8087',
  disaster: 'http://localhost:8088',
};

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  // 开发模式预构建重型依赖，避免运行时才发现新依赖而触发页面 reload
  optimizeDeps: {
    include: [
      'react',
      'react-dom',
      'react-router-dom',
      'antd',
      '@ant-design/icons',
      '@tanstack/react-query',
      'axios',
      'zustand',
      'date-fns',
      'echarts/core',
      'echarts/charts',
      'echarts/components',
      'echarts/renderers',
      'echarts/features',
      'leaflet',
    ],
  },
  server: {
    port: 3000,
    proxy: {
      // ---- environment 服务 (8084) ----
      '/api/environment/latest': {
        target: BACKEND.environment,
        changeOrigin: true,
        rewrite: (p) => p.replace('/api/environment/latest', '/api/environment/records'),
      },
      '/api/environment/history': {
        target: BACKEND.environment,
        changeOrigin: true,
        rewrite: (p) => p.replace('/api/environment/history', '/api/environment/records'),
      },
      '/api/alarms': {
        target: BACKEND.environment,
        changeOrigin: true,
        rewrite: (p) => p.replace('/api/alarms', '/api/environment/alerts'),
      },
      '/api/environment': {
        target: BACKEND.environment,
        changeOrigin: true,
      },
      // ---- traffic 服务 (8082) ----
      // 后端已提供 /segments（实时流量+相位）与 /simulation/run（优化+前后仿真）真实接口，
      // 此前为绕开缺失接口的 rewrite 映射已移除，前端路径与后端保持一致
      '/api/traffic': {
        target: BACKEND.traffic,
        changeOrigin: true,
      },
      // ---- energy 服务 (8083) ----
      '/api/energy': {
        target: BACKEND.energy,
        changeOrigin: true,
      },
      // ---- water 服务 (8085) ----
      '/api/water': {
        target: BACKEND.water,
        changeOrigin: true,
      },
      // ---- disaster 服务 (8088) ----
      // city-disaster 已独立部署（/current 态势、/simulate 情景推演、/scenarios 历史场景），
      // 旧指向 water 的 /api/water/disaster rewrite 映射已移除，前端路径与后端保持一致
      '/api/disaster': {
        target: BACKEND.disaster,
        changeOrigin: true,
      },
      // ---- govai 服务 (8086) ----
      // 会话持久化端点 /api/ai/sessions* 已被下方 /api/ai 泛规则覆盖，无需单独配置
      '/api/ai/chat': {
        target: BACKEND.govai,
        changeOrigin: true,
        rewrite: (p) => p.replace('/api/ai/chat', '/api/govai/chat'),
      },
      '/api/ai/explain-alert': {
        target: BACKEND.govai,
        changeOrigin: true,
        rewrite: (p) => p.replace('/api/ai/explain-alert', '/api/govai/alert/explain'),
      },
      '/api/ai/retrieve-plan': {
        target: BACKEND.govai,
        changeOrigin: true,
        rewrite: (p) => p.replace('/api/ai/retrieve-plan', '/api/govai/plan/retrieve'),
      },
      '/api/ai': {
        target: BACKEND.govai,
        changeOrigin: true,
        rewrite: (p) => p.replace('/api/ai', '/api/govai'),
      },
      '/api/reports': {
        target: BACKEND.govai,
        changeOrigin: true,
        rewrite: (p) => p.replace('/api/reports', '/api/govai/report'),
      },
      // ---- infra 服务 (8087) ----
      '/api/infra': {
        target: BACKEND.infra,
        changeOrigin: true,
      },
      // ---- collector 服务 (8081) ----
      '/api/dashboard': {
        target: BACKEND.collector,
        changeOrigin: true,
        rewrite: (p) => p.replace('/api/dashboard', '/api/collector'),
      },
      '/api/events': {
        target: BACKEND.collector,
        changeOrigin: true,
        rewrite: (p) => p.replace('/api/events', '/api/collector/events'),
      },
    },
  },
  build: {
    rollupOptions: {
      output: {
        // 手动分包：重型第三方库独立成 chunk，利用浏览器长缓存并行加载
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('echarts') || id.includes('zrender')) return 'vendor-echarts';
            if (id.includes('leaflet')) return 'vendor-leaflet';
            if (
              id.includes('antd') ||
              id.includes('@ant-design') ||
              id.includes('@rc-component') ||
              id.includes('/rc-') ||
              id.includes('dayjs')
            ) {
              return 'vendor-antd';
            }
            // react 生态：react-query / react-router 的底层依赖不含 "react" 路径，
            // 必须与其同 chunk，否则会形成 vendor <-> vendor-react 循环
            if (
              id.includes('react') ||
              id.includes('scheduler') ||
              id.includes('use-sync-external-store') ||
              id.includes('@tanstack/query-core') ||
              id.includes('@remix-run/router')
            ) {
              return 'vendor-react';
            }
            return 'vendor';
          }
        },
      },
    },
  },
});
