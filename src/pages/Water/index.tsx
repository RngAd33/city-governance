// ============================================================
// WaterRisk - Water disaster risk monitoring page
// 数据来源：city-water 服务（/api/water/*，30s 轮询接收实时水文数据）
// 后端不可用时降级为本地演示数据，保证页面可用
// ============================================================
import React, { useMemo, useState } from 'react';
import { Card, Select, DatePicker, Tag, Row, Col, Alert, Table, Tabs, Statistic, Space, Spin, Tooltip } from 'antd';
import {
  CloudOutlined,
  WarningOutlined,
  ExclamationCircleOutlined,
  DashboardOutlined,
  CheckCircleOutlined,
  AlertOutlined,
  ThunderboltOutlined,
  RiseOutlined,
} from '@ant-design/icons';
import dayjs, { Dayjs } from 'dayjs';
import { useQuery } from '@tanstack/react-query';
import WaterLevelChart from './components/WaterLevelChart';
import RiskMap from './components/RiskMap';
import RiskAssessment from './components/RiskAssessment';
import StatCard from '@/components/Common/StatCard';
import useAppStore from '@/stores/appStore';
import {
  getWaterStations,
  getWaterSystemStatus,
  getWaterStationDetail,
  getWaterDrainageAnalysis,
} from '@/api/dashboard';
import type {
  WaterStation,
  FloodZone,
  DrainageNode,
  Level,
  RawWaterStation,
  WaterSystemStatus,
  WaterLevelRecordItem,
  DrainageAnalysisItem,
} from '@/types';

const { RangePicker } = DatePicker;

// 与后端数据刷新节奏对齐的接收间隔（轮询接收实时水文数据）
const REFRESH_INTERVAL = 30000;

// 从 API 响应中健壮解包两层 data（AxiosResponse.data -> ApiResponse.data）
const unwrap = (res: any, fallback: any = null) => res?.data?.data ?? res?.data ?? fallback;

const CITY_OPTIONS = [
  { label: '南京市', value: '南京市' },
  { label: '上海市', value: '上海市' },
  { label: '北京市', value: '北京市' },
  { label: '广州市', value: '广州市' },
];

const LEVEL_TAG_COLORS: Record<Level, string> = {
  normal: 'success',
  warning: 'warning',
  danger: 'error',
  critical: 'error',
};

const SYSTEM_STATUS_LABELS: Record<WaterSystemStatus['systemStatus'], string> = {
  NORMAL: '正常',
  WARNING: '预警',
  CRITICAL: '严重',
};

const ADEQUACY_LABELS: Record<DrainageAnalysisItem['adequacyLevel'], string> = {
  SUFFICIENT: '充足',
  ADEQUATE: '基本满足',
  INADEQUATE: '不足',
  CRITICAL: '严重不足',
};

// 后端不可用时保留的演示数据
const FALLBACK_STATIONS: WaterStation[] = [
  { id: 'ws001', name: '黄浦江水位站', position: [31.2304, 121.4737], currentLevel: 3.8, warningLevel: 3.5, dangerLevel: 4.5, trend: 'rising', status: 'warning', updatedAt: '2026-10-03 14:00' },
  { id: 'ws002', name: '苏州河水位站', position: [31.2404, 121.4837], currentLevel: 2.9, warningLevel: 3.0, dangerLevel: 4.0, trend: 'stable', status: 'normal', updatedAt: '2026-10-03 14:00' },
  { id: 'ws003', name: '淀山湖水位站', position: [31.1004, 121.0537], currentLevel: 3.2, warningLevel: 3.0, dangerLevel: 3.8, trend: 'rising', status: 'warning', updatedAt: '2026-10-03 14:00' },
  { id: 'ws004', name: '长江口水位站', position: [31.4304, 121.9037], currentLevel: 4.8, warningLevel: 4.0, dangerLevel: 5.0, trend: 'rising', status: 'danger', updatedAt: '2026-10-03 14:00' },
  { id: 'ws005', name: '杭州湾水位站', position: [30.7304, 121.9737], currentLevel: 2.5, warningLevel: 3.5, dangerLevel: 4.5, trend: 'falling', status: 'normal', updatedAt: '2026-10-03 14:00' },
  { id: 'ws006', name: '太湖水位站', position: [31.1304, 120.2237], currentLevel: 3.6, warningLevel: 3.5, dangerLevel: 4.2, trend: 'rising', status: 'warning', updatedAt: '2026-10-03 14:00' },
];

