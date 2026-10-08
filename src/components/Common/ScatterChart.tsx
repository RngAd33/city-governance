import React from 'react';
import { Card } from 'antd';
import type { EChartsOption } from '@/utils/echarts';
import ChartContainer from './ChartContainer';

interface ScatterChartProps {
  title: string;
  data: Array<{ x: number; y: number; name?: string; color?: string }>;
  xLabel?: string;
  yLabel?: string;
  height?: number;
}

const ScatterChart: React.FC<ScatterChartProps> = ({
  title,
  data,
  xLabel = '',
  yLabel = '',
  height = 300,
}) => {
  const option: EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        return `${params.data.name || ''}<br/>${xLabel}: ${params.data.x}<br/>${yLabel}: ${params.data.y}`;
      },
    },
    grid: {
      left: '10%',
      right: '10%',
      bottom: '10%',
      top: '10%',
    },
    xAxis: {
      name: xLabel,
      nameLocation: 'middle',
      nameGap: 30,
      type: 'value',
    },
    yAxis: {
      name: yLabel,
      nameLocation: 'middle',
      nameGap: 40,
      type: 'value',
    },
    series: [
      {
        type: 'scatter',
        data: data.map((d) => ({
          value: [d.x, d.y],
          name: d.name,
          itemStyle: d.color ? { color: d.color } : undefined,
        })),
        symbolSize: 12,
      },
    ],
  };

  return (
    <Card title={title} bodyStyle={{ padding: 8 }} style={{ borderRadius: 8 }}>
      <ChartContainer option={option} height={height} />
    </Card>
  );
};

export default ScatterChart;
