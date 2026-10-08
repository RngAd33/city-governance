import React, { useCallback, useEffect, useRef } from 'react';
import { Card, Empty, Spin, Tooltip } from 'antd';
import { FireOutlined, InfoCircleOutlined } from '@ant-design/icons';
import * as echarts from '@/utils/echarts';
import type { Building } from '@/types';


const buildingTypeColors: Record<string, string> = {
  office: '#1890ff',
  hospital: '#ff4d4f',
  commercial: '#52c41a',
  residential: '#fa8c16',
  school: '#722ed1',
};

const buildingTypeLabels: Record<string, string> = {
  office: '办公楼用电',
  hospital: '医院用电',
  commercial: '商业用电',
  residential: '居民用电',
  school: '学校用电',
};

interface BuildingRankingProps {
  data: Building[];
  loading?: boolean;
  onBuildingClick?: (building: Building) => void;
}

const BuildingRanking: React.FC<BuildingRankingProps> = ({
  data,
  loading = false,
  onBuildingClick,
}) => {
  const chartRef = useRef<HTMLDivElement>(null);
  const chartInstanceRef = useRef<echarts.ECharts | null>(null);

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
    if (!chartInstanceRef.current || data.length === 0) return;

    // 按能耗升序排序，配合类目轴自下而上的渲染顺序，实现从高到低的排行展示
    const sortedData = [...data].sort(
      (a, b) => (a.avgDailyConsumption ?? 0) - (b.avgDailyConsumption ?? 0)
    );

    const option: echarts.EChartsOption = {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter: (params: unknown) => {
          const p = params as Array<{ name: string; value: number; dataIndex: number }>;
          const item = sortedData[p[0].dataIndex];
          if (!item) return '';
          const consumption = item.avgDailyConsumption ?? 0;
          const typeLabel = buildingTypeLabels[(item.type || '').toLowerCase()] || item.type;
          return `
            <div style="padding: 4px 8px;">
              <div style="font-weight: 500; margin-bottom: 4px;">${item.name}</div>
              <div>能耗: <strong>${consumption.toFixed(1)}</strong> kWh</div>
              <div>费用: ¥${(consumption * 0.8).toFixed(2)}</div>
              <div>碳排放: ${(consumption * 0.58).toFixed(1)} kg</div>
              <div>类型: ${typeLabel}</div>
            </div>
          `;
        },
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        top: '8%',
        containLabel: true,
      },
      xAxis: {
        type: 'value',
        axisLabel: {
          formatter: '{value} kWh',
          color: '#8c8c8c',
        },
        splitLine: { lineStyle: { type: 'dashed', color: '#f0f0f0' } },
      },
      yAxis: {
        type: 'category',
        data: sortedData.map((item) => item.name),
        axisLabel: { color: '#595959', fontSize: 11 },
        axisLine: { show: false },
        axisTick: { show: false },
      },
      series: [
        {
          name: '能耗',
          type: 'bar',
          data: sortedData.map((item) => ({
            value: item.avgDailyConsumption,
            itemStyle: {
              color: buildingTypeColors[(item.type || '').toLowerCase()] || '#1890ff',
              borderRadius: [0, 4, 4, 0],
            },
          })),
          barWidth: '64%',
          label: {
            show: true,
            position: 'right',
            formatter: '{c} kWh',
            fontSize: 10,
            color: '#8c8c8c',
          },
        },
      ],
    };

    chartInstanceRef.current.setOption(option, true);
    // 容器高度随数据条数变化（chartHeight），需同步重算画布尺寸
    chartInstanceRef.current.resize();
  }, [data]);

  const handleBarClick = useCallback(
    (params: { dataIndex: number }) => {
      // 点击下标对应排序后的展示顺序
      const sortedData = [...data].sort(
        (a, b) => (a.avgDailyConsumption ?? 0) - (b.avgDailyConsumption ?? 0)
      );
      const building = sortedData[params.dataIndex];
      if (building) {
        onBuildingClick?.(building);
      }
    },
    [data, onBuildingClick]
  );

  useEffect(() => {
    if (!chartInstanceRef.current) return;
    chartInstanceRef.current.off('click');
    chartInstanceRef.current.on('click', handleBarClick);
  }, [handleBarClick]);

  // 每根柱条分配更高的分类带高度（条宽+间距之和固定占分类带），使条宽与条间距同步加大
  const chartHeight = Math.max(320, Math.min(data.length * 48, 560));

  return (
    <Card
      title={
        <span>
          <FireOutlined style={{ marginRight: 8, color: '#ff4d4f' }} />
          建筑能耗排名
        </span>
      }
      size="small"
      extra={
        <Tooltip title="点击柱状图查看建筑详情">
          <InfoCircleOutlined style={{ color: '#8c8c8c' }} />
        </Tooltip>
      }
      style={{ height: '100%' }}
      bodyStyle={{ padding: 0, height: 'calc(100% - 46px)' }}
    >
      <div style={{ position: 'relative', height: chartHeight }}>
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
            <Spin size="large" tip="加载排名数据..." />
          </div>
        )}
        {!loading && data.length === 0 && (
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
            <Empty description="暂无能耗数据" />
          </div>
        )}
      </div>
    </Card>
  );
};

export default BuildingRanking;
