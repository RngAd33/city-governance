import React from 'react';
import { Card, Statistic, Tooltip } from 'antd';
import { ArrowUpOutlined, ArrowDownOutlined, QuestionCircleOutlined } from '@ant-design/icons';

interface MetricCardProps {
  title: string;
  value: number | string;
  unit?: string;
  level?: 'NORMAL' | 'WARNING' | 'DANGER';
  trend?: number;
  icon?: React.ReactNode;
  color?: string;
  tooltip?: string;
}

const levelColorMap: Record<string, string> = {
  NORMAL: '#52c41a',
  WARNING: '#faad14',
  DANGER: '#ff4d4f',
};

const MetricCard: React.FC<MetricCardProps> = ({
  title,
  value,
  unit,
  level = 'NORMAL',
  trend,
  icon,
  color,
  tooltip,
}) => {
  const accentColor = color || levelColorMap[level] || '#1890ff';

  return (
    <Card
      hoverable
      style={{
        borderTop: `3px solid ${accentColor}`,
        borderRadius: 8,
      }}
      bodyStyle={{ padding: 16 }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <div style={{ fontSize: 14, color: '#666', marginBottom: 8, display: 'flex', alignItems: 'center', gap: 4 }}>
            {title}
            {tooltip && (
              <Tooltip title={tooltip}>
                <QuestionCircleOutlined style={{ color: '#999' }} />
              </Tooltip>
            )}
          </div>
          <Statistic
            value={value}
            suffix={unit}
            valueStyle={{ fontSize: 28, fontWeight: 600 }}
          />
          {trend !== undefined && (
            <div style={{ marginTop: 8, fontSize: 12 }}>
              {trend >= 0 ? (
                <span style={{ color: '#ff4d4f' }}>
                  <ArrowUpOutlined /> {Math.abs(trend)}%
                </span>
              ) : (
                <span style={{ color: '#52c41a' }}>
                  <ArrowDownOutlined /> {Math.abs(trend)}%
                </span>
              )}
              <span style={{ color: '#999', marginLeft: 4 }}>较昨日</span>
            </div>
          )}
        </div>
        {icon && (
          <div
            style={{
              width: 48,
              height: 48,
              borderRadius: 8,
              background: `${accentColor}15`,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 24,
              color: accentColor,
            }}
          >
            {icon}
          </div>
        )}
      </div>
    </Card>
  );
};

export default MetricCard;
