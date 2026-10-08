import React, { useState, useEffect, useRef } from 'react';
import {
  Layout,
  Card,
  Select,
  Row,
  Col,
  Statistic,
  message,
} from 'antd';
import {
  ThunderboltOutlined,

  EnvironmentOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import useAppStore from '@/stores/appStore';
import { getEnergyBuildings, getSavingPotential, getBuildingConsumptionHistory } from '@/api/dashboard';
import type { Building, BuildingConsumptionBucket, SavingPotentialResult, SavingRecommendation } from '@/types';
import BuildingRanking from './components/BuildingRanking';
import EnergyTrendChart from './components/EnergyTrend';
import type { EnergyTimeRange, EnergyTrendSeries } from './components/EnergyTrend';
import HighConsumptionList from './components/HighConsumptionList';
import SavingRecommendations from './components/SavingRecommendations';
import BuildingDetailModal from './components/BuildingDetailModal';
import GridIndicator from './components/GridIndicator';

const { Content } = Layout;
const { Option } = Select;

const timeRangeOptions = [
  { value: 'day', label: '今日' },
  { value: 'week', label: '本周' },
  { value: 'month', label: '本月' },
  { value: 'year', label: '本年' },
];

/** 能耗趋势时间范围 → 查询参数（增大桶粒度控制点位：24/28/30/365 个桶） */
const TREND_QUERY: Record<EnergyTimeRange, { hours: number; bucketHours: number }> = {
  day: { hours: 24, bucketHours: 1 },
  week: { hours: 168, bucketHours: 6 },
  month: { hours: 720, bucketHours: 24 },
  year: { hours: 8760, bucketHours: 24 },
};

const EnergyMonitor: React.FC = () => {
  const { setTimeRange } = useAppStore();
  const [buildings, setBuildings] = useState<Building[]>([]);
  const [loading, setLoading] = useState(false);
  const [showAverage, setShowAverage] = useState(true);
  const [selectedBuilding, setSelectedBuilding] = useState<string | null>(null);
  const [savingRecommendations, setSavingRecommendations] = useState<SavingRecommendation[]>([]);
  const [savingLoading, setSavingLoading] = useState(false);
  const [trendRange, setTrendRange] = useState<EnergyTimeRange>('day');
  const [trendSeries, setTrendSeries] = useState<EnergyTrendSeries[]>([]);
  const [trendLoading, setTrendLoading] = useState(false);
  const trendReqIdRef = useRef(0);

  useEffect(() => {
    loadBuildings();
  }, []);

  // 节电潜力 → 节能建议视图模型（节电潜力越大优先级越高）
  const toSavingRecommendation = (b: Building, pot: SavingPotentialResult, index: number): SavingRecommendation => ({
    id: `${b.buildingId}-${index}`,
    title: `${b.name} · 节能建议`,
    description: pot.recommendations[0] ?? '暂无具体建议',
    buildingName: b.buildingId,
    priority: pot.estimatedSavingKwh >= 200000 ? 'high' : pot.estimatedSavingKwh >= 100000 ? 'medium' : 'low',
    estimatedPayback: '待评估',
    savingPotential: {
      kwh: pot.estimatedSavingKwh,
      cost: pot.estimatedSavingCost,
      carbon: pot.estimatedCarbonReduction,
    },
    implementationDifficulty: 'moderate',
    details: pot.recommendations.join('；'),
  });

  // 并行聚合各建筑节电潜力（后端无批量接口，逐个请求；单个失败静默跳过，不阻断其他建筑）
  const loadSavingRecommendations = async (list: Building[]) => {
    if (list.length === 0) {
      setSavingRecommendations([]);
      return;
    }
    setSavingLoading(true);
    try {
      const results = await Promise.all(
        list.map(async (b, index) => {
          try {
            const resp = await getSavingPotential(b.buildingId);
            const pot = ((resp as any)?.data ?? null) as SavingPotentialResult | null;
            return pot ? toSavingRecommendation(b, pot, index) : null;
          } catch {
            return null;
          }
        }),
      );
      setSavingRecommendations(results.filter((r): r is SavingRecommendation => r !== null));
    } finally {
      setSavingLoading(false);
    }
  };

  const loadBuildings = async () => {
    setLoading(true);
    try {
      const resp = await getEnergyBuildings();
      const data = ((resp as any)?.data ?? []) as Building[];
      setBuildings(data);
      // 建筑数据就绪后聚合节电潜力（依赖建筑列表的 buildingId，仅调用 B001-B008 这类后端有效 ID）
      loadSavingRecommendations(data);
    } catch (error) {
      message.error('加载建筑数据失败');
    } finally {
      setLoading(false);
    }
  };

  // 并行取各建筑能耗历史（后端按桶聚合 total_kwh 均值）；请求序号防竞态：快速切范围时丢弃过期响应
  const loadTrend = async (list: Building[], rangeKey: EnergyTimeRange) => {
    if (list.length === 0) {
      setTrendSeries([]);
      return;
    }
    const reqId = ++trendReqIdRef.current;
    setTrendLoading(true);
    try {
      const { hours, bucketHours } = TREND_QUERY[rangeKey];
      const results = await Promise.all(
        list.map(async (b) => {
          try {
            const resp = await getBuildingConsumptionHistory(b.buildingId, hours, bucketHours);
            const buckets = ((resp as any)?.data ?? []) as BuildingConsumptionBucket[];
            return {
              buildingName: b.name,
              points: buckets.map((x) => ({ ts: new Date(x.bucketTime).getTime(), value: x.avgTotalKwh })),
            };
          } catch {
            return { buildingName: b.name, points: [] };
          }
        }),
      );
      if (reqId !== trendReqIdRef.current) return;
      setTrendSeries(results);
    } finally {
      if (reqId === trendReqIdRef.current) setTrendLoading(false);
    }
  };

  // 建筑列表或时间范围变化时重取趋势（buildings 初始为空时不发起请求）
  useEffect(() => {
    loadTrend(buildings, trendRange);
  }, [buildings, trendRange]);

  const totalConsumption = buildings.reduce((sum, b) => sum + (b.avgDailyConsumption || 0), 0);
  const avgConsumption = buildings.length > 0 ? totalConsumption / buildings.length : 0;
  const highConsumptionCount = buildings.filter((b) => (b.avgDailyConsumption || 0) > 500).length;

  return (
    <Content style={{ padding: 24 }}>
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <GridIndicator />
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="建筑总数"
              value={buildings.length}
              prefix={<EnvironmentOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="总日均能耗"
              value={totalConsumption.toFixed(0)}
              suffix="kWh"
              prefix={<ThunderboltOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="平均能耗"
              value={avgConsumption.toFixed(1)}
              suffix="kWh"
              prefix={<ThunderboltOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="高能耗建筑"
              value={highConsumptionCount}
              prefix={<WarningOutlined />}
              valueStyle={{ color: highConsumptionCount > 0 ? '#ff4d4f' : '#52c41a' }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={12}>
          <Card title="建筑能耗排行" extra={
            <Select defaultValue="day" style={{ width: 100 }} onChange={(v) => setTimeRange(v as any)}>
              {timeRangeOptions.map((opt) => (
                <Option key={opt.value} value={opt.value}>{opt.label}</Option>
              ))}
            </Select>
          }>
            <BuildingRanking
              data={buildings}
              loading={loading}
              onBuildingClick={(b) => setSelectedBuilding(b.buildingId)}
            />
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <EnergyTrendChart
            series={trendSeries}
            loading={trendLoading}
            timeRange={trendRange}
            onTimeRangeChange={setTrendRange}
            showAverage={showAverage}
            onShowAverageChange={setShowAverage}
          />
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={12}>
          <Card title="高能耗建筑列表">
            <HighConsumptionList
              data={buildings}
              loading={loading}
              onBuildingClick={(b) => setSelectedBuilding(b.buildingId)}
            />
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="节能建议">
            <SavingRecommendations data={savingRecommendations} loading={savingLoading} />
          </Card>
        </Col>
      </Row>

      <BuildingDetailModal
        fetchDetail={async (id) => buildings.find((b) => b.buildingId === id) ?? null}
        open={!!selectedBuilding}
        buildingId={selectedBuilding}
        onClose={() => setSelectedBuilding(null)}
      />
    </Content>
  );
};

export default EnergyMonitor;
