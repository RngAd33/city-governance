// ============================================================
// WorkOrderList - Work order cards with progress tracking
// ============================================================
import React from 'react';
import { Card, Tag, Progress, Space, Empty, List } from 'antd';
import {
  ClockCircleOutlined,
  UserOutlined,
  CheckCircleOutlined,
  SyncOutlined,
  CloseCircleOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import type { WorkOrder } from '@/types';

const STATUS_CONFIG: Record<WorkOrder['status'], { color: string; label: string; icon: React.ReactNode }> = {
  pending: { color: 'default', label: '待处理', icon: <ClockCircleOutlined /> },
  inProgress: { color: 'processing', label: '进行中', icon: <SyncOutlined spin /> },
  completed: { color: 'success', label: '已完成', icon: <CheckCircleOutlined /> },
  cancelled: { color: 'error', label: '已取消', icon: <CloseCircleOutlined /> },
};

const PRIORITY_CONFIG: Record<WorkOrder['priority'], { color: string; label: string }> = {
  low: { color: 'default', label: '低' },
  medium: { color: 'processing', label: '中' },
  high: { color: 'warning', label: '高' },
  urgent: { color: 'error', label: '紧急' },
};

interface WorkOrderListProps {
  workOrders: WorkOrder[];
  onOrderClick?: (order: WorkOrder) => void;
}

const WorkOrderList: React.FC<WorkOrderListProps> = ({ workOrders, onOrderClick }) => {
  if (workOrders.length === 0) {
    return <Empty description="暂无工单" style={{ marginTop: 40 }} />;
  }

  return (
    <div style={{ maxHeight: 400, overflowY: 'auto', paddingRight: 4 }}>
      <List
        dataSource={workOrders}
        renderItem={(order) => {
          const statusCfg = STATUS_CONFIG[order.status];
          const priorityCfg = PRIORITY_CONFIG[order.priority];
          const isOverdue = new Date(order.dueDate) < new Date() && order.status !== 'completed';

          return (
            <List.Item
              key={order.id}
              style={{ padding: 0, marginBottom: 12 }}
              onClick={() => onOrderClick?.(order)}
            >
              <Card
                size="small"
                hoverable
                style={{
                  width: '100%',
                  borderLeft: `4px solid ${priorityCfg.color === 'error' ? '#ff4d4f' : priorityCfg.color === 'warning' ? '#faad14' : priorityCfg.color === 'processing' ? '#1890ff' : '#d9d9d9'}`,
                }}
                styles={{ body: { padding: '12px 16px' } }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 8 }}>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <Space>
                      <Tag color={statusCfg.color} icon={statusCfg.icon} style={{ marginInlineEnd: 0 }}>
                        {statusCfg.label}
                      </Tag>
                      <Tag color={priorityCfg.color} style={{ marginInlineEnd: 0 }}>
                        {priorityCfg.label}优先级
                      </Tag>
                      {isOverdue && (
                        <Tag color="error" icon={<ExclamationCircleOutlined />} style={{ marginInlineEnd: 0 }}>
                          已逾期
                        </Tag>
                      )}
                    </Space>
                    <div style={{ marginTop: 8, fontWeight: 600, fontSize: 14 }}>{order.title}</div>
                    <div style={{ fontSize: 12, color: '#8c8c8c', marginTop: 4 }}>
                      工单号: {order.id}
                    </div>
                  </div>
                </div>

                <div style={{ display: 'flex', gap: 16, marginBottom: 8, fontSize: 12, color: '#595959' }}>
                  <Space>
                    <UserOutlined />
                    <span>{order.department}</span>
                  </Space>
                  <Space>
                    <UserOutlined />
                    <span>{order.assigner}</span>
                  </Space>
                  <Space>
                    <ClockCircleOutlined />
                    <span>截止: {order.dueDate}</span>
                  </Space>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <Progress
                    percent={order.progress}
                    size="small"
                    strokeColor={order.progress === 100 ? '#52c41a' : '#1890ff'}
                    style={{ flex: 1, margin: 0 }}
                  />
                  <span style={{ fontSize: 12, color: '#8c8c8c', minWidth: 36, textAlign: 'right' }}>
                    {order.progress}%
                  </span>
                </div>
              </Card>
            </List.Item>
          );
        }}
      />
    </div>
  );
};

export default WorkOrderList;
