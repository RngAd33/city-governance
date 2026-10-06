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

### GET /api/collector/sources
查询环境数据源及运行状态

**响应**
```json
{
  "timestamp": "2026-10-03T19:35:00+08:00",
  "sources": [
    {
      "name": "open-meteo",
      "source": "OPEN_METEO",
      "categories": ["weather"],
      "available": true,
      "fallback": false,
      "lastFetchTime": "2026-10-03T19:35:57+08:00",
      "lastSuccess": true,
      "successCount": 2,
      "failureCount": 0
    }
  ]
}
```

### POST /api/collector/sources/fetch
手动触发环境数据源采集

**参数**: category (weather | airquality | all，默认 all)

**响应**
```json
{
  "success": true,
  "results": [
    {
      "category": "weather",
      "sourceUsed": "open-meteo",
      "eventCount": 4,
      "fallback": false,
      "metrics": ["temperature", "humidity", "wind_speed", "precipitation"],
      "message": "采集成功"
    }
  ]
}
```

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
报告生成（报告中心"生成报告"入口）

**请求体**（兼容 type/reportType、date/period 字段名）
```json
{
  "type": "日报",
  "city": "南京市",
  "date": "今日"
}
```

**参数说明**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| type / reportType | string | 是 | 报告类型：日报/周报/简报/风险提示（或 DAILY/WEEKLY/BRIEF/RISK_ALERT） |
| city | string | 否 | 城市，默认南京市 |
| date / period | string | 否 | 报告周期描述，默认"今日" |

**响应**：`{ id, title, type, typeCode, city, period, author, status, createdAt, content, reportType, report, recordId }`

### GET /api/govai/report/list
报告列表（报告中心）

**参数**: type（可选，日报/周报/简报/风险提示或英文代码），按创建时间倒序

**响应**: `{ total, reports: [{ id, title, type, typeCode, city, period, author, status, createdAt }] }`（摘要，不含正文）

### GET /api/govai/report/{id}
报告详情（含完整 Markdown 正文）

**响应**: 列表摘要字段 + `content`；未找到返回 404

### GET /api/govai/report/{id}/pdf
报告 PDF 导出（报告中心"导出 PDF"入口）：将报告渲染为 PDF 文件返回

**响应**
- `200`: `Content-Type: application/pdf`，二进制流；`Content-Disposition: attachment; filename*=UTF-8''<报告标题>.pdf`
- `404`: 报告不存在
- `500`: `{ "error": "..." }`（如部署环境缺少中文字体）

**说明**：Markdown 正文简化排版（标题/列表/表格/分隔线）；中文字体按级探测（classpath:/fonts/report-cn.ttf → 系统字体），容器部署时可将中文 TTF 字体放入 `/app/fonts/report-cn.ttf`

### POST /api/govai/plan/retrieve
预案检索

## 7. 基础设施运维服务 (city-infra) - 端口 8087

### GET /api/infra/status
设施实时状态总览（南京5类重点设施最新评分与告警级别）

**响应**: `{ facilities: [{ facility, facilityName, statusScore, statusTime, alertLevel, alertTime }], summary: { facilityCount, avgScore, normalCount, attentionCount, warningCount, dangerCount, lastUpdateTime } }`

### GET /api/infra/history
设施状态评分趋势（基础设施运维页面趋势图）

**参数**: facility（默认 bridge，可选 bridge/tunnel/road/pipeline/power_grid）、hours（默认 24，范围 1~2160）

**响应**: `FacilityStatusRecord` 数组（按时间升序）：`[{ id, facility, metricType, value, unit, timestamp, city, source }]`

**降采样说明**: 窗口 ≤48 小时返回原始点（数据约 50 秒/条）；窗口 >48 小时按自然小时聚合，每点为该小时评分均值（`value` 保留1位小数，`timestamp` 为小时整点，其余字段取组内最后一条）——响应结构与原始点一致，前端无需改动

### GET /api/infra/work-orders
南京地区基础设施运维工单列表（按创建时间倒序）
