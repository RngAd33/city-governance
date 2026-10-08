export type Level = 'normal' | 'warning' | 'danger' | 'critical';

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface AlarmItem {
  id: string;
  title: string;
  location: string;
  severity: Level;
  timestamp: string;
  acknowledged: boolean;
  description?: string;
}

export interface EnvironmentRecord {
  id: string;
  recordId: string;
  city: string;
  stationName: string;
  lat: number;
  lng: number;
  metric: string;
  value: number;
  unit: string;
  level: Level;
  ts: string;
}

export interface TrafficFlowData {
  segmentId: string;
  intersectionId: string;
  direction: string;
  flow: number;
  avgSpeed: number;
  queueLength: number;
  congestionLevel?: string;
}

export interface SignalPhase {
  phaseId: string;
  direction: string;
  greenTime: number;
  yellowTime: number;
  redTime: number;
  duration?: number;
  color?: string;
  name?: string;
}

export interface Intersection {
  id: string;
  intersectionId?: string;
  name: string;
  area?: string;
  lat?: number;
  lng?: number;
  congestionLevel?: string;
  currentFlow?: number;
  phases: SignalPhase[];
}

export interface SimulationResult {
  totalWaitTime: number;
  avgQueueLength: number;
  throughput: number;
  avgSpeed: number;
  stopCount: number;
}

export interface OptimizationResult {
  intersectionId: string;
  optimizedPhases: SignalPhase[];
  beforeSimulation: SimulationResult;
  afterSimulation: SimulationResult;
  improvementPercent: number;
}

export interface Building {
  buildingId: string;
  name: string;
  type: string;
  area: number;
  floors: number;
  buildYear: number;
  lat: number;
  lng: number;
  avgDailyConsumption: number;
  /** 碳排放因子（kg/kWh，city-energy 返回） */
  carbonFactor?: number;
}

/** 建筑能耗分析（GET /api/energy/buildings/{buildingId}/analysis，基于能耗记录） */
export interface BuildingAnalysis {
  buildingId: string;
  avgDailyConsumption: number;
  peakConsumption: number;
  energyIntensity: number;
  carbonFootprint: number;
  efficiencyRating: string;
  anomalyDetected: boolean;
  anomalyDescription?: string;
}

/** 节电潜力分析（GET /api/energy/saving-potential/{buildingId}） */
export interface SavingPotentialResult {
  estimatedSavingKwh: number;
  estimatedSavingCost: number;
  estimatedCarbonReduction: number;
  recommendations: string[];
}

/** 节能建议视图模型（能源页按建筑聚合 saving-potential 生成） */
export interface SavingRecommendation {
  id: string;
  title: string;
  description: string;
  buildingName: string;
  priority: 'high' | 'medium' | 'low';
  estimatedPayback: string;
  savingPotential: { kwh: number; cost: number; carbon: number };
  implementationDifficulty: 'easy' | 'moderate' | 'complex';
  details?: string;
}

/** 建筑能耗历史桶（GET /api/energy/buildings/{buildingId}/history，服务端按时间桶聚合 total_kwh 均值） */
export interface BuildingConsumptionBucket {
  /** 桶起始时间（UTC ISO 带 Z，前端 new Date() 自动转本地展示） */
  bucketTime: string;
  /** 桶内 total_kwh 样本均值（kWh 水平） */
  avgTotalKwh: number;
  /** 桶内样本数 */
  sampleCount?: number;
}

export interface WaterStation {
  id: string;
  name: string;
  position: [number, number];
  currentLevel: number;
  warningLevel: number;
  dangerLevel: number;
  trend: 'rising' | 'falling' | 'stable';
  status: Level;
  updatedAt: string;
}

// ============================================================
// 后端实时数据（city-water / city-collector）
// ============================================================

/** 后端水文站点实体（GET /api/water/stations） */
export interface RawWaterStation {
  id: number;
  stationId: string;
  name: string;
  type: 'RIVER' | 'LAKE' | 'RESERVOIR' | 'DRAINAGE';
  lat: number;
  lng: number;
  warningLevel: number;
  dangerLevel: number;
  currentLevel: number;
  maxCapacity: number;
  /** 由后端 getter 序列化得到：当前水位占最大容量百分比 */
  capacityPercentage?: number;
}

