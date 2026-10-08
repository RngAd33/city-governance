import { format } from 'date-fns';
import { zhCN } from 'date-fns/locale';

export function formatNumber(value: number, decimals: number = 2): string {
  if (value === null || value === undefined) return '-';
  return Number(value).toFixed(decimals);
}

export function formatDateTime(isoString: string): string {
  if (!isoString) return '-';
  try {
    return format(new Date(isoString), 'yyyy-MM-dd HH:mm:ss', { locale: zhCN });
  } catch {
    return isoString;
  }
}

export function formatPercent(value: number): string {
  if (value === null || value === undefined) return '-';
  return `${(value * 100).toFixed(1)}%`;
}

export function getLevelColor(level: string): string {
  const colorMap: Record<string, string> = {
    normal: '#52c41a',
    warning: '#faad14',
    danger: '#ff4d4f',
    low: '#52c41a',
    medium: '#faad14',
    high: '#fa8c16',
    critical: '#ff4d4f',
  };
  return colorMap[level] || '#8c8c8c';
}

export function getMetricUnit(metric: string): string {
  const unitMap: Record<string, string> = {
    pm25: 'μg/m³',
    pm10: 'μg/m³',
    no2: 'μg/m³',
    o3: 'μg/m³',
    temperature: '°C',
    humidity: '%',
    wind_speed: 'm/s',
  };
  return unitMap[metric] || '';
}

export function getMetricLabel(metric: string): string {
  const labelMap: Record<string, string> = {
    pm25: 'PM2.5',
    pm10: 'PM10',
    no2: 'NO₂',
    o3: 'O₃',
    temperature: '温度',
    humidity: '湿度',
    wind_speed: '风速',
  };
  return labelMap[metric] || metric;
}
