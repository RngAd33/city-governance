# API 接口参考

## 1. 数据采集服务 (city-collector) - 端口 8081

### GET /api/collector/status
服务状态检查

**响应**
```json
{
  "status": "UP",
  "service": "city-collector",
  "timestamp": "2026-10-02T10:00:00+08:00"
}
```

### POST /api/collector/trigger
手动触发一轮数据采集

### GET /api/collector/events/recent
获取最近事件

**参数**: limit (默认20)

## 2. 交通优化服务 (city-traffic) - 端口 8082

### GET /api/traffic/status
服务状态检查

### POST /api/traffic/optimize
信号配时优化

**请求体**
```json
{
  "intersectionId": "INT-001",
  "phases": [
    {"direction": "NS", "greenTime": 30},
    {"direction": "EW", "greenTime": 25}
  ],
  "flowData": [
    {"direction": "NS", "flow": 800},
    {"direction": "EW", "flow": 600}
  ]
}
```

### POST /api/traffic/simulation/compare
优化前后对比仿真

## 3. 能耗分析服务 (city-energy) - 端口 8083

### GET /api/energy/buildings
获取建筑列表

### GET /api/energy/buildings/{id}/analysis
获取建筑能耗分析

### POST /api/energy/predict
建筑负荷预测

## 4. 环境监测服务 (city-environment) - 端口 8084

### GET /api/environment/records
获取环境记录

**参数**: city, metric, startDate, endDate

### GET /api/environment/aqi
获取当前AQI

### GET /api/environment/alerts
获取活动告警

## 5. 水务风险服务 (city-water) - 端口 8085

### GET /api/water/stations
获取监测站列表

### GET /api/water/stations/{id}/risk
获取风险评估

### POST /api/water/simulate
运行洪涝模拟

## 6. 政务AI服务 (city-govai) - 端口 8086

### POST /api/govai/query
自然语言查询

**请求体**
```json
{
  "query": "当前城市运行状态如何？"
}
```

### POST /api/govai/alert/explain
告警解释

### POST /api/govai/report/generate
报告生成

### POST /api/govai/plan/retrieve
预案检索
