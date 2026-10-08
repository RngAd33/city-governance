// ============================================================
// SessionList - 历史会话面板（会话列表 / 当前高亮 / 新建 / 删除）
// 数据来源：city-govai 对话持久化（GET /sessions、DELETE /sessions/{id}）
// ============================================================
import React from 'react';
import { Card, List, Button, Empty, Popconfirm, Spin } from 'antd';
import { PlusOutlined, DeleteOutlined, MessageOutlined } from '@ant-design/icons';
import type { GovaiSession } from '@/types';

interface SessionListProps {
  sessions: GovaiSession[];
  /** 会话列表加载中 */
  loading?: boolean;
  /** 当前选中的会话（高亮） */
  currentSessionId: string | null;
  /** 正在加载消息回放的会话（显示 Spin） */
  loadingSessionId?: string | null;
  onSelect: (sessionId: string) => void;
  onNew: () => void;
  onDelete: (sessionId: string) => void;
}

const SessionList: React.FC<SessionListProps> = ({
  sessions,
  loading = false,
  currentSessionId,
  loadingSessionId = null,
  onSelect,
  onNew,
  onDelete,
}) => {
  return (
    <Card
      title="历史会话"
      size="small"
      style={{ height: 700, display: 'flex', flexDirection: 'column' }}
      styles={{ body: { flex: 1, padding: 8, overflowY: 'auto' } }}
      extra={
        <Button type="primary" size="small" icon={<PlusOutlined />} onClick={onNew}>
          新建对话
        </Button>
      }
    >
      <Spin spinning={loading}>
        {sessions.length === 0 && !loading ? (
          <Empty
            description="暂无历史会话"
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            style={{ marginTop: 60 }}
          />
        ) : (
          <List
            size="small"
            dataSource={sessions}
            renderItem={(session) => {
              const isActive = session.sessionId === currentSessionId;
              const isReplaying = loadingSessionId === session.sessionId;
              return (
                <List.Item
                  onClick={(e) => {
                    // 点击删除按钮（及弹层）时不触发选中；配合按钮侧 stopPropagation 双保险
                    if ((e.target as HTMLElement).closest('button')) return;
                    onSelect(session.sessionId);
                  }}
                  style={{
                    cursor: 'pointer',
                    padding: '8px 10px',
                    borderRadius: 6,
                    marginBottom: 4,
                    background: isActive ? '#e6f4ff' : undefined,
                    border: isActive ? '1px solid #91caff' : '1px solid transparent',
                  }}
                >
                  <div style={{ width: '100%', minWidth: 0 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 4 }}>
                      <span
                        title={session.title}
                        style={{
                          fontSize: 13,
                          fontWeight: isActive ? 600 : 400,
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap',
                          flex: 1,
                          minWidth: 0,
                        }}
                      >
                        {session.title || '未命名会话'}
                      </span>
                      {isReplaying ? (
                        <Spin size="small" />
                      ) : (
                        <Popconfirm
                          title="删除该会话？"
                          description="会话及其全部消息将被删除"
                          okText="删除"
                          okButtonProps={{ danger: true }}
                          cancelText="取消"
                          onConfirm={() => onDelete(session.sessionId)}
                        >
                          <Button
                            type="text"
                            size="small"
                            danger
                            icon={<DeleteOutlined />}
                            onClick={(e) => e.stopPropagation()}
                            style={{ flexShrink: 0 }}
                          />
                        </Popconfirm>
                      )}
                    </div>
                    <div
                      style={{
                        marginTop: 4,
                        fontSize: 11,
                        color: '#8c8c8c',
                        display: 'flex',
                        alignItems: 'center',
                        gap: 8,
                      }}
                    >
                      <span>
                        <MessageOutlined style={{ marginRight: 2 }} />
                        {session.messageCount}
                      </span>
                      <span>{session.updatedAt}</span>
                    </div>
                  </div>
                </List.Item>
              );
            }}
          />
        )}
      </Spin>
    </Card>
  );
};

export default SessionList;
