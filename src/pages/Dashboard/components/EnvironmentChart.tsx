// ============================================================
// EnvironmentChart - ECharts line chart for environment trends
// ============================================================
import React, { useEffect, useRef, useCallback, useState } from 'react';
import * as echarts from '@/utils/echarts';
import { Segmented } from 'antd';
import type { EnvironmentRecord } from '@/types';

const METRIC_OPTIONS = [
  { label: 'PM2.5', value: 'PM2.5' },
  { label: 'PM10', value: 'PM10' },
  { label: 'NO₂', value: 'NO2' },
  { label: 'O₃', value: 'O3' },
  { label: 'SO₂', value: 'SO2' },
  { label: 'CO', value: 'CO' },
];

const METRIC_COLORS: Record<string, string> = {
  'PM2.5': '#ffcc00',
  'PM10': '#ff7200',
  'NO2': '#a80202',
  'O3': '#2d99f6',
  'SO2': '#f8ec11',
  'CO': '#5a00f3',
};

// 指标名归一化：图表传入展示名（如 PM2.5），接口数据为规范小写名（如 pm25）
const normalizeMetric = (m: string) => (m || '').toLowerCase().replace(/[\s.]/g, '');

interface EnvironmentChartProps {
  data: EnvironmentRecord[];
  title?: string;
}

const EnvironmentChart: React.FC<EnvironmentChartProps> = ({
  data,
  title = '环境趋势',
}) => {
  const chartRef = useRef<HTMLDivElement>(null);
  const instanceRef = useRef<echarts.ECharts | null>(null);
  const [activeMetric, setActiveMetric] = useState('PM2.5');

  const renderChart = useCallback(() => {
    if (!chartRef.current) return;

    if (!instanceRef.current) {
      instanceRef.current = echarts.init(chartRef.current);
    }

    const chart = instanceRef.current;
    const filtered = data.filter((d) => normalizeMetric(d.metric) === normalizeMetric(activeMetric));
    const color = METRIC_COLORS[activeMetric] || '#1890ff';
    const unit = filtered[0]?.unit || '';

    const option: echarts.EChartsOption = {
      title: {
        text: title,
        left: 'center',
        textStyle: { fontSize: 14, fontWeight: 500 },
      },
      tooltip: {
        trigger: 'axis',
        formatter: (params: any) => {
          const p = params[0];
          return `${p.axisValue}<br/>${p.marker} ${p.seriesName}: <strong>${p.value} ${unit}</strong>`;
        },
      },
      grid: { left: 60, right: 30, top: 50, bottom: 40 },
      xAxis: {
        type: 'category',
        data: filtered.map((d) => d.ts),
        axisLabel: { fontSize: 11 },
        boundaryGap: false,
      },
      yAxis: {
        type: 'value',
        name: unit,
        nameTextStyle: { fontSize: 11, color: '#8c8c8c' },
        axisLabel: { fontSize: 11 },
        splitLine: { lineStyle: { type: 'dashed' } },
      },
      series: [
        {
          name: activeMetric,
          type: 'line',
          data: filtered.map((d) => d.value),
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          lineStyle: { width: 2, color },
          itemStyle: { color },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: color + '40' },
              { offset: 1, color: color + '05' },
            ]),
          },
        },
      ],
      dataZoom: [
        { type: 'inside', start: 0, end: 100 },
        { type: 'slider', start: 0, end: 100, height: 20, bottom: 5 },
      ],
    };

    chart.setOption(option, true);
  }, [data, activeMetric, title]);

  useEffect(() => {
    renderChart();
  }, [renderChart]);

  useEffect(() => {
    const handleResize = () => instanceRef.current?.resize();
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  return (
    <div style={{ width: '100%', height: '100%' }}>
      <div style={{ marginBottom: 12, textAlign: 'right' }}>
        <Segmented
          options={METRIC_OPTIONS}
          value={activeMetric}
          onChange={(v) => setActiveMetric(v as string)}
          size="small"
        />
      </div>
      <div ref={chartRef} style={{ width: '100%', height: 280 }} />
    </div>
  );
};

export default EnvironmentChart;
