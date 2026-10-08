import React from 'react';
import { Card, Statistic, Tooltip } from 'antd';
import { ArrowUpOutlined, ArrowDownOutlined, QuestionCircleOutlined } from '@ant-design/icons';

interface StatCardProps {
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

const StatCard: React.FC<StatCardProps> = ({
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
        width: '100%',
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
            // 数值与单位不折行：窄卡内 "1,199 辆/h" 折行会撑高卡片，导致同行统计卡高低不齐
            valueStyle={{ fontSize: 28, fontWeight: 600, whiteSpace: 'nowrap' }}
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
              width: 40,
              height: 40,
              borderRadius: 8,
              background: `${accentColor}15`,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 24,
              color: accentColor,
              flexShrink: 0,
            }}
          >
            {icon}
          </div>
        )}
      </div>
    </Card>
  );
};

export default StatCard;
