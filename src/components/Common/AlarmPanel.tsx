import React from 'react';
import { List, Button, Badge, Empty, Typography, Space } from 'antd';
import { BellOutlined, CheckOutlined } from '@ant-design/icons';
import type { AlarmItem } from '@/types';
import StatusTag from './StatusTag';

const { Text } = Typography;

interface AlarmPanelProps {
  alarms: AlarmItem[];
  onAcknowledge?: (id: string) => void;
  onViewAll?: () => void;
  maxItems?: number;
}

const AlarmPanel: React.FC<AlarmPanelProps> = ({
  alarms,
  onAcknowledge,
  onViewAll,
  maxItems = 10,
}) => {
  const displayAlarms = alarms.slice(0, maxItems);

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <Space>
          <BellOutlined style={{ fontSize: 16, color: '#faad14' }} />
          <Text strong>实时告警</Text>
          <Badge count={alarms.filter((a) => !a.acknowledged).length} size="small" />
        </Space>
        {onViewAll && (
          <Button type="link" size="small" onClick={onViewAll}>
            查看全部
          </Button>
        )}
      </div>

      {displayAlarms.length === 0 ? (
        <Empty description="暂无告警" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      ) : (
        <List
          size="small"
          dataSource={displayAlarms}
          renderItem={(alarm) => (
            <List.Item
              style={{
                padding: '8px 0',
                opacity: alarm.acknowledged ? 0.6 : 1,
                borderLeft: `3px solid ${alarm.severity === 'danger' ? '#ff4d4f' : alarm.severity === 'warning' ? '#faad14' : '#52c41a'}`,
                paddingLeft: 8,
                marginBottom: 4,
              }}
            >
              <List.Item.Meta
                title={
                  <Space size={8}>
                    <StatusTag level={alarm.severity} />
                    <Text style={{ fontSize: 13 }}>{alarm.title}</Text>
                  </Space>
                }
                description={
                  <Space size={8}>
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      {alarm.location}
                    </Text>
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      {alarm.timestamp}
                    </Text>
                    {!alarm.acknowledged && onAcknowledge && (
                      <Button
                        type="link"
                        size="small"
                        icon={<CheckOutlined />}
                        onClick={() => onAcknowledge(alarm.id)}
                      >
                        确认
                      </Button>
                    )}
                  </Space>
                }
              />
            </List.Item>
          )}
        />
      )}
    </div>
  );
};

export default AlarmPanel;
