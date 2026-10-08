// ============================================================
// TrendChart - ECharts line chart for environment trends
// ============================================================
import React, { useEffect, useRef, useCallback, useState } from 'react';
import * as echarts from '@/utils/echarts';
import { Segmented } from 'antd';
import type { EnvironmentRecord } from '@/types';

type TimeRange = '24h' | '7d' | '30d';

interface TrendChartProps {
  data: EnvironmentRecord[];
  metric: string;
  title?: string;
  timeRange?: TimeRange;
  onTimeRangeChange?: (range: TimeRange) => void;
}

const THRESHOLDS: Record<string, number> = {
  'PM2.5': 75,
  'PM10': 150,
  'NO2': 80,
  'O3': 160,
  'SO2': 150,
  'CO': 4,
};

// 指标名归一化：图表传入展示名（如 PM2.5），接口数据为规范小写名（如 pm25）
const normalizeMetric = (m: string) => (m || '').toLowerCase().replace(/[\s.]/g, '');

const TrendChart: React.FC<TrendChartProps> = ({
  data,
  metric,
  title = '趋势图',
  timeRange: controlledTimeRange,
  onTimeRangeChange,
}) => {
  const chartRef = useRef<HTMLDivElement>(null);
  const instanceRef = useRef<echarts.ECharts | null>(null);
  const [innerTimeRange, setInnerTimeRange] = useState<TimeRange>('24h');
  // 受控优先：父级控制时间范围并触发按范围重新拉取数据；未受控时回退为内部状态
  const timeRange = controlledTimeRange ?? innerTimeRange;

  const renderChart = useCallback(() => {
    if (!chartRef.current) return;

    if (!instanceRef.current) {
      instanceRef.current = echarts.init(chartRef.current);
    }

    const chart = instanceRef.current;
    const filtered = data.filter((d) => normalizeMetric(d.metric) === normalizeMetric(metric));
    const threshold = THRESHOLDS[metric] || 100;
    const unit = filtered[0]?.unit || '';

    const option: echarts.EChartsOption = {
      title: {
        text: `${title} (${timeRange})`,
        left: 'center',
        textStyle: { fontSize: 14, fontWeight: 500 },
      },
      tooltip: {
        trigger: 'axis',
        formatter: (params: any) => {
          const p = params[0];
          const val = p.value;
          const isAnomaly = val > threshold;
          return `
            <div style="min-width: 160px;">
              <strong>${p.axisValue}</strong><br/>
              ${p.marker} ${metric}: <strong>${val} ${unit}</strong>
              ${isAnomaly ? '<span style="color:#ff4d4f;"> (超标)</span>' : ''}
            </div>
          `;
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
          name: metric,
          type: 'line',
          data: filtered.map((d) => d.value),
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          lineStyle: { width: 2, color: '#1890ff' },
          itemStyle: { color: '#1890ff' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#1890ff30' },
              { offset: 1, color: '#1890ff05' },
            ]),
          },
          markLine: {
            silent: true,
            data: [{ yAxis: threshold }],
            lineStyle: { color: '#ff4d4f', type: 'dashed', width: 1 },
            label: {
              formatter: `阈值: ${threshold}${unit}`,
              fontSize: 10,
              color: '#ff4d4f',
            },
          },
        },
      ],
      dataZoom: [
        { type: 'inside', start: 0, end: 100 },
        { type: 'slider', start: 0, end: 100, height: 20, bottom: 5 },
      ],
    };

    chart.setOption(option, true);
  }, [data, metric, title, timeRange]);

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
          options={[
            { label: '24小时', value: '24h' },
            { label: '7天', value: '7d' },
            { label: '30天', value: '30d' },
          ]}
          value={timeRange}
          onChange={(v) => {
            const r = v as TimeRange;
            setInnerTimeRange(r);
            onTimeRangeChange?.(r);
          }}
          size="small"
        />
      </div>
      <div ref={chartRef} style={{ width: '100%', height: 320 }} />
    </div>
  );
};

export default TrendChart;
