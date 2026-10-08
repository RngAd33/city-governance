// ============================================================
// GovAIAssistant - AI assistant for city governance
// ============================================================
import React, { useState, useCallback, useEffect } from 'react';
import { Card, Row, Col, message } from 'antd';
import ChatPanel from './components/ChatPanel';
import QuickActions from './components/QuickActions';
import ContextPanel from './components/ContextPanel';
import SessionList from './components/SessionList';
import {
  govaiChat,
  fetchGovaiSessions,
  fetchGovaiSessionMessages,
  deleteGovaiSession,
} from '@/api/dashboard';
import type {
  ChatMessage,
  Level,
  GovaiSession,
  GovaiMessage,
  KnowledgeHit,
  SourceCitation,
} from '@/types';


const INITIAL_MESSAGES: ChatMessage[] = [
  {
    id: '1',
    role: 'assistant',
    content: '您好，我是城市治理AI助手。我可以帮助您分析城市运行数据、生成报告、检索应急预案等。请问有什么可以帮您的？',
    timestamp: new Date().toLocaleString('zh-CN'),
    sources: [],
  },
];

// ---- 后端数据 → 前端视图模型 ----

// RAG 知识命中条目 → 来源引用（category 粗略映射展示类型）
const hitToSource = (hit: KnowledgeHit, index: number): SourceCitation => {
  const category = `${hit.category ?? ''}`.toLowerCase();
  const type: SourceCitation['type'] =
    category.includes('regulation') || category.includes('law') || category.includes('预案') || category.includes('法规')
      ? 'regulation'
      : category.includes('data') || category.includes('数据')
        ? 'data'
        : 'document';
  return {
    id: String(hit.id ?? index),
    title: hit.title,
    // 知识条目的 source 多为文件名而非 URL，非 HTTP 链接不渲染跳转
    url: /^https?:\/\//i.test(hit.source ?? '') ? (hit.source as string) : '',
    type,
  };
};

const hitsToSources = (hits?: KnowledgeHit[] | null): SourceCitation[] =>
  (hits ?? []).map(hitToSource);

// 持久化的分号分隔标题字符串 → 来源引用（消息回放用）
const splitHitTitles = (raw?: string | null): SourceCitation[] =>
  (raw ?? '')
    .split(/[;；]/)
    .map((s) => s.trim())
    .filter(Boolean)
    .map((title, i) => ({ id: `hit-${i}`, title, url: '', type: 'document' as const }));

// 持久化消息 → 聊天消息
const toChatMessage = (m: GovaiMessage): ChatMessage => ({
  id: String(m.id),
  role: m.role,
  content: m.content,
  timestamp: m.createdAt,
  sources: m.role === 'assistant' ? splitHitTitles(m.knowledgeHits) : [],
  provider: m.provider ?? undefined,
});

