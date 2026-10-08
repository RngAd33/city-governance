import React from 'react';
import { Progress, Space, Typography } from 'antd';
import { LoadingOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons';

const { Text } = Typography;

interface SimulationProgressProps {
  status: 'idle' | 'running' | 'completed' | 'failed';
  progress?: number;
  message?: string;
}

const SimulationProgress: React.FC<SimulationProgressProps> = ({
  status,
  progress = 0,
  message,
}) => {
  const statusConfig = {
    idle: { color: '#999', text: '待启动', icon: null },
    running: { color: '#1890ff', text: '仿真运行中', icon: <LoadingOutlined /> },
    completed: { color: '#52c41a', text: '仿真完成', icon: <CheckCircleOutlined /> },
    failed: { color: '#ff4d4f', text: '仿真失败', icon: <CloseCircleOutlined /> },
  };

  const config = statusConfig[status];

  return (
    <div style={{ padding: 16, background: '#f5f5f5', borderRadius: 8 }}>
      <Space style={{ marginBottom: 8 }}>
        {config.icon}
        <Text strong style={{ color: config.color }}>
          {config.text}
        </Text>
        {message && <Text type="secondary">{message}</Text>}
      </Space>
      <Progress
        percent={progress}
        status={status === 'failed' ? 'exception' : status === 'completed' ? 'success' : 'active'}
        strokeColor={config.color}
      />
    </div>
  );
};

export default SimulationProgress;
