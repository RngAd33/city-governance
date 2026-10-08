export const CITY_LIST = ['南京', '深圳', '武汉'];

export const METRIC_LIST = ['pm25', 'pm10', 'no2', 'o3', 'temperature', 'humidity', 'wind_speed'];

export const ALARM_LEVELS = {
  NORMAL: 'green',
  WARNING: 'yellow',
  DANGER: 'red',
} as const;

export const TRAFFIC_LEVELS = {
  LOW: 'green',
  MEDIUM: 'yellow',
  HIGH: 'orange',
  CRITICAL: 'red',
} as const;

export const API_BASE_URL = '/api';
