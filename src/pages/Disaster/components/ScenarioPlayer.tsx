// ============================================================
// ScenarioPlayer - Video-like player for disaster simulation
// ============================================================
import React, { useState, useEffect, useRef } from 'react';
import { Slider, Button, Space, Tag, Select, Row, Col, Tooltip } from 'antd';
import {
  PlayCircleOutlined,
  PauseCircleOutlined,
  StepForwardOutlined,
  StepBackwardOutlined,
  ReloadOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons';

interface ScenarioPlayerProps {
  /** 模拟总时长（分钟，与 city-disaster timeline/floodSpread 的 time 单位一致） */
  totalDuration: number;
  currentTime: number;
  isPlaying: boolean;
  speed: number;
  events: { time: number; label: string; type: 'info' | 'warning' | 'danger' }[];
  /** 当前时刻推演状态（水位/库容/风险），未生成模拟结果时不传 */
  statusText?: string;
  /** 未生成模拟结果时禁用播放控件（避免"在播放但地图无变化"的困惑） */
  disabled?: boolean;
  onTimeChange: (time: number) => void;
  onPlayPause: () => void;
  onSpeedChange: (speed: number) => void;
  onSeek: (time: number) => void;
}

const ScenarioPlayer: React.FC<ScenarioPlayerProps> = ({
  totalDuration,
  currentTime,
  isPlaying,
  speed,
  events,
  statusText,
  disabled = false,
  onTimeChange,
  onPlayPause,
  onSpeedChange,
  onSeek,
}) => {
  const [localTime, setLocalTime] = useState(currentTime);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    setLocalTime(currentTime);
  }, [currentTime]);

  useEffect(() => {
    if (isPlaying) {
      intervalRef.current = setInterval(() => {
        setLocalTime((prev) => {
          const next = prev + 0.1 * speed;
          if (next >= totalDuration) {
            onTimeChange(totalDuration);
            return totalDuration;
          }
          onTimeChange(next);
          return next;
        });
      }, 100);
    } else {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    }
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [isPlaying, speed, totalDuration, onTimeChange]);

  // 以“小时:分钟”渲染模拟时间（参数单位为分钟）
  const formatTime = (minutes: number): string => {
    const h = Math.floor(minutes / 60);
    const m = Math.floor(minutes % 60);
    return `${h}:${m.toString().padStart(2, '0')}`;
  };

  const handleSliderChange = (value: number) => {
    setLocalTime(value);
    onSeek(value);
  };

  const handleStepForward = () => {
    const next = Math.min(totalDuration, localTime + 10);
    setLocalTime(next);
    onSeek(next);
  };

  const handleStepBackward = () => {
    const prev = Math.max(0, localTime - 10);
    setLocalTime(prev);
    onSeek(prev);
  };

  const handleReset = () => {
    setLocalTime(0);
    onSeek(0);
  };

  const getEventColor = (type: 'info' | 'warning' | 'danger') => {
    switch (type) {
      case 'info': return 'processing';
      case 'warning': return 'warning';
      case 'danger': return 'error';
    }
  };

  return (
    <div style={{ padding: 16 }}>
      {/* Time Display */}
      <div style={{ textAlign: 'center', marginBottom: 16 }}>
        <span style={{ fontSize: 32, fontWeight: 700, fontFamily: 'monospace', color: '#262626' }}>
          {formatTime(localTime)}
        </span>
        <span style={{ fontSize: 14, color: '#8c8c8c', marginLeft: 8 }}>
          / {formatTime(totalDuration)}
        </span>
        {statusText && (
          <div style={{ marginTop: 8 }}>
            <Tag color="blue" style={{ fontFamily: 'monospace', fontSize: 12, marginInlineEnd: 0 }}>
              {statusText}
            </Tag>
          </div>
        )}
      </div>

      {/* Timeline Slider */}
      <div style={{ marginBottom: 16, padding: '0 8px' }}>
        <Slider
          min={0}
          max={totalDuration}
          value={localTime}
          onChange={handleSliderChange}
          tooltip={{ formatter: (val) => formatTime(val || 0) }}
          marks={
            events.reduce((acc, evt) => {
              acc[evt.time] = {
                label: (
                  <Tag color={getEventColor(evt.type)} style={{ fontSize: 10, padding: '0 4px' }}>
                    {evt.label}
                  </Tag>
                ),
              };
              return acc;
            }, {} as Record<number, { label: React.ReactNode }>)
          }
        />
      </div>

      {/* Controls */}
      <Row justify="center" align="middle" gutter={16}>
        <Col>
          <Space size="middle">
            <Button
              icon={<StepBackwardOutlined />}
              onClick={handleStepBackward}
              disabled={localTime <= 0}
              shape="circle"
            />
            <Tooltip title={disabled && !isPlaying ? '请先点击“开始模拟”生成推演结果' : undefined}>
              <Button
                type="primary"
                size="large"
                icon={isPlaying ? <PauseCircleOutlined /> : <PlayCircleOutlined />}
                onClick={onPlayPause}
                disabled={disabled && !isPlaying}
                shape="circle"
                style={{ width: 48, height: 48 }}
              />
            </Tooltip>
            <Button
              icon={<StepForwardOutlined />}
              onClick={handleStepForward}
              disabled={localTime >= totalDuration}
              shape="circle"
            />
            <Button
              icon={<ReloadOutlined />}
              onClick={handleReset}
              shape="circle"
            />
          </Space>
        </Col>
        <Col>
          <Space>
            <span style={{ fontSize: 12, color: '#8c8c8c' }}>速度:</span>
            <Select
              value={speed}
              onChange={onSpeedChange}
              size="small"
              style={{ width: 80 }}
              options={[
                { label: '0.5x', value: 0.5 },
                { label: '1x', value: 1 },
                { label: '2x', value: 2 },
                { label: '5x', value: 5 },
                { label: '10x', value: 10 },
                { label: '30x', value: 30 },
              ]}
            />
          </Space>
        </Col>
      </Row>

      {/* Event Markers */}
      {events.length > 0 && (
        <div style={{ marginTop: 16, display: 'flex', flexWrap: 'wrap', gap: 8, justifyContent: 'center' }}>
          {events.map((evt, idx) => (
            <Tag
              key={idx}
              color={getEventColor(evt.type)}
              style={{ cursor: 'pointer' }}
              onClick={() => {
                setLocalTime(evt.time);
                onSeek(evt.time);
              }}
            >
              <ClockCircleOutlined /> {evt.label} @ {formatTime(evt.time)}
            </Tag>
          ))}
        </div>
      )}
    </div>
  );
};

export default ScenarioPlayer;
