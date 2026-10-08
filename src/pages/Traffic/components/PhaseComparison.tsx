import React from 'react';
import { Tag, Empty } from 'antd';
import { ArrowUpOutlined, ArrowDownOutlined } from '@ant-design/icons';
import type { SignalPhase } from '@/types';

interface PhaseComparisonProps {
  /** 优化前配时方案（本次仿真提交的当前配时） */
  before: SignalPhase[];
  /** 优化后配时方案（后端优化算法输出） */
  after: SignalPhase[];
}

// 配时条各灯色的分段定义（按周期占比渲染）
const SEGMENTS: Array<{ key: 'greenTime' | 'yellowTime' | 'redTime'; label: string; color: string }> = [
  { key: 'greenTime', label: '绿灯', color: '#52c41a' },
  { key: 'yellowTime', label: '黄灯', color: '#fadb14' },
  { key: 'redTime', label: '红灯', color: '#ff4d4f' },
];

const directionLabel = (direction: string) =>
  direction === 'NS' ? '南北向 (NS)' : direction === 'EW' ? '东西向 (EW)' : `${direction} 方向`;

const phaseCycle = (phase: SignalPhase) =>
  Math.max(1, (phase.greenTime ?? 0) + (phase.yellowTime ?? 0) + (phase.redTime ?? 0));

// 单行配时条：按绿/黄/红时长在周期中的占比渲染分段
const PhaseBar: React.FC<{ phase: SignalPhase; tone: 'before' | 'after' }> = ({ phase, tone }) => {
  const total = phaseCycle(phase);
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
      <span style={{ width: 52, fontSize: 12, color: '#8c8c8c', flexShrink: 0 }}>
        {tone === 'before' ? '优化前' : '优化后'}
      </span>
      <div
        style={{
          flex: 1,
          display: 'flex',
          height: 18,
          borderRadius: 4,
          overflow: 'hidden',
          background: '#f5f5f5',
          opacity: tone === 'before' ? 0.8 : 1,
        }}
      >
        {SEGMENTS.map((seg) => {
          const value = Number(phase[seg.key] ?? 0);
          if (value <= 0) return null;
          return (
            <div
              key={seg.key}
              title={`${seg.label} ${value}s`}
              style={{
                width: `${(value / total) * 100}%`,
                background: seg.color,
                color: seg.key === 'yellowTime' ? '#613400' : '#fff',
                fontSize: 11,
                lineHeight: '18px',
                textAlign: 'center',
                whiteSpace: 'nowrap',
                overflow: 'hidden',
              }}
            >
              {value}s
            </div>
          );
        })}
      </div>
      <span style={{ width: 76, fontSize: 12, color: '#595959', textAlign: 'right', flexShrink: 0 }}>
        周期 {total}s
      </span>
    </div>
  );
};

// 单方向对比块：优化前/后配时条 + 绿灯时长变化标记
const DirectionBlock: React.FC<{
  direction: string;
  before?: SignalPhase;
  after?: SignalPhase;
}> = ({ direction, before, after }) => {
  const greenDelta =
    before && after ? (after.greenTime ?? 0) - (before.greenTime ?? 0) : 0;
  return (
    <div style={{ padding: 12, border: '1px solid #f0f0f0', borderRadius: 8, marginBottom: 12 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
        <Tag color="blue" style={{ marginInlineEnd: 0 }}>
          {directionLabel(direction)}
        </Tag>
        {before && after && greenDelta !== 0 && (
          <span style={{ fontSize: 12, color: greenDelta > 0 ? '#52c41a' : '#ff4d4f' }}>
            {greenDelta > 0 ? <ArrowUpOutlined /> : <ArrowDownOutlined />} 绿灯 {greenDelta > 0 ? '+' : ''}
            {greenDelta}s
          </span>
        )}
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
        {before && <PhaseBar phase={before} tone="before" />}
        {after && <PhaseBar phase={after} tone="after" />}
      </div>
    </div>
  );
};

/**
 * 信号配时方案对比
 * 按方向对齐优化前后相位，用绿/黄/红分段条直观展示配时变化
 */
const PhaseComparison: React.FC<PhaseComparisonProps> = ({ before, after }) => {
  const directions = Array.from(
    new Set([...after.map((p) => p.direction), ...before.map((p) => p.direction)]),
  );

  if (directions.length === 0) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无配时方案数据" />;
  }

  return (
    <div style={{ padding: '4px 0' }}>
      {directions.map((direction) => (
        <DirectionBlock
          key={direction}
          direction={direction}
          before={before.find((p) => p.direction === direction)}
          after={after.find((p) => p.direction === direction)}
        />
      ))}
      {/* 图例 */}
      <div style={{ display: 'flex', gap: 16, fontSize: 12, color: '#8c8c8c' }}>
        {SEGMENTS.map((seg) => (
          <span key={seg.key}>
            <span
              style={{
                display: 'inline-block',
                width: 10,
                height: 10,
                borderRadius: 2,
                background: seg.color,
                marginRight: 4,
              }}
            />
            {seg.label}
          </span>
        ))}
      </div>
    </div>
  );
};

export default PhaseComparison;
