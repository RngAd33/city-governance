import React, { useCallback, useEffect, useState } from 'react';
import { Card, Row, Col, Statistic, Tag, Spin, Empty } from 'antd';
import { GlobalOutlined, ThunderboltOutlined } from '@ant-design/icons';
import { getGridLatest } from '@/api/dashboard';

interface GridRecord {
  id: number;
  zone: string;
  metric: string;
  value: number;
  unit: string;
  estimated?: boolean;
  source?: string;
  ts: string;
}

// 指标展示元数据
const METRIC_META: Record<string, { label: string; precision: number; color: string }> = {
  grid_carbon_intensity: { label: '电网碳强度', precision: 1, color: '#fa8c16' },
  grid_renewable_percentage: { label: '可再生占比', precision: 1, color: '#52c41a' },
};

const METRIC_ORDER = ['grid_carbon_intensity', 'grid_renewable_percentage'];

const formatYear = (ts: string) => {
  const d = new Date(ts);
  return Number.isNaN(d.getTime()) ? ts : `${d.getFullYear()}年`;
};

/**
 * 电网指标卡片区（国内免费公开真实数据）
 * 数据源 Our World in Data（Ember），中国年度碳强度与可再生占比（全国口径）
 */
const GridIndicator: React.FC = () => {
  const [latest, setLatest] = useState<Record<string, GridRecord>>({});
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const resp = await getGridLatest('CN');
      const list = ((resp as any)?.data ?? []) as GridRecord[];
      const map: Record<string, GridRecord> = {};
      list.forEach((r) => {
        map[r.metric] = r;
      });
      setLatest(map);
    } catch {
      // 服务未启动/暂无数据时保持空态展示
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  // 定时刷新（每30分钟；年度数据更新频率低，刷新压力可忽略）
  useEffect(() => {
    const timer = setInterval(load, 1800000);
    return () => clearInterval(timer);
  }, [load]);

  const hasData = METRIC_ORDER.some((m) => latest[m]);
  const carbon = latest.grid_carbon_intensity;

  return (
    <Card
      title={
        <span>
          <GlobalOutlined style={{ marginRight: 8, color: '#fa8c16' }} />
          电网指标（国内 · 年度）
        </span>
      }
      extra={<Tag color="blue">OWID/Ember · 全国口径</Tag>}
      bodyStyle={{ paddingTop: 12 }}
    >
      {loading && !hasData ? (
        <div style={{ display: 'flex', justifyContent: 'center', padding: 40 }}>
          <Spin tip="加载电网数据..." />
        </div>
      ) : !hasData ? (
        <Empty description="暂无电网数据：采集器每24小时同步一次国内年度数据" />
      ) : (
        <>
          <Row gutter={[16, 16]}>
            {METRIC_ORDER.map((metric) => {
              const meta = METRIC_META[metric];
              const record = latest[metric];
              return (
                <Col xs={24} sm={12} lg={12} key={metric}>
                  <Statistic
                    title={meta.label}
                    value={record ? Number(record.value.toFixed(meta.precision)) : '--'}
                    suffix={record ? record.unit : ''}
                    prefix={<ThunderboltOutlined style={{ color: meta.color }} />}
                    valueStyle={{ color: meta.color }}
                  />
                  {record && (
                    <div style={{ marginTop: 4, fontSize: 12, color: '#8c8c8c' }}>
                      数据年份：{formatYear(record.ts)}
                    </div>
                  )}
                </Col>
              );
            })}
          </Row>
          <div style={{ marginTop: 12, fontSize: 12, color: '#8c8c8c' }}>
            数据说明：国内暂无公开免费的省级（江苏）电力数据源，此处采用全国口径年度统计
            {carbon ? `（${formatYear(carbon.ts)}）` : ''}作为参考；平台环境数据（气象/空气质量）均为南京本地实测。
            来源：Our World in Data（Ember 数据源），每24小时同步一次。
          </div>
        </>
      )}
    </Card>
  );
};

export default GridIndicator;
