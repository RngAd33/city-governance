import React, { useEffect, useRef } from 'react';
import { Card, Segmented, Empty, Spin, Switch, Tooltip } from 'antd';
import { LineChartOutlined } from '@ant-design/icons';
import * as echarts from '@/utils/echarts';


type EnergyTimeRange = 'day' | 'week' | 'month' | 'year';

/** 单条建筑能耗趋势序列（points.ts 为桶起始毫秒时间戳，组件按 timeRange 格式化轴标签） */
export interface EnergyTrendSeries {
  buildingName: string;
  points: Array<{ ts: number; value: number }>;
}

export type { EnergyTimeRange };

interface EnergyTrendProps {
  series: EnergyTrendSeries[];
  loading?: boolean;
  timeRange: EnergyTimeRange;
  onTimeRangeChange?: (range: EnergyTimeRange) => void;
  showAverage?: boolean;
  onShowAverageChange?: (show: boolean) => void;
}

const timeRangeOptions = [
  { label: '日', value: 'day' as EnergyTimeRange },
  { label: '周', value: 'week' as EnergyTimeRange },
  { label: '月', value: 'month' as EnergyTimeRange },
  { label: '年', value: 'year' as EnergyTimeRange },
];

const EnergyTrend: React.FC<EnergyTrendProps> = ({
  series: seriesData,
  loading = false,
  timeRange,
  onTimeRangeChange,
  showAverage = true,
  onShowAverageChange,
}) => {
  const chartRef = useRef<HTMLDivElement>(null);
  const chartInstanceRef = useRef<echarts.ECharts | null>(null);

  const hasTrendData = seriesData.some((s) => s.points.length > 0);

  useEffect(() => {
    if (!chartRef.current) return;

    const chart = echarts.init(chartRef.current);
    chartInstanceRef.current = chart;

    const handleResize = () => chart.resize();
    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
      chart.dispose();
    };
  }, []);

  useEffect(() => {
    // 没有时间序列点时跳过渲染，交由外层空态提示
    if (!chartInstanceRef.current || !seriesData.some((s) => s.points.length > 0)) return;

    const colors = ['#1890ff', '#52c41a', '#fa8c16', '#ff4d4f', '#722ed1', '#13c2c2', '#eb2f96', '#2f54eb'];

    // 时间轴取各序列并集并按时间戳数值升序（跨年/乱序输入也正确）
    const allTimestamps = new Set<number>();
    seriesData.forEach((s) => s.points.forEach((point) => allTimestamps.add(point.ts)));
    const sortedTimestamps = Array.from(allTimestamps).sort((a, b) => a - b);

    // 采样点查找表：避免逐点 find 造成的 O(n²) 开销
    const pointMaps = seriesData.map(
      (s) => new Map(s.points.map((p): [number, number] => [p.ts, p.value])),
    );

    // 轴/提示标签按所选时间范围格式化（ts 为本地时区毫秒）
    const pad = (n: number) => `${n}`.padStart(2, '0');
    const formatAxisTime = (ts: number) => {
      const d = new Date(ts);
      const md = `${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
      const hm = `${pad(d.getHours())}:${pad(d.getMinutes())}`;
      if (timeRange === 'day') return hm;
      if (timeRange === 'week') return `${md} ${hm}`;
      return md;
    };

    const chartSeries: echarts.SeriesOption[] = seriesData.map((s, index) => ({
      name: s.buildingName,
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      data: sortedTimestamps.map((ts) => pointMaps[index].get(ts) ?? null),
      lineStyle: { width: 2, color: colors[index % colors.length] },
      itemStyle: { color: colors[index % colors.length] },
      connectNulls: true,
    }));

    if (showAverage) {
      const avgData = sortedTimestamps.map((ts) => {
        const values = pointMaps
          .map((map) => map.get(ts))
          .filter((v): v is number => v !== undefined);
        return values.length > 0
          ? values.reduce((a, b) => a + b, 0) / values.length
          : null;
      });

      chartSeries.push({
        name: '平均值',
        type: 'line',
        smooth: true,
        symbol: 'none',
        data: avgData,
        lineStyle: { width: 2, type: 'dashed', color: '#8c8c8c' },
        itemStyle: { color: '#8c8c8c' },
        connectNulls: true,
      });
    }

    const option: echarts.EChartsOption = {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' },
        formatter: (params: unknown) => {
          const p = params as Array<{ seriesName: string; value: number; color: string; axisValue?: string | number }>;
          const axisTs = Number(p[0]?.axisValue);
          let html = `<div style="padding: 4px 8px;"><div style="font-weight: 500; margin-bottom: 4px;">${Number.isFinite(axisTs) ? formatAxisTime(axisTs) : p[0]?.axisValue ?? ''}</div>`;
          p.forEach((item) => {
            if (item.value !== null && item.value !== undefined) {
              html += `<div style="display: flex; align-items: center; gap: 4px;">
                <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: ${item.color};"></span>
                <span>${item.seriesName}: <strong>${item.value.toFixed(1)}</strong> kWh</span>
              </div>`;
            }
          });
          html += '</div>';
          return html;
        },
      },
      legend: {
        data: seriesData.map((s) => s.buildingName),
        bottom: 0,
        textStyle: { fontSize: 11 },
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '12%',
        top: '8%',
        containLabel: true,
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: sortedTimestamps,
        axisLabel: {
          color: '#8c8c8c',
          fontSize: 11,
          formatter: (value: string | number) => formatAxisTime(Number(value)),
        },
        axisLine: { lineStyle: { color: '#f0f0f0' } },
      },
      yAxis: {
        type: 'value',
        name: 'kWh',
        nameTextStyle: { color: '#8c8c8c' },
        axisLabel: { color: '#8c8c8c' },
        splitLine: { lineStyle: { type: 'dashed', color: '#f0f0f0' } },
      },
      series: chartSeries,
    };

    chartInstanceRef.current.setOption(option, true);
  }, [seriesData, showAverage, timeRange]);

  return (
    <Card
      title={
        <span>
          <LineChartOutlined style={{ marginRight: 8, color: '#1890ff' }} />
          能耗趋势
        </span>
      }
      size="small"
      extra={
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <Tooltip title="显示/隐藏平均线">
            <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
              <span style={{ fontSize: 12, color: '#8c8c8c' }}>平均线</span>
              <Switch
                size="small"
                checked={showAverage}
                onChange={onShowAverageChange}
              />
            </div>
          </Tooltip>
          <Segmented
            size="small"
            value={timeRange}
            onChange={(value) => onTimeRangeChange?.(value as EnergyTimeRange)}
            options={timeRangeOptions}
          />
        </div>
      }
      style={{ height: '100%' }}
      bodyStyle={{ padding: 0, height: 'calc(100% - 46px)' }}
    >
      <div style={{ position: 'relative', height: '100%', minHeight: 300 }}>
        <div ref={chartRef} style={{ width: '100%', height: '100%' }} />
        {loading && (
          <div
            style={{
              position: 'absolute',
              inset: 0,
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              background: '#fff',
            }}
          >
            <Spin size="large" tip="加载趋势数据..." />
          </div>
        )}
        {!loading && !hasTrendData && (
          <div
            style={{
              position: 'absolute',
              inset: 0,
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              background: '#fff',
            }}
          >
            <Empty description="暂无趋势数据" />
          </div>
        )}
      </div>
    </Card>
  );
};

export default EnergyTrend;
