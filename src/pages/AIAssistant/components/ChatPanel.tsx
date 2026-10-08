// ============================================================
// ChatPanel - Chat interface with message list, input, and AI responses
// ============================================================
import React, { useState, useRef, useEffect } from 'react';
import { Input, Button, Avatar, Spin, Tooltip, message, Empty, Tag } from 'antd';
import {
  SendOutlined,
  CopyOutlined,
  CheckOutlined,
  UserOutlined,
  RobotOutlined,
  FileTextOutlined,
} from '@ant-design/icons';
import type { ChatMessage, SourceCitation } from '@/types';

const { TextArea } = Input;

interface ChatPanelProps {
  messages: ChatMessage[];
  onSendMessage: (content: string) => void;
  isLoading?: boolean;
}

const ChatPanel: React.FC<ChatPanelProps> = ({ messages, onSendMessage, isLoading = false }) => {
  const [inputValue, setInputValue] = useState('');
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<any>(null);

  useEffect(() => {
    // 仅滚动对话记录窗口自身，避免 scrollIntoView 连带滚动外层页面
    const el = listRef.current;
    if (el) el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' });
  }, [messages, isLoading]);

  const handleSend = () => {
    const trimmed = inputValue.trim();
    if (!trimmed || isLoading) return;
    onSendMessage(trimmed);
    setInputValue('');
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleCopy = async (text: string, messageId: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedId(messageId);
      message.success('已复制到剪贴板');
      setTimeout(() => setCopiedId(null), 2000);
    } catch {
      message.error('复制失败');
    }
  };

  const renderMarkdown = (content: string): string => {
    // Simple markdown rendering
    return content
      .replace(/### (.*)/g, '<h3 style="margin: 12px 0 6px; font-size: 15px;">$1</h3>')
      .replace(/## (.*)/g, '<h2 style="margin: 14px 0 8px; font-size: 16px;">$1</h2>')
      .replace(/# (.*)/g, '<h1 style="margin: 16px 0 10px; font-size: 18px;">$1</h1>')
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
      .replace(/`(.*?)`/g, '<code style="background: #f5f5f5; padding: 2px 6px; border-radius: 3px; font-size: 13px;">$1</code>')
      .replace(/\n- (.*)/g, '<li style="margin-left: 20px;">$1</li>')
      .replace(/\n\d+\. (.*)/g, '<li style="margin-left: 20px;">$1</li>')
      .replace(/\n\n/g, '<br/><br/>')
      .replace(/\n/g, '<br/>');
  };

  const renderSourceCitation = (source: SourceCitation) => (
    <a
      key={source.id}
      href={source.url || undefined}
      target="_blank"
      rel="noopener noreferrer"
      style={{ marginRight: 8, fontSize: 12, color: '#1890ff' }}
    >
      <FileTextOutlined /> {source.title}
    </a>
  );

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%', minHeight: 0, overflow: 'hidden' }}>
      {/* Message List（独立滚动：flex 收缩 + overflow 自成滚动区） */}
      <div ref={listRef} style={{ flex: 1, minHeight: 0, overflowY: 'auto', padding: '16px 8px' }}>
        {messages.length === 0 && !isLoading && (
          <Empty
            description="开始对话，获取城市治理建议"
            style={{ marginTop: 80 }}
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          />
        )}

        {messages.map((msg) => (
          <div
            key={msg.id}
            style={{
              display: 'flex',
              justifyContent: msg.role === 'user' ? 'flex-end' : 'flex-start',
              marginBottom: 16,
            }}
          >
            {msg.role === 'assistant' && (
              <Avatar
                icon={<RobotOutlined />}
                style={{ marginRight: 8, flexShrink: 0, background: '#1890ff' }}
              />
            )}
            <div
              style={{
                maxWidth: '80%',
                display: 'flex',
                flexDirection: 'column',
                alignItems: msg.role === 'user' ? 'flex-end' : 'flex-start',
              }}
            >
              <div
                style={{
                  background: msg.role === 'user' ? '#1890ff' : '#fff',
                  color: msg.role === 'user' ? '#fff' : '#262626',
                  padding: '12px 16px',
                  borderRadius: 12,
                  borderTopRightRadius: msg.role === 'user' ? 4 : 12,
                  borderTopLeftRadius: msg.role === 'user' ? 12 : 4,
                  boxShadow: '0 1px 2px rgba(0,0,0,0.1)',
                  fontSize: 14,
                  lineHeight: 1.6,
                }}
                dangerouslySetInnerHTML={{ __html: renderMarkdown(msg.content) }}
              />
              {msg.sources && msg.sources.length > 0 && (
                <div style={{ marginTop: 8, fontSize: 12, color: '#8c8c8c' }}>
                  <div style={{ marginBottom: 4 }}>参考来源:</div>
                  <div>{msg.sources.map(renderSourceCitation)}</div>
                </div>
              )}
              <div style={{ marginTop: 4, fontSize: 11, color: '#bfbfbf' }}>
                {msg.timestamp}
                {msg.role === 'assistant' && msg.provider && (
                  <Tag
                    color="blue"
                    style={{ marginLeft: 6, fontSize: 10, lineHeight: '16px', padding: '0 4px' }}
                  >
                    {msg.provider}
                  </Tag>
                )}
                {msg.role === 'assistant' && (
                  <Tooltip title="复制">
                    <Button
                      type="text"
                      size="small"
                      icon={copiedId === msg.id ? <CheckOutlined style={{ color: '#52c41a' }} /> : <CopyOutlined />}
                      onClick={() => handleCopy(msg.content, msg.id)}
                      style={{ marginLeft: 8, height: 20, width: 20 }}
                    />
                  </Tooltip>
                )}
              </div>
            </div>
            {msg.role === 'user' && (
              <Avatar
                icon={<UserOutlined />}
                style={{ marginLeft: 8, flexShrink: 0, background: '#87d068' }}
              />
            )}
          </div>
        ))}

        {isLoading && (
          <div style={{ display: 'flex', justifyContent: 'flex-start', marginBottom: 16 }}>
            <Avatar icon={<RobotOutlined />} style={{ marginRight: 8, background: '#1890ff' }} />
            <div
              style={{
                background: '#fff',
                padding: '12px 16px',
                borderRadius: 12,
                borderTopLeftRadius: 4,
                boxShadow: '0 1px 2px rgba(0,0,0,0.1)',
                display: 'flex',
                alignItems: 'center',
                gap: 8,
              }}
            >
              <Spin size="small" />
              <span style={{ color: '#8c8c8c', fontSize: 13 }}>正在思考...</span>
            </div>
          </div>
        )}

      </div>

      {/* Input Area */}
      <div style={{ padding: '12px 16px', borderTop: '1px solid #f0f0f0', background: '#fff' }}>
        <div style={{ display: 'flex', gap: 8, alignItems: 'flex-end' }}>
          <TextArea
            ref={textareaRef}
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="输入您的问题，例如：分析当前城市内涝风险..."
            autoSize={{ minRows: 1, maxRows: 4 }}
            disabled={isLoading}
            style={{ flex: 1 }}
          />
          <Button
            type="primary"
            icon={<SendOutlined />}
            onClick={handleSend}
            disabled={!inputValue.trim() || isLoading}
            style={{ height: 40 }}
          />
        </div>
        <div style={{ marginTop: 8, fontSize: 11, color: '#bfbfbf' }}>
          按 Enter 发送，Shift + Enter 换行
        </div>
      </div>
    </div>
  );
};

export default ChatPanel;
