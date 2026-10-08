# 城市环境能源与交通协同优化平台 - 前端项目

## 项目概述

本项目是城市环境能源与交通协同优化平台的前端部分，基于 React 18 + TypeScript + Vite 构建，提供城市运行总览、环境监测、交通信号优化仿真、能源与建筑节能、水务与内涝风险、灾害模拟、基础设施运维、政务AI助手等功能。

## 技术栈

- **框架**: React 18 + TypeScript
- **构建工具**: Vite
- **UI组件库**: Ant Design 5
- **图表**: ECharts
- **地图**: Leaflet + OpenStreetMap
- **状态管理**: Zustand
- **请求层**: Axios + React Query
- **实时通信**: WebSocket / SSE
- **样式**: Tailwind CSS
- **容器化**: Docker + Nginx 静态托管

## 项目结构

```
src/
├── api/              接口封装
├── components/
│   ├── Common/       通用组件
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
```

### 预览生产构建

```bash
npm run preview
```

### Docker部署

```bash
docker build -t city-governance-frontend .
docker run -p 80:80 city-governance-frontend
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

## 核心组件

### 通用组件 (Common)
- Loading - 加载状态
- ErrorAlert - 错误提示
- StatusTag - 状态标签
- MetricCard - 指标卡片
- StatCard - 统计卡片
- ChartContainer - 图表容器
- MapContainer - 地图容器
- DataTable - 数据表格
- TimeRangePicker - 时间范围选择器
- SearchBar - 搜索栏
- EmptyState - 空状态
- ReportPreview - 报告预览

### 布局组件 (Layout)
- AppLayout - 应用布局（侧边栏 + 顶栏 + 内容区）

## 状态管理

使用 Zustand 管理全局状态：
- 城市选择
- 时间范围
- 告警列表
- 交通仿真状态
- AI聊天消息

## API 接口

前端统一通过 `/api` 前缀调用后端接口：
- `GET /api/dashboard/summary` - 仪表盘摘要
- `GET /api/environment/latest` - 最新环境数据
- `GET /api/traffic/segments` - 交通路段
- `POST /api/traffic/simulation/run` - 运行交通仿真
- `GET /api/energy/buildings` - 建筑列表
- `GET /api/water/stations` - 水位站
- `POST /api/ai/chat` - AI对话
- `POST /api/ai/report/generate` - 生成报告

## 许可证

MIT
