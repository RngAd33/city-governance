# 前端架构设计

## 1. 架构风格

- **组件化架构**：基于 React 函数组件 + Hooks
- **状态管理**：Zustand 轻量级全局状态
- **路由**：React Router 6 声明式路由
- **样式方案**：Tailwind CSS + CSS Modules
- **类型系统**：TypeScript 严格模式

## 2. 技术选型

| 层次 | 技术 | 说明 |
|------|------|------|
| 视图层 | React 18 | 函数组件 + Hooks |
| 状态层 | Zustand | 轻量级状态管理 |
| 路由层 | React Router 6 | 声明式路由 |
| UI层 | Ant Design 5 | 企业级组件库 |
| 图表层 | ECharts | 数据可视化 |
| 地图层 | Leaflet | 开源地图 |
| 构建层 | Vite | 快速构建 |
| 样式层 | Tailwind CSS | 原子化CSS |

## 3. 目录结构

```
src/
├── api/              接口封装
├── components/       通用组件
├── pages/            页面组件
├── stores/           状态管理
├── hooks/            自定义Hook
├── utils/            工具函数
├── types/            TypeScript类型
├── assets/           静态资源
└── router/           路由配置
```

## 4. 状态管理

### 4.1 全局状态（Zustand）

```typescript
interface AppState {
  selectedCity: string;
  setSelectedCity: (city: string) => void;
  selectedTimeRange: [string, string];
  setSelectedTimeRange: (range: [string, string]) => void;
  alarms: AlarmItem[];
  setAlarms: (alarms: AlarmItem[]) => void;
  traffic: any;
  setIntersection: (intersection: any) => void;
  aiChatMessages: ChatMessage[];
  addChatMessage: (message: ChatMessage) => void;
  simulationStatus: 'idle' | 'running' | 'completed' | 'failed';
  setSimulationStatus: (status: string) => void;
}
```

### 4.2 局部状态（useState）

- 组件内部状态
- 表单状态
- UI交互状态

## 5. 路由设计

```typescript
const router = createBrowserRouter([
  { path: '/', element: <CityOverviewDashboard /> },
  { path: '/monitor/environment', element: <EnvironmentMonitor /> },
  { path: '/monitor/energy', element: <EnergyMonitor /> },
  { path: '/traffic/simulation', element: <TrafficSimulation /> },
  { path: '/water/risk', element: <WaterRisk /> },
  { path: '/disaster/simulation', element: <DisasterSimulation /> },
  { path: '/infra/ops', element: <InfrastructureOps /> },
  { path: '/ai-assistant', element: <GovAIAssistant /> },
  { path: '/reports', element: <ReportCenter /> },
  { path: '/settings', element: <SystemSettings /> },
]);
```

## 6. API 层设计

### 6.1 Axios 实例

```typescript
const axiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
});
```

### 6.2 接口封装

- `getDashboardSummary()` - 获取仪表盘摘要
- `getEnvironmentLatest(city)` - 获取最新环境数据
- `getEnvironmentHistory(city, metric, hours)` - 获取历史环境数据
- `getTrafficSegments()` - 获取交通路段
- `runTrafficSimulation(params)` - 运行交通仿真
- `getSimulationResult(scenarioId)` - 获取仿真结果
- `getEnergyBuildings()` - 获取建筑列表
- `getWaterStations()` - 获取水位站
- `getDisasterScenarios()` - 获取灾害场景
- `getAlarms(page, size)` - 获取告警列表
- `chatWithAI(query)` - AI对话
- `generateReport(type, city)` - 生成报告

## 7. 组件设计原则

1. **单一职责**：每个组件只做一件事
2. **可复用性**：通用组件抽取
3. **可测试性**：组件可独立测试
4. **可维护性**：代码清晰易懂

## 8. 性能优化

- **代码分割**：React.lazy + Suspense
- **按需加载**：Ant Design 按需引入
- **缓存策略**：React Query 数据缓存
- **虚拟列表**：大数据列表优化
