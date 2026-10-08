import axiosInstance from './index';
import type {
  ApiResponse,
  AlarmItem,
  EnvironmentRecord,
  TrafficFlowData,
  SimulationResult,
  OptimizationResult,
  Building,
  BuildingAnalysis,
  SavingPotentialResult,
  BuildingConsumptionBucket,
  WaterStation,
  WaterSystemStatus,
  WaterStationDetail,
  DrainageAnalysisItem,
  CollectorEventRecord,
  ChatMessage,
  GovaiReport,
  GovaiReportListResult,
  EmergencyPlan,
  KnowledgeHit,
  GovaiSession,
  GovaiMessage,
  DisasterCurrentStatus,
  DisasterSimulationResult,
} from '@/types';

// Dashboard summary
export const getDashboardSummary = () => {
  return axiosInstance.get<ApiResponse<Record<string, unknown>>>('/dashboard/summary');
};

// Environment APIs
export const getEnvironmentLatest = (city: string) => {
  return axiosInstance.get<ApiResponse<EnvironmentRecord[]>>('/environment/latest', {
    params: { city },
  });
};

export const getEnvironmentHistory = (city: string, metric: string, hours: number) => {
  return axiosInstance.get<ApiResponse<EnvironmentRecord[]>>('/environment/history', {
    params: { city, metric, hours },
  });
};

// Traffic APIs
export const getTrafficSegments = () => {
  return axiosInstance.get<ApiResponse<TrafficFlowData[]>>('/traffic/segments');
};

// 运行信号优化仿真（city-traffic /simulation/run）
// 后端按策略执行配时优化 + 前后仿真对比，直接返回 OptimizationResult（非 ApiResponse 包裹）：
// {intersectionId, strategy, optimizedPhases, beforeSimulation, afterSimulation, improvementPercent}
export const runTrafficSimulation = (params: {
  intersectionId: string;
  phases: Array<{ phaseId?: string; direction: string; greenTime: number; yellowTime: number; redTime: number }>;
  strategy?: string;
}) => {
  return axiosInstance.post<OptimizationResult>('/traffic/simulation/run', params);
};

export const getSimulationResult = (scenarioId: string) => {
  return axiosInstance.get<ApiResponse<SimulationResult>>(`/traffic/simulation/${scenarioId}`);
};

// Energy APIs
export const getEnergyBuildings = () => {
  return axiosInstance.get<ApiResponse<Building[]>>('/energy/buildings');
};

// 建筑能耗分析（基于能耗记录：日均/峰值/强度/碳足迹/能效评级/异常检测）
// 无有效记录时后端返回全 0 与 efficiencyRating=N/A（anomalyDescription 说明原因）
// skipErrorToast：调用方静默容错并展示空态，避免批量请求失败时重复提示
export const getBuildingAnalysis = (buildingId: string) => {
  return axiosInstance.get<BuildingAnalysis>(`/energy/buildings/${buildingId}/analysis`, {
    skipErrorToast: true,
  });
};

// 建筑节电潜力（预计年节电/省费/减碳 + 针对性建议列表；无效 ID 后端返回 500）
export const getSavingPotential = (buildingId: string) => {
  return axiosInstance.get<SavingPotentialResult>(`/energy/saving-potential/${buildingId}`, {
    skipErrorToast: true,
  });
};

// 建筑能耗历史（服务端按 bucketHours 时间桶聚合 total_kwh 均值；hours 1~8760，bucketHours 1~24）
// 能源页“能耗趋势”按 日/周/月/年 并行取 B001-B008 后绘制多建筑曲线与平均线
// skipErrorToast：请求异常时调用方静默保持空态，避免 8 路请求重复弹错
export const getBuildingConsumptionHistory = (buildingId: string, hours: number, bucketHours = 1) => {
  return axiosInstance.get<BuildingConsumptionBucket[]>(`/energy/buildings/${buildingId}/history`, {
    params: { hours, bucketHours },
    skipErrorToast: true,
  });
};

// 电网指标 API（OWID 中国年度数据，zone=CN；英国实时源已移除）
export const getGridLatest = (zone = 'CN') => {
  return axiosInstance.get<ApiResponse<Record<string, unknown>[]>>('/energy/grid/latest', {
    params: { zone },
  });
};

export const getGridHistory = (metric: string, hours: number, zone = 'CN') => {
  return axiosInstance.get<ApiResponse<Record<string, unknown>[]>>('/energy/grid/history', {
    params: { zone, metric, hours },
  });
};

// 基础设施 API（city-infra 实时监控：设施状态总览与趋势）
export const getInfraStatus = () => {
  return axiosInstance.get<Record<string, unknown>>('/infra/status');
};

export const getInfraHistory = (facility: string, hours = 24) => {
  return axiosInstance.get<Record<string, unknown>[]>('/infra/history', {
    params: { facility, hours },
  });
};

// 基础设施工单列表（city-infra GET /infra/work-orders，南京地区运维工单，按创建时间倒序）
export const getInfraWorkOrders = () => {
  return axiosInstance.get<Record<string, unknown>[]>('/infra/work-orders');
};

// Water APIs
export const getWaterStations = () => {
  return axiosInstance.get<ApiResponse<WaterStation[]>>('/water/stations');
};

// 水务系统统计（站点总数/正常/警戒/危险/排水瓶颈，city-water 聚合接口）
export const getWaterSystemStatus = () => {
  return axiosInstance.get<ApiResponse<WaterSystemStatus>>('/water/status');
};

// 站点实时水位与最近监测记录
export const getWaterStationDetail = (id: string | number) => {
  return axiosInstance.get<ApiResponse<WaterStationDetail>>(`/water/stations/${id}/level`);
};

