import React from 'react';
import { Tag } from 'antd';

interface StatusTagProps {
  level: string;
  label?: string;
}

const levelColorMap: Record<string, string> = {
  NORMAL: 'success',
  WARNING: 'warning',
  DANGER: 'error',
  LOW: 'success',
  MEDIUM: 'warning',
  HIGH: 'orange',
  CRITICAL: 'error',
  INFO: 'processing',
  normal: 'success',
  warning: 'warning',
  danger: 'error',
  low: 'success',
  medium: 'warning',
  high: 'orange',
  critical: 'error',
  info: 'processing',
};

const levelLabelMap: Record<string, string> = {
  NORMAL: '正常',
  WARNING: '预警',
  DANGER: '危险',
  LOW: '低',
  MEDIUM: '中',
  HIGH: '高',
  CRITICAL: '严重',
  INFO: '信息',
  normal: '正常',
  warning: '预警',
  danger: '危险',
  low: '低',
  medium: '中',
  high: '高',
  critical: '严重',
  info: '信息',
};

const StatusTag: React.FC<StatusTagProps> = ({ level, label }) => {
  return <Tag color={levelColorMap[level] || 'default'}>{label || levelLabelMap[level] || level}</Tag>;
};

export default StatusTag;
