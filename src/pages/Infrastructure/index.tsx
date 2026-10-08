// ============================================================
// InfrastructureOps - 基础设施运维管理页
// 设施实时状态与趋势来自 city-infra 服务（RabbitMQ 实时入库）；
// 工单列表来自 city-infra 工单库（work_orders 表，南京地区运维工单）
// ============================================================
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Card, Row, Col, Statistic, Tag, Alert, Space, Spin } from 'antd';
import {
  CheckCircleOutlined,
  WarningOutlined,
  ExclamationCircleOutlined,
  ToolOutlined,
} from '@ant-design/icons';
import InfraStatusPanel, { type InfraFacilityStatus } from './components/InfraStatusPanel';
import WorkOrderList from './components/WorkOrderList';
import TrendChart from '@/components/Common/TrendChart';
import { getInfraStatus, getInfraHistory, getInfraWorkOrders } from '@/api/dashboard';
import type { WorkOrder } from '@/types';

// 工单状态映射：后端 work_orders 表枚举（大写下划线）→ 前端视图模型
const WORK_ORDER_STATUS_MAP: Record<string, WorkOrder['status']> = {
  PENDING: 'pending',
  IN_PROGRESS: 'inProgress',
  COMPLETED: 'completed',
  CANCELLED: 'cancelled',
};

// 工单优先级映射：LOW/MEDIUM/HIGH/URGENT → 前端视图模型
const WORK_ORDER_PRIORITY_MAP: Record<string, WorkOrder['priority']> = {
  LOW: 'low',
  MEDIUM: 'medium',
  HIGH: 'high',
  URGENT: 'urgent',
};

// progress 字段缺失时按状态推断的兜底进度
const PROGRESS_FALLBACK: Record<WorkOrder['status'], number> = {
  pending: 0,
  inProgress: 50,
  completed: 100,
  cancelled: 0,
};

// 后端工单实体 → 前端 WorkOrder 视图模型（字段缺失兜底，避免脏数据导致渲染异常）
const mapWorkOrder = (raw: Record<string, unknown>): WorkOrder => {
  const status = WORK_ORDER_STATUS_MAP[String(raw.status ?? '').toUpperCase()] ?? 'pending';
  const priority = WORK_ORDER_PRIORITY_MAP[String(raw.priority ?? '').toUpperCase()] ?? 'medium';
  const progress = raw.progress != null ? Number(raw.progress) : PROGRESS_FALLBACK[status];
  return {
    id: String(raw.orderId ?? raw.id ?? ''),
    title: String(raw.title ?? ''),
    status,
    priority,
    department: String(raw.assignedDept ?? '--'),
    assigner: String(raw.assignedPerson ?? '--'),
    progress: Number.isFinite(progress) ? progress : PROGRESS_FALLBACK[status],
    dueDate: raw.dueDate != null ? String(raw.dueDate) : '',
    createdAt: raw.createdAt != null ? String(raw.createdAt) : '',
  };
};

// 趋势范围与后端 hours 参数映射（与 TrendChart 的 24小时/7天/30天切换联动）
const RANGE_HOURS: Record<string, number> = { '24h': 24, '7d': 168, '30d': 720 };

interface InfraSummary {
  facilityCount: number;
  avgScore: number | null;
  normalCount: number;
  attentionCount: number;
  warningCount: number;
  dangerCount: number;
  lastUpdateTime: string | null;
}

