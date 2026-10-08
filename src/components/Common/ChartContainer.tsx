import React, { useEffect, useRef } from 'react';
import * as echarts from '@/utils/echarts';
import { Card } from 'antd';

interface ChartContainerProps {
  option: echarts.EChartsOption;
  title?: string;
  height?: number;
  loading?: boolean;
  onChartReady?: (chart: echarts.ECharts) => void;
}

const ChartContainer: React.FC<ChartContainerProps> = ({
  option,
  title,
  height = 300,
  loading = false,
  onChartReady,
}) => {
  const chartRef = useRef<HTMLDivElement>(null);
  const chartInstance = useRef<echarts.ECharts | null>(null);

  useEffect(() => {
    if (!chartRef.current) return;

    chartInstance.current = echarts.init(chartRef.current);

    const handleResize = () => {
      chartInstance.current?.resize();
    };

    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
      chartInstance.current?.dispose();
    };
  }, []);

  useEffect(() => {
    if (chartInstance.current && option) {
      chartInstance.current.setOption(option, true);
      if (onChartReady) {
        onChartReady(chartInstance.current);
      }
    }
  }, [option]);

  return (
    <Card
      title={title}
      loading={loading}
      bodyStyle={{ padding: 8 }}
      style={{ borderRadius: 8 }}
    >
      <div ref={chartRef} style={{ width: '100%', height }} />
    </Card>
  );
};

export default ChartContainer;
