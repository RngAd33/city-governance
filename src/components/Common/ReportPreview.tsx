import React from 'react';
import { Card, Button, Space, Tag, Divider, Typography } from 'antd';
import { DownloadOutlined, PrinterOutlined, FileTextOutlined } from '@ant-design/icons';
import type { Report } from '@/types';

const { Title, Paragraph, Text } = Typography;

interface ReportPreviewProps {
  report: Report;
  onExport?: (format: 'pdf' | 'word') => void;
  onPrint?: () => void;
}

const ReportPreview: React.FC<ReportPreviewProps> = ({ report, onExport, onPrint }) => {
  return (
    <Card
      title={
        <Space>
          <FileTextOutlined />
          <span>{report.title}</span>
          <Tag color="blue">{report.type}</Tag>
        </Space>
      }
      extra={
        <Space>
          <Button icon={<PrinterOutlined />} onClick={onPrint}>
            打印
          </Button>
          <Button icon={<DownloadOutlined />} onClick={() => onExport?.('pdf')}>
            导出PDF
          </Button>
          <Button icon={<DownloadOutlined />} onClick={() => onExport?.('word')}>
            导出Word
          </Button>
        </Space>
      }
      style={{ borderRadius: 8 }}
    >
      <div style={{ padding: '24px 0' }}>
        <Text type="secondary">生成时间：{report.createdAt}</Text>
        <Divider />
        <div
          style={{
            background: '#fff',
            padding: 32,
            borderRadius: 8,
            border: '1px solid #e8e8e8',
            minHeight: 400,
          }}
        >
          <Title level={3} style={{ textAlign: 'center' }}>
            {report.title}
          </Title>
          <Divider />
          <Paragraph style={{ fontSize: 14, lineHeight: 1.8, whiteSpace: 'pre-wrap' }}>
            {report.content}
          </Paragraph>
        </div>
      </div>
    </Card>
  );
};

export default ReportPreview;
