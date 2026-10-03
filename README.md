# 城市环境能源与交通协同优化平台

面向政务AI融合的城市治理平台原型，融合环境监测、能源调度、建筑节能、交通流优化、水务管理、灾害模拟、基础设施运维等场景。

## 技术架构

```
公开API / 政务开放平台 / 公开数据集
        ↓
Java 数据采集与清洗服务 (city-collector)
        ↓
RabbitMQ 城市事件总线 (8个队列)
        ↓
┌──────────────────────────────────────────┐
│  交通优化服务  │  能耗分析服务            │
│  环境监测服务  │  水务风险服务            │
│  政务AI决策服务（本地LLM + RAG）         │
└──────────────────────────────────────────┘
        ↓
MySQL + Redis + 向量库
        ↓
可视化大屏 / 政务驾驶舱 / 自然语言问答
```

## 服务模块

| 服务 | 端口 | 功能 |
|------|------|------|
| city-collector | 8081 | 多源数据采集与模拟 |
| city-traffic | 8082 | 交通信号配时优化（遗传算法） |
| city-energy | 8083 | 能耗分析与建筑节能 |
| city-environment | 8084 | 环境监测与告警 |
| city-water | 8085 | 水务风险与灾害模拟 |
| city-govai | 8086 | 政务AI决策（LLM+RAG） |

## 快速启动

### Docker Compose 部署

```bash
# 编译所有模块
mvn clean package -DskipTests

# 启动全部服务
docker-compose up -d

# 查看日志
docker-compose logs -f
```

### 本地开发

```bash
# 1. 启动基础设施
docker-compose up -d rabbitmq mysql redis

# 2. 按顺序启动服务
cd city-collector && mvn spring-boot:run
cd city-traffic && mvn spring-boot:run
cd city-energy && mvn spring-boot:run
cd city-environment && mvn spring-boot:run
cd city-water && mvn spring-boot:run
cd city-govai && mvn spring-boot:run
```

## RabbitMQ 事件总线

| 队列 | Routing Key | 用途 |
|------|-------------|------|
| env.weather.queue | env.weather.city | 气象数据 |
| env.airquality.queue | env.airquality.city | 空气质量数据 |
| traffic.flow.queue | traffic.flow.segment | 交通流数据 |
| traffic.signal.queue | traffic.signal.optimization | 信号配时优化结果 |
| energy.building.queue | energy.building.consumption | 建筑能耗数据 |
| water.monitor.queue | water.level.station | 水位/渍水数据 |
| disaster.sim.queue | disaster.flood.scenario | 灾害模拟事件 |
| govai.decision.queue | govai.decision.suggest | 政务AI决策建议 |

## API 接口

### 数据采集服务 (city-collector)
- `GET /api/collector/status` - 服务状态
- `POST /api/collector/trigger` - 手动触发数据采集
- `GET /api/collector/events/recent` - 最近事件

### 交通优化服务 (city-traffic)
- `GET /api/traffic/status` - 服务状态
- `POST /api/traffic/optimize` - 信号配时优化
- `GET /api/traffic/simulation/runs` - 仿真运行记录
- `POST /api/traffic/simulation/compare` - 优化前后对比

### 能耗分析服务 (city-energy)
- `GET /api/energy/status` - 服务状态
- `GET /api/energy/buildings` - 建筑列表
- `GET /api/energy/buildings/{id}/analysis` - 建筑能耗分析
- `GET /api/energy/high-consumption` - 高能耗建筑
- `POST /api/energy/predict` - 负荷预测
- `GET /api/energy/saving-potential/{buildingId}` - 节能潜力

### 环境监测服务 (city-environment)
- `GET /api/environment/status` - 服务状态
- `GET /api/environment/records` - 环境记录
- `GET /api/environment/aqi` - 当前AQI
- `GET /api/environment/alerts` - 活动告警
- `POST /api/environment/alerts/{id}/acknowledge` - 确认告警
- `GET /api/environment/trends` - 趋势分析

### 水务风险服务 (city-water)
- `GET /api/water/status` - 服务状态
- `GET /api/water/stations` - 监测站列表
- `GET /api/water/stations/{id}/level` - 当前水位
- `GET /api/water/stations/{id}/risk` - 风险评估
- `POST /api/water/simulate` - 洪涝模拟
- `GET /api/water/scenarios` - 灾害场景
- `GET /api/water/drainage/analysis` - 排水分析

### 政务AI决策服务 (city-govai)
- `GET /api/govai/status` - 服务状态
- `POST /api/govai/query` - 自然语言查询
- `POST /api/govai/alert/explain` - 告警解释
- `POST /api/govai/report/generate` - 报告生成
- `POST /api/govai/plan/retrieve` - 预案检索
- `GET /api/govai/knowledge/search` - 知识库搜索

## 核心算法

### 交通信号配时优化（遗传算法）
- 种群规模：50
- 迭代次数：100
- 交叉概率：0.8
- 变异概率：0.1
- 适应度函数：最小化总等待时间
- 约束条件：最小绿灯10s，最大周期180s，行人最小15s

### 洪涝模拟
- 水量平衡模型：水位变化 = 降雨 × 径流系数 - 排水速率
- 风险等级：LOW (<60%), MEDIUM (60-80%), HIGH (80-95%), CRITICAL (>95%)

## 技术栈

- Java 21 + Spring Boot 3.2.5
- RabbitMQ 3.13
- MySQL 8.0
- Redis 7
- Docker Compose
- Maven 多模块

## 许可证

MIT
