// ============================================================
// CityOverviewDashboard - Main dashboard page
// 各数据区块独立请求、独立 loading：单个接口慢/失败不再阻塞整页
// ============================================================
import React, { lazy, Suspense, useMemo, useState } from 'react';
import { Row, Col, Card, message, Skeleton, Spin } from 'antd';
import {
  CloudOutlined,
  CarOutlined,
  ThunderboltOutlined,
  AlertOutlined,
} from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import StatCard from './components/StatCard';
import type { StatCardProps } from './components/StatCard';
import AlarmList from './components/AlarmList';
import {
  getEnvironmentLatest,
  getAlarms,
  getTrafficSegments,
  getEnergyBuildings,
  getWaterStations,
} from '@/api/dashboard';
import type {
  AlarmItem,
  Building,
  EnvironmentRecord,
  RawWaterStation,
  TrafficFlowData,
  WaterStation,
} from '@/types';

// 重型图表/地图按需加载：页面框架与 KPI 卡片先行渲染，图层/图表模块就绪后异步填充
const CityMap = lazy(() => import('./components/CityMap'));
const EnvironmentChart = lazy(() => import('./components/EnvironmentChart'));
const TrafficChart = lazy(() => import('./components/TrafficChart'));

// 区块级加载骨架：懒加载模块就绪前，先给出数据窗口的形状，避免布局跳动
const SectionSkeleton: React.FC<{ height: number }> = ({ height }) => (
  <Skeleton.Node active style={{ width: '100%', height }} />
);

// 从各 API 响应中健壮解包两层 data（AxiosResponse.data -> ApiResponse.data）
const unwrap = (res: any, fallback: any = null) => res?.data?.data ?? res?.data ?? fallback;

const REFRESH_INTERVAL = 30000;