const FALLBACK_FLOOD_ZONES: FloodZone[] = [
  { id: 'fz001', name: '黄浦江低洼区域', coordinates: [[31.2204, 121.4637], [31.2404, 121.4637], [31.2404, 121.4837], [31.2204, 121.4837]], riskLevel: 'warning', area: 12.5, population: 45000 },
  { id: 'fz002', name: '苏州河沿岸区域', coordinates: [[31.2304, 121.4737], [31.2504, 121.4737], [31.2504, 121.4937], [31.2304, 121.4937]], riskLevel: 'danger', area: 8.3, population: 32000 },
  { id: 'fz003', name: '淀山湖周边区域', coordinates: [[31.0904, 121.0437], [31.1104, 121.0437], [31.1104, 121.0637], [31.0904, 121.0637]], riskLevel: 'normal', area: 25.0, population: 15000 },
];

const FALLBACK_DRAINAGE: DrainageNode[] = [
  { id: 'dn001', name: '黄浦江排水泵站A', position: [31.2354, 121.4787], capacity: 50, currentFlow: 35, status: 'warning' },
  { id: 'dn002', name: '苏州河排水泵站B', position: [31.2454, 121.4887], capacity: 40, currentFlow: 20, status: 'normal' },
  { id: 'dn003', name: '淀山湖排水泵站C', position: [31.1054, 121.0587], capacity: 60, currentFlow: 55, status: 'danger' },
  { id: 'dn004', name: '长江口排水泵站D', position: [31.4354, 121.9087], capacity: 80, currentFlow: 70, status: 'warning' },
];

const generateTimeSeriesData = (baseLevel: number, hours: number = 24) => {
  const data = [];
  const now = dayjs();
  for (let i = hours; i >= 0; i--) {
    const time = now.subtract(i, 'hour').format('MM-DD HH:mm');
    const variation = Math.sin(i / 4) * 0.3 + Math.random() * 0.2;
    data.push({ time, value: +(baseLevel + variation).toFixed(2) });
  }
  return data;
};

const generateRainfallData = (hours: number = 24) => {
  const data = [];
  const now = dayjs();
  for (let i = hours; i >= 0; i--) {
    const time = now.subtract(i, 'hour').format('MM-DD HH:mm');
    const rainfall = Math.max(0, Math.sin(i / 6) * 15 + Math.random() * 10 - 5);
    data.push({ time, value: +rainfall.toFixed(1) });
  }
  return data;
};

// ---- 后端水文数据 → 前端视图模型 ----

// 容量百分比（优先使用后端计算值）
const capacityPercent = (raw: RawWaterStation): number => {
  if (typeof raw.capacityPercentage === 'number') return raw.capacityPercentage;
  return raw.maxCapacity > 0 ? (raw.currentLevel / raw.maxCapacity) * 100 : 0;
};

// 综合水位阈值与容量占比判定站点等级
const resolveStatus = (raw: RawWaterStation): Level => {
  const pct = capacityPercent(raw);
  if (raw.currentLevel >= raw.dangerLevel || pct > 95) return 'critical';
  if (raw.currentLevel >= raw.warningLevel || pct > 80) return 'danger';
  if (pct > 60) return 'warning';
  return 'normal';
};

// 依据最近两条监测记录判定水位趋势（recentRecords 按时间倒序）
const resolveTrend = (records: WaterLevelRecordItem[]): WaterStation['trend'] => {
  if (records.length < 2) return 'stable';
  const [latest, prev] = records;
  if (latest.waterLevel > prev.waterLevel) return 'rising';
  if (latest.waterLevel < prev.waterLevel) return 'falling';
  return 'stable';
};

