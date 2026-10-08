// ============================================================
// AlarmList - Scrollable alarm list with severity tags
// ============================================================
import React, { useEffect, useRef } from 'react';
import { List, Tag, Button, Empty, Badge } from 'antd';
import { CheckOutlined } from '@ant-design/icons';
import type { AlarmItem } from '@/types';

const SEVERITY_COLORS: Record<string, string> = {
  normal: 'success',
  warning: 'warning',
  danger: 'error',
};

const SEVERITY_LABELS: Record<string, string> = {
  normal: '正常',
  warning: '警告',
  danger: '危险',
};

interface AlarmListProps {
  alarms: AlarmItem[];
  onAcknowledge?: (id: string) => void;
}

const AlarmList: React.FC<AlarmListProps> = ({ alarms, onAcknowledge }) => {
  const listRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (listRef.current) {
      listRef.current.scrollTop = 0;
    }
  }, [alarms.length]);

  if (alarms.length === 0) {
    return <Empty description="暂无告警" style={{ marginTop: 40 }} />;
  }

  return (
    <div
      ref={listRef}
      style={{ maxHeight: 360, overflowY: 'auto', paddingRight: 4 }}
    >
      <List
        dataSource={alarms}
        renderItem={(item) => (
          <List.Item
            style={{
              padding: '12px 16px',
              background: item.acknowledged ? '#fafafa' : '#fff',
              borderRadius: 6,
              marginBottom: 8,
              border: '1px solid #f0f0f0',
              opacity: item.acknowledged ? 0.6 : 1,
            }}
          >
            <div
              style={{
                width: '100%',
                display: 'flex',
                alignItems: 'flex-start',
                gap: 12,
              }}
            >
              <Badge
                status={
                  item.severity === 'danger'
                    ? 'error'
                    : item.severity === 'warning'
                      ? 'warning'
                      : 'success'
                }
              />
              <div style={{ flex: 1, minWidth: 0 }}>
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    marginBottom: 4,
                  }}
                >
                  <span style={{ fontWeight: 500, fontSize: 14 }}>
                    {item.title}
                  </span>
                  <Tag
                    color={SEVERITY_COLORS[item.severity]}
                    style={{ marginInlineEnd: 0, fontSize: 11 }}
                  >
                    {SEVERITY_LABELS[item.severity]}
                  </Tag>
                </div>
                <div style={{ fontSize: 12, color: '#8c8c8c', marginBottom: 2 }}>
                  {item.location} · {item.location} · {item.timestamp}
                </div>
                <div style={{ fontSize: 12, color: '#595959' }}>
                  指标: {item.title}
                </div>
              </div>
              {!item.acknowledged && onAcknowledge && (
                <Button
                  type="primary"
                  size="small"
                  icon={<CheckOutlined />}
                  onClick={() => onAcknowledge(item.id)}
                >
                  确认
                </Button>
              )}
            </div>
          </List.Item>
        )}
      />
    </div>
  );
};

export default AlarmList;
