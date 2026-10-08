// ============================================================
// ReportCenter - Report generation and management page
// 数据来源：city-govai 报告模块（/api/govai/report*，经 Vite 代理 /api/reports）
// ============================================================
import React, { useCallback, useEffect, useState } from 'react';
import { Card, Row, Col, Select, Button, List, Tag, Space, Modal, Form, Input, Spin, Alert, message } from 'antd';
import {
  FileTextOutlined,
  PlusOutlined,
  EyeOutlined,
  FilePdfOutlined,
  CalendarOutlined,
} from '@ant-design/icons';
import ReportPreview from './components/ReportPreview';
import { downloadReportPdf } from './pdfExport';
import useAppStore from '@/stores/appStore';
import { fetchReportList, fetchReportDetail, generateReport } from '@/api/dashboard';
import { formatDateTime } from '@/utils/format';
import type { Report, GovaiReport } from '@/types';

const REPORT_TYPES = [
  { value: 'daily', label: '日报', color: 'processing' },
  { value: 'weekly', label: '周报', color: 'success' },
  { value: 'briefing', label: '简报', color: 'warning' },
  { value: 'risk', label: '风险提示', color: 'error' },
];

// 列表项图标配色
const TYPE_THEME: Record<Report['type'], { bg: string; color: string }> = {
  daily: { bg: '#e6f7ff', color: '#1890ff' },
  weekly: { bg: '#f6ffed', color: '#52c41a' },
  briefing: { bg: '#fffbe6', color: '#faad14' },
  risk: { bg: '#fff1f0', color: '#ff4d4f' },
};

// 前端类型值 ↔ 后端枚举（city-govai 支持 DAILY/WEEKLY/BRIEF/RISK_ALERT，或中文 日报/周报/简报/风险提示）
const TYPE_TO_BACKEND: Record<string, string> = {
  daily: 'DAILY',
  weekly: 'WEEKLY',
  briefing: 'BRIEF',
  risk: 'RISK_ALERT',
};

const BACKEND_TYPE_TO_UI: Record<string, Report['type']> = {
  DAILY: 'daily',
  WEEKLY: 'weekly',
  BRIEF: 'briefing',
  RISK_ALERT: 'risk',
};

// 后端状态原值 → 展示文案与配色
const STATUS_META: Record<string, { label: string; color: string }> = {
  COMPLETED: { label: '已完成', color: 'success' },
  GENERATING: { label: '生成中', color: 'processing' },
  FAILED: { label: '生成失败', color: 'error' },
};

// 后端报告 → 前端视图模型（数据入口显式映射）
const toViewReport = (raw: GovaiReport): Report => ({
  id: raw.id,
  title: raw.title,
  type: BACKEND_TYPE_TO_UI[raw.typeCode] ?? 'daily',
  typeLabel: raw.type,
  city: raw.city,
  period: raw.period,
  author: raw.author,
  status: raw.status,
  createdAt: raw.createdAt,
  content: raw.content ?? '',
});