const mapStation = (raw: RawWaterStation, records: WaterLevelRecordItem[]): WaterStation => ({
  id: raw.stationId,
  name: raw.name,
  position: [raw.lat, raw.lng],
  currentLevel: raw.currentLevel,
  warningLevel: raw.warningLevel,
  dangerLevel: raw.dangerLevel,
  trend: resolveTrend(records),
  status: resolveStatus(raw),
  updatedAt: records[0]?.timestamp
    ? dayjs(records[0].timestamp).format('MM-DD HH:mm')
    : dayjs().format('MM-DD HH:mm'),
});

// 排水充足等级 → 站点等级
const adequacyToLevel = (level: DrainageAnalysisItem['adequacyLevel']): Level => {
  switch (level) {
    case 'CRITICAL':
      return 'danger';
    case 'INADEQUATE':
      return 'warning';
    default:
      return 'normal';
  }
};

// 排水分析表格行
interface DrainageRow {
  key: string;
  name: string;
  inflow: number;
  capacity: number;
  ratio: number;
  status: Level;
  levelLabel: string;
  recommendations: string[];
}

const WaterRisk: React.FC = () => {
  const { city, setCity } = useAppStore();
  const [selectedStation, setSelectedStation] = useState<string>('');
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs]>([dayjs().subtract(1, 'day'), dayjs()]);
  const [activeTab, setActiveTab] = useState('assessment');

  // ---- 接收后端水文数据（30s 轮询） ----
  const stationsQuery = useQuery({
    queryKey: ['water', 'stations'],
    queryFn: () => getWaterStations().then((r) => unwrap(r, []) as RawWaterStation[]),
    refetchInterval: REFRESH_INTERVAL,
  });

  const statusQuery = useQuery({
    queryKey: ['water', 'system-status'],
    queryFn: () =>
      getWaterSystemStatus().then((r) => unwrap(r, null) as WaterSystemStatus | null),
    refetchInterval: REFRESH_INTERVAL,
  });

  const drainageQuery = useQuery({
    queryKey: ['water', 'drainage-analysis'],
    queryFn: () =>
      getWaterDrainageAnalysis().then((r) => unwrap(r, []) as DrainageAnalysisItem[]),
    refetchInterval: REFRESH_INTERVAL,
  });

  const rawStations = stationsQuery.data ?? [];
  const usingRealData = rawStations.length > 0;

  // 各站点最近监测记录（用于趋势判定、实时趋势图与降雨统计）
  // 注意：详情接口按数据库主键 id 查询，而非业务站号 stationId
  const detailsQuery = useQuery({
    queryKey: ['water', 'station-details', rawStations.map((s) => s.id).join(',')],
    queryFn: async () => {
      const entries = await Promise.all(
        rawStations.map(async (s) => {
          try {
            const res = await getWaterStationDetail(s.id);
            const detail = unwrap(res, null);
            return [s.stationId, (detail?.recentRecords ?? []) as WaterLevelRecordItem[]] as const;
          } catch {
            return [s.stationId, [] as WaterLevelRecordItem[]] as const;
          }
        }),
      );
      return Object.fromEntries(entries) as Record<string, WaterLevelRecordItem[]>;
    },
    enabled: rawStations.length > 0,
    refetchInterval: REFRESH_INTERVAL,
  });

  const recordsByStation = detailsQuery.data ?? {};

  const stations: WaterStation[] = useMemo(() => {
    if (!usingRealData) return FALLBACK_STATIONS;
    return rawStations.map((s) => mapStation(s, recordsByStation[s.stationId] ?? []));
  }, [usingRealData, rawStations, recordsByStation]);

  // 数据刷新后自动纠正失效的选中站点
  const activeStationId = stations.some((s) => s.id === selectedStation)
    ? selectedStation
    : stations[0]?.id ?? '';

  // 系统统计：优先使用后端聚合接口，缺失时本地汇总
  const summary: WaterSystemStatus = useMemo(() => {
    if (statusQuery.data) return statusQuery.data;
    let normal = 0;
    let warning = 0;
    let critical = 0;
    stations.forEach((s) => {
      if (s.status === 'normal') normal += 1;
      else if (s.status === 'warning') warning += 1;
      else critical += 1;
    });
    const bottlenecks = (drainageQuery.data ?? []).filter(
      (d) => d.adequacyLevel === 'INADEQUATE' || d.adequacyLevel === 'CRITICAL',
    ).length;
    return {
      totalStations: stations.length,
      normalStations: normal,
      warningStations: warning,
      criticalStations: critical,
      bottleneckStations: bottlenecks,
      systemStatus: critical > 0 ? 'CRITICAL' : warning > 0 ? 'WARNING' : 'NORMAL',
    } as WaterSystemStatus;
  }, [statusQuery.data, drainageQuery.data, stations]);

  const avgLevel = stations.length
    ? stations.reduce((sum, s) => sum + s.currentLevel, 0) / stations.length
    : 0;
  const maxStation = stations.reduce<WaterStation | null>(
    (max, s) => (!max || s.currentLevel > max.currentLevel ? s : max),
    null,
  );

  // ---- 图表数据（真实记录优先，离线时使用演示序列） ----
  const stationData = useMemo(() => {
    const data: Record<string, { time: string; value: number }[]> = {};
    if (!usingRealData) {
      stations.forEach((s) => {
        data[s.id] = generateTimeSeriesData(s.currentLevel);
      });
      return data;
    }
    Object.entries(recordsByStation).forEach(([id, records]) => {
      data[id] = [...records].reverse().map((r) => ({
        time: dayjs(r.timestamp).format('MM-DD HH:mm'),
        value: r.waterLevel,
      }));
    });
    return data;
  }, [usingRealData, stations, recordsByStation]);

  const rainfallData = useMemo(() => {
    if (!usingRealData) return generateRainfallData();
    const records = recordsByStation[activeStationId] ?? [];
    return [...records].reverse().map((r) => ({
      time: dayjs(r.timestamp).format('MM-DD HH:mm'),
      value: +((r.rainfall ?? 0).toFixed(1)),
    }));
  }, [usingRealData, recordsByStation, activeStationId]);

  // 降雨与流量统计（基于所选站点最近监测记录）
  const rainfallSummary = useMemo(() => {
    if (!usingRealData) return { latest: 45.2, avg: 32.8, flow: 35, real: false };
    const records = recordsByStation[activeStationId] ?? [];
    if (records.length === 0) return { latest: null, avg: null, flow: null, real: true };
    const rainValues = records.map((r) => r.rainfall ?? 0);
    return {
      latest: rainValues[0] ?? 0,
      avg: +(rainValues.reduce((a, b) => a + b, 0) / rainValues.length).toFixed(1),
      flow: records[0]?.flowRate ?? 0,
      real: true,
    };
  }, [usingRealData, recordsByStation, activeStationId]);

  const highRiskStations = stations.filter((s) => s.status === 'danger' || s.status === 'critical');

  // 排水分析表格数据（后端分析结果优先，离线时演示数据）
  const drainageRows: DrainageRow[] = useMemo(() => {
    const list = drainageQuery.data ?? [];
    if (list.length > 0) {
      return list.map((d) => ({
        key: d.stationId,
        name: d.stationName,
        inflow: +d.inflow.toFixed(2),
        capacity: +d.drainageCapacity.toFixed(2),
        ratio: +d.adequacyRatio.toFixed(2),
        status: adequacyToLevel(d.adequacyLevel),
        levelLabel: ADEQUACY_LABELS[d.adequacyLevel] ?? d.adequacyLevel,
        recommendations: d.recommendations ?? [],
      }));
    }
    return FALLBACK_DRAINAGE.map((n) => ({
      key: n.id,
      name: n.name,
      inflow: n.currentFlow,
      capacity: n.capacity,
      ratio: n.currentFlow > 0 ? +(n.capacity / n.currentFlow).toFixed(2) : 0,
      status: n.status,
      levelLabel: n.status === 'normal' ? '充足' : n.status === 'warning' ? '基本满足' : '不足',
      recommendations: [] as string[],
    }));
  }, [drainageQuery.data]);

  // 地图排水管网图层（按站点坐标定位排水分析结果）
  const drainageNodes: DrainageNode[] = useMemo(() => {
    const list = drainageQuery.data ?? [];
    if (list.length === 0) return FALLBACK_DRAINAGE;
    return list
      .map((d) => {
        const st = stations.find((s) => s.id === d.stationId);
        if (!st) return null;
        return {
          id: d.stationId,
          name: d.stationName,
          position: st.position,
          capacity: +d.drainageCapacity.toFixed(1),
          currentFlow: +d.inflow.toFixed(1),
          status: adequacyToLevel(d.adequacyLevel),
        } as DrainageNode;
      })
      .filter((n): n is DrainageNode => n !== null);
  }, [drainageQuery.data, stations]);

  const assessmentColumns = [
    { title: '站名', dataIndex: 'name', key: 'name' },
    { title: '当前水位(m)', dataIndex: 'currentLevel', key: 'currentLevel', sorter: (a: WaterStation, b: WaterStation) => a.currentLevel - b.currentLevel },
    { title: '警戒水位(m)', dataIndex: 'warningLevel', key: 'warningLevel' },
    { title: '危险水位(m)', dataIndex: 'dangerLevel', key: 'dangerLevel' },
    {
      title: '趋势',
      dataIndex: 'trend',
      key: 'trend',
      render: (trend: WaterStation['trend']) => (
        <Tag color={trend === 'rising' ? 'error' : trend === 'falling' ? 'success' : 'default'}>
          {trend === 'rising' ? '↑ 上升' : trend === 'falling' ? '↓ 下降' : '→ 平稳'}
        </Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: Level) => (
        <Tag color={LEVEL_TAG_COLORS[status]}>
          {status === 'normal' ? '正常' : status === 'warning' ? '警戒' : status === 'danger' ? '危险' : '严重'}
        </Tag>
      ),
    },
  ];

  const drainageColumns = [
    { title: '站点名称', dataIndex: 'name', key: 'name' },
    { title: '入流量(m³/s)', dataIndex: 'inflow', key: 'inflow' },
    { title: '排水能力(m³/s)', dataIndex: 'capacity', key: 'capacity' },
    {
      title: '充足率',
      dataIndex: 'ratio',
      key: 'ratio',
      render: (ratio: number) => (
        <Tag color={ratio >= 1.2 ? 'success' : ratio >= 1 ? 'default' : ratio >= 0.8 ? 'warning' : 'error'}>
          {ratio}
        </Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: Level, record: DrainageRow) => (
        <Tag color={LEVEL_TAG_COLORS[status]}>{record.levelLabel}</Tag>
      ),
    },
    {
      title: '建议',
      key: 'recommendations',
      render: (_: unknown, record: DrainageRow) =>
        record.recommendations.length > 0 ? (
          <Tooltip
            title={
              <div>
                {record.recommendations.map((r) => (
                  <div key={r}>· {r}</div>
                ))}
              </div>
            }
          >
            <a>{record.recommendations.length} 条建议</a>
          </Tooltip>
        ) : (
          '--'
        ),
    },
  ];

  return (
    <Spin spinning={stationsQuery.isLoading}>
      <div style={{ padding: 24, background: '#f0f2f5', minHeight: '100%' }}>
        {/* Header */}
        <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h2 style={{ margin: 0, fontSize: 20, fontWeight: 600 }}>水患风险监测</h2>
            <span style={{ color: '#8c8c8c', fontSize: 13 }}>实时监控城市水位变化，预警洪涝风险</span>
          </div>
          <Space>
            <Tag color={usingRealData ? 'green' : 'orange'}>
              {usingRealData ? '后端实时数据' : '演示数据'}
            </Tag>
            {stationsQuery.dataUpdatedAt > 0 && (
              <span style={{ color: '#8c8c8c', fontSize: 12 }}>
                更新于 {dayjs(stationsQuery.dataUpdatedAt).format('HH:mm:ss')}
              </span>
            )}
            <Select value={city} onChange={setCity} style={{ width: 120 }} options={CITY_OPTIONS} />
            <RangePicker value={dateRange} onChange={(dates) => { if (dates && dates[0] && dates[1]) setDateRange([dates[0], dates[1]]); }} />
          </Space>
        </div>

        {/* 实时统计概览（后端 water/status + 站点水位汇总） */}
        <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
          <Col xs={12} sm={8} lg={4}>
            <StatCard
              title="监测站点总数"
              value={summary.totalStations}
              unit="个"
              level="NORMAL"
              icon={<DashboardOutlined />}
              tooltip="city-water 服务在册的水文监测站点数量"
            />
          </Col>
          <Col xs={12} sm={8} lg={4}>
            <StatCard
              title="正常站点"
              value={summary.normalStations}
              unit="个"
              level="NORMAL"
              icon={<CheckCircleOutlined />}
              tooltip="水位与容量均在安全范围内的站点"
            />
          </Col>
          <Col xs={12} sm={8} lg={4}>
            <StatCard
              title="警戒站点"
              value={summary.warningStations}
              unit="个"
              level={summary.warningStations > 0 ? 'WARNING' : 'NORMAL'}
              icon={<WarningOutlined />}
              tooltip="容量占比 80%~95% 的站点"
            />
          </Col>
          <Col xs={12} sm={8} lg={4}>
            <StatCard
              title="危险/严重站点"
              value={summary.criticalStations}
              unit="个"
              level={summary.criticalStations > 0 ? 'DANGER' : 'NORMAL'}
              icon={<AlertOutlined />}
              tooltip="容量占比超过 95% 的站点"
            />
          </Col>
          <Col xs={12} sm={8} lg={4}>
            <StatCard
              title="排水瓶颈"
              value={summary.bottleneckStations}
              unit="个"
              level={summary.bottleneckStations > 0 ? 'WARNING' : 'NORMAL'}
              icon={<ThunderboltOutlined />}
              tooltip="排水充足率低于 0.8 的站点数量"
            />
          </Col>
          <Col xs={12} sm={8} lg={4}>
            <StatCard
              title="平均水位"
              value={avgLevel.toFixed(2)}
              unit="m"
              level={maxStation && (maxStation.status === 'danger' || maxStation.status === 'critical') ? 'WARNING' : 'NORMAL'}
              icon={<RiseOutlined />}
              tooltip={maxStation ? `最高: ${maxStation.name} ${maxStation.currentLevel}m` : undefined}
            />
          </Col>
        </Row>

        {/* 系统状态与高风险预警 */}
        {(highRiskStations.length > 0 || summary.systemStatus !== 'NORMAL') && (
          <Alert
            message={
              <span>
                <WarningOutlined style={{ color: '#ff4d4f', marginRight: 8 }} />
                <strong>水务系统状态：{SYSTEM_STATUS_LABELS[summary.systemStatus]}</strong>
                {' · '}
                正常 {summary.normalStations} / 警戒 {summary.warningStations} / 危险 {summary.criticalStations} / 排水瓶颈 {summary.bottleneckStations}
                {highRiskStations.length > 0 && (
                  <>
                    ；高风险站点：{highRiskStations.map((s) => s.name).join('、')}，请立即采取应急措施！
                  </>
                )}
              </span>
            }
            type={highRiskStations.length > 0 || summary.systemStatus === 'CRITICAL' ? 'error' : 'warning'}
            showIcon
            closable
            style={{ marginBottom: 16 }}
          />
        )}

        {/* Top Row: Station Selector + Rainfall */}
        <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
          <Col xs={24} lg={12}>
            <Card title="水位站选择" size="small">
              <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
                {stations.map((s) => (
                  <Tag
                    key={s.id}
                    color={LEVEL_TAG_COLORS[s.status]}
                    style={{ cursor: 'pointer', padding: '4px 12px', fontSize: 13, border: s.id === activeStationId ? '2px solid #1677ff' : undefined }}
                    onClick={() => setSelectedStation(s.id)}
                  >
                    {s.name} ({s.currentLevel}m)
                  </Tag>
                ))}
              </div>
            </Card>
          </Col>
          <Col xs={24} lg={12}>
            <Card title={`降雨与流量（${stations.find((s) => s.id === activeStationId)?.name ?? '-'}）`} size="small">
              <Row gutter={16}>
                <Col span={8}>
                  <Statistic
                    title="最新降雨量"
                    value={rainfallSummary.latest ?? '-'}
                    suffix={rainfallSummary.latest === null ? '' : 'mm'}
                    prefix={<CloudOutlined />}
                    valueStyle={{ color: '#1890ff' }}
                  />
                </Col>
                <Col span={8}>
                  <Statistic
                    title="近期平均降雨"
                    value={rainfallSummary.avg ?? '-'}
                    suffix={rainfallSummary.avg === null ? '' : 'mm'}
                    prefix={<CloudOutlined />}
                    valueStyle={{ color: '#faad14' }}
                  />
                </Col>
                <Col span={8}>
                  <Statistic
                    title="最新流量"
                    value={rainfallSummary.flow ?? '-'}
                    suffix={rainfallSummary.flow === null ? '' : 'm³/s'}
                    prefix={<ExclamationCircleOutlined />}
                    valueStyle={{ color: '#52c41a' }}
                  />
                </Col>
              </Row>
            </Card>
          </Col>
        </Row>

        {/* Middle Row: Chart + Map */}
        <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
          <Col xs={24} lg={12}>
            <Card title="水位变化趋势" size="small" style={{ height: '100%' }}>
              <WaterLevelChart
                stations={stations}
                data={stationData}
                rainfallData={rainfallData}
                selectedStation={activeStationId}
                onStationChange={setSelectedStation}
              />
            </Card>
          </Col>
          <Col xs={24} lg={12}>
            <Card title="风险地图" size="small" style={{ height: '100%' }}>
              <div style={{ height: 400 }}>
                <RiskMap
                  stations={stations}
                  floodZones={FALLBACK_FLOOD_ZONES}
                  drainageNodes={drainageNodes}
                  selectedStation={activeStationId}
                  onStationSelect={setSelectedStation}
                />
              </div>
            </Card>
          </Col>
        </Row>

        {/* Bottom Row: Assessment + Drainage */}
        <Row gutter={[16, 16]}>
          <Col xs={24} lg={14}>
            <Card title="风险评估" size="small" style={{ height: '100%' }}>
              <Tabs
                activeKey={activeTab}
                onChange={setActiveTab}
                items={[
                  {
                    key: 'assessment',
                    label: '风险卡片',
                    children: <RiskAssessment stations={stations} onStationClick={setSelectedStation} />,
                  },
                  {
                    key: 'table',
                    label: '数据表格',
                    children: (
                      <Table
                        dataSource={stations}
                        columns={assessmentColumns}
                        rowKey="id"
                        size="small"
                        pagination={false}
                        scroll={{ y: 300 }}
                      />
                    ),
                  },
                ]}
              />
            </Card>
          </Col>
          <Col xs={24} lg={10}>
            <Card title="排水分析" size="small" style={{ height: '100%' }}>
              <Table
                dataSource={drainageRows}
                columns={drainageColumns}
                rowKey="key"
                size="small"
                pagination={false}
                scroll={{ y: 300 }}
              />
            </Card>
          </Col>
        </Row>
      </div>
    </Spin>
  );
};

export default WaterRisk;
