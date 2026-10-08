import React from 'react';
import { Tag, Button, Space, Typography } from 'antd';
import { CopyOutlined, FileTextOutlined } from '@ant-design/icons';
import type { ChatMessage } from '@/types';

const { Paragraph, Text } = Typography;

interface AIChatMessageProps {
  message: ChatMessage;
  onCopy?: (content: string) => void;
}

const AIChatMessage: React.FC<AIChatMessageProps> = ({ message, onCopy }) => {
  const isUser = message.role === 'user';

  return (
    <div
      style={{
        display: 'flex',
        justifyContent: isUser ? 'flex-end' : 'flex-start',
        marginBottom: 16,
      }}
    >
      <div
        style={{
          maxWidth: '80%',
          padding: '12px 16px',
          borderRadius: 12,
          background: isUser ? '#1890ff' : '#f5f5f5',
          color: isUser ? '#fff' : '#333',
        }}
      >
        <Paragraph style={{ margin: 0, color: 'inherit', whiteSpace: 'pre-wrap' }}>
          {message.content}
        </Paragraph>

        {!isUser && message.sources && message.sources.length > 0 && (
          <div style={{ marginTop: 8, paddingTop: 8, borderTop: '1px solid #e8e8e8' }}>
            <Text type="secondary" style={{ fontSize: 12 }}>
              依据来源：
            </Text>
            <Space size={4} wrap style={{ marginTop: 4 }}>
              {message.sources.map((source) => (
                <Tag key={source.id} icon={<FileTextOutlined />} style={{ fontSize: 11 }}>
                  {source.title}
                </Tag>
              ))}
            </Space>
          </div>
        )}

        <div
          style={{
            marginTop: 8,
            display: 'flex',
            justifyContent: 'flex-end',
            gap: 8,
          }}
        >
          {onCopy && (
            <Button
              type="text"
              size="small"
              icon={<CopyOutlined />}
              onClick={() => onCopy(message.content)}
              style={{ color: isUser ? '#fff' : '#666' }}
            >
              复制
            </Button>
          )}
        </div>
      </div>
    </div>
  );
};

export default AIChatMessage;
