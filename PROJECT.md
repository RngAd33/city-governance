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

## 核心功能

### 1. 城市运行总览 (Dashboard)
- 4个KPI卡片：AQI、交通拥堵指数、建筑能耗、水位风险
- 城市地图：多图层叠加（站点、路段、水位站）
- 实时告警面板
- 环境趋势图表
- 交通流量图表

### 2. 环境监测 (Environment)
- 指标选择器：PM2.5、PM10、NO₂、O₃、SO₂、CO
- 趋势图表：24h/7d/30d时间范围
- 站点地图：颜色编码站点状态
- 站点数据表格：排序、分页、异常高亮

### 3. 能源与建筑节能 (Energy)
- 建筑类型选择器
- 能耗趋势图表：日/周/月/年切换
- 建筑能耗排行
- 高能耗建筑列表
- 节能建议卡片
- 建筑详情模态框

### 4. 交通信号优化仿真 (Traffic)
- 路口选择器：搜索、区域筛选
- 优化策略选择：遗传算法、强化学习、规则优化
- 信号相位可视化：圆形相位图
- 优化前后对比表格
- 仿真地图：拥堵着色、动态流粒子

### 5. 水务与内涝风险 (Water)
- 水位站选择器
- 水位趋势图表：警戒/危险水位参考线
- 风险地图：淹没区域、排水网络
- 风险评估卡片
- 内涝场景播放器

### 6. 灾害模拟 (Disaster)
- 场景选择器：洪涝、台风、暴雨
- 仿真参数控制：降雨量、持续时间
- 影响地图：淹没扩散动画
- 影响评估：面积、人口、道路
- 场景回放控制

### 7. 基础设施运维 (Infrastructure)
- 设施状态概览卡片
- 设施列表：排序、筛选
- 工单列表：进度条、优先级
- 部门协同流程

### 8. 政务AI助手 (AIAssistant)
- 聊天面板：消息列表、输入框
- 快捷操作：生成日报、查看告警、检索预案
- 上下文面板：相关数据、依据来源
- 报告预览

### 9. 报告中心 (Reports)
- 报告类型选择器
- 报告列表：预览、导出
- 报告生成表单

### 10. 系统配置 (Settings)
- 城市选择
- 时间范围默认值
- 告警阈值配置
- 数据刷新间隔
- 主题切换

## 部署方式

### Docker部署

```bash
# 构建镜像
docker build -t city-governance-frontend .

# 运行容器
docker run -p 80:80 city-governance-frontend
```

### Docker Compose部署

```bash
# 启动全部服务（包括后端、RabbitMQ、数据库）
docker-compose up -d

# 查看日志
docker-compose logs -f
```

### 本地开发

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build
```

## 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| VITE_API_BASE_URL | 后端API地址 | /api |
| VITE_WS_URL | WebSocket地址 | ws://localhost:8080/ws |

## 许可证

MIT
