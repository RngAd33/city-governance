import React from 'react';
import { Card, Radio, Spin } from 'antd';
import type { EChartsOption } from '@/utils/echarts';
import ChartContainer from './ChartContainer';

interface TrendChartProps {
  title: string;
  data: Array<{ time: string; value: number }>;
  unit?: string;
  color?: string;
  height?: number;
  showRange?: boolean;
  /** 受控时间范围；传入时由父级控制（切换会触发父级按范围重新取数），未传入回退内部状态 */
  range?: string;
  /** 数据加载中显示遮罩而非卸载图表，避免范围选中态被重置 */
  loading?: boolean;
  ranges?: Array<{ label: string; value: string }>;
  onRangeChange?: (range: string) => void;
  referenceLines?: Array<{ yAxis: number; label: string; color?: string }>;
}

const TrendChart: React.FC<TrendChartProps> = ({
  title,
  data,
  unit = '',
  color = '#1890ff',
  height = 300,
  showRange = false,
  range: controlledRange,
  loading = false,
  ranges = [
    { label: '24小时', value: '24h' },
    { label: '7天', value: '7d' },
    { label: '30天', value: '30d' },
  ],
  onRangeChange,
  referenceLines = [],
}) => {
  const [innerRange, setInnerRange] = React.useState('24h');
  // 受控优先：父级掌握时间范围并据此重新取数（与 Environment 页 TrendChart 同步策略一致）
  const range = controlledRange ?? innerRange;

  const option: EChartsOption = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const p = params[0];
        return `${p.axisValue}<br/>${p.marker} ${p.seriesName}: ${p.value}${unit}`;
      },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.map((d) => d.time),
      axisLabel: {
        rotate: data.length > 10 ? 45 : 0,
      },
    },
    yAxis: {
      type: 'value',
      name: unit,
      nameTextStyle: { color: '#999' },
    },
    series: [
      {
        name: title,
        type: 'line',
        smooth: true,
        data: data.map((d) => d.value),
        lineStyle: { color, width: 2 },
        itemStyle: { color },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: `${color}30` },
              { offset: 1, color: `${color}05` },
            ],
          },
        },
        markLine: referenceLines.length > 0 ? {
          data: referenceLines.map((line) => ({
            yAxis: line.yAxis,
            label: { formatter: line.label },
            lineStyle: { color: line.color || '#ff4d4f', type: 'dashed' },
          })),
        } : undefined,
      },
    ],
  };

  return (
    <Card
      title={title}
      extra={
        showRange ? (
          <Radio.Group
            size="small"
            value={range}
            onChange={(e) => {
              setInnerRange(e.target.value);
              onRangeChange?.(e.target.value);
            }}
            optionType="button"
            buttonStyle="solid"
            options={ranges}
          />
        ) : undefined
      }
      bodyStyle={{ padding: 8 }}
      style={{ borderRadius: 8 }}
    >
      <div style={{ position: 'relative' }}>
        <ChartContainer option={option} height={height} />
        {loading && (
          <div
            style={{
              position: 'absolute',
              inset: 0,
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              background: 'rgba(255, 255, 255, 0.6)',
            }}
          >
            <Spin />
          </div>
        )}
      </div>
    </Card>
  );
};

export default TrendChart;