const CityOverviewDashboard: React.FC = () => {
  // React Query：请求去重 + 缓存（全局 staleTime 5 分钟）+ 30s 自动刷新
  // 页面隐藏时暂停轮询，重新聚焦后按需带回
  const envQuery = useQuery({
    queryKey: ['dashboard', 'environment-latest', '南京'],
    queryFn: () =>
      getEnvironmentLatest('南京').then((r) => unwrap(r, []) as EnvironmentRecord[]),
    refetchInterval: REFRESH_INTERVAL,
  });

  const alarmsQuery = useQuery({
    queryKey: ['dashboard', 'alarms'],
    queryFn: () =>
      getAlarms(1, 50).then((r) => {
        const data = unwrap(r, []);
        return (Array.isArray(data) ? data : data?.records ?? []) as AlarmItem[];
      }),
    refetchInterval: REFRESH_INTERVAL,
  });

  const trafficQuery = useQuery({
    queryKey: ['dashboard', 'traffic-segments'],
    queryFn: () => getTrafficSegments().then((r) => unwrap(r, []) as TrafficFlowData[]),
    refetchInterval: REFRESH_INTERVAL,
  });

  const buildingsQuery = useQuery({
    queryKey: ['dashboard', 'energy-buildings'],
    queryFn: () => getEnergyBuildings().then((r) => unwrap(r, []) as Building[]),
    refetchInterval: REFRESH_INTERVAL,
  });

  const waterQuery = useQuery({
    queryKey: ['dashboard', 'water-stations'],
    // 后端 /water/stations 返回站点实体（lat/lng），转换为 CityMap 所需的
    // WaterStation 视图模型（position/status 等），避免组件访问缺失字段崩溃
    queryFn: () =>
      getWaterStations().then((r) =>
        (unwrap(r, []) as RawWaterStation[]).map(
          (s): WaterStation => ({
            id: s.stationId ?? String(s.id),
            name: s.name,
            position: [s.lat, s.lng],
            currentLevel: s.currentLevel,
            warningLevel: s.warningLevel,
            dangerLevel: s.dangerLevel,
            trend: 'stable',
            status:
              s.currentLevel >= s.dangerLevel
                ? 'danger'
                : s.currentLevel >= s.warningLevel
                  ? 'warning'
                  : 'normal',
            updatedAt: '',
          }),
        ),
      ),
    refetchInterval: REFRESH_INTERVAL,
  });

  const envRecords = envQuery.data ?? [];
  // 地图仅需每站最新一条记录：接口返回全量历史（数千条），逐条建标记会拖垮渲染性能
  const mapEnvRecords = useMemo(() => {
    const latestByStation = new Map<string, EnvironmentRecord>();
    envRecords.forEach((r) => {
      const prev = latestByStation.get(r.stationName);
      if (!prev || (r.ts || '') > (prev.ts || '')) latestByStation.set(r.stationName, r);
    });
    return Array.from(latestByStation.values());
  }, [envRecords]);
  const trafficData = trafficQuery.data ?? [];
  const buildings = buildingsQuery.data ?? [];
  const waterStations = waterQuery.data ?? [];

  // 从真实数据本地计算 summary 指标（后端无聚合端点）
  const stats: StatCardProps[] = useMemo(() => {
    // 接口按时间正序返回全量历史，find 只能拿到最老一条：须遍历取时间戳最新的 pm25
    const latestPm25 = envRecords.reduce<EnvironmentRecord | null>(
      (acc, r) => (r.metric === 'pm25' && (!acc || (r.ts || '') > (acc.ts || '')) ? r : acc),
      null,
    );
    const aqi = Math.round((latestPm25?.value ?? 85) * 1.2);
    const congested = trafficData.filter((t) =>
      ['CONGESTED', 'SEVERE', 'congested', 'severe'].includes(t.congestionLevel || ''),
    ).length;
    const congestionIndex = trafficData.length
      ? Math.min(Math.round((congested / trafficData.length) * 100), 100)
      : 62;
    const energyConsumption = buildings.reduce(
      (sum, b) => sum + (b.avgDailyConsumption || 0),
      0,
    );
    const maxWater = waterStations.reduce(
      (max, w) => Math.max(max, w.currentLevel || 0),
      0,
    );

    return [
      {
        title: 'AQI',
        value: aqi,
        unit: '',
        level: aqi > 150 ? 'danger' : aqi > 100 ? 'warning' : 'normal',
        icon: <CloudOutlined />,
        trend: 'up',
        trendValue: 3.2,
        loading: envQuery.isLoading,
      },
      {
        title: '交通拥堵指数',
        value: congestionIndex,
        unit: '',
        level:
          congestionIndex > 80 ? 'danger' : congestionIndex > 60 ? 'warning' : 'normal',
        icon: <CarOutlined />,
        trend: 'down',
        trendValue: 1.5,
        loading: trafficQuery.isLoading,
      },
      {
        title: '建筑能耗',
        value: energyConsumption,
        unit: 'kWh',
        level:
          energyConsumption > 25000
            ? 'danger'
            : energyConsumption > 15000
              ? 'warning'
              : 'normal',
        icon: <ThunderboltOutlined />,
        trend: 'up',
        trendValue: 5.8,
        loading: buildingsQuery.isLoading,
      },
      {
        title: '最高水位',
        value: maxWater || 2.8,
        unit: 'm',
        level: maxWater > 4 ? 'danger' : maxWater > 3 ? 'warning' : 'normal',
        icon: <AlertOutlined />,
        trend: 'flat',
        trendValue: 0,
        loading: waterQuery.isLoading,
      },
    ];
  }, [
    envRecords,
    trafficData,
    buildings,
    waterStations,
    envQuery.isLoading,
    trafficQuery.isLoading,
    buildingsQuery.isLoading,
    waterQuery.isLoading,
  ]);

  // 告警确认：本地叠加标记，不阻断查询缓存与轮询刷新
  const [acknowledgedIds, setAcknowledgedIds] = useState<Set<string>>(new Set());
  const displayAlarms = useMemo(
    () =>
      (alarmsQuery.data ?? []).map((a) =>
        acknowledgedIds.has(a.id) ? { ...a, acknowledged: true } : a,
      ),
    [alarmsQuery.data, acknowledgedIds],
  );

  const handleAcknowledge = (id: string) => {
    setAcknowledgedIds((prev) => new Set(prev).add(id));
    message.success('告警已确认');
  };

  const mapLoading =
    buildingsQuery.isLoading || envQuery.isLoading || waterQuery.isLoading;

  return (
    <div style={{ padding: 24 }}>
      <h2 style={{ marginBottom: 24, fontSize: 20, fontWeight: 600 }}>
        城市治理总览
      </h2>

      {/* Top row: 4 stat cards */}
      <Row gutter={[16, 16]}>
        {stats.map((s, i) => (
          <Col xs={24} sm={12} lg={6} key={i}>
            <StatCard {...s} />
          </Col>
        ))}
      </Row>

      {/* Middle row: Map (60%) + Alarm list (40%) */}
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={15}>
          <Card
            title="城市地图"
            style={{ height: 480, borderRadius: 8 }}
            styles={{ body: { padding: 12, height: 420 } }}
          >
            <Spin spinning={mapLoading}>
              <Suspense fallback={<SectionSkeleton height={420} />}>
                <CityMap
                  buildings={buildings}
                  environmentRecords={mapEnvRecords}
                  waterStations={waterStations}
                />
              </Suspense>
            </Spin>
          </Card>
        </Col>
        <Col xs={24} lg={9}>
          <Card
            title="实时告警"
            style={{ height: 480, borderRadius: 8 }}
            styles={{ body: { padding: 12, height: 420, overflow: 'hidden' } }}
          >
            <Spin spinning={alarmsQuery.isLoading}>
              <AlarmList alarms={displayAlarms} onAcknowledge={handleAcknowledge} />
            </Spin>
          </Card>
        </Col>
      </Row>

      {/* Bottom row: Environment trends + Traffic flow */}
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={12}>
          <Card title="环境趋势" style={{ borderRadius: 8 }}>
            <Spin spinning={envQuery.isLoading}>
              <Suspense fallback={<SectionSkeleton height={320} />}>
                <EnvironmentChart data={envRecords} />
              </Suspense>
            </Spin>
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="交通流量" style={{ borderRadius: 8 }}>
            <Spin spinning={trafficQuery.isLoading}>
              <Suspense fallback={<SectionSkeleton height={280} />}>
                <TrafficChart data={trafficData} />
              </Suspense>
            </Spin>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default CityOverviewDashboard;
