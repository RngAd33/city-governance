import React from 'react';
import { Card } from 'antd';
import type { EChartsOption } from '@/utils/echarts';
import ChartContainer from './ChartContainer';

interface BarChartProps {
  title: string;
  data: Array<{ name: string; value: number; color?: string }>;
  unit?: string;
  color?: string;
  height?: number;
  horizontal?: boolean;
  onBarClick?: (name: string) => void;
}

const BarChart: React.FC<BarChartProps> = ({
  title,
  data,
  unit = '',
  color = '#1890ff',
  height = 300,
  horizontal = false,
  onBarClick,
}) => {
  const option: EChartsOption = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        const p = params[0];
        return `${p.name}<br/>${p.marker} ${p.value}${unit}`;
      },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
    xAxis: {
      type: horizontal ? 'value' : 'category',
      data: horizontal ? undefined : data.map((d) => d.name),
      axisLabel: {
        rotate: horizontal ? 0 : data.length > 5 ? 30 : 0,
      },
    },
    yAxis: {
      type: horizontal ? 'category' : 'value',
      data: horizontal ? data.map((d) => d.name) : undefined,
      name: unit,
    },
    series: [
      {
        type: 'bar',
        data: data.map((d) => ({
          value: d.value,
          itemStyle: { color: d.color || color },
        })),
        barMaxWidth: 40,
        label: {
          show: true,
          position: horizontal ? 'right' : 'top',
          formatter: '{c}',
        },
      },
    ],
  };

  return (
    <Card title={title} bodyStyle={{ padding: 8 }} style={{ borderRadius: 8 }}>
      <ChartContainer
        option={option}
        height={height}
        onChartReady={(chart) => {
          if (onBarClick) {
            chart.on('click', (params: any) => {
              onBarClick(params.name);
            });
          }
        }}
      />
    </Card>
  );
};

export default BarChart;
