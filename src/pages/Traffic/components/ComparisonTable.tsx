import React from 'react';
import { Table, Tag, Progress, Statistic, Row, Col, Card } from 'antd';
import {
  ArrowUpOutlined,
  ArrowDownOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';

export interface MetricImprovement {
  key?: string;
  label: string;
  before: number;
  after: number;
  unit: string;
  improvementPercent: number;
  isImproved: boolean;
}

interface ComparisonTableProps {
  improvements: MetricImprovement[];
  loading?: boolean;
}

const ComparisonTable: React.FC<ComparisonTableProps> = ({
  improvements,
  loading = false,
}) => {
  const overallImprovement =
    improvements.length > 0
      ? improvements.reduce((sum, item) => sum + item.improvementPercent, 0) /
        improvements.length
      : 0;

  const improvedCount = improvements.filter((i) => i.isImproved).length;
  const degradedCount = improvements.length - improvedCount;

  const columns: ColumnsType<MetricImprovement> = [
    {
      title: '指标',
      dataIndex: 'label',
      key: 'label',
      width: 120,
      render: (text: string) => <span style={{ fontWeight: 500 }}>{text}</span>,
    },
    {
      title: '优化前',
      dataIndex: 'before',
      key: 'before',
      width: 100,
      render: (value: number, record) => (
        <span>
          {value.toFixed(1)} <span style={{ fontSize: 11, color: '#8c8c8c' }}>{record.unit}</span>
        </span>
      ),
    },
    {
      title: '优化后',
      dataIndex: 'after',
      key: 'after',
      width: 100,
      render: (value: number, record) => (
        <span style={{ fontWeight: 500 }}>
          {value.toFixed(1)} <span style={{ fontSize: 11, color: '#8c8c8c' }}>{record.unit}</span>
        </span>
      ),
    },
    {
      title: '变化率',
      dataIndex: 'improvementPercent',
      key: 'improvementPercent',
      width: 140,
      render: (value: number, record) => {
        const isPositive = record.isImproved;
        const color = isPositive ? '#52c41a' : '#ff4d4f';
        const icon = isPositive ? <ArrowDownOutlined /> : <ArrowUpOutlined />;
        return (
          <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
            <span style={{ color, fontWeight: 500 }}>
              {icon} {Math.abs(value).toFixed(1)}%
            </span>
            <Tag color={isPositive ? 'success' : 'error'} style={{ marginInlineEnd: 0 }}>
              {isPositive ? '改善' : '恶化'}
            </Tag>
          </div>
        );
      },
    },
    {
      title: '改善程度',
      key: 'progress',
      width: 160,
      render: (_, record) => {
        const absValue = Math.min(Math.abs(record.improvementPercent), 100);
        const color = record.isImproved ? '#52c41a' : '#ff4d4f';
        return (
          <Progress
            percent={absValue}
            size="small"
            strokeColor={color}
            format={(percent) => `${percent?.toFixed(0)}%`}
          />
        );
      },
    },
  ];

  return (
    <div style={{ padding: 16 }}>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={8}>
          <Card size="small" style={{ textAlign: 'center' }}>
            <Statistic
              title="综合改善率"
              value={Math.abs(overallImprovement)}
              precision={1}
              suffix="%"
              valueStyle={{
                color: overallImprovement > 0 ? '#52c41a' : '#ff4d4f',
                fontSize: 24,
              }}
              prefix={
                overallImprovement > 0 ? (
                  <CheckCircleOutlined />
                ) : (
                  <CloseCircleOutlined />
                )
              }
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card size="small" style={{ textAlign: 'center' }}>
            <Statistic
              title="改善指标"
              value={improvedCount}
              suffix={`/${improvements.length}`}
              valueStyle={{ color: '#52c41a', fontSize: 24 }}
              prefix={<CheckCircleOutlined />}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card size="small" style={{ textAlign: 'center' }}>
            <Statistic
              title="恶化指标"
              value={degradedCount}
              suffix={`/${improvements.length}`}
              valueStyle={{ color: degradedCount > 0 ? '#ff4d4f' : '#52c41a', fontSize: 24 }}
              prefix={
                degradedCount > 0 ? <CloseCircleOutlined /> : <CheckCircleOutlined />
              }
            />
          </Card>
        </Col>
      </Row>

      <Table<MetricImprovement>
        columns={columns}
        dataSource={improvements}
        loading={loading}
        pagination={false}
        size="small"
        rowKey={(record) => record.key || record.label}
        scroll={{ y: 300 }}
        summary={(pageData) => {
          if (pageData.length === 0) return null;
          return (
            <Table.Summary.Row style={{ background: '#fafafa', fontWeight: 500 }}>
              <Table.Summary.Cell index={0}>综合评估</Table.Summary.Cell>
              <Table.Summary.Cell index={1}>-</Table.Summary.Cell>
              <Table.Summary.Cell index={2}>-</Table.Summary.Cell>
              <Table.Summary.Cell index={3}>
                <Tag color={overallImprovement > 0 ? 'success' : 'error'}>
                  {overallImprovement > 0 ? '+' : ''}
                  {overallImprovement.toFixed(1)}%
                </Tag>
              </Table.Summary.Cell>
              <Table.Summary.Cell index={4}>
                <Progress
                  percent={Math.min(Math.abs(overallImprovement), 100)}
                  size="small"
                  strokeColor={overallImprovement > 0 ? '#52c41a' : '#ff4d4f'}
                  showInfo={false}
                />
              </Table.Summary.Cell>
            </Table.Summary.Row>
          );
        }}
      />
    </div>
  );
};

export default ComparisonTable;
