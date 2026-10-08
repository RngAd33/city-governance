// ============================================================
// EnvironmentMonitor - Environment monitoring page
// ============================================================
import React, { useEffect, useState, useCallback } from 'react';
import { Row, Col, Card, Spin, message } from 'antd';
import MetricSelector from './components/MetricSelector';
import StationMap from './components/StationMap';
import StationTable from './components/StationTable';
import TrendChart from './components/TrendChart';
import ReplaySelector from './components/ReplaySelector';
import { getEnvironmentLatest, getEnvironmentHistory } from '@/api/dashboard';
import type { EnvironmentRecord } from '@/types';

// 环境数据服务当前仅提供南京市数据（与采集器模拟数据一致）
const CITY = '南京';

// 时间范围选项与后端 hours 参数映射（与 TrendChart 的 24小时/7天/30天切换联动）
type TimeRange = '24h' | '7d' | '30d';
const RANGE_HOURS: Record<TimeRange, number> = { '24h': 24, '7d': 168, '30d': 720 };

// 兼容后端裸数组与 {code,message,data} 包装两种响应结构
const unwrap = (res: any, fallback: any = null) => res?.data?.data ?? res?.data ?? fallback;

const EnvironmentMonitor: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [metric, setMetric] = useState('PM2.5');
  const [timeRange, setTimeRange] = useState<TimeRange>('24h');
  const [records, setRecords] = useState<EnvironmentRecord[]>([]);
  const [trendData, setTrendData] = useState<EnvironmentRecord[]>([]);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [latestRes, historyRes] = await Promise.all([
        getEnvironmentLatest(CITY),
        getEnvironmentHistory(CITY, metric, RANGE_HOURS[timeRange]),
      ]);
      setRecords(unwrap(latestRes, []));
      setTrendData(unwrap(historyRes, []));
    } catch (err) {
      message.error('环境数据加载失败');
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, [metric, timeRange]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleReplay = async (date: string) => {
    setLoading(true);
    try {
      const res = await getEnvironmentHistory(CITY, metric, RANGE_HOURS[timeRange]);
      setTrendData(unwrap(res, []));
      message.success(`已加载 ${date} 的历史数据`);
    } catch (err) {
      message.error('历史数据加载失败');
    } finally {
      setLoading(false);
    }
  };

  // Deduplicate stations for map display
  const stationMapData = records.reduce<EnvironmentRecord[]>((acc, curr) => {
    if (!acc.find((r) => r.stationName === curr.stationName)) {
      acc.push(curr);
    }
    return acc;
  }, []);

  return (
    <Spin spinning={loading}>
      <div style={{ padding: 24 }}>
        <h2 style={{ marginBottom: 24, fontSize: 20, fontWeight: 600 }}>
          环境监测
        </h2>

        {/* Metric selector */}
        <MetricSelector value={metric} onChange={setMetric} />

        {/* Large trend chart */}
        <Card style={{ marginBottom: 16, borderRadius: 8 }}>
          <TrendChart
            data={trendData}
            metric={metric}
            title={`${metric} 趋势`}
            timeRange={timeRange}
            onTimeRangeChange={setTimeRange}
          />
        </Card>

        {/* Bottom row: Station map (left) + Station table (right) */}
        <Row gutter={[16, 16]}>
          <Col xs={24} lg={10}>
            <Card
              title="站点分布"
              style={{ borderRadius: 8 }}
              styles={{ body: { padding: 12 } }}
            >
              <StationMap stations={stationMapData} />
            </Card>
          </Col>
          <Col xs={24} lg={14}>
            <Card
              title="站点数据"
              style={{ borderRadius: 8 }}
              styles={{ body: { padding: 12 } }}
            >
              <ReplaySelector onReplay={handleReplay} loading={loading} />
              <StationTable data={records} loading={loading} />
            </Card>
          </Col>
        </Row>
      </div>
    </Spin>
  );
};

export default EnvironmentMonitor;
