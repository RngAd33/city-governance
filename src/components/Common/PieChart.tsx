import React from 'react';
import { Card } from 'antd';
import type { EChartsOption } from '@/utils/echarts';
import ChartContainer from './ChartContainer';

interface PieChartProps {
  title: string;
  data: Array<{ name: string; value: number; color?: string }>;
  height?: number;
  donut?: boolean;
  onSliceClick?: (name: string) => void;
}

const PieChart: React.FC<PieChartProps> = ({
  title,
  data,
  height = 300,
  donut = false,
  onSliceClick,
}) => {
  const option: EChartsOption = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: { fontSize: 12 },
    },
    series: [
      {
        type: 'pie',
        radius: donut ? ['40%', '70%'] : '60%',
        center: ['35%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 4,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: {
          show: true,
          formatter: '{b}\n{d}%',
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold',
          },
        },
        data: data.map((d) => ({
          name: d.name,
          value: d.value,
          itemStyle: d.color ? { color: d.color } : undefined,
        })),
      },
    ],
  };

  return (
    <Card title={title} bodyStyle={{ padding: 8 }} style={{ borderRadius: 8 }}>
      <ChartContainer
        option={option}
        height={height}
        onChartReady={(chart) => {
          if (onSliceClick) {
            chart.on('click', (params: any) => {
              onSliceClick(params.name);
            });
          }
        }}
      />
    </Card>
  );
};

export default PieChart;
