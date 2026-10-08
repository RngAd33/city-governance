import React from 'react';
import { Card } from 'antd';
import type { EChartsOption } from '@/utils/echarts';
import ChartContainer from './ChartContainer';

interface HeatmapChartProps {
  title: string;
  xData: string[];
  yData: string[];
  data: Array<[number, number, number]>; // [xIndex, yIndex, value]
  height?: number;
  colorRange?: [string, string];
}

const HeatmapChart: React.FC<HeatmapChartProps> = ({
  title,
  xData,
  yData,
  data,
  height = 300,
  colorRange = ['#52c41a', '#faad14', '#ff4d4f'],
}) => {
  const option: EChartsOption = {
    tooltip: {
      position: 'top',
      formatter: (params: any) => {
        return `${xData[params.value[0]]} - ${yData[params.value[1]]}<br/>数值: ${params.value[2]}`;
      },
    },
    grid: {
      left: '10%',
      right: '10%',
      bottom: '15%',
      top: '10%',
    },
    xAxis: {
      type: 'category',
      data: xData,
      splitArea: { show: true },
    },
    yAxis: {
      type: 'category',
      data: yData,
      splitArea: { show: true },
    },
    visualMap: {
      min: 0,
      max: Math.max(...data.map((d) => d[2])),
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '0%',
      inRange: {
        color: colorRange,
      },
    },
    series: [
      {
        type: 'heatmap',
        data,
        label: { show: true },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowColor: 'rgba(0, 0, 0, 0.5)',
          },
        },
      },
    ],
  };

  return (
    <Card title={title} bodyStyle={{ padding: 8 }} style={{ borderRadius: 8 }}>
      <ChartContainer option={option} height={height} />
    </Card>
  );
};

export default HeatmapChart;
