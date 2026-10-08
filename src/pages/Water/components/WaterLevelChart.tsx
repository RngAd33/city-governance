// ============================================================
// WaterLevelChart - ECharts line chart for water level trends
// with warning/danger reference lines and rainfall overlay
// ============================================================
import React, { useEffect, useRef, useCallback } from 'react';
import * as echarts from '@/utils/echarts';
import { Segmented } from 'antd';
import type { WaterStation } from '@/types';

interface WaterLevelChartProps {
  stations: WaterStation[];
  data: Record<string, { time: string; value: number }[]>;
  rainfallData: { time: string; value: number }[];
  selectedStation: string;
  onStationChange: (stationId: string) => void;
  title?: string;
}

const WaterLevelChart: React.FC<WaterLevelChartProps> = ({
  stations,
  data,
  rainfallData,
  selectedStation,
  onStationChange,
  title = '水位趋势',
}) => {
  const chartRef = useRef<HTMLDivElement>(null);
  const instanceRef = useRef<echarts.ECharts | null>(null);

  const station = stations.find((s) => s.id === selectedStation);
  const stationData = data[selectedStation] || [];

  const renderChart = useCallback(() => {
    if (!chartRef.current) return;

    if (!instanceRef.current) {
      instanceRef.current = echarts.init(chartRef.current);
    }

    const chart = instanceRef.current;
    const warningLevel = station?.warningLevel ?? 3.0;
    const dangerLevel = station?.dangerLevel ?? 4.5;

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
          if (!Array.isArray(params) || params.length === 0) return '';
          const time = params[0].axisValue;
          let html = `<div style="min-width: 160px;"><strong>${time}</strong>`;
          params.forEach((p: any) => {
            if (p.seriesName === '降雨量') {
              html += `<br/>${p.marker} ${p.seriesName}: <strong>${p.value} mm</strong>`;
            } else {
              html += `<br/>${p.marker} ${p.seriesName}: <strong>${p.value} m</strong>`;
            }
          });
          html += '</div>';
          return html;
        },
      },
      legend: {
        data: ['水位', '降雨量'],
        bottom: 0,
        textStyle: { fontSize: 11 },
      },
      grid: {
        left: 60,
        right: 60,
        top: 50,
        bottom: 50,
      },
      xAxis: {
        type: 'category',
        data: stationData.map((d) => d.time),
        axisLabel: { fontSize: 11 },
        boundaryGap: false,
      },
      yAxis: [
        {
          type: 'value',
          name: '水位 (m)',
          nameTextStyle: { fontSize: 11, color: '#8c8c8c' },
          axisLabel: { fontSize: 11 },
          splitLine: { lineStyle: { type: 'dashed' } },
        },
        {
          type: 'value',
          name: '降雨量 (mm)',
          nameTextStyle: { fontSize: 11, color: '#8c8c8c' },
          axisLabel: { fontSize: 11 },
          splitLine: { show: false },
          inverse: true,
        },
      ],
      series: [
        {
          name: '水位',
          type: 'line',
          data: stationData.map((d) => d.value),
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          lineStyle: { width: 2, color: '#1890ff' },
          itemStyle: { color: '#1890ff' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#1890ff40' },
              { offset: 1, color: '#1890ff05' },
            ]),
          },
          markLine: {
            silent: true,
            symbol: 'none',
            data: [
              {
                yAxis: warningLevel,
                lineStyle: { color: '#faad14', type: 'dashed', width: 2 },
                label: { formatter: `警戒水位 ${warningLevel}m`, fontSize: 10, color: '#faad14' },
              },
              {
                yAxis: dangerLevel,
                lineStyle: { color: '#ff4d4f', type: 'dashed', width: 2 },
                label: { formatter: `危险水位 ${dangerLevel}m`, fontSize: 10, color: '#ff4d4f' },
              },
            ],
          },
        },
        {
          name: '降雨量',
          type: 'bar',
          yAxisIndex: 1,
          data: rainfallData.map((d) => d.value),
          barWidth: '40%',
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#69c0ff' },
              { offset: 1, color: '#bae7ff' },
            ]),
            borderRadius: [2, 2, 0, 0],
          },
        },
      ],
      dataZoom: [
        { type: 'inside', start: 0, end: 100 },
        { type: 'slider', start: 0, end: 100, height: 20, bottom: 25 },
      ],
    };

    chart.setOption(option, true);
  }, [stationData, rainfallData, station, title]);

  useEffect(() => {
    renderChart();
  }, [renderChart]);

  useEffect(() => {
    const handleResize = () => {
      instanceRef.current?.resize();
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  return (
    <div style={{ width: '100%', height: '100%' }}>
      <div style={{ marginBottom: 12, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Segmented
          options={stations.map((s) => ({ label: s.name, value: s.id }))}
          value={selectedStation}
          onChange={(v) => onStationChange(v as string)}
          size="small"
        />
        {station && (
          <span style={{ fontSize: 12, color: '#8c8c8c' }}>
            当前: <strong style={{ color: '#262626' }}>{station.currentLevel}m</strong>
            {' · '}警戒: {station.warningLevel}m · 危险: {station.dangerLevel}m
          </span>
        )}
      </div>
      <div ref={chartRef} style={{ width: '100%', height: 300 }} />
    </div>
  );
};

export default WaterLevelChart;
