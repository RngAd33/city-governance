import React from 'react';
import { Card, Progress, Tag, List, Typography, Space, Divider, Alert } from 'antd';
import {
  BulbOutlined,
  CheckCircleOutlined,

  ThunderboltOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons';


const { Paragraph, Text } = Typography;

interface AIConclusionProps {
  conclusion: any | null;
  loading?: boolean;
}

const AIConclusion: React.FC<AIConclusionProps> = ({ conclusion, loading = false }) => {
  if (loading) {
    return (
      <Card title="AI 优化结论" size="small" loading={loading} style={{ width: '100%' }} />
    );
  }

  if (!conclusion) {
    return (
      <Card title="AI 优化结论" size="small" style={{ width: '100%' }}>
        <Alert
          message="暂无分析结论"
          description="请先运行交通仿真模拟，系统将自动生成AI优化分析报告。"
          type="info"
          showIcon
        />
      </Card>
    );
  }

  const getConfidenceColor = (score: number) => {
    if (score >= 80) return '#52c41a';
    if (score >= 60) return '#fa8c16';
    return '#ff4d4f';
  };

  const getConfidenceLabel = (score: number) => {
    if (score >= 80) return '高置信度';
    if (score >= 60) return '中等置信度';
    return '低置信度';
  };

  return (
    <Card
      title={
        <Space>
          <BulbOutlined style={{ color: '#faad14' }} />
          <span>AI 优化结论</span>
        </Space>
      }
      size="small"
      style={{ width: '100%' }}
      extra={
        <Tag icon={<SafetyCertificateOutlined />} color="processing">
          AI 生成
        </Tag>
      }
    >
      <div style={{ padding: '4px 0' }}>
        {/* Confidence Score */}
        <div style={{ marginBottom: 16 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
            <Text strong>分析置信度</Text>
            <Text style={{ color: getConfidenceColor(conclusion.confidenceScore) }}>
              {conclusion.confidenceScore}% - {getConfidenceLabel(conclusion.confidenceScore)}
            </Text>
          </div>
          <Progress
            percent={conclusion.confidenceScore}
            strokeColor={getConfidenceColor(conclusion.confidenceScore)}
            showInfo={false}
          />
        </div>

        <Divider style={{ margin: '12px 0' }} />

        {/* Summary */}
        <div style={{ marginBottom: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
            <BulbOutlined style={{ color: '#faad14' }} />
            <Text strong>分析摘要</Text>
          </div>
          <Paragraph
            style={{
              fontSize: 13,
              color: '#595959',
              lineHeight: 1.8,
              marginBottom: 0,
              padding: 12,
              background: '#f6ffed',
              borderRadius: 6,
              border: '1px solid #b7eb8f',
            }}
          >
            {conclusion.summary}
          </Paragraph>
        </div>

        <Divider style={{ margin: '12px 0' }} />

        {/* Key Findings */}
        <div style={{ marginBottom: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
            <CheckCircleOutlined style={{ color: '#52c41a' }} />
            <Text strong>关键发现</Text>
          </div>
          <List
            size="small"
            dataSource={conclusion.keyFindings}
            renderItem={(item, index) => (
              <List.Item style={{ padding: '4px 0', border: 'none' }}>
                <Space>
                  <Tag color="blue" style={{ marginInlineEnd: 0 }}>
                    {index + 1}
                  </Tag>
                  <Text style={{ fontSize: 13 }}>{String(item)}</Text>
                </Space>
              </List.Item>
            )}
          />
        </div>

        <Divider style={{ margin: '12px 0' }} />

        {/* Recommendations */}
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
            <ThunderboltOutlined style={{ color: '#1890ff' }} />
            <Text strong>行动建议</Text>
          </div>
          <List
            size="small"
            dataSource={conclusion.recommendations}
            renderItem={(item, index) => (
              <List.Item style={{ padding: '4px 0', border: 'none' }}>
                <Space>
                  <Tag color="geekblue" style={{ marginInlineEnd: 0 }}>
                    建议 {index + 1}
                  </Tag>
                  <Text style={{ fontSize: 13 }}>{String(item)}</Text>
                </Space>
              </List.Item>
            )}
          />
        </div>
      </div>
    </Card>
  );
};

export default AIConclusion;