/** 水务系统统计（GET /api/water/status） */
export interface WaterSystemStatus {
  totalStations: number;
  normalStations: number;
  warningStations: number;
  criticalStations: number;
  bottleneckStations: number;
  systemStatus: 'NORMAL' | 'WARNING' | 'CRITICAL';
}

/** 水位监测记录（/api/water/stations/{id}/level → recentRecords） */
export interface WaterLevelRecordItem {
  stationId: string;
  timestamp: string;
  waterLevel: number;
  flowRate: number;
  rainfall: number;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
}

/** 站点实时水位详情（/api/water/stations/{id}/level） */
export interface WaterStationDetail {
  stationId: string;
  stationName: string;
  currentLevel: number;
  warningLevel: number;
  dangerLevel: number;
  maxCapacity: number;
  capacityPercentage: number;
  recentRecords: WaterLevelRecordItem[];
}

/** 排水能力分析（GET /api/water/drainage/analysis） */
export interface DrainageAnalysisItem {
  stationId: string;
  stationName: string;
  inflow: number;
  drainageCapacity: number;
  adequacyRatio: number;
  adequacyLevel: 'SUFFICIENT' | 'ADEQUATE' | 'INADEQUATE' | 'CRITICAL';
  recommendations: string[];
}

/** 采集器实时事件（GET /api/events/recent → city-collector） */
export interface CollectorEventRecord {
  eventId: string;
  domain: string;
  city: string;
  metric: string;
  value: number;
  unit: string;
  source: string;
  ts: string;
}

export interface FloodZone {
  id: string;
  name: string;
  coordinates: [number, number][];
  riskLevel: Level;
  area: number;
  population: number;
}

export interface DrainageNode {
  id: string;
  name: string;
  position: [number, number];
  capacity: number;
  currentFlow: number;
  status: Level;
}

export interface DisasterScenario {
  id: string;
  name: 'flood' | 'typhoon' | 'heavyRain';
  label: string;
  description: string;
  icon: string;
}

export interface SimulationParams {
  rainfall: number;
  duration: number;
  windSpeed: number;
  intensity: number;
}

export interface ImpactArea {
  id: string;
  name: string;
  type: 'road' | 'facility' | 'residential' | 'zone';
  status: Level;
  affectedPopulation: number;
  description: string;
  /** 真实坐标 [纬度, 经度]（city-disaster 模拟结果；无坐标的区域不在地图上渲染） */
  position?: [number, number];
}

export interface SourceCitation {
  id: string;
  title: string;
  url: string;
  type: 'document' | 'data' | 'regulation';
}

// ---- city-govai 对话持久化（会话 / 消息 / 知识命中）----
export interface KnowledgeHit {
  id: number | string;
  title: string;
  category?: string;
  source?: string;
  relevanceScore?: number;
}

