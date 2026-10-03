# 数据流设计

## 1. 数据采集流

```
模拟器/外部API → 数据清洗 → 统一事件格式 → RabbitMQ事件总线
```

### 1.0 环境数据源（city-collector）

环境数据支持多数据源接入，优先使用真实API，失败时自动降级模拟器兜底：

| 数据源 | 类型 | 指标 | 说明 |
|--------|------|------|------|
| Open-Meteo | 真实API | temperature/humidity/wind_speed/precipitation | 免费开放，无需密钥 |
| OpenAQ | 真实API | pm25/pm10/no2/o3 | 需配置 OPENAQ_API_KEY |
| simulator | 兜底 | 同上全量 | 真实数据源全部失败时使用 |

数据源开关与采集周期通过 `collector.*` 配置；采集数据经范围校验清洗后，
带数据源标注（source字段）发布到事件总线并持久化。

### 1.1 采集频率
- 气象数据：每10分钟（真实API）
- 空气质量：每30分钟（真实API）
- 模拟器全量数据：每10秒（演示节奏）
- 交通流：每5分钟
- 能耗数据：每小时
- 水位数据：每15分钟

### 1.2 数据清洗
- 异常值过滤
- 单位统一
- 时间戳标准化
- 数据源标注

## 2. 事件处理流

```
RabbitMQ → 消费者 → 业务处理 → 结果存储 → 事件发布
```

### 2.1 交通优化流
```
traffic.flow.queue → TrafficEventConsumer → 遗传算法优化 → 结果存储 → traffic.signal.queue
```

### 2.2 环境监测流
```
env.weather.queue → EnvironmentEventConsumer → 规则引擎 → 告警存储
env.airquality.queue → EnvironmentEventConsumer → AQI计算 → 告警存储
```

### 2.3 能耗分析流
```
energy.building.queue → EnergyEventConsumer → 能耗分析 → 建议存储
```

### 2.4 水务风险流
```
water.monitor.queue → WaterEventConsumer → 风险评估 → 模拟触发 → disaster.sim.queue
```

### 2.5 政务AI流
```
各队列 → DecisionConsumer → AI分析 → 建议生成 → govai.decision.queue
```

## 3. 数据存储流

```
业务数据 → MySQL（关系型数据）
缓存数据 → Redis（热点数据）
时序数据 → InfluxDB/TDengine（历史趋势）
向量数据 → 向量库（知识库检索）
```

## 4. 数据服务流

```
REST API → Service → Repository → Database
                ↓
         Cache (Redis)
```

## 5. 实时推送流

```
数据更新 → WebSocket → 前端大屏
```

## 6. 数据质量保障

- 数据校验规则
- 异常检测机制
- 数据补全策略
- 数据审计日志
