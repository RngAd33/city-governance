// ============================================================
// ContextPanel - Right side panel with related data, sources,
// confidence indicator, and related alarms
// ============================================================
import React from 'react';
import { Card, List, Tag, Progress, Empty, Space } from 'antd';
import {
  FileTextOutlined,
  DatabaseOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import type { SourceCitation, AlarmItem, Level } from '@/types';

interface ContextPanelProps {
  relatedData: { id: string; title: string; value: string; unit?: string; level?: Level }[];
  sources: SourceCitation[];
  confidence: number;
  relatedAlarms: AlarmItem[];
}

const LEVEL_COLORS: Record<Level, string> = {
  normal: 'success',
  warning: 'warning',
  danger: 'error',
  critical: 'error',
};

const LEVEL_LABELS: Record<Level, string> = {
  normal: '正常',
  warning: '警告',
  danger: '危险',
  critical: '严重',
};

const ContextPanel: React.FC<ContextPanelProps> = ({
  relatedData,
  sources,
  confidence,
  relatedAlarms,
}) => {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16, height: '100%', overflowY: 'auto' }}>
      {/* Confidence Indicator */}
      <Card title="置信度" size="small">
        <div style={{ textAlign: 'center', padding: '8px 0' }}>
          <Progress
            type="circle"
            percent={confidence}
            size={80}
            strokeColor={confidence >= 80 ? '#52c41a' : confidence >= 60 ? '#faad14' : '#ff4d4f'}
            format={(pct) => <span style={{ fontSize: 18, fontWeight: 600 }}>{pct}%</span>}
          />
          <div style={{ marginTop: 8, fontSize: 12, color: '#8c8c8c' }}>
            {confidence >= 80 ? '高置信度' : confidence >= 60 ? '中等置信度' : '低置信度'}
          </div>
        </div>
      </Card>

      {/* Related Data */}
      <Card title="相关数据" size="small">
        {relatedData.length === 0 ? (
          <Empty description="暂无相关数据" image={Empty.PRESENTED_IMAGE_SIMPLE} style={{ padding: '16px 0' }} />
        ) : (
          <List
            size="small"
            dataSource={relatedData}
            renderItem={(item) => (
              <List.Item style={{ padding: '8px 0' }}>
                <div style={{ width: '100%', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Space>
                    <DatabaseOutlined style={{ color: '#1890ff' }} />
                    <span style={{ fontSize: 13 }}>{item.title}</span>
                  </Space>
                  <Space>
                    <span style={{ fontWeight: 600, fontSize: 14 }}>{item.value}</span>
                    {item.unit && <span style={{ fontSize: 12, color: '#8c8c8c' }}>{item.unit}</span>}
                    {item.level && (
                      <Tag color={LEVEL_COLORS[item.level]} style={{ marginInlineEnd: 0, fontSize: 11 }}>
                        {LEVEL_LABELS[item.level]}
                      </Tag>
                    )}
                  </Space>
                </div>
              </List.Item>
            )}
          />
        )}
      </Card>

      {/* Source Documents */}
      <Card title="参考来源" size="small">
        {sources.length === 0 ? (
          <Empty description="暂无参考来源" image={Empty.PRESENTED_IMAGE_SIMPLE} style={{ padding: '16px 0' }} />
        ) : (
          <List
            size="small"
            dataSource={sources}
            renderItem={(source) => (
              <List.Item style={{ padding: '8px 0' }}>
                <div style={{ width: '100%' }}>
                  <Space>
                    <FileTextOutlined style={{ color: '#1890ff' }} />
                    <a
                      href={source.url || undefined}
                      target="_blank"
                      rel="noopener noreferrer"
                      style={{ fontSize: 13 }}
                    >
                      {source.title}
                    </a>
                  </Space>
                  <div style={{ marginTop: 4, marginLeft: 24 }}>
                    <Tag style={{ fontSize: 11 }}>
                      {source.type === 'document' ? '文档' : source.type === 'data' ? '数据' : '法规'}
                    </Tag>
                  </div>
                </div>
              </List.Item>
            )}
          />
        )}
      </Card>

      {/* Related Alarms */}
      <Card title="相关告警" size="small">
        {relatedAlarms.length === 0 ? (
          <Empty description="暂无相关告警" image={Empty.PRESENTED_IMAGE_SIMPLE} style={{ padding: '16px 0' }} />
        ) : (
          <List
            size="small"
            dataSource={relatedAlarms}
            renderItem={(alarm) => (
              <List.Item style={{ padding: '8px 0' }}>
                <div style={{ width: '100%' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
                    <Space>
                      {alarm.severity === 'normal' ? (
                        <CheckCircleOutlined style={{ color: '#52c41a' }} />
                      ) : (
                        <ExclamationCircleOutlined style={{ color: alarm.severity === 'warning' ? '#faad14' : '#ff4d4f' }} />
                      )}
                      <span style={{ fontSize: 13, fontWeight: 500 }}>{alarm.title}</span>
                    </Space>
                    <Tag color={LEVEL_COLORS[alarm.severity]} style={{ marginInlineEnd: 0, fontSize: 11 }}>
                      {LEVEL_LABELS[alarm.severity]}
                    </Tag>
                  </div>
                  <div style={{ fontSize: 12, color: '#8c8c8c', marginLeft: 24 }}>
                    {alarm.location} · {alarm.timestamp}
                  </div>
                </div>
              </List.Item>
            )}
          />
        )}
      </Card>
    </div>
  );
};

export default ContextPanel;
