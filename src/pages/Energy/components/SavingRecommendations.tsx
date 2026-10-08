import React, { useState } from 'react';
import { Card, Empty, Collapse, Tag, Typography, Progress } from 'antd';
import {
  ThunderboltOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  DollarOutlined,
  EnvironmentOutlined,
  DownOutlined,
} from '@ant-design/icons';
import type { SavingRecommendation } from '@/types';


const { Panel } = Collapse;
const { Text, Paragraph } = Typography;

const priorityConfig: Record<string, { label: string; color: string }> = {
  high: { label: '高优先级', color: 'error' },
  medium: { label: '中优先级', color: 'warning' },
  low: { label: '低优先级', color: 'default' },
};

const difficultyConfig: Record<string, { label: string; color: string }> = {
  easy: { label: '易实施', color: 'success' },
  moderate: { label: '中等难度', color: 'warning' },
  complex: { label: '复杂', color: 'error' },
};

interface SavingRecommendationsProps {
  data: SavingRecommendation[];
  loading?: boolean;
}

const SavingRecommendations: React.FC<SavingRecommendationsProps> = ({
  data,
  loading = false,
}) => {
  const [expandedId, setExpandedId] = useState<string | null>(null);

  // 兼容调用方传入非建议结构的数据（如建筑列表）：先过滤出包含 savingPotential 的条目，避免渲染期访问不存在的字段导致整页崩溃
  const recommendations = data.filter((item) => item?.savingPotential);

  const totalSaving = recommendations.reduce((sum, item) => sum + (item.savingPotential?.kwh ?? 0), 0);
  const _totalCost = recommendations.reduce((sum, item) => sum + (item.savingPotential?.cost ?? 0), 0);
  const _totalCarbon = recommendations.reduce((sum, item) => sum + (item.savingPotential?.carbon ?? 0), 0);
  void _totalCost;
  void _totalCarbon;

  return (
    <Card
      title={
        <span>
          <ThunderboltOutlined style={{ marginRight: 8, color: '#52c41a' }} />
          节能建议
        </span>
      }
      size="small"
      extra={
        <Tag color="success">
          总节电潜力: {totalSaving.toFixed(0)} kWh/年
        </Tag>
      }
      style={{ height: '100%' }}
      bodyStyle={{ padding: 0, height: 'calc(100% - 46px)', overflow: 'auto' }}
    >
      {loading ? (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
          <Progress type="circle" percent={0} status="active" />
        </div>
      ) : recommendations.length === 0 ? (
        <Empty description="暂无节能建议" style={{ marginTop: 48 }} />
      ) : (
        <Collapse
          ghost
          expandIcon={({ isActive }) => <DownOutlined rotate={isActive ? 180 : 0} />}
          onChange={(key) => setExpandedId(String(key) || null)}
        >
          {recommendations.map((item) => (
            <Panel
              key={item.id}
              header={
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, width: '100%' }}>
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                      <Text strong style={{ fontSize: 13 }}>{item.title}</Text>
                      <Tag color={priorityConfig[item.priority]?.color || 'default'} style={{ marginInlineEnd: 0 }}>
                        {priorityConfig[item.priority]?.label || item.priority}
                      </Tag>
                    </div>
                    <div style={{ fontSize: 12, color: '#8c8c8c' }}>
                      {item.buildingName}
                    </div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: 14, fontWeight: 500, color: '#52c41a' }}>
                      {item.savingPotential.kwh.toFixed(0)} kWh
                    </div>
                    <div style={{ fontSize: 11, color: '#8c8c8c' }}>
                      年节电潜力
                    </div>
                  </div>
                </div>
              }
            >
              <div style={{ padding: '4px 0' }}>
                <Paragraph style={{ fontSize: 13, color: '#595959', marginBottom: 12 }}>
                  {item.description}
                </Paragraph>

                <div style={{ display: 'flex', gap: 16, marginBottom: 12 }}>
                  <div style={{ flex: 1, textAlign: 'center' }}>
                    <div style={{ fontSize: 11, color: '#8c8c8c', marginBottom: 4 }}>
                      <ThunderboltOutlined /> 节电潜力
                    </div>
                    <div style={{ fontSize: 16, fontWeight: 500, color: '#1890ff' }}>
                      {item.savingPotential.kwh.toFixed(0)}
                    </div>
                    <div style={{ fontSize: 11, color: '#8c8c8c' }}>kWh/年</div>
                  </div>
                  <div style={{ flex: 1, textAlign: 'center' }}>
                    <div style={{ fontSize: 11, color: '#8c8c8c', marginBottom: 4 }}>
                      <DollarOutlined /> 节省费用
                    </div>
                    <div style={{ fontSize: 16, fontWeight: 500, color: '#1890ff' }}>
                      ¥{item.savingPotential.cost.toFixed(0)}
                    </div>
                    <div style={{ fontSize: 11, color: '#8c8c8c' }}>元/年</div>
                  </div>
                  <div style={{ flex: 1, textAlign: 'center' }}>
                    <div style={{ fontSize: 11, color: '#8c8c8c', marginBottom: 4 }}>
                      <EnvironmentOutlined /> 减少碳排放
                    </div>
                    <div style={{ fontSize: 16, fontWeight: 500, color: '#1890ff' }}>
                      {item.savingPotential.carbon.toFixed(0)}
                    </div>
                    <div style={{ fontSize: 11, color: '#8c8c8c' }}>kg/年</div>
                  </div>
                </div>

                <div style={{ display: 'flex', gap: 16, fontSize: 12 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                    <ClockCircleOutlined style={{ color: '#8c8c8c' }} />
                    <Text type="secondary">回收期:</Text>
                    <Text strong>{item.estimatedPayback}</Text>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                    <CheckCircleOutlined style={{ color: '#8c8c8c' }} />
                    <Text type="secondary">实施难度:</Text>
                    <Tag color={difficultyConfig[item.implementationDifficulty]?.color || 'default'} style={{ marginInlineEnd: 0 }}>
                      {difficultyConfig[item.implementationDifficulty]?.label || item.implementationDifficulty}
                    </Tag>
                  </div>
                </div>

                {item.details && expandedId === item.id && (
                  <>
                    <div style={{ marginTop: 12, paddingTop: 12, borderTop: '1px solid #f0f0f0' }}>
                      <Text strong style={{ fontSize: 13 }}>详细说明：</Text>
                      <br />
                      <Text style={{ fontSize: 12, color: '#595959' }}>
                        {item.details}
                      </Text>
                    </div>
                  </>
                )}
              </div>
            </Panel>
          ))}
        </Collapse>
      )}
    </Card>
  );
};

export default SavingRecommendations;
