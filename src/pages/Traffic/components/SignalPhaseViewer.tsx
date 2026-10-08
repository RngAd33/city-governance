import React, { useState, useEffect, useCallback } from 'react';
import { Card, Tag, Slider, Statistic, Row, Col, Space } from 'antd';
import {
  ClockCircleOutlined,
} from '@ant-design/icons';
import type { SignalPhase } from '@/types';

interface SignalPhaseViewerProps {
  phases: SignalPhase[];
  currentPhaseIndex?: number;
  isSimulating?: boolean;
  /** 卡片标题（默认“信号相位”） */
  title?: string;
}

const SignalPhaseViewer: React.FC<SignalPhaseViewerProps> = ({
  phases,
  currentPhaseIndex = 0,
  isSimulating = false,
  title = '信号相位',
}) => {
  const [activePhaseIndex, setActivePhaseIndex] = useState(currentPhaseIndex);
  const [progress, setProgress] = useState(0);
  const [isAnimating, setIsAnimating] = useState(false);

  const totalDuration = phases.reduce((sum, phase) => sum + (phase.duration || 0), 0);
  const activePhase = phases[activePhaseIndex];

  const getPhaseColor = (color?: string) => {
    const colorMap: Record<string, string> = {
      green: '#52c41a',
      yellow: '#fadb14',
      red: '#ff4d4f',
    };
    return colorMap[color || 'green'] || '#52c41a';
  };

  const getPhaseName = (color?: string) => {
    const nameMap: Record<string, string> = {
      green: '绿灯',
      yellow: '黄灯',
      red: '红灯',
    };
    return nameMap[color || 'green'] || '绿灯';
  };

  const animatePhaseTransition = useCallback(() => {
    if (phases.length === 0) return;

    setIsAnimating(true);
    setProgress(0);

    const duration = activePhase?.duration || 5;
    const interval = 50;
    const steps = (duration * 1000) / interval;
    let currentStep = 0;

    const timer = setInterval(() => {
      currentStep++;
      setProgress((currentStep / steps) * 100);

      if (currentStep >= steps) {
        clearInterval(timer);
        setActivePhaseIndex((prev) => (prev + 1) % phases.length);
        setIsAnimating(false);
      }
    }, interval);

    return () => clearInterval(timer);
  }, [activePhase, phases.length]);

  useEffect(() => {
    if (isSimulating) {
      const cleanup = animatePhaseTransition();
      return cleanup;
    }
  }, [isSimulating, animatePhaseTransition, activePhaseIndex]);

  useEffect(() => {
    setActivePhaseIndex(currentPhaseIndex);
  }, [currentPhaseIndex]);

  if (phases.length === 0) {
    return (
      <Card title={title} size="small" style={{ width: '100%' }}>
        <div style={{ textAlign: 'center', color: '#8c8c8c', padding: 24 }}>
          暂无信号相位数据
        </div>
      </Card>
    );
  }

  return (
    <Card
      title={
        <Space>
          <ClockCircleOutlined />
          <span>{title}</span>
          {isSimulating && <Tag color="processing">实时模拟中</Tag>}
        </Space>
      }
      size="small"
      style={{ width: '100%' }}
      extra={
        <Tag color={activePhase ? getPhaseColor(activePhase.color) : 'default'}>
          {activePhase ? getPhaseName(activePhase.color) : '-'}
        </Tag>
      }
    >
      <div style={{ padding: '8px 0' }}>
        {/* Circular Phase Diagram */}
        <div
          style={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            marginBottom: 16,
          }}
        >
          <div
            style={{
              position: 'relative',
              width: 160,
              height: 160,
              borderRadius: '50%',
              background: `conic-gradient(${phases
                .map((phase, index) => {
                  const startAngle =
                    phases.slice(0, index).reduce((sum, p) => sum + (p.duration || 0), 0) *
                    (360 / (totalDuration || 1));
                  const endAngle =
                    phases.slice(0, index + 1).reduce((sum, p) => sum + (p.duration || 0), 0) *
                    (360 / (totalDuration || 1));
                  const color = getPhaseColor(phase.color);
                  return `${color} ${startAngle}deg ${endAngle}deg`;
                })
                .join(', ')})`,
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              boxShadow: isAnimating ? '0 0 20px rgba(24, 144, 255, 0.5)' : 'none',
              transition: 'box-shadow 0.3s ease',
            }}
          >
            <div
              style={{
                width: 100,
                height: 100,
                borderRadius: '50%',
                background: '#fff',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'center',
                alignItems: 'center',
                boxShadow: 'inset 0 2px 8px rgba(0,0,0,0.1)',
              }}
            >
              <div style={{ fontSize: 28, fontWeight: 600, color: '#262626' }}>
                {(activePhase?.duration || 0).toFixed(0)}s
              </div>
              <div style={{ fontSize: 12, color: '#8c8c8c' }}>当前相位时长</div>
            </div>
          </div>
        </div>

        {/* Phase Progress */}
        {isAnimating && activePhase && (
          <div style={{ marginBottom: 16 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
              <span style={{ fontSize: 12, color: '#8c8c8c' }}>
                {activePhase.name} - {activePhase.direction}
              </span>
              <span style={{ fontSize: 12, color: '#8c8c8c' }}>
                {Math.round(progress)}%
              </span>
            </div>
            <Slider
              value={progress}
              disabled
              tooltip={{ formatter: null }}
              styles={{
                track: { background: getPhaseColor(activePhase.color) },
              }}
            />
          </div>
        )}

        {/* Phase List */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {phases.map((phase, index) => {
            const isActive = index === activePhaseIndex;
            return (
              <div
                key={phase.phaseId}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  padding: '8px 12px',
                  borderRadius: 6,
                  background: isActive ? '#f0f5ff' : '#fafafa',
                  border: isActive ? '1px solid #91caff' : '1px solid transparent',
                  transition: 'all 0.3s ease',
                }}
              >
                <div
                  style={{
                    width: 12,
                    height: 12,
                    borderRadius: '50%',
                    background: getPhaseColor(phase.color),
                    marginRight: 12,
                    boxShadow: isActive ? `0 0 8px ${getPhaseColor(phase.color)}` : 'none',
                  }}
                />
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 13, fontWeight: isActive ? 500 : 400 }}>
                    {(phase as any).name || phase.direction}
                  </div>
                  <div style={{ fontSize: 11, color: '#8c8c8c' }}>{phase.direction}</div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: 13, fontWeight: 500 }}>{(phase.duration || 0).toFixed(0)}s</div>
                  <div style={{ fontSize: 11, color: '#8c8c8c' }}>
                    {getPhaseName(phase.color)}
                  </div>
                </div>
                {isActive && (
                  <Tag color="processing" style={{ marginLeft: 8 }}>
                    当前
                  </Tag>
                )}
              </div>
            );
          })}
        </div>

        {/* Summary Stats */}
        <Row gutter={16} style={{ marginTop: 16 }}>
          <Col span={8}>
            <Statistic
              title="相位数量"
              value={phases.length}
              suffix="个"
              valueStyle={{ fontSize: 18 }}
            />
          </Col>
          <Col span={8}>
            <Statistic
              title="周期时长"
              value={totalDuration}
              suffix="秒"
              valueStyle={{ fontSize: 18 }}
            />
          </Col>
          <Col span={8}>
            <Statistic
              title="当前相位"
              value={activePhaseIndex + 1}
              suffix={`/${phases.length}`}
              valueStyle={{ fontSize: 18 }}
            />
          </Col>
        </Row>
      </div>
    </Card>
  );
};

export default SignalPhaseViewer;