const formatTrendTime = (ts: string, rangeKey: string) => {
  const d = new Date(ts);
  if (Number.isNaN(d.getTime())) return ts;
  const pad = (n: number) => String(n).padStart(2, '0');
  if (rangeKey === '30d') return `${d.getMonth() + 1}-${pad(d.getDate())}`;
  if (rangeKey === '7d') return `${d.getMonth() + 1}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

const InfrastructureOps: React.FC = () => {
  const [facilities, setFacilities] = useState<InfraFacilityStatus[]>([]);
  const [summary, setSummary] = useState<InfraSummary | null>(null);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState('bridge');
  const [range, setRange] = useState('24h');
  const [trend, setTrend] = useState<Array<{ time: string; value: number }>>([]);
  const [trendLoading, setTrendLoading] = useState(false);
  const [workOrders, setWorkOrders] = useState<WorkOrder[]>([]);

  const loadStatus = useCallback(async () => {
    setLoading(true);
    try {
      const resp = await getInfraStatus();
      const body = ((resp as any)?.data ?? {}) as { facilities?: InfraFacilityStatus[]; summary?: InfraSummary };
      setFacilities(body.facilities ?? []);
      setSummary(body.summary ?? null);
    } catch {
      // 服务未启动/暂无数据时保持空态展示
    } finally {
      setLoading(false);
    }
  }, []);

  const loadWorkOrders = useCallback(async () => {
    try {
      const resp = await getInfraWorkOrders();
      const list = ((resp as any)?.data ?? []) as Record<string, unknown>[];
      setWorkOrders(Array.isArray(list) ? list.map(mapWorkOrder) : []);
    } catch {
      // 接口未就绪/服务未启动时保持空列表，不使用模拟数据
      setWorkOrders([]);
    }
  }, []);

  const loadTrend = useCallback(async (facility: string, rangeKey: string) => {
    setTrendLoading(true);
    try {
      const resp = await getInfraHistory(facility, RANGE_HOURS[rangeKey] ?? 24);
      const list = (((resp as any)?.data ?? []) as Array<{ value: number; timestamp: string }>);
      // 数据点过多时抽样，避免图表卡顿
      const step = Math.max(1, Math.ceil(list.length / 200));
      const points = list
        .filter((_, i) => i % step === 0)
        .map((r) => ({ time: formatTrendTime(r.timestamp, rangeKey), value: Number(Number(r.value).toFixed(1)) }));
      setTrend(points);
    } catch {
      setTrend([]);
    } finally {
      setTrendLoading(false);
    }
  }, []);

  useEffect(() => {
    loadStatus();
    loadWorkOrders();
  }, [loadStatus, loadWorkOrders]);

  useEffect(() => {
    loadTrend(selected, range);
  }, [loadTrend, selected, range]);

  // 每30秒刷新一次状态与工单（采集端每10秒上报一轮）
  useEffect(() => {
    const timer = setInterval(() => {
      loadStatus();
      loadWorkOrders();
    }, 30000);
    return () => clearInterval(timer);
  }, [loadStatus, loadWorkOrders]);

  const criticalFacilities = useMemo(
    () => facilities.filter((f) => f.alertLevel != null && f.alertLevel >= 2),
    [facilities]
  );

  const selectedName = facilities.find((f) => f.facility === selected)?.facilityName ?? selected;
  const alertCount = (summary?.warningCount ?? 0) + (summary?.dangerCount ?? 0);

  return (
    <div style={{ padding: 24, background: '#f0f2f5', minHeight: '100%' }}>
      {/* 页头 */}
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h2 style={{ margin: 0, fontSize: 20, fontWeight: 600 }}>基础设施运维</h2>
          <span style={{ color: '#8c8c8c', fontSize: 13 }}>城市基础设施实时状态监控（数据经采集器实时上报）</span>
        </div>
        <Space>
          <Tag color="blue">南京市 · 实时监控</Tag>
          {loading && <Spin size="small" />}
        </Space>
      </div>

      {/* 告警面板 */}
      {criticalFacilities.length > 0 && (
        <Alert
          message={
            <span>
              <ExclamationCircleOutlined style={{ color: '#ff4d4f', marginRight: 8 }} />
              <strong>设施告警：</strong>
              {criticalFacilities.map((f) => f.facilityName).join('、')} 当前处于告警状态，请关注处理。
            </span>
          }
          type="error"
          showIcon
          closable
          style={{ marginBottom: 16 }}
        />
      )}

      {/* 总览统计卡 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={12} sm={6}>
          <Card size="small">
            <Statistic title="设施类别" value={summary?.facilityCount ?? 0} prefix={<ToolOutlined />} valueStyle={{ color: '#1890ff' }} />
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card size="small">
            <Statistic title="平均状态评分" value={summary?.avgScore ?? '--'} suffix="分" prefix={<CheckCircleOutlined />} valueStyle={{ color: '#52c41a' }} />
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card size="small">
            <Statistic title="告警设施" value={alertCount} prefix={<WarningOutlined />} valueStyle={{ color: '#faad14' }} />
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card size="small">
            <Statistic title="严重告警" value={summary?.dangerCount ?? 0} prefix={<ExclamationCircleOutlined />} valueStyle={{ color: '#ff4d4f' }} />
          </Card>
        </Col>
      </Row>

      {/* 设施状态 + 工单 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} lg={16}>
          <Card
            title="设施实时状态"
            size="small"
            style={{ height: '100%' }}
            extra={<span style={{ fontSize: 12, color: '#8c8c8c' }}>点击设施卡片查看趋势</span>}
          >
            <InfraStatusPanel facilities={facilities} selected={selected} onSelect={setSelected} />
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card
            title="工单列表"
            size="small"
            style={{ height: '100%' }}
            extra={
              <Tag color={workOrders.length > 0 ? 'green' : 'default'} style={{ marginInlineEnd: 0 }}>
                {workOrders.length > 0 ? `后端实时 · ${workOrders.length} 单` : '暂无数据'}
              </Tag>
            }
          >
            <WorkOrderList workOrders={workOrders} />
          </Card>
        </Col>
      </Row>

      {/* 状态趋势（图表保持挂载：加载中仅遮罩，避免范围选中态被重置） */}
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24}>
          <TrendChart
            title={`状态评分趋势 · ${selectedName}`}
            data={trend}
            unit="分"
            color="#1890ff"
            showRange
            range={range}
            loading={trendLoading}
            onRangeChange={setRange}
          />
        </Col>
      </Row>

      {/* 数据说明 */}
      <Row gutter={[16, 16]}>
        <Col xs={24}>
          <Card size="small">
            <div style={{ fontSize: 12, color: '#8c8c8c', lineHeight: 1.8 }}>
              数据说明：设施状态评分（0-100）与告警级别（0=正常/1=注意/2=警告/3=严重）由 city-collector 采集端每10秒上报一轮，
              经消息队列（infra.monitor.queue）实时入库展示，页面每30秒自动刷新；趋势图为设施历史状态评分序列。
              工单列表来自 city-infra 工单库（work_orders 表，南京地区运维工单，按创建时间倒序）。
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default InfrastructureOps;
