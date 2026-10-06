# 城市环境能源与交通协同优化平台

面向政务AI融合的城市治理平台原型，融合环境监测、能源调度、建筑节能、交通流优化、水务管理、灾害模拟、基础设施运维等场景。

## 技术架构

```
公开API / 政务开放平台 / 公开数据集
        ↓
Java 数据采集与清洗服务 (city-collector)
        ↓
RabbitMQ 城市事件总线 (12个队列)
        ↓
┌──────────────────────────────────────────┐
│  交通优化服务  │  能耗分析服务            │
│  环境监测服务  │  水务监测服务            │
│  灾害模拟服务  │  基础设施运维服务        │
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
| city-water | 8085 | 水务监测与排水分析 |
| city-disaster | 8088 | 灾害风险评估与模拟推演 |
| city-govai | 8086 | 政务AI决策（LLM+RAG） |
| city-infra | 8087 | 基础设施运维监控 |

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
cd city-disaster && mvn spring-boot:run
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
| energy.grid.queue | energy.grid.indicator | 电网指标数据 |
| water.monitor.queue | water.level.station | 水位/渍水数据 |
| water.flood.queue | water.flood.forecast | 长江径流量预报（GloFAS） |
| disaster.water.queue | water.level.station | 灾害触发（水位事件独立副本） |
| disaster.sim.queue | disaster.flood.scenario | 灾害模拟事件 |
| infra.monitor.queue | infra.facility.status | 基础设施状态 |
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

### 水务监测服务 (city-water)
- `GET /api/water/status` - 服务状态
- `GET /api/water/stations` - 监测站列表
- `GET /api/water/stations/{id}/level` - 当前水位
- `GET /api/water/drainage/analysis` - 排水分析

### 灾害模拟服务 (city-disaster)
- `GET /api/disaster/current` - 当前灾害态势
- `POST /api/disaster/simulate` - 灾害情景模拟
- `GET /api/disaster/scenarios` - 历史灾害场景
- `GET /api/disaster/stations/{id}/risk` - 站点风险评估

### 政务AI决策服务 (city-govai)
- `GET /api/govai/status` - 服务状态
- `POST /api/govai/query` - 自然语言查询
- `POST /api/govai/alert/explain` - 告警解释
- `POST /api/govai/report/generate` - 报告生成
- `GET /api/govai/report/list` - 报告列表
- `GET /api/govai/report/{id}` - 报告详情
- `GET /api/govai/report/{id}/pdf` - 报告 PDF 导出
- `POST /api/govai/plan/retrieve` - 预案检索
- `GET /api/govai/knowledge/search` - 知识库搜索

### 基础设施运维服务 (city-infra)
- `GET /api/infra/status` - 设施实时状态总览
- `GET /api/infra/history` - 设施状态评分趋势（>48h 窗口按小时降采样）
- `GET /api/infra/work-orders` - 运维工单列表

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
