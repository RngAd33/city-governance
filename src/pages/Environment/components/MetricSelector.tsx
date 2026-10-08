// ============================================================
// MetricSelector - Tab component for environment metric selection
// ============================================================
import React from 'react';
import { Segmented } from 'antd';

interface MetricOption {
  label: string;
  value: string;
}

const METRICS: MetricOption[] = [
  { label: 'PM2.5', value: 'PM2.5' },
  { label: 'PM10', value: 'PM10' },
  { label: 'NO₂', value: 'NO2' },
  { label: 'O₃', value: 'O3' },
  { label: 'SO₂', value: 'SO2' },
  { label: 'CO', value: 'CO' },
];

interface MetricSelectorProps {
  value: string;
  onChange: (metric: string) => void;
}

const MetricSelector: React.FC<MetricSelectorProps> = ({ value, onChange }) => {
  return (
    <Segmented
      options={METRICS}
      value={value}
      onChange={(v) => onChange(v as string)}
      size="large"
      style={{ marginBottom: 16 }}
    />
  );
};

export default MetricSelector;
