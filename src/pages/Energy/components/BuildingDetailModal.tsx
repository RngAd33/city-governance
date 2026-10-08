import React, { useEffect, useState } from 'react';
import { Modal, Descriptions, Tag, Card, Row, Col, Statistic, List, Spin, Empty, Alert } from 'antd';
import {
  ThunderboltOutlined,
  DollarOutlined,
  EnvironmentOutlined,
  BulbOutlined,
  DashboardOutlined,
} from '@ant-design/icons';
import { getBuildingAnalysis, getSavingPotential } from '@/api/dashboard';
import type { Building, BuildingAnalysis, SavingPotentialResult } from '@/types';

// 建筑类型中文映射（city-energy 指标枚举 electricity_consumption_{type}）
const buildingTypeLabels: Record<string, string> = {
  office: '办公楼用电',
  hospital: '医院用电',
  commercial: '商业用电',
  residential: '居民用电',
  school: '学校用电',
};

interface BuildingDetailModalProps {
  open: boolean;
  buildingId: string | null;
  onClose: () => void;
  fetchDetail: (id: string) => Promise<Building | null>;
}

const BuildingDetailModal: React.FC<BuildingDetailModalProps> = ({
  open,
  buildingId,
  onClose,
  fetchDetail,
}) => {
  const [detail, setDetail] = useState<Building | null>(null);
  const [loading, setLoading] = useState(false);
  const [analysis, setAnalysis] = useState<BuildingAnalysis | null>(null);
  const [saving, setSaving] = useState<SavingPotentialResult | null>(null);
  const [extraLoading, setExtraLoading] = useState(false);

  useEffect(() => {
    if (open && buildingId) {
      loadDetail();
      loadBuildingExtras(buildingId);
    } else {
      setDetail(null);
      setAnalysis(null);
      setSaving(null);
    }
  }, [open, buildingId]);

  const loadDetail = async () => {
    if (!buildingId) return;
    setLoading(true);
    try {
      const data = await fetchDetail(buildingId);
      setDetail(data);
    } catch (error) {
      console.error('Failed to load building detail:', error);
    } finally {
      setLoading(false);
    }
  };

  // 能耗分析与节电潜力：与基础信息并行加载，单个失败保持空态不影响弹窗其他内容
  const loadBuildingExtras = async (id: string) => {
    setExtraLoading(true);
    setAnalysis(null);
    setSaving(null);
    const [analysisResp, savingResp] = await Promise.allSettled([
      getBuildingAnalysis(id),
      getSavingPotential(id),
    ]);
    if (analysisResp.status === 'fulfilled') {
      setAnalysis(((analysisResp.value as any)?.data ?? null) as BuildingAnalysis | null);
    }
    if (savingResp.status === 'fulfilled') {
      setSaving(((savingResp.value as any)?.data ?? null) as SavingPotentialResult | null);
    }
    setExtraLoading(false);
  };

  return (
    <Modal
      title="建筑能耗详情"
      open={open}
      onCancel={onClose}
      footer={null}
      width={700}
      destroyOnClose
    >
      {loading ? (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', padding: 48 }}>
          <Spin size="large" tip="加载建筑详情..." />
        </div>
      ) : detail ? (
        <div>
          <Descriptions
            column={2}
            size="small"
            bordered
            style={{ marginBottom: 16 }}
          >
            <Descriptions.Item label="建筑名称" span={2}>
              <strong>{detail.name}</strong>
            </Descriptions.Item>
            <Descriptions.Item label="建筑类型">
              <Tag color="blue">{buildingTypeLabels[(detail.type || '').toLowerCase()] || detail.type}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="建筑面积">
              {detail.area.toFixed(0)} m²
            </Descriptions.Item>
            <Descriptions.Item label="日均能耗">
              {detail.avgDailyConsumption?.toFixed(1) || '--'} kWh
            </Descriptions.Item>
            <Descriptions.Item label="碳排放因子">
              {(detail.carbonFactor ?? 0.58).toFixed(2)} kg/kWh
            </Descriptions.Item>
          </Descriptions>

          <Row gutter={16} style={{ marginBottom: 16 }}>
            <Col span={8}>
              <Card size="small" style={{ textAlign: 'center' }}>
                <Statistic
                  title="日均能耗"
                  value={detail.avgDailyConsumption || 0}
                  suffix="kWh"
                  valueStyle={{ color: '#1890ff', fontSize: 18 }}
                  prefix={<ThunderboltOutlined />}
                />
              </Card>
            </Col>
            <Col span={8}>
              <Card size="small" style={{ textAlign: 'center' }}>
                <Statistic
                  title="能耗成本"
                  value={(detail.avgDailyConsumption || 0) * 0.8}
                  suffix="元"
                  precision={0}
                  valueStyle={{ color: '#52c41a', fontSize: 18 }}
                  prefix={<DollarOutlined />}
                />
              </Card>
            </Col>
            <Col span={8}>
              <Card size="small" style={{ textAlign: 'center' }}>
                <Statistic
                  title="碳排放"
                  value={(detail.avgDailyConsumption || 0) * (detail.carbonFactor ?? 0.58)}
                  suffix="kg"
                  precision={0}
                  valueStyle={{ color: '#722ed1', fontSize: 18 }}
                  prefix={<EnvironmentOutlined />}
                />
              </Card>
            </Col>
          </Row>

          <Card
            title={
              <span>
                <DashboardOutlined style={{ marginRight: 8, color: '#1890ff' }} />
                能耗分析
              </span>
            }
            size="small"
            style={{ marginBottom: 16 }}
          >
            {extraLoading ? (
              <div style={{ display: 'flex', justifyContent: 'center', padding: 16 }}>
                <Spin />
              </div>
            ) : analysis && analysis.avgDailyConsumption > 0 ? (
              <>
                <Row gutter={16}>
                  <Col span={6}>
                    <Statistic title="日均能耗" value={analysis.avgDailyConsumption} suffix="kWh" precision={1} valueStyle={{ fontSize: 16 }} />
                  </Col>
                  <Col span={6}>
                    <Statistic title="峰值能耗" value={analysis.peakConsumption} suffix="kWh" precision={1} valueStyle={{ fontSize: 16 }} />
                  </Col>
                  <Col span={6}>
                    <Statistic title="能耗强度" value={analysis.energyIntensity} suffix="kWh/m²" precision={2} valueStyle={{ fontSize: 16 }} />
                  </Col>
                  <Col span={6}>
                    <Statistic title="碳足迹" value={analysis.carbonFootprint} suffix="kg" precision={1} valueStyle={{ fontSize: 16 }} />
                  </Col>
                </Row>
                <div style={{ marginTop: 8, fontSize: 12, color: '#8c8c8c' }}>
                  能效评级：{analysis.efficiencyRating}
                  {analysis.anomalyDescription ? ` · ${analysis.anomalyDescription}` : ''}
                </div>
                {analysis.anomalyDetected && (
                  <Alert type="warning" showIcon message={analysis.anomalyDescription || '检测到能耗异常'} style={{ marginTop: 8 }} />
                )}
              </>
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={analysis?.anomalyDescription || '暂无能耗分析数据'} />
            )}
          </Card>

          <Card
            title={
              <span>
                <BulbOutlined style={{ marginRight: 8, color: '#faad14' }} />
                节能建议
              </span>
            }
            size="small"
            extra={saving ? <Tag color="success">预计年节电 {saving.estimatedSavingKwh.toFixed(0)} kWh</Tag> : undefined}
          >
            {extraLoading ? (
              <div style={{ display: 'flex', justifyContent: 'center', padding: 16 }}>
                <Spin />
              </div>
            ) : (
              <List
                size="small"
                dataSource={saving?.recommendations ?? []}
                locale={{ emptyText: '暂无节能建议' }}
                renderItem={(item, index) => (
                  <List.Item>
                    <Tag color="gold">{index + 1}</Tag>
                    <span>{item}</span>
                  </List.Item>
                )}
              />
            )}
          </Card>
        </div>
      ) : (
        <Empty description="暂无数据" />
      )}
    </Modal>
  );
};

export default BuildingDetailModal;