// 排水能力分析（入流/排水能力/充足率/建议）
export const getWaterDrainageAnalysis = () => {
  return axiosInstance.get<ApiResponse<DrainageAnalysisItem[]>>('/water/drainage/analysis');
};

// Disaster APIs（city-disaster 服务，Vite 代理 /api/disaster → localhost:8088，路径与后端一致）

// 当前灾害态势：站点实时水位/降水/长江径流量（真实采集数据）
export const fetchDisasterCurrent = () => {
  return axiosInstance.get<DisasterCurrentStatus>('/disaster/current');
};

// 灾害情景模拟：以真实站点水位为起点，后端水量平衡模型推演并入库
// 请求体可选：stationId（默认选库容占比最高站点）、rainfallMm（默认50）、durationHours（默认24）
export const runDisasterSimulation = (params: {
  stationId?: string;
  rainfallMm?: number;
  durationHours?: number;
}) => {
  return axiosInstance.post<DisasterSimulationResult>('/disaster/simulate', params, { timeout: 60000 });
};

// 历史模拟场景列表（FloodScenario 数组，非 ApiResponse 包裹）
export const getDisasterScenarios = () => {
  return axiosInstance.get<Record<string, unknown>[]>('/disaster/scenarios');
};

// Alarm APIs
export const getAlarms = (page: number, size: number) => {
  return axiosInstance.get<ApiResponse<{ records: AlarmItem[]; total: number }>>('/alarms', {
    params: { page, size },
  });
};

// AI Chat
export const chatWithAI = (query: string) => {
  return axiosInstance.post<ApiResponse<ChatMessage>>('/ai/chat', { query });
};

// ---- 报告中心：city-govai 报告模块（真实数据）----
// Vite 代理 /api/reports → /api/govai/report（覆盖 list / {id} / generate）

// 报告列表（摘要不含正文，按生成时间倒序；type 可选筛选，后端枚举：DAILY/WEEKLY/BRIEF/RISK_ALERT）
export const fetchReportList = (type?: string) => {
  return axiosInstance.get<GovaiReportListResult>('/reports/list', {
    params: type ? { type } : undefined,
  });
};

// 报告详情（含 Markdown 正文；不存在时后端返回 404）
export const fetchReportDetail = (id: number) => {
  return axiosInstance.get<GovaiReport>(`/reports/${id}`);
};

// 报告 PDF 导出：后端将 Markdown 正文渲染为 A4 PDF（中文标题/元信息/表格），返回二进制流
// 200 为 application/pdf（文件名见 Content-Disposition 的 filename*=UTF-8''）；404 空体；500 为 JSON {error}
// 注意 responseType=blob 时错误响应体同样以 Blob 返回，调用方需转文本解析（见 pages/Reports/pdfExport.ts）
// skipErrorToast：错误提示由调用方解析响应体（JSON error / 404）后自行展示，避免与拦截器通用提示重复
export const fetchReportPdf = (id: number) => {
  return axiosInstance.get<Blob>(`/reports/${id}/pdf`, {
    responseType: 'blob',
    timeout: 120000,
    skipErrorToast: true,
  });
};

// 生成报告：后端按实时数据生成并入库，直接返回完整报告（含正文），生成耗时较长单独放宽超时
export const generateReport = (type: string, city: string) => {
  return axiosInstance.post<GovaiReport>('/reports/generate', { type, city }, { timeout: 120000 });
};

// Alert explanation
export const explainAlert = (type: string, city: string, value: number) => {
  return axiosInstance.post<ApiResponse<{ explanation: string }>>('/ai/explain-alert', {
    type,
    city,
    value,
  });
};

// Collector APIs - 最近实时采集事件（含 TRAFFIC/WATER 等领域，受控于 city-collector）
export const getRecentEvents = (limit = 200) => {
  return axiosInstance.get<ApiResponse<CollectorEventRecord[]>>('/events/recent', {
    params: { limit },
  });
};

// Emergency plan retrieval
export const retrievePlan = (eventType: string, severity: string) => {
  return axiosInstance.post<ApiResponse<EmergencyPlan>>('/ai/retrieve-plan', {
    eventType,
    severity,
  });
};

// ---- govai 服务（AI 助手）：对话持久化（会话/消息 MySQL 存储）----
// Vite 代理：/api/ai/chat → /api/govai/chat；/api/ai/sessions* → /api/govai/sessions*（经 /api/ai 泛规则）

export interface GovaiChatResult {
  sessionId: string;
  title: string;
  answer: string;
  messageCount: number;
  knowledgeHits: KnowledgeHit[];
  provider: string;
  timestamp: string;
}

// 多轮对话提问：sessionId 为空时后端自动新建会话
export const govaiChat = (query: string, sessionId?: string | null) => {
  // AI 生成耗时较长（后端大模型调用），单独放宽该请求超时
  return axiosInstance.post<GovaiChatResult>(
    '/ai/chat',
    { message: query, sessionId: sessionId ?? undefined },
    { timeout: 90000 }
  );
};

// 历史会话列表（updatedAt 倒序）
export const fetchGovaiSessions = () => {
  return axiosInstance.get<{ total: number; sessions: GovaiSession[] }>('/ai/sessions');
};

// 会话消息回放（按 seq 升序）
export const fetchGovaiSessionMessages = (sessionId: string) => {
  return axiosInstance.get<{ sessionId: string; title: string; total: number; messages: GovaiMessage[] }>(
    `/ai/sessions/${sessionId}/messages`
  );
};

// 删除会话及其全部消息
export const deleteGovaiSession = (sessionId: string) => {
  return axiosInstance.delete<{ sessionId: string; deleted: boolean }>(`/ai/sessions/${sessionId}`);
};