const ReportCenter: React.FC = () => {
  const { city } = useAppStore();
  const [selectedType, setSelectedType] = useState<string>('all');
  const [reports, setReports] = useState<Report[]>([]);
  const [listLoading, setListLoading] = useState(false);
  const [listError, setListError] = useState<string | null>(null);
  const [selectedReport, setSelectedReport] = useState<Report | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [generateVisible, setGenerateVisible] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [exportingId, setExportingId] = useState<number | null>(null);
  const [form] = Form.useForm();

  // 报告列表：类型筛选联动（全部=不传 type，具体类型由后端筛选）
  const loadReports = useCallback(async (typeKey: string) => {
    setListLoading(true);
    setListError(null);
    try {
      const resp = await fetchReportList(typeKey === 'all' ? undefined : TYPE_TO_BACKEND[typeKey]);
      setReports((resp.data?.reports ?? []).map(toViewReport));
    } catch (err) {
      setReports([]);
      setListError(err instanceof Error ? err.message : '报告列表加载失败，请确认后端服务可用');
    } finally {
      setListLoading(false);
    }
  }, []);

  useEffect(() => {
    loadReports(selectedType);
  }, [loadReports, selectedType]);

  // 列表项不含正文：点击后拉取详情
  const handleViewReport = useCallback(async (report: Report) => {
    setSelectedReport(null);
    setDetailLoading(true);
    try {
      const resp = await fetchReportDetail(report.id);
      setSelectedReport(toViewReport(resp.data));
    } catch (err) {
      message.error(err instanceof Error ? err.message : '报告正文加载失败');
    } finally {
      setDetailLoading(false);
    }
  }, []);

  // 生成：后端按实时数据生成并入库，完成后直接进入预览并刷新列表
  const handleGenerateReport = () => {
    form.validateFields().then(async (values: { type: string }) => {
      setGenerating(true);
      try {
        const resp = await generateReport(TYPE_TO_BACKEND[values.type] ?? values.type, city);
        const created = toViewReport(resp.data);
        message.success(`《${created.title}》生成完成`);
        setGenerateVisible(false);
        form.resetFields();
        setSelectedReport(created);
        await loadReports(selectedType);
      } catch (err) {
        message.error(err instanceof Error ? err.message : '报告生成失败，请稍后重试');
      } finally {
        setGenerating(false);
      }
    });
  };

  // 列表行导出 PDF：下载期间该行按钮 loading，避免重复点击
  const handleExportPdf = async (report: Report) => {
    setExportingId(report.id);
    try {
      await downloadReportPdf(report);
    } finally {
      setExportingId(null);
    }
  };

  return (
    <div style={{ padding: 24, background: '#f0f2f5', minHeight: '100%' }}>
      {/* Header */}
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h2 style={{ margin: 0, fontSize: 20, fontWeight: 600 }}>报告中心</h2>
          <span style={{ color: '#8c8c8c', fontSize: 13 }}>生成、管理和导出城市治理报告</span>
        </div>
        <Space>
          <Select value={city} style={{ width: 120 }} options={[{ label: city, value: city }]} />
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setGenerateVisible(true)}>
            生成报告
          </Button>
        </Space>
      </div>

      {/* Report Type Selector */}
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24}>
          <Card size="small">
            <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'center' }}>
              <span style={{ fontSize: 13, color: '#8c8c8c' }}>报告类型:</span>
              <Select
                value={selectedType}
                onChange={setSelectedType}
                style={{ width: 140 }}
                options={[
                  { label: '全部', value: 'all' },
                  ...REPORT_TYPES.map((t) => ({ label: t.label, value: t.value })),
                ]}
              />
              <span style={{ marginLeft: 'auto', fontSize: 13, color: '#8c8c8c' }}>
                共 {reports.length} 份报告
              </span>
            </div>
          </Card>
        </Col>
      </Row>

      {listError && (
        <Alert
          type="error"
          showIcon
          message={listError}
          style={{ marginBottom: 16 }}
          action={
            <Button size="small" onClick={() => loadReports(selectedType)}>
              重试
            </Button>
          }
        />
      )}

      {/* Report List */}
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={selectedReport || detailLoading ? 14 : 24}>
          <Card title="报告列表" size="small">
            <List
              loading={listLoading}
              dataSource={reports}
              renderItem={(report) => {
                const typeConfig = REPORT_TYPES.find((t) => t.value === report.type);
                const theme = TYPE_THEME[report.type];
                const statusMeta = STATUS_META[report.status];
                return (
                  <List.Item
                    key={report.id}
                    style={{ padding: '12px 16px', cursor: 'pointer' }}
                    onClick={() => handleViewReport(report)}
                    actions={[
                      <Button
                        key="view"
                        type="link"
                        size="small"
                        icon={<EyeOutlined />}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleViewReport(report);
                        }}
                      >
                        预览
                      </Button>,
                      <Button
                        key="export"
                        type="link"
                        size="small"
                        icon={<FilePdfOutlined />}
                        loading={exportingId === report.id}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleExportPdf(report);
                        }}
                      >
                        导出 PDF
                      </Button>,
                    ]}
                  >
                    <List.Item.Meta
                      avatar={
                        <div style={{
                          width: 40,
                          height: 40,
                          borderRadius: 8,
                          background: theme.bg,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          fontSize: 18,
                        }}>
                          <FileTextOutlined style={{ color: theme.color }} />
                        </div>
                      }
                      title={
                        <Space>
                          <span style={{ fontWeight: 500 }}>{report.title}</span>
                          <Tag color={typeConfig?.color} style={{ marginInlineEnd: 0, fontSize: 11 }}>
                            {typeConfig?.label ?? report.typeLabel}
                          </Tag>
                          <Tag color={statusMeta?.color} style={{ fontSize: 11 }}>
                            {statusMeta?.label ?? report.status}
                          </Tag>
                        </Space>
                      }
                      description={
                        <Space size={16} style={{ fontSize: 12, color: '#8c8c8c' }}>
                          <span><CalendarOutlined /> {formatDateTime(report.createdAt)}</span>
                          <span>编制: {report.author}</span>
                          <span>{report.city} · {report.period}</span>
                        </Space>
                      }
                    />
                  </List.Item>
                );
              }}
              pagination={{ pageSize: 8, showSizeChanger: false }}
            />
          </Card>
        </Col>

        {/* Preview Panel */}
        {(selectedReport || detailLoading) && (
          <Col xs={24} lg={10}>
            <Card
              title="报告预览"
              size="small"
              extra={
                <Button type="link" size="small" onClick={() => { setSelectedReport(null); setDetailLoading(false); }}>
                  关闭
                </Button>
              }
              style={{ maxHeight: 600, overflow: 'auto' }}
            >
              {detailLoading ? (
                <div style={{ padding: '80px 0', textAlign: 'center' }}>
                  <Spin />
                  <div style={{ marginTop: 12, color: '#8c8c8c', fontSize: 12 }}>正在加载报告正文...</div>
                </div>
              ) : (
                selectedReport && <ReportPreview report={selectedReport} />
              )}
            </Card>
          </Col>
        )}
      </Row>

      {/* Generate Report Modal */}
      <Modal
        title="生成新报告"
        open={generateVisible}
        onOk={handleGenerateReport}
        onCancel={() => setGenerateVisible(false)}
        okText="生成"
        cancelText="取消"
        confirmLoading={generating}
        maskClosable={!generating}
        width={460}
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="type"
            label="报告类型"
            rules={[{ required: true, message: '请选择报告类型' }]}
          >
            <Select
              placeholder="选择报告类型"
              options={REPORT_TYPES.map((t) => ({ label: t.label, value: t.value }))}
            />
          </Form.Item>
          <Form.Item label="生成城市">
            <Input value={city} disabled />
          </Form.Item>
          <div style={{ fontSize: 12, color: '#8c8c8c' }}>
            报告标题、日期与正文由后端基于实时数据自动生成，生成完成后将直接打开预览。
          </div>
        </Form>
      </Modal>
    </div>
  );
};

export default ReportCenter;
