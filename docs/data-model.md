# 数据模型设计

## 1. 统一事件模型 (CityEvent)

```json
{
  "eventId": "UUID",
  "domain": "traffic | environment | energy | water | disaster | infra",
  "city": "南京",
  "location": {
    "lat": 32.06,
    "lng": 118.79,
    "address": "南京市鼓楼区"
  },
  "metric": "pm25 | flow | speed | water_level | electricity",
  "value": 45.2,
  "unit": "μg/m³",
  "ts": "2026-10-02T10:00:00+08:00",
  "source": "openaq | open-meteo | dataset | api | simulator"
}
```

## 2. 交通领域模型

### 2.1 SignalPhase (信号相位)
| 字段 | 类型 | 说明 |
|------|------|------|
| phaseId | String | 相位ID |
| direction | String | 方向 (NS/EW) |
| greenTime | int | 绿灯时间(秒) |
| yellowTime | int | 黄灯时间(秒) |
| redTime | int | 红灯时间(秒) |

### 2.2 Intersection (路口)
| 字段 | 类型 | 说明 |
|------|------|------|
| intersectionId | String | 路口ID |
| name | String | 路口名称 |
| lat | Double | 纬度 |
| lng | Double | 经度 |
| phases | List<SignalPhase> | 相位列表 |

### 2.3 TrafficFlowData (交通流数据)
| 字段 | 类型 | 说明 |
|------|------|------|
| segmentId | String | 路段ID |
| intersectionId | String | 路口ID |
| direction | String | 方向 |
| flow | Double | 流量(辆/小时) |
| avgSpeed | Double | 平均车速(km/h) |
| queueLength | Double | 排队长度(m) |

## 3. 能源领域模型

### 3.1 Building (建筑)
| 字段 | 类型 | 说明 |
|------|------|------|
| buildingId | String | 建筑ID |
| name | String | 建筑名称 |
| type | String | 类型(OFFICE/SCHOOL/HOSPITAL/RESIDENTIAL/COMMERCIAL) |
| area | Double | 面积(m²) |
| floors | int | 楼层数 |
| buildYear | int | 建造年份 |
| avgDailyConsumption | Double | 日均能耗(kWh) |
| carbonFactor | Double | 碳排放因子(kgCO2/kWh) |

### 3.2 EnergyConsumptionRecord (能耗记录)
| 字段 | 类型 | 说明 |
|------|------|------|
| buildingId | String | 建筑ID |
| timestamp | DateTime | 时间戳 |
| electricityKwh | Double | 电耗(kWh) |
| heatingKwh | Double | 热耗(kWh) |
| coolingKwh | Double | 冷耗(kWh) |
| totalKwh | Double | 总能耗(kWh) |
| carbonEmission | Double | 碳排放(kg) |

## 4. 环境领域模型

### 4.1 EnvironmentRecord (环境记录)
| 字段 | 类型 | 说明 |
|------|------------|
| recordId | String | 记录ID |
| city | String | 城市 |
| stationName | String | 站点名称 |
| metric | String | 指标 |
| value | Double | 数值 |
| unit | String | 单位 |
| level | String | 等级(NORMAL/WARNING/DANGER) |

### 4.2 AlertRule (告警规则)
| 字段 | 类型 | 说明 |
|------|------|------|
| ruleId | String | 规则ID |
| name | String | 规则名称 |
| metric | String | 监控指标 |
| operator | String | 操作符(GT/LT/GTE/LTE) |
| threshold | Double | 阈值 |
| severity | String | 严重级别(INFO/WARNING/CRITICAL) |

## 5. 水务领域模型

### 5.1 WaterStation (水位站)
| 字段 | 类型 | 说明 |
|------|------|------|
| stationId | String | 站点ID |
| name | String | 站点名称 |
| type | String | 类型(RIVER/LAKE/RESERVOIR/DRAINAGE) |
| warningLevel | Double | 警戒水位(m) |
| dangerLevel | Double | 危险水位(m) |
| currentLevel | Double | 当前水位(m) |

### 5.2 FloodScenario (洪涝场景)
| 字段 | 类型 | 说明 |
|------|------|------|
| scenarioId | String | 场景ID |
| stationId | String | 站点ID |
| rainfallMm | Double | 降雨量(mm) |
| durationHours | int | 持续时长(小时) |
| riskLevel | String | 风险等级(LOW/MEDIUM/HIGH/CRITICAL) |
| affectedArea | Double | 影响面积(km²) |
| affectedPopulation | int | 影响人口 |
