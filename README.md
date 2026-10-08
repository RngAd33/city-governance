# 城市环境能源与交通协同优化平台 - 前端

基于 React 18 + TypeScript + Vite 的政务驾驶舱前端，提供城市运行总览、环境监测、交通信号优化仿真、能源与建筑节能、水务与内涝风险、灾害模拟、基础设施运维、政务AI助手等功能。

## 技术栈

- **框架**: React 18 + TypeScript
- **构建工具**: Vite
- **UI组件库**: Ant Design 5
- **图表**: ECharts
- **地图**: Leaflet + OpenStreetMap
- **状态管理**: Zustand
- **请求层**: Axios + React Query
- **实时通信**: WebSocket / SSE
- **表单**: React Hook Form + Zod
- **样式**: CSS Modules / Tailwind CSS
- **容器化**: Docker + Nginx 静态托管

## 快速开始

### 安装依赖

```bash
npm install
```

### 开发模式

```bash
npm run dev
```

### 生产构建

```bash
npm run build
npm run preview
```

### Docker部署

```bash
docker build -t city-governance-frontend .
docker run -p 80:80 city-governance-frontend
```

## 项目结构

```
src/
├── api/              接口封装
├── components/       通用组件
│   ├── Common/       基础通用组件
│   └── Layout/       布局组件
├── pages/            页面组件
│   ├── Dashboard/    城市运行总览
│   ├── Environment/  环境监测
│   ├── Energy/       能源与建筑节能
│   ├── Traffic/      交通信号优化仿真
│   ├── Water/        水务与内涝风险
│   ├── Disaster/     灾害模拟
│   ├── Infrastructure/ 基础设施运维
│   ├── AIAssistant/  政务AI助手
│   ├── Reports/      报告中心
│   └── Settings/     系统配置
├── stores/           状态管理
├── hooks/            自定义Hook
├── utils/            工具函数
├── types/            TypeScript类型
├── assets/           静态资源
├── router/           路由配置
├── main.tsx          应用入口
└── index.css         全局样式
```

## 页面路由

| 路由 | 页面 |
|------|------|
| / | 城市运行总览 |
| /monitor/environment | 环境监测 |
| /monitor/energy | 能源与建筑节能 |
| /traffic/simulation | 交通信号优化仿真 |
| /water/risk | 水务与内涝风险 |
| /disaster/simulation | 灾害模拟 |
| /infra/ops | 基础设施运维 |
| /ai-assistant | 政务AI助手 |
| /reports | 报告中心 |
| /settings | 系统配置 |

## 许可证

MIT
