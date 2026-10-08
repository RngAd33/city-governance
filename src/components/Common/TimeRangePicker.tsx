import React from 'react';
import { Radio, DatePicker, Space } from 'antd';
import type { RadioChangeEvent } from 'antd';
import dayjs, { Dayjs } from 'dayjs';

const { RangePicker } = DatePicker;

interface TimeRangePickerProps {
  value?: [Dayjs, Dayjs];
  onChange?: (range: [Dayjs, Dayjs]) => void;
  presets?: Array<{ label: string; value: [Dayjs, Dayjs] }>;
  showCustom?: boolean;
}

const defaultPresets = [
  { label: '24小时', value: [dayjs().subtract(24, 'hour'), dayjs()] as [Dayjs, Dayjs] },
  { label: '7天', value: [dayjs().subtract(7, 'day'), dayjs()] as [Dayjs, Dayjs] },
  { label: '30天', value: [dayjs().subtract(30, 'day'), dayjs()] as [Dayjs, Dayjs] },
];

const TimeRangePicker: React.FC<TimeRangePickerProps> = ({
  value,
  onChange,
  presets = defaultPresets,
  showCustom = true,
}) => {
  const [mode] = React.useState<string>('preset');

  const handlePresetChange = (e: RadioChangeEvent) => {
    const preset = presets.find((p) => p.label === e.target.value);
    if (preset && onChange) {
      onChange(preset.value);
    }
  };

  const handleCustomChange = (dates: [Dayjs | null, Dayjs | null] | null) => {
    if (dates && dates[0] && dates[1] && onChange) {
      onChange([dates[0], dates[1]]);
    }
  };

  return (
    <Space direction="vertical" size={8}>
      <Radio.Group
        size="small"
        onChange={handlePresetChange}
        value={mode}
        optionType="button"
        buttonStyle="solid"
      >
        {presets.map((preset) => (
          <Radio.Button key={preset.label} value={preset.label}>
            {preset.label}
          </Radio.Button>
        ))}
        {showCustom && <Radio.Button value="custom">自定义</Radio.Button>}
      </Radio.Group>
      {mode === 'custom' && showCustom && (
        <RangePicker
          showTime
          value={value}
          onChange={handleCustomChange}
          style={{ width: 320 }}
        />
      )}
    </Space>
  );
};

export default TimeRangePicker;
