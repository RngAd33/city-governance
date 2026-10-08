import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Card, Button, Progress, Steps, message, Row, Col, Space, Table, Tag } from 'antd';
import {
  PlayCircleOutlined,
  ReloadOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  SyncOutlined,
  RobotOutlined,
  DatabaseOutlined,
  CarOutlined,
  DashboardOutlined,
  OrderedListOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { useQuery } from '@tanstack/react-query';
import type {
  Intersection,
  SimulationResult,
  SignalPhase,
  OptimizationResult,
  CollectorEventRecord,
} from '@/types';
import { getTrafficSegments, runTrafficSimulation, getRecentEvents } from '@/api/dashboard';
import StatCard from '@/components/Common/StatCard';
import useAppStore from '@/stores/appStore';
import IntersectionSelector from './components/IntersectionSelector';
import StrategySelector from './components/StrategySelector';
import ComparisonTable from './components/ComparisonTable';
import SimulationMap from './components/SimulationMap';
import AIConclusion from './components/AIConclusion';
import SignalPhaseViewer from './components/SignalPhaseViewer';
import PhaseComparison from './components/PhaseComparison';

const STRATEGY_LABELS: Record<string, string> = {
  genetic: '遗传算法',
  reinforcement: '强化学习',
  rule: '规则优化',
};

// 与后端采集器刷新节奏对齐的接收间隔（轮询接收实时交通事件）
const REFRESH_INTERVAL = 30000;

// 交通采集事件指标的中文名
const TRAFFIC_METRIC_LABELS: Record<string, string> = {
  flow: '车流量',
  speed: '平均车速',
  queue_length: '排队长度',
};

// 从 API 响应中健壮解包两层 data（AxiosResponse.data -> ApiResponse.data）
const unwrap = (res: any, fallback: any = null) => res?.data?.data ?? res?.data ?? fallback;

// /traffic/segments 暂未返回坐标，按路口编号补全真实经纬度（来源：intersections 表）
const INTERSECTION_COORDS: Record<string, [number, number]> = {
  'INT-001': [32.06, 118.79],
  'INT-002': [32.07, 118.78],
  'INT-003': [32.05, 118.8],
};

const TrafficSimulation: React.FC = () => {
  const traffic = useAppStore((s) => s.traffic);
  const setSelectedIntersection = useAppStore((s) => s.setSelectedIntersection);
  const setIntersections = useAppStore((s) => s.setIntersections);
  const setSimulationProgress = useAppStore((s) => s.setSimulationProgress);
  const setSimulationResult = useAppStore((s) => s.setSimulationResult);
  const resetSimulationStore = useAppStore((s) => s.resetSimulation);

  const [loading, setLoading] = useState(false);
  const [running, setRunning] = useState(false);
  const [currentStep, setCurrentStep] = useState(0);
  // 仿真响应（后端 /simulation/run 直接返回 OptimizationResult）
  const [localResult, setLocalResult] = useState<OptimizationResult | null>(null);
  // 本次仿真提交的“优化前”配时方案（与后端返回的 optimizedPhases 对照展示）
  const [beforePhases, setBeforePhases] = useState<SignalPhase[]>([]);

  // ---- 接收后端交通实时事件（city-collector 事件流，30s 轮询） ----
  const eventsQuery = useQuery({
    queryKey: ['traffic', 'recent-events'],
    queryFn: () => getRecentEvents(200).then((r) => unwrap(r, []) as CollectorEventRecord[]),
    refetchInterval: REFRESH_INTERVAL,
  });

  const trafficEvents = useMemo(
    () => (eventsQuery.data ?? []).filter((e) => e.domain === 'TRAFFIC'),
    [eventsQuery.data],
  );

  // 从接收到的 flow/speed/queue_length 事件汇总统计指标
  const trafficStats = useMemo(() => {
    const valuesOf = (metric: string) =>
      trafficEvents.filter((e) => e.metric === metric).map((e) => e.value);
    const avg = (arr: number[]) => (arr.length ? arr.reduce((a, b) => a + b, 0) / arr.length : 0);

    const avgFlow = Math.round(avg(valuesOf('flow')));
    const avgSpeed = +avg(valuesOf('speed')).toFixed(1);
    const avgQueue = Math.round(avg(valuesOf('queue_length')));
    // 拥堵指数 0~10：排队长度 500m 视为极端拥堵（与后端告警阈值 6/8 对齐）
    const congestionIndex = Math.min(10, +((avgQueue / 500) * 10).toFixed(1));
    const latestTs = trafficEvents.reduce<string | null>(
      (max, e) => (!max || e.ts > max ? e.ts : max),
      null,
    );

    return { count: trafficEvents.length, avgFlow, avgSpeed, avgQueue, congestionIndex, latestTs };
  }, [trafficEvents]);

  // 最新接收到的交通数据明细
  const latestEvents = useMemo(
    () =>
      [...trafficEvents]
        .sort((a, b) => (a.ts < b.ts ? 1 : -1))
        .slice(0, 8)
        .map((e) => ({
          key: e.eventId,
          time: dayjs(e.ts).format('MM-DD HH:mm:ss'),
          metric: TRAFFIC_METRIC_LABELS[e.metric] ?? e.metric,
          value: `${e.value} ${e.unit}`,
          source: e.source,
        })),
    [trafficEvents],
  );

  const latestEventColumns = [
    { title: '采集时间', dataIndex: 'time', key: 'time', width: 160 },
    { title: '指标', dataIndex: 'metric', key: 'metric', width: 120 },
    { title: '数值', dataIndex: 'value', key: 'value', width: 140 },
    { title: '数据来源', dataIndex: 'source', key: 'source' },
  ];

  const {
    intersections = [],
    selectedIntersection = null,
    strategy = 'genetic',
    simulationProgress = 0,
  } = traffic || {};

  useEffect(() => {
    fetchIntersections();
    // 路口流量取自后端滑动窗口，跟随采集节奏定时刷新
    const timer = setInterval(fetchIntersections, REFRESH_INTERVAL);
    return () => clearInterval(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const fetchIntersections = async () => {
    setLoading(true);
    try {
      const resp: any = await getTrafficSegments();
      // 兼容 axios 两层包裹：AxiosResponse.data -> ApiResponse.data
      let list = resp?.data?.data ?? resp?.data ?? resp ?? [];
      if (!Array.isArray(list)) list = [];
      // 后端返回 TrafficFlowData 时按 segmentId 去重转换为路口列表
      const seen = new Set<string>();
      const mapped: Intersection[] = [];
      for (const item of list) {
        const id = item.intersectionId || item.id || item.segmentId;
        if (!id || seen.has(id)) continue;
        seen.add(id);
        // 坐标优先取后端字段，缺失时按路口编号补全，保证地图标记分散展示
        const coords = INTERSECTION_COORDS[id];
        mapped.push({
          id,
          name: item.name || item.roadName || `路口 ${id}`,
          area: item.area || item.direction || '未分类',
          lat: item.lat ?? coords?.[0],
          lng: item.lng ?? coords?.[1],
          congestionLevel: item.congestionLevel || 'smooth',
          currentFlow: item.flow ?? item.currentFlow ?? 0,
          phases: item.phases || [],
        });
      }
      setIntersections(mapped);
      // 刷新后同步选中路口引用，保证详情卡中的流量/拥堵状态为最新
      const current = useAppStore.getState().traffic.selectedIntersection;
      if (current) {
        const fresh = mapped.find((i) => i.id === current.id);
        if (fresh) setSelectedIntersection(fresh);
      }
    } catch (error: any) {
      console.error('Failed to fetch intersections:', error);
      message.warning('获取路口数据失败，当前使用模拟数据');
      // 降级：使用模拟数据保证页面可用
      setIntersections([
        { id: 'INT-001', name: '中山北路-中央路交叉口', area: '鼓楼区', congestionLevel: 'moderate', currentFlow: 1200, phases: [] },
        { id: 'INT-002', name: '北京东路-太平北路交叉口', area: '玄武区', congestionLevel: 'smooth', currentFlow: 800, phases: [] },
        { id: 'INT-003', name: '汉中路-上海路交叉口', area: '秦淮区', congestionLevel: 'congested', currentFlow: 1800, phases: [] },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const runSimulation = useCallback(async () => {
    if (!selectedIntersection) {
      message.warning('请先选择路口');
      return;
    }

    setRunning(true);
    setCurrentStep(1);
    setLocalResult(null);
    setSimulationResult(null);
    setSimulationProgress(5);

    try {
      setCurrentStep(1);
      setSimulationProgress(15);
      await new Promise((r) => setTimeout(r, 500));

      setCurrentStep(2);
      setSimulationProgress(35);
      await new Promise((r) => setTimeout(r, 600));

      setCurrentStep(3);
      setSimulationProgress(60);

      // 规范化为标准相位结构：补全 phaseId 与缺省配时，作为“优化前”方案提交
      const phases: SignalPhase[] = (
        selectedIntersection.phases?.length
          ? selectedIntersection.phases
          : [
              { phaseId: 'P1', direction: 'NS', greenTime: 30, yellowTime: 3, redTime: 30 },
              { phaseId: 'P2', direction: 'EW', greenTime: 25, yellowTime: 3, redTime: 35 },
            ]
      ).map((p, idx) => ({
        phaseId: p.phaseId ?? `P${idx + 1}`,
        direction: p.direction,
        greenTime: p.greenTime,
        yellowTime: p.yellowTime ?? 3,
        redTime: p.redTime ?? 30,
      }));
      setBeforePhases(phases);

      const response: any = await runTrafficSimulation({
        intersectionId: selectedIntersection.id,
        phases,
        strategy,
      });

      setCurrentStep(4);
      setSimulationProgress(85);
      await new Promise((r) => setTimeout(r, 500));

      // 兼容 ApiResponse 与直接返回两种格式
      const payload = response?.data ?? response;

      setCurrentStep(5);
      setSimulationProgress(100);

      const result: SimulationResult = {
        totalWaitTime: payload?.afterSimulation?.totalWaitTime ?? payload?.totalWaitTime ?? 0,
        avgQueueLength: payload?.afterSimulation?.avgQueueLength ?? payload?.avgQueueLength ?? 0,
        throughput: payload?.afterSimulation?.throughput ?? payload?.throughput ?? 0,
        avgSpeed: payload?.afterSimulation?.avgSpeed ?? payload?.avgSpeed ?? 0,
        stopCount: payload?.afterSimulation?.stopCount ?? payload?.stopCount ?? 0,
      };

      setLocalResult(payload as OptimizationResult);
      setSimulationResult(result);
      message.success('仿真模拟完成');
    } catch (error: any) {
      console.error('Simulation failed:', error);
      message.error(error?.message || '仿真请求失败，请检查后端服务');
      setSimulationProgress(0);
      setCurrentStep(0);
    } finally {
      setRunning(false);
    }
  }, [selectedIntersection, strategy, setSimulationProgress, setSimulationResult]);

  // 构造优化前后指标对比
  const improvements = useMemo(() => {
    const before: any = localResult?.beforeSimulation;
    const after: any = localResult?.afterSimulation;
    if (!before || !after) return [];

    const make = (label: string, b: number, a: number, unit: string, lowerIsBetter: boolean) => {
      const diff = b - a;
      const pct = b !== 0 ? (diff / b) * 100 : 0;
      return {
        key: label,
        label,
        before: b,
        after: a,
        unit,
        improvementPercent: Math.abs(pct),
        isImproved: lowerIsBetter ? a <= b : a >= b,
      };
    };

    return [
      make('平均等待时间', before.totalWaitTime ?? 0, after.totalWaitTime ?? 0, 's', true),
      make('排队长度', before.avgQueueLength ?? 0, after.avgQueueLength ?? 0, 'm', true),
      make('通行量', before.throughput ?? 0, after.throughput ?? 0, '辆/h', false),
      make('平均车速', before.avgSpeed ?? 0, after.avgSpeed ?? 0, 'km/h', false),
      make('启停次数', before.stopCount ?? 0, after.stopCount ?? 0, '次', true),
    ];
  }, [localResult]);

  // SignalPhaseViewer 视图模型：以绿灯时长作为相位占空比，绿/红交替着色以区分相位
  const viewerPhases = useMemo(() => {
    const palette = ['green', 'red', 'yellow'];
    return (localResult?.optimizedPhases ?? []).map((p, idx) => ({
      ...p,
      name: `${p.direction} 方向放行`,
      duration: p.greenTime,
      color: palette[idx % palette.length],
    }));
  }, [localResult]);

  // 构造AI结论
  const aiConclusion = useMemo(() => {
    const payload: any = localResult;
    if (!payload) return null;
    const improved = improvements.filter((i) => i.isImproved).length;
    const strategyLabel = STRATEGY_LABELS[strategy] || strategy;
    return {
      confidenceScore: Math.min(60 + improvements.length * 8, 95),
      summary:
        `采用${strategyLabel}对路口 ${selectedIntersection?.name || ''} 进行信号配时优化。` +
        `优化后平均等待时间 ${payload.afterSimulation?.totalWaitTime ?? '-'}s，` +
        `排队长度 ${payload.afterSimulation?.avgQueueLength ?? '-'}m，` +
        `通行量 ${payload.afterSimulation?.throughput ?? '-'} 辆/h。` +
        (payload.improvementPercent != null
          ? `后端综合改善率 ${Number(payload.improvementPercent).toFixed(1)}%。`
          : '') +
        (improved > 0
          ? `共 ${improvements.length} 项指标中 ${improved} 项得到改善。`
          : '当前流量分布下优化收益有限，建议采集更多历史流量数据后重新仿真。'),
      keyFindings: improvements.map(
        (i) => `${i.label}: ${i.before.toFixed(1)} → ${i.after.toFixed(1)} ${i.unit}（${i.isImproved ? '改善' : '未改善'} ${i.improvementPercent.toFixed(1)}%）`,
      ),
      recommendations: [
        '建议在早晚高峰时段动态应用优化配时方案',
        '持续采集流量数据，按周滚动重新优化',
        '结合相邻路口进行绿波协调控制',
      ],
    };
  }, [localResult, improvements, strategy, selectedIntersection]);

  const handleReset = () => {
    setLocalResult(null);
    setBeforePhases([]);
    resetSimulationStore();
    setCurrentStep(0);
  };

  return (
    <div>
      {/* 实时交通数据统计（接收 city-collector 事件流后汇总） */}
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={12} sm={8} lg={4} style={{ display: 'flex' }}>
          <StatCard
            title="采样条数"
            value={trafficStats.count}
            unit="条"
            level="NORMAL"
            icon={<DatabaseOutlined />}
            tooltip="最近一轮从 city-collector 接收到的交通事件数量"
          />
        </Col>
        <Col xs={12} sm={8} lg={4} style={{ display: 'flex' }}>
          <StatCard
            title="平均流量"
            value={trafficStats.avgFlow}
            unit="辆/h"
            level={trafficStats.avgFlow > 1600 ? 'WARNING' : 'NORMAL'}
            icon={<CarOutlined />}
            tooltip="采集事件中 flow 指标的平均值"
          />
        </Col>
        <Col xs={12} sm={8} lg={4} style={{ display: 'flex' }}>
          <StatCard
            title="平均车速"
            value={trafficStats.avgSpeed}
            unit="km/h"
            level={trafficStats.avgSpeed < 20 ? 'DANGER' : trafficStats.avgSpeed < 30 ? 'WARNING' : 'NORMAL'}
            icon={<DashboardOutlined />}
            tooltip="采集事件中 speed 指标的平均值"
          />
        </Col>
        <Col xs={12} sm={8} lg={4} style={{ display: 'flex' }}>
          <StatCard
            title="平均排队长度"
            value={trafficStats.avgQueue}
            unit="m"
            level={trafficStats.avgQueue > 300 ? 'DANGER' : trafficStats.avgQueue > 150 ? 'WARNING' : 'NORMAL'}
            icon={<OrderedListOutlined />}
            tooltip="采集事件中 queue_length 指标的平均值"
          />
        </Col>
        <Col xs={12} sm={8} lg={4} style={{ display: 'flex' }}>
          <StatCard
            title="拥堵指数"
            value={trafficStats.congestionIndex}
            unit=""
            level={trafficStats.congestionIndex > 8 ? 'DANGER' : trafficStats.congestionIndex > 6 ? 'WARNING' : 'NORMAL'}
            icon={<ThunderboltOutlined />}
            tooltip="由平均排队长度换算（0~10，6 预警 / 8 危险）"
          />
        </Col>
        <Col xs={12} sm={8} lg={4} style={{ display: 'flex' }}>
          <StatCard
            title="最新采集时间"
            value={trafficStats.latestTs ? dayjs(trafficStats.latestTs).format('HH:mm:ss') : '--'}
            level="NORMAL"
            icon={<ClockCircleOutlined />}
            tooltip="最近一条交通事件的采集时间"
          />
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} md={12} lg={8} style={{ display: 'flex' }}>
          <Card title="选择路口" size="small" style={{ width: '100%' }}>
            <IntersectionSelector
              intersections={intersections}
              loading={loading}
              onSelect={(intersection) => setSelectedIntersection(intersection)}
            />
          </Card>
        </Col>
        <Col xs={24} md={12} lg={8} style={{ display: 'flex' }}>
          <Card title="优化策略" size="small" style={{ width: '100%' }}>
            <StrategySelector />
          </Card>
        </Col>
        <Col xs={24} md={24} lg={8} style={{ display: 'flex' }}>
          <Card title="仿真控制" size="small" style={{ width: '100%' }}>
            <Space direction="vertical" style={{ width: '100%' }} size={16}>
              <Space>
                <Button
                  type="primary"
                  icon={<PlayCircleOutlined />}
                  onClick={runSimulation}
                  loading={running}
                  disabled={!selectedIntersection}
                >
                  开始仿真
                </Button>
                <Button icon={<ReloadOutlined />} onClick={handleReset} disabled={running}>
                  重置
                </Button>
              </Space>
              <Progress
                percent={simulationProgress}
                status={running ? 'active' : simulationProgress === 100 ? 'success' : 'normal'}
              />
              <Steps
                direction="vertical"
                size="small"
                current={currentStep}
                items={[
                  { title: '初始化', icon: <ClockCircleOutlined /> },
                  { title: '数据采集', icon: <SyncOutlined /> },
                  { title: '运行仿真', icon: <PlayCircleOutlined /> },
                  { title: 'AI分析', icon: <RobotOutlined /> },
                  { title: '完成', icon: <CheckCircleOutlined /> },
                ]}
              />
            </Space>
          </Card>
        </Col>
      </Row>

      {improvements.length > 0 && (
        <>
          <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
            <Col xs={24} lg={12} style={{ display: 'flex' }}>
              <Card title="优化前后对比" size="small" style={{ width: '100%' }}>
                <ComparisonTable improvements={improvements} />
              </Card>
            </Col>
            <Col xs={24} lg={12} style={{ display: 'flex' }}>
              <AIConclusion conclusion={aiConclusion} />
            </Col>
          </Row>
          {/* 信号配时方案：本次提交的“优化前”配时 vs 后端优化输出 */}
          {localResult && beforePhases.length > 0 && (
            <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
              <Col xs={24} lg={14} style={{ display: 'flex' }}>
                <Card
                  title="信号配时方案对比"
                  size="small"
                  style={{ width: '100%' }}
                  extra={
                    localResult.improvementPercent != null ? (
                      <Tag color={Number(localResult.improvementPercent) >= 0 ? 'green' : 'red'}>
                        综合改善 {Number(localResult.improvementPercent).toFixed(1)}%
                      </Tag>
                    ) : null
                  }
                >
                  <PhaseComparison
                    before={beforePhases}
                    after={localResult.optimizedPhases ?? []}
                  />
                </Card>
              </Col>
              <Col xs={24} lg={10} style={{ display: 'flex' }}>
                <SignalPhaseViewer
                  title="优化后信号相位"
                  phases={viewerPhases}
                  isSimulating={!running}
                />
              </Col>
            </Row>
          )}
        </>
      )}

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col span={24}>
          <SimulationMap
            intersections={intersections}
            selectedIntersection={selectedIntersection}
            onIntersectionClick={(i) => setSelectedIntersection(i)}
            loading={loading}
            height={420}
          />
        </Col>
      </Row>

      {/* 最新接收的交通数据明细 */}
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col span={24}>
          <Card
            title="最新交通采集数据"
            size="small"
            extra={
              <Tag color={trafficStats.count > 0 ? 'green' : 'orange'}>
                {trafficStats.count > 0 ? '后端实时数据' : '暂无实时数据'}
              </Tag>
            }
          >
            <Table
              dataSource={latestEvents}
              columns={latestEventColumns}
              rowKey="key"
              size="small"
              pagination={false}
              locale={{ emptyText: '暂未接收到交通事件，请确认 city-collector 服务运行中' }}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default TrafficSimulation;
