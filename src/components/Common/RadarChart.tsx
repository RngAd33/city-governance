import React from 'react';
import { Card } from 'antd';
import type { EChartsOption } from '@/utils/echarts';
import ChartContainer from './ChartContainer';

interface RadarChartProps {
  title: string;
  indicators: Array<{ name: string; max: number }>;
  data: Array<{ name: string; value: number[] }>;
  height?: number;
}

const RadarChart: React.FC<RadarChartProps> = ({
  title,
  indicators,
  data,
  height = 300,
}) => {
  const option: EChartsOption = {
    tooltip: {},
    legend: {
      data: data.map((d) => d.name),
      bottom: 0,
    },
    radar: {
      indicator: indicators,
      radius: '60%',
      center: ['50%', '50%'],
    },
    series: [
      {
        type: 'radar',
        data: data.map((d) => ({
          name: d.name,
          value: d.value,
          areaStyle: { opacity: 0.2 },
        })),
      },
    ],
  };

  return (
    <Card title={title} bodyStyle={{ padding: 8 }} style={{ borderRadius: 8 }}>
      <ChartContainer option={option} height={height} />
    </Card>
  );
};

export default RadarChart;
