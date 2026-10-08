// ============================================================
// StatCard - Reusable KPI card with color-coded status
// ============================================================
import React from 'react';
import { Card, Statistic, Tag } from 'antd';
import {
  ArrowUpOutlined,
  ArrowDownOutlined,
  DashOutlined,
} from '@ant-design/icons';

type Level = 'normal' | 'warning' | 'danger';
type TrendDirection = 'up' | 'down' | 'flat';

const LEVEL_COLORS: Record<Level, string> = {
  normal: '#52c41a',
  warning: '#faad14',
  danger: '#ff4d4f',
};

const LEVEL_LABELS: Record<Level, string> = {
  normal: '正常',
  warning: '警告',
  danger: '危险',
};

const TREND_ICONS: Record<TrendDirection, React.ReactNode> = {
  up: <ArrowUpOutlined style={{ color: '#ff4d4f' }} />,
  down: <ArrowDownOutlined style={{ color: '#52c41a' }} />,
  flat: <DashOutlined style={{ color: '#8c8c8c' }} />,
};

export interface StatCardProps {
  title: string;
  value: number;
  unit: string;
  level: Level;
  icon: React.ReactNode;
  trend: TrendDirection;
  trendValue: number;
  /** 数据未就绪时以骨架占位，就绪后填充真实值，避免出现过渡假数字 */
  loading?: boolean;
}

const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  unit,
  level,
  icon,
  trend,
  trendValue,
  loading = false,
}) => {
  const color = LEVEL_COLORS[level];

  return (
    <Card
      hoverable
      styles={{ body: { padding: '20px 24px' } }}
      style={{
        borderTop: `3px solid ${color}`,
        borderRadius: 8,
        height: '100%',
      }}
    >
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
        }}
      >
        <div>
          <div style={{ fontSize: 14, color: '#8c8c8c', marginBottom: 8 }}>
            {title}
          </div>
          <Statistic
            value={value}
            suffix={unit}
            loading={loading}
            valueStyle={{ fontSize: 28, fontWeight: 600, color: '#262626' }}
          />
        </div>
        <div style={{ fontSize: 32, color, lineHeight: 1 }}>{icon}</div>
      </div>
      <div
        style={{
          marginTop: 12,
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}
      >
        <Tag color={color} style={{ marginInlineEnd: 0 }}>
          {LEVEL_LABELS[level]}
        </Tag>
        <span style={{ fontSize: 12, color: '#8c8c8c' }}>
          {TREND_ICONS[trend]}
          <span style={{ marginLeft: 4 }}>{trendValue}%</span>
        </span>
      </div>
    </Card>
  );
};

export default StatCard;
