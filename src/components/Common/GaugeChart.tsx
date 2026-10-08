import React from 'react';
import { Card } from 'antd';
import type { EChartsOption } from '@/utils/echarts';
import ChartContainer from './ChartContainer';

interface GaugeChartProps {
  title: string;
  value: number;
  min?: number;
  max?: number;
  unit?: string;
  thresholds?: Array<{ value: number; color: string }>;
  height?: number;
}

const GaugeChart: React.FC<GaugeChartProps> = ({
  title,
  value,
  min = 0,
  max = 100,
  unit = '',
  thresholds = [
    { value: 60, color: '#52c41a' },
    { value: 80, color: '#faad14' },
    { value: 100, color: '#ff4d4f' },
  ],
  height = 250,
}) => {
  const getColor = () => {
    for (const t of thresholds) {
      if (value <= t.value) return t.color;
    }
    return thresholds[thresholds.length - 1]?.color || '#1890ff';
  };

  const option: EChartsOption = {
    series: [
      {
        type: 'gauge',
        startAngle: 200,
        endAngle: -20,
        min,
        max,
        splitNumber: 5,
        axisLine: {
          lineStyle: {
            width: 20,
            color: thresholds.map((t) => [t.value / max, t.color]),
          },
        },
        pointer: {
          itemStyle: { color: getColor() },
          width: 6,
          length: '60%',
        },
        axisTick: { distance: -20, length: 6, lineStyle: { color: '#fff', width: 1 } },
        splitLine: { distance: -20, length: 20, lineStyle: { color: '#fff', width: 2 } },
        axisLabel: { color: '#999', distance: 25, fontSize: 12 },
        title: {
          offsetCenter: [0, '70%'],
          fontSize: 14,
          color: '#666',
        },
        detail: {
          valueAnimation: true,
          formatter: `{value}${unit}`,
          color: getColor(),
          fontSize: 28,
          offsetCenter: [0, '40%'],
        },
        data: [{ value, name: title }],
      },
    ],
  };

  return (
    <Card bodyStyle={{ padding: 8 }} style={{ borderRadius: 8 }}>
      <ChartContainer option={option} height={height} />
    </Card>
  );
};

export default GaugeChart;
