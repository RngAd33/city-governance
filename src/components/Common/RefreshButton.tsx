import React, { useState } from 'react';
import { Button, Tooltip } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';

interface RefreshButtonProps {
  onRefresh: () => void;
  loading?: boolean;
  interval?: number;
  showInterval?: boolean;
}

const RefreshButton: React.FC<RefreshButtonProps> = ({
  onRefresh,
  loading = false,
  interval = 30,
  showInterval = true,
}) => {
  const [countdown, setCountdown] = useState(interval);

  React.useEffect(() => {
    if (!showInterval) return;

    const timer = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          onRefresh();
          return interval;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [interval, onRefresh, showInterval]);

  return (
    <Tooltip title={showInterval ? `${countdown}秒后自动刷新` : '手动刷新'}>
      <Button
        icon={<ReloadOutlined spin={loading} />}
        onClick={() => {
          onRefresh();
          setCountdown(interval);
        }}
        loading={loading}
      >
        {showInterval ? `${countdown}s` : '刷新'}
      </Button>
    </Tooltip>
  );
};

export default RefreshButton;
