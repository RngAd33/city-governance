import React from 'react';
import { Card, Row, Col, Tag, Progress, Empty } from 'antd';

export interface InfraFacilityStatus {
  facility: string;
  facilityName: string;
  statusScore: number | null;
  statusTime: string | null;
  alertLevel: number | null;
  alertTime: string | null;
}

// 告警级别展示元数据（0=正常 1=注意 2=警告 3=严重）
const ALERT_META: Record<number, { label: string; color: string }> = {
  0: { label: '正常', color: 'success' },
  1: { label: '注意', color: 'processing' },
  2: { label: '警告', color: 'warning' },
  3: { label: '严重', color: 'error' },
};

// 状态评分颜色（>=90 优 / >=75 良 / >=60 中 / 其余差）
const scoreColor = (score: number) => {
  if (score >= 90) return '#52c41a';
  if (score >= 75) return '#1890ff';
  if (score >= 60) return '#faad14';
  return '#ff4d4f';
};

const formatTime = (ts: string | null) => {
  if (!ts) return '--';
  const d = new Date(ts);
  if (Number.isNaN(d.getTime())) return ts;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};

interface InfraStatusPanelProps {
  facilities: InfraFacilityStatus[];
  selected?: string;
  onSelect?: (facility: string) => void;
}

/**
 * 设施实时状态卡片区（数据来自 city-infra 实时监控）
 * 点击卡片可联动下方状态趋势图
 */
const InfraStatusPanel: React.FC<InfraStatusPanelProps> = ({ facilities, selected, onSelect }) => {
  if (facilities.length === 0) {
    return <Empty description="暂无设施状态数据：采集端每10秒上报一轮" style={{ marginTop: 40 }} />;
  }

  return (
    <Row gutter={[12, 12]}>
      {facilities.map((f) => {
        const alert = f.alertLevel != null ? ALERT_META[f.alertLevel] : null;
        const score = f.statusScore ?? 0;
        const active = selected === f.facility;
        return (
          <Col xs={24} sm={12} xl={8} key={f.facility}>
            <Card
              size="small"
              hoverable
              onClick={() => onSelect?.(f.facility)}
              style={active ? { borderColor: '#1890ff' } : undefined}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
                <span style={{ fontWeight: 600 }}>{f.facilityName}</span>
                {alert ? (
                  <Tag color={alert.color} style={{ marginInlineEnd: 0 }}>{alert.label}</Tag>
                ) : (
                  <Tag style={{ marginInlineEnd: 0 }}>无数据</Tag>
                )}
              </div>
              <Progress
                percent={Math.round(score)}
                strokeColor={scoreColor(score)}
                format={() => `${(f.statusScore ?? 0).toFixed(1)} 分`}
              />
              <div style={{ marginTop: 8, fontSize: 12, color: '#8c8c8c' }}>
                最后更新：{formatTime(f.statusTime)}
              </div>
            </Card>
          </Col>
        );
      })}
    </Row>
  );
};

export default InfraStatusPanel;