const GovAIAssistant: React.FC = () => {
  const [messages, setMessages] = useState<ChatMessage[]>(INITIAL_MESSAGES);
  const [isLoading, setIsLoading] = useState(false);
  const [confidence, setConfidence] = useState(0);
  // 当前会话 ID（null 表示尚未开启后端会话，首条消息发送后由后端创建）
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [sessions, setSessions] = useState<GovaiSession[]>([]);
  const [sessionsLoading, setSessionsLoading] = useState(false);
  const [replayLoadingId, setReplayLoadingId] = useState<string | null>(null);
  // 参考来源跟随最近一次 assistant 回复更新
  const [sources, setSources] = useState<SourceCitation[]>([]);

  // 相关数据/相关告警暂无对应后端数据源，保持空态展示（不再使用模拟数据）
  const relatedData: { id: string; title: string; value: string; unit?: string; level?: Level }[] = [];

  // 加载历史会话列表（挂载时拉取，发送/删除后刷新）
  const loadSessions = useCallback(async () => {
    setSessionsLoading(true);
    try {
      const resp = await fetchGovaiSessions();
      const data = (resp as any)?.data ?? {};
      setSessions(data.sessions ?? []);
    } catch {
      // 会话列表加载失败不阻断聊天功能，保持当前列表
    } finally {
      setSessionsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadSessions();
  }, [loadSessions]);

  const handleSendMessage = useCallback(async (content: string) => {
    const userMessage: ChatMessage = {
      id: Date.now().toString(),
      role: 'user',
      content,
      timestamp: new Date().toLocaleString('zh-CN'),
      sources: [],
    };

    setMessages((prev) => [...prev, userMessage]);
    setIsLoading(true);

    try {
      // 真实调用 city-govai（经 Vite 代理 /api/ai/chat → /api/govai/chat），
      // 携带 sessionId 延续多轮上下文；sessionId 为空时后端自动新建会话
      const resp = await govaiChat(content, sessionId);
      const data = (resp as any)?.data ?? {};
      const hitSources = hitsToSources(data.knowledgeHits);
      const aiResponse: ChatMessage = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: data.answer ?? '（未收到有效回答）',
        timestamp: data.timestamp ? String(data.timestamp) : new Date().toLocaleString('zh-CN'),
        sources: hitSources,
        provider: data.provider,
      };
      setMessages((prev) => [...prev, aiResponse]);
      if (data.sessionId) setSessionId(data.sessionId);
      setSources(hitSources);
      // 后端暂未返回置信度：有知识库命中时按高置信度展示
      setConfidence(hitSources.length > 0 ? 90 : 85);
      // 刷新会话列表（新会话出现在列表顶部 / 消息数更新）
      loadSessions();
    } catch {
      setMessages((prev) => [...prev, {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: '抱歉，AI服务暂时不可用，请稍后重试。',
        timestamp: new Date().toLocaleString('zh-CN'),
        sources: [],
      }]);
      setConfidence(0);
    } finally {
      setIsLoading(false);
    }
  }, [sessionId, loadSessions]);

  // 点击历史会话：拉取消息回放（createdAt → timestamp，knowledgeHits 分号拆分展示）
  const handleSelectSession = useCallback(async (sid: string) => {
    if (sid === sessionId || replayLoadingId) return;
    setReplayLoadingId(sid);
    try {
      const resp = await fetchGovaiSessionMessages(sid);
      const data = (resp as any)?.data ?? {};
      const replay: ChatMessage[] = ((data.messages ?? []) as GovaiMessage[]).map(toChatMessage);
      setMessages(replay.length > 0 ? replay : INITIAL_MESSAGES);
      setSessionId(sid);
      // 右栏来源与会话最后一条 assistant 消息同步
      const lastAssistant = [...replay].reverse().find((m) => m.role === 'assistant');
      setSources(lastAssistant?.sources ?? []);
      setConfidence(lastAssistant && lastAssistant.sources.length > 0 ? 90 : 0);
    } catch {
      message.error('会话加载失败，请稍后重试');
    } finally {
      setReplayLoadingId(null);
    }
  }, [sessionId, replayLoadingId]);

  // 新建会话：重置为初始状态（首条消息发送后由后端创建）
  const handleNewSession = useCallback(() => {
    setSessionId(null);
    setMessages(INITIAL_MESSAGES);
    setSources([]);
    setConfidence(0);
  }, []);

  // 删除会话：若为当前会话则重置聊天区
  const handleDeleteSession = useCallback(async (sid: string) => {
    try {
      await deleteGovaiSession(sid);
      message.success('会话已删除');
      if (sid === sessionId) {
        handleNewSession();
      }
      loadSessions();
    } catch {
      message.error('删除失败，请稍后重试');
    }
  }, [sessionId, handleNewSession, loadSessions]);

  const handleQuickAction = useCallback((actionId: string) => {
    const actionMessages: Record<string, string> = {
      dailyReport: '请生成今日城市运行日报',
      weeklyReport: '请生成本周城市运行周报',
      viewAlarms: '请查看当前所有活跃告警',
      searchPlan: '请检索城市内涝应急预案',
      energySaving: '请提供建筑节能优化建议',
      trafficOpt: '请分析当前交通流量并给出优化建议',
      safetyCheck: '请进行城市安全隐患排查',
      envAnalysis: '请分析当前环境质量状况',
    };

    const msg = actionMessages[actionId];
    if (msg) {
      handleSendMessage(msg);
    }
  }, [handleSendMessage]);

  return (
    <div style={{ padding: 24, background: '#f0f2f5', minHeight: '100%' }}>
      {/* Header */}
      <div style={{ marginBottom: 16 }}>
        <h2 style={{ margin: 0, fontSize: 20, fontWeight: 600 }}>城市治理AI助手</h2>
        <span style={{ color: '#8c8c8c', fontSize: 13 }}>基于大语言模型的智能决策支持系统</span>
      </div>

      {/* Main Content */}
      <Row gutter={[16, 16]}>
        {/* Left: Session History */}
        <Col xs={24} lg={5}>
          <SessionList
            sessions={sessions}
            loading={sessionsLoading}
            currentSessionId={sessionId}
            loadingSessionId={replayLoadingId}
            onSelect={handleSelectSession}
            onNew={handleNewSession}
            onDelete={handleDeleteSession}
          />
        </Col>

        {/* Middle: Chat Interface */}
        <Col xs={24} lg={11}>
          <Card style={{ height: 700, display: 'flex', flexDirection: 'column', overflow: 'hidden' }} styles={{ body: { flex: 1, minHeight: 0, padding: 0, display: 'flex', flexDirection: 'column' } }}>
            <ChatPanel
              messages={messages}
              onSendMessage={handleSendMessage}
              isLoading={isLoading}
            />
          </Card>
        </Col>

        {/* Right: Context Panel + Quick Actions */}
        <Col xs={24} lg={8}>
          <Row gutter={[16, 16]} style={{ height: '100%' }}>
            <Col xs={24} span={24}>
              <div style={{ height: 340 }}>
                <ContextPanel
                  relatedData={relatedData}
                  sources={sources}
                  confidence={confidence}
                  relatedAlarms={[]}
                />
              </div>
            </Col>
            <Col xs={24} span={24}>
              <QuickActions onAction={handleQuickAction} />
            </Col>
          </Row>
        </Col>
      </Row>
    </div>
  );
};

export default GovAIAssistant;
