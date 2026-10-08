// ============================================================
// RiskAssessment - Risk level cards for water stations
// ============================================================
import React from 'react';
import { Card, Tag, Progress, Row, Col, Empty } from 'antd';
import {
  ArrowUpOutlined,
  ArrowDownOutlined,
  DashOutlined,
} from '@ant-design/icons';
import type { WaterStation, Level } from '@/types';

const LEVEL_COLORS: Record<Level, string> = {
  normal: '#52c41a',
  warning: '#faad14',
  danger: '#ff4d4f',
  critical: '#a8071a',
};

const LEVEL_LABELS: Record<Level, string> = {
  normal: '正常',
  warning: '警戒',
  danger: '危险',
  critical: '严重',
};

const LEVEL_TAG_COLORS: Record<Level, string> = {
  normal: 'success',
  warning: 'warning',
  danger: 'error',
  critical: 'error',
};

interface RiskAssessmentProps {
  stations: WaterStation[];
  onStationClick?: (stationId: string) => void;
}

const RiskAssessment: React.FC<RiskAssessmentProps> = ({ stations, onStationClick }) => {
  const getTrendIcon = (trend: WaterStation['trend']) => {
    switch (trend) {
      case 'rising':
        return <ArrowUpOutlined style={{ color: '#ff4d4f' }} />;
      case 'falling':
        return <ArrowDownOutlined style={{ color: '#52c41a' }} />;
      default:
        return <DashOutlined style={{ color: '#8c8c8c' }} />;
    }
  };

  const getTrendText = (trend: WaterStation['trend']) => {
    switch (trend) {
      case 'rising':
        return '上升';
      case 'falling':
        return '下降';
      default:
        return '平稳';
    }
  };

  const getTimeToDanger = (station: WaterStation): string => {
    if (station.trend !== 'rising') return '--';
    const remaining = station.dangerLevel - station.currentLevel;
    if (remaining <= 0) return '已超限';
    // Assume rising rate of 0.1m per hour for estimation
    const hours = remaining / 0.1;
    if (hours < 1) return '< 1小时';
    if (hours < 24) return `约 ${Math.round(hours)} 小时`;
    return `约 ${Math.round(hours / 24)} 天`;
  };

  const getRecommendedActions = (station: WaterStation): string[] => {
    const actions: string[] = [];
    if (station.status === 'normal') {
      actions.push('常规监测');
      actions.push('定期巡查');
    } else if (station.status === 'warning') {
      actions.push('加强监测频率');
      actions.push('通知相关部门');
      actions.push('检查排水设施');
    } else if (station.status === 'danger') {
      actions.push('启动应急预案');
      actions.push('疏散低洼地区人员');
      actions.push('开启所有排水泵');
      actions.push('持续监测水位变化');
    } else {
      actions.push('立即启动最高级别响应');
      actions.push('全面疏散受影响区域');
      actions.push('调动所有应急资源');
      actions.push('实时上报指挥中心');
    }
    return actions;
  };

  const getProgressPercent = (station: WaterStation): number => {
    const range = station.dangerLevel - station.warningLevel * 0.5;
    const value = station.currentLevel - station.warningLevel * 0.5;
    return Math.min(100, Math.max(0, (value / range) * 100));
  };

  if (stations.length === 0) {
    return <Empty description="暂无水位站数据" style={{ marginTop: 40 }} />;
  }

  return (
    <div style={{ maxHeight: 400, overflowY: 'auto', paddingRight: 4 }}>
      <Row gutter={[12, 12]}>
        {stations.map((station) => {
          const color = LEVEL_COLORS[station.status];
          const actions = getRecommendedActions(station);
          const timeToDanger = getTimeToDanger(station);

          return (
            <Col xs={24} sm={12} lg={8} key={station.id}>
              <Card
                hoverable
                size="small"
                onClick={() => onStationClick?.(station.id)}
                style={{
                  borderLeft: `4px solid ${color}`,
                  height: '100%',
                }}
                styles={{ body: { padding: '16px' } }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12 }}>
                  <div>
                    <div style={{ fontWeight: 600, fontSize: 15, marginBottom: 4 }}>{station.name}</div>
                    <div style={{ fontSize: 12, color: '#8c8c8c' }}>更新时间: {station.updatedAt}</div>
                  </div>
                  <Tag color={LEVEL_TAG_COLORS[station.status]} style={{ marginInlineEnd: 0 }}>
                    {LEVEL_LABELS[station.status]}
                  </Tag>
                </div>

                <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, marginBottom: 8 }}>
                  <span style={{ fontSize: 24, fontWeight: 700, color }}>{station.currentLevel}</span>
                  <span style={{ fontSize: 12, color: '#8c8c8c' }}>m</span>
                  <span style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 4, fontSize: 12 }}>
                    {getTrendIcon(station.trend)}
                    <span style={{ color: station.trend === 'rising' ? '#ff4d4f' : station.trend === 'falling' ? '#52c41a' : '#8c8c8c' }}>
                      {getTrendText(station.trend)}
                    </span>
                  </span>
                </div>

                <Progress
                  percent={getProgressPercent(station)}
                  showInfo={false}
                  strokeColor={color}
                  size="small"
                  style={{ marginBottom: 12 }}
                />

                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, marginBottom: 8 }}>
                  <span style={{ color: '#8c8c8c' }}>警戒: {station.warningLevel}m</span>
                  <span style={{ color: '#8c8c8c' }}>危险: {station.dangerLevel}m</span>
                </div>

                <div style={{
                  background: '#fafafa',
                  borderRadius: 6,
                  padding: '8px 12px',
                  marginBottom: 12,
                  fontSize: 12,
                }}>
                  <span style={{ color: '#8c8c8c' }}>预计到达危险水位: </span>
                  <strong style={{ color: timeToDanger === '已超限' ? '#ff4d4f' : '#262626' }}>{timeToDanger}</strong>
                </div>

                <div style={{ fontSize: 12, color: '#595959', marginBottom: 8 }}>
                  <strong>建议措施:</strong>
                </div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}>
                  {actions.map((action, idx) => (
                    <Tag key={idx} style={{ fontSize: 11 }}>{action}</Tag>
                  ))}
                </div>
              </Card>
            </Col>
          );
        })}
      </Row>
    </div>
  );
};

export default RiskAssessment;
