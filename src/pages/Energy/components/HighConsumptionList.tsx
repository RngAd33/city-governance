import React from 'react';
import { Card, List, Tag, Empty, Spin, Badge, Tooltip } from 'antd';
import {
  WarningOutlined,
  FireOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  MinusOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import type { Building } from '@/types';

const buildingTypeLabels: Record<string, string> = {
  office: '办公楼用电',
  hospital: '医院用电',
  commercial: '商业用电',
  residential: '居民用电',
  school: '学校用电',
};

const buildingTypeColors: Record<string, string> = {
  office: 'blue',
  hospital: 'red',
  commercial: 'green',
  residential: 'orange',
  school: 'purple',
};

interface HighConsumptionListProps {
  data: Building[];
  loading?: boolean;
  onBuildingClick?: (building: Building) => void;
}

const HighConsumptionList: React.FC<HighConsumptionListProps> = ({
  data,
  loading = false,
  onBuildingClick,
}) => {
  const getTrendIcon = (trend: string) => {
    switch (trend) {
      case 'up':
        return <ArrowUpOutlined style={{ color: '#ff4d4f' }} />;
      case 'down':
        return <ArrowDownOutlined style={{ color: '#52c41a' }} />;
      default:
        return <MinusOutlined style={{ color: '#8c8c8c' }} />;
    }
  };

  const getAlertTag = (item: Building) => {
    if ((item as any).alertLevel === 'critical') {
      return (
        <Tooltip title="能耗严重超标">
          <Tag icon={<ExclamationCircleOutlined />} color="error" style={{ marginInlineEnd: 0 }}>
            严重超标
          </Tag>
        </Tooltip>
      );
    }
    if ((item as any).alertLevel === 'warning') {
      return (
        <Tooltip title="能耗超出预警值">
          <Tag icon={<WarningOutlined />} color="warning" style={{ marginInlineEnd: 0 }}>
            预警
          </Tag>
        </Tooltip>
      );
    }
    return null;
  };

  return (
    <Card
      title={
        <span>
          <FireOutlined style={{ marginRight: 8, color: '#fa8c16' }} />
          高能耗建筑
        </span>
      }
      size="small"
      extra={
        <Badge
          count={data.filter((d) => (d as any).isAbnormal).length}
          style={{ backgroundColor: '#ff4d4f' }}
        />
      }
      style={{ height: '100%' }}
      bodyStyle={{ padding: 0, height: 'calc(100% - 46px)', overflow: 'auto' }}
    >
      {loading ? (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
          <Spin size="large" tip="加载高能耗数据..." />
        </div>
      ) : data.length === 0 ? (
        <Empty description="暂无高能耗建筑" style={{ marginTop: 48 }} />
      ) : (
        <List
          dataSource={data}
          renderItem={(item) => (
            <List.Item
              style={{
                padding: '12px 16px',
                cursor: 'pointer',
                transition: 'background 0.3s',
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.background = '#f0f5ff';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.background = 'transparent';
              }}
              onClick={() => onBuildingClick?.(item)}
            >
              <List.Item.Meta
                avatar={
                  <div
                    style={{
                      width: 40,
                      height: 40,
                      borderRadius: 8,
                      background: (item as any).isAbnormal ? '#fff1f0' : '#f0f5ff',
                      display: 'flex',
                      justifyContent: 'center',
                      alignItems: 'center',
                      fontSize: 18,
                      color: (item as any).isAbnormal ? '#ff4d4f' : '#1890ff',
                    }}
                  >
                    <FireOutlined />
                  </div>
                }
                title={
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontWeight: 500 }}>{item.name}</span>
                    <Tag color={buildingTypeColors[(item.type || '').toLowerCase()] || 'default'} style={{ marginInlineEnd: 0 }}>
                      {buildingTypeLabels[(item.type || '').toLowerCase()] || item.type}
                    </Tag>
                    {getAlertTag(item)}
                  </div>
                }
                description={
                  <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginTop: 4 }}>
                    <span style={{ fontSize: 12, color: '#8c8c8c' }}>
                      日均能耗: <strong style={{ color: '#262626' }}>{item.avgDailyConsumption?.toFixed(1) || '--'}</strong> kWh
                    </span>
                    <span
                      style={{
                        fontSize: 12,
                        display: 'flex',
                        alignItems: 'center',
                        gap: 2,
                        color: (item as any).trend === 'up' ? '#ff4d4f' : (item as any).trend === 'down' ? '#52c41a' : '#8c8c8c',
                      }}
                    >
                      {getTrendIcon((item as any).trend || 'flat')}
                      {Math.abs((item as any).trendPercent || 0).toFixed(1)}%
                    </span>
                  </div>
                }
              />
            </List.Item>
          )}
        />
      )}
    </Card>
  );
};

export default HighConsumptionList;
