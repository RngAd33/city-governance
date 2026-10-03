# 架构设计文档

## 1. 系统概述

城市环境能源与交通协同优化平台是一个面向政务AI融合的城市治理原型系统，采用微服务架构，通过RabbitMQ事件总线实现多源数据接入、交通信号优化、能耗分析、环境监测、水务风险模拟和政务AI决策。

## 2. 架构风格

- **微服务架构**：6个独立服务，各司其职
- **事件驱动**：RabbitMQ主题交换机实现服务间异步通信
- **分层设计**：Controller → Service → Repository
- **领域驱动**：按城市治理领域划分服务边界

## 3. 技术选型

| 层次 | 技术 | 说明 |
|------|------|------|
| 开发语言 | Java 21 | LTS版本，虚拟线程支持 |
| 框架 | Spring Boot 3.2.5 | 快速开发，自动配置 |
| 消息队列 | RabbitMQ 3.13 | 事件总线，主题交换机 |
| 关系数据库 | MySQL 8.0 | 业务数据持久化 |
| 缓存 | Redis 7 | 热点数据缓存 |
| 容器化 | Docker Compose | 一键部署 |
| 构建工具 | Maven 3.8.7 | 多模块项目管理 |

## 4. 事件总线设计

### 4.1 交换机
- **名称**：`city.event.topic`
- **类型**：Topic Exchange
- **持久化**：是

### 4.2 队列与路由键

| 队列 | Routing Key | 生产者 | 消费者 |
|------|-------------|--------|--------|
| env.weather.queue | env.weather.city | collector | environment |
| env.airquality.queue | env.airquality.city | collector | environment |
| traffic.flow.queue | traffic.flow.segment | collector | traffic |
| traffic.signal.queue | traffic.signal.optimization | traffic | govai |
| energy.building.queue | energy.building.consumption | collector | energy |
| water.monitor.queue | water.level.station | collector | water |
| disaster.sim.queue | disaster.flood.scenario | water | govai |
| govai.decision.queue | govai.decision.suggest | govai | 决策执行 |

### 4.3 事件格式

```json
{
  "eventId": "uuid",
  "domain": "traffic | environment | energy | water | disaster | infra",
  "city": "南京",
  "location": { "lat": 32.06, "lng": 118.79, "address": "..." },
  "metric": "pm25 | flow | speed | water_level | electricity",
  "value": 45.2,
  "unit": "μg/m³",
  "ts": "2026-10-02T10:00:00+08:00",
  "source": "openaq | open-meteo | dataset | api | simulator"
}
```

## 5. 核心算法

### 5.1 交通信号配时优化（遗传算法）

**编码**：每个染色体表示一组信号相位绿灯时间
**适应度函数**：
```
fitness = Σ(flow_i × cycleLength × (1 - greenRatio_i)²) + penalty
```
**约束**：
- 最小绿灯时间 ≥ 10s
- 最大周期 ≤ 180s
- 行人过街 ≥ 15s

**算子**：
- 选择：轮盘赌选择
- 交叉：单点交叉，概率0.8
- 变异：高斯变异，概率0.1

### 5.2 洪涝模拟

**水量平衡模型**：
```
ΔLevel = Rainfall × RunoffCoefficient - DrainageRate
```

**风险等级**：
- LOW: < 60% 容量
- MEDIUM: 60-80%
- HIGH: 80-95%
- CRITICAL: > 95%

### 5.3 建筑能耗分析

**能耗强度**：
```
EnergyIntensity = AnnualConsumption / Area (kWh/m²/year)
```

**负荷预测（度日法）**：
```
Load = BaseLoad + Area × UValue × |T_target - T_outdoor| / 1000
```

## 6. 数据流

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│  数据采集    │────→│  RabbitMQ    │────→│  各服务消费  │
│  collector  │     │  事件总线     │     │  & 处理     │
└─────────────┘     └──────────────┘     └─────────────┘
                                               │
                    ┌──────────────┐           │
                    │  MySQL       │←──────────┘
                    │  Redis       │
                    └──────────────┘
```

## 7. 部署架构

```
Docker Compose
├── rabbitmq (5672, 15672)
├── mysql (3306)
├── redis (6379)
├── collector (8081)
├── traffic (8082)
├── energy (8083)
├── environment (8084)
├── water (8085)
└── govai (8086)
```

## 8. 安全设计

- 所有AI建议标注依据来源
- 优化结果仅在仿真环境验证
- 涉及个人出行数据需脱敏处理
- AI建议不替代人工审批
