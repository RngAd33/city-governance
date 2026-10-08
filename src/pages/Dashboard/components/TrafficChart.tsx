// ============================================================
// TrafficChart - ECharts bar/line combo for traffic flow
// ============================================================
import React, { useEffect, useRef, useCallback } from 'react';
import * as echarts from '@/utils/echarts';
import type { TrafficFlowData } from '@/types';

interface TrafficChartProps {
  data: TrafficFlowData[];
  title?: string;
}

const TrafficChart: React.FC<TrafficChartProps> = ({
  data,
  title = '交通流量',
}) => {
  const chartRef = useRef<HTMLDivElement>(null);
  const instanceRef = useRef<echarts.ECharts | null>(null);

  const renderChart = useCallback(() => {
    if (!chartRef.current) return;

    if (!instanceRef.current) {
      instanceRef.current = echarts.init(chartRef.current);
    }

    const chart = instanceRef.current;

    // Use index as time label since TrafficFlowData doesn't have a time field
    const labels = data.map((_, i) => `${i * 15}min`);
    const _avgCongestion =
      data.reduce((sum, d) => sum + d.queueLength, 0) / (data.length || 1);
    void _avgCongestion;

    const option: echarts.EChartsOption = {
      title: {
        text: title,
        left: 'center',
        textStyle: { fontSize: 14, fontWeight: 500 },
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' },
        formatter: (params: any) => {
          const flow = params[0];
          const speed = params[1];
          return `
            <div style="min-width: 140px;">
              <strong>${flow.axisValue}</strong><br/>
              ${flow.marker} 流量: <strong>${flow.value}</strong> 辆/时<br/>
              ${speed?.marker ?? ''} 平均速度: <strong>${speed?.value ?? '-'}</strong> km/h
            </div>
          `;
        },
      },
      legend: {
        data: ['交通流量', '平均速度'],
        bottom: 0,
        textStyle: { fontSize: 11 },
      },
      grid: { left: 60, right: 60, top: 50, bottom: 50 },
      xAxis: {
        type: 'category',
        data: labels,
        axisLabel: { fontSize: 11 },
      },
      yAxis: [
        {
          type: 'value',
          name: '流量 (辆/时)',
          nameTextStyle: { fontSize: 11, color: '#8c8c8c' },
          axisLabel: { fontSize: 11 },
          splitLine: { lineStyle: { type: 'dashed' } },
        },
        {
          type: 'value',
          name: '速度 (km/h)',
          nameTextStyle: { fontSize: 11, color: '#8c8c8c' },
          axisLabel: { fontSize: 11 },
          splitLine: { show: false },
        },
      ],
      series: [
        {
          name: '交通流量',
          type: 'bar',
          data: data.map((d) => d.flow),
          barWidth: '50%',
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#1890ff' },
              { offset: 1, color: '#69c0ff' },
            ]),
            borderRadius: [4, 4, 0, 0],
          },
        },
        {
          name: '平均速度',
          type: 'line',
          yAxisIndex: 1,
          data: data.map((d) => d.avgSpeed),
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          lineStyle: { width: 2, color: '#ff4d4f' },
          itemStyle: { color: '#ff4d4f' },
          markLine: {
            silent: true,
            data: [{ yAxis: 30 }],
            lineStyle: { color: '#ff4d4f', type: 'dashed' },
            label: { formatter: '拥堵阈值', fontSize: 10 },
          },
        },
      ],
      dataZoom: [
        { type: 'inside', start: 0, end: 100 },
        { type: 'slider', start: 0, end: 100, height: 20, bottom: 25 },
      ],
    };

    chart.setOption(option, true);
  }, [data, title]);

  useEffect(() => {
    renderChart();
  }, [renderChart]);

  useEffect(() => {
    const handleResize = () => instanceRef.current?.resize();
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  return (
    <div ref={chartRef} style={{ width: '100%', height: 280 }} />
  );
};

export default TrafficChart;
