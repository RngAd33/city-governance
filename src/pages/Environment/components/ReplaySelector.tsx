// ============================================================
// ReplaySelector - Date/time picker for historical data replay
// ============================================================
import React from 'react';
import { DatePicker, Button, Space, message } from 'antd';
import { PlayCircleOutlined } from '@ant-design/icons';
import dayjs, { Dayjs } from 'dayjs';

interface ReplaySelectorProps {
  onReplay: (date: string) => void;
  loading?: boolean;
}

const ReplaySelector: React.FC<ReplaySelectorProps> = ({
  onReplay,
  loading,
}) => {
  const [selectedDate, setSelectedDate] = React.useState<Dayjs | null>(null);

  const handleReplay = () => {
    if (!selectedDate) {
      message.warning('请先选择日期');
      return;
    }
    onReplay(selectedDate.format('YYYY-MM-DD'));
  };

  const handleQuickSelect = (daysAgo: number) => {
    const date = dayjs().subtract(daysAgo, 'day');
    setSelectedDate(date);
    onReplay(date.format('YYYY-MM-DD'));
  };

  return (
    <Space wrap size="middle" style={{ marginBottom: 16 }}>
      <span style={{ fontSize: 14, color: '#595959' }}>历史回放:</span>
      <DatePicker
        value={selectedDate}
        onChange={(date) => setSelectedDate(date)}
        allowClear={false}
        disabledDate={(current) => current && current > dayjs().endOf('day')}
        placeholder="选择日期"
      />
      <Button
        type="primary"
        icon={<PlayCircleOutlined />}
        onClick={handleReplay}
        loading={loading}
      >
        回放
      </Button>
      <Space size="small">
        <Button size="small" onClick={() => handleQuickSelect(1)}>
          昨天
        </Button>
        <Button size="small" onClick={() => handleQuickSelect(3)}>
          3天前
        </Button>
        <Button size="small" onClick={() => handleQuickSelect(7)}>
          7天前
        </Button>
      </Space>
    </Space>
  );
};

export default ReplaySelector;