export interface GovaiSession {
  sessionId: string;
  title: string;
  messageCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface GovaiMessage {
  id: number | string;
  sessionId: string;
  role: 'user' | 'assistant';
  content: string;
  /** 后端持久化格式：分号分隔的知识命中标题，可缺省 */
  knowledgeHits?: string | null;
  provider?: string | null;
  seq?: number;
  createdAt: string;
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
  sources: SourceCitation[];
  /** 生成该回答的模型提供方（city-govai 返回，仅 assistant 消息） */
  provider?: string;
}

export interface QuickAction {
  id: string;
  label: string;
  icon: React.ReactNode;
  description: string;
  action: string;
}

export interface Facility {
  id: string;
  name: string;
  type: string;
  location: string;
  status: Level;
  lastMaintenance: string;
  nextMaintenance: string;
  description: string;
}

export interface WorkOrder {
  id: string;
  title: string;
  status: 'pending' | 'inProgress' | 'completed' | 'cancelled';
  priority: 'low' | 'medium' | 'high' | 'urgent';
  department: string;
  assigner: string;
  progress: number;
  dueDate: string;
  createdAt: string;
}

export interface Report {
  id: number;
  title: string;
  type: 'daily' | 'weekly' | 'briefing' | 'risk';
  /** 类型中文名（后端 type 字段，如 '日报'） */
  typeLabel: string;
  city: string;
  period: string;
  author: string;
  /** 后端状态原值（如 COMPLETED） */
  status: string;
  createdAt: string;
  /** Markdown 正文（列表项为空；详情/生成结果填充） */
  content: string;
}

// ============================================================
// 后端报告数据（city-govai /api/govai/report*，报告中心真实数据）
// ============================================================

/** 后端报告实体：列表项不含 content，详情/生成返回完整报告 */
export interface GovaiReport {
  id: number;
  title: string;
  /** 类型中文名，如 '日报' */
  type: string;
  /** 类型枚举：DAILY / WEEKLY / BRIEF / RISK_ALERT */
  typeCode: string;
  city: string;
  period: string;
  author: string;
  status: string;
  createdAt: string;
  content?: string;
}

/** 报告列表返回（GET /api/govai/report/list） */
export interface GovaiReportListResult {
  total: number;
  reports: GovaiReport[];
}

export interface AlertConfig {
  waterLevel: number;
  rainfall: number;
  windSpeed: number;
  temperature: number;
}

export interface EmergencyPlan {
  planName: string;
  applicableScope: string;
  responseLevel: string;
  actionItems: string[];
  responsibleDepartments: string[];
}

// ============================================================
// 后端灾害数据（city-disaster /api/disaster，真实采集与水量平衡推演）
// ============================================================

/** 后端风险等级（WaterLevelRecord.RiskLevel） */
export type DisasterRiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

/** 单站点态势（GET /api/disaster/current） */
export interface DisasterStationStatus {
  stationId: string;
  name: string;
  type: 'RIVER' | 'LAKE' | 'RESERVOIR' | 'DRAINAGE';
  lat: number;
  lng: number;
  currentLevel: number;
  warningLevel: number;
  dangerLevel: number;
  maxCapacity: number;
  capacityPercentage: number;
  riskLevel: DisasterRiskLevel;
  trend: 'rising' | 'falling' | 'stable';
  updatedAt?: string | null;
}

/** 长江径流量信息（Open-Meteo GloFAS 预报） */
export interface DisasterRiverDischarge {
  stationName: string;
  current?: number | null;
  forecastMax?: number | null;
  unit: string;
  updatedAt?: string | null;
}

/** 当前灾害态势（GET /api/disaster/current） */
export interface DisasterCurrentStatus {
  city: string;
  generatedAt: string;
  sources: string[];
  weather: { rainfallMm: number; unit: string; source: string; updatedAt?: string | null };
  riverDischarge?: DisasterRiverDischarge | null;
  stations: DisasterStationStatus[];
  overallRisk: DisasterRiskLevel;
  recommended?: {
    stationId: string;
    stationName: string;
    rainfallMm: number;
    durationHours: number;
  } | null;
}

/** 模拟时间轴节点（minute 为分钟） */
export interface DisasterTimelinePoint {
  minute: number;
  waterLevel: number;
  capacityPercentage: number;
  riskLevel: DisasterRiskLevel;
}

/** 模拟影响区域（position 为 [纬度, 经度]） */
export interface DisasterImpactArea {
  name: string;
  type: 'core' | 'direct' | 'outer';
  position: [number, number];
  radiusKm: number;
  areaKm2: number;
  population: number;
  riskLevel: DisasterRiskLevel;
}

/** 洪水扩散帧（time 为分钟，polygon 为 [纬度, 经度] 顶点列表） */
export interface DisasterSpreadFrame {
  time: number;
  radiusKm: number;
  polygon: [number, number][];
}

/** 灾害模拟结果（POST /api/disaster/simulate） */
export interface DisasterSimulationResult {
  scenarioId: string;
  scenarioName: string;
  generatedAt: string;
  station: {
    stationId: string;
    name: string;
    type: string;
    lat: number;
    lng: number;
    currentLevel: number;
    warningLevel: number;
    dangerLevel: number;
    maxCapacity: number;
  };
  rainfallMm: number;
  durationHours: number;
  projectedLevel: number;
  riskLevel: DisasterRiskLevel;
  riskScore: number;
  affectedArea: number;
  affectedPopulation: number;
  estimatedDamage: number;
  roadClosures: string[];
  evacuationZones: string[];
  recommendedActions: string[];
  timeline: DisasterTimelinePoint[];
  impactAreas: DisasterImpactArea[];
  floodSpread: DisasterSpreadFrame[];
  riverDischarge?: DisasterRiverDischarge | null;
}
