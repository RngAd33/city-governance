package com.cityplatform.govai.service;

import com.cityplatform.govai.model.entity.ChatMessage;
import com.cityplatform.govai.model.entity.ChatSession;
import com.cityplatform.govai.repository.ChatMessageRepository;
import com.cityplatform.govai.repository.ChatSessionRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static com.cityplatform.govai.constant.ReportConstants.SESSION_TITLE_MAX_LENGTH;

/**
 * 对话历史服务
 * 负责对话会话的创建/查询/删除与消息持久化（MySQL chat_session / chat_message），
 * 并为大模型提供最近若干轮对话作为多轮上下文
 */
@Service
@Slf4j
public class ChatHistoryService {

    @Resource
    private ChatMessageRepository messageRepository;

    @Resource
    private ChatSessionRepository sessionRepository;

    /**
     * 获取或创建会话：sessionId 存在则复用；否则以首条消息生成标题创建新会话
     */
    @Transactional
    public ChatSession getOrCreateSession(String sessionId, String firstMessage) {
        if (sessionId != null && !sessionId.isBlank()) {
            Optional<ChatSession> existing = sessionRepository.findBySessionId(sessionId);
            if (existing.isPresent()) {
                return existing.get();
            }
            log.info("会话 {} 不存在，将创建新会话", sessionId);
        }
        String newSessionId = UUID.randomUUID().toString().replace("-", "");
        String title = buildTitle(firstMessage);
        ChatSession session = sessionRepository.save(new ChatSession(newSessionId, title));
        log.info("创建对话会话: {} ({})", newSessionId, title);
        return session;
    }

    /**
     * 追加一条消息并更新会话统计（消息数/更新时间）
     */
    @Transactional
    public ChatMessage appendMessage(ChatSession session, String role, String content,
                                     String knowledgeHits, String provider) {
        int nextSeq = (session.getMessageCount() == null ? 0 : session.getMessageCount()) + 1;
        ChatMessage message = new ChatMessage(session.getSessionId(), role, content, nextSeq);
        message.setKnowledgeHits(knowledgeHits);
        message.setProvider(provider);
        messageRepository.save(message);

        session.setMessageCount(nextSeq);
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);
        return message;
    }

    /**
     * 会话最近消息（按时间升序返回，供大模型多轮上下文使用）
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getRecentHistory(String sessionId, int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        List<ChatMessage> desc = messageRepository.findBySessionIdOrderBySeqDesc(sessionId, PageRequest.of(0, limit));
        List<ChatMessage> asc = new ArrayList<>(desc);
        Collections.reverse(asc);
        return asc;
    }

    /**
     * 会话全部消息（按序号升序，供前端回放对话）
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getMessages(String sessionId) {
        return messageRepository.findBySessionIdOrderBySeqAsc(sessionId);
    }

    /**
     * 历史会话列表（最近更新在前）
     */
    @Transactional(readOnly = true)
    public List<ChatSession> listSessions() {
        return sessionRepository.findAllByOrderByUpdatedAtDesc();
    }

    /**
     * 查询单个会话
     */
    @Transactional(readOnly = true)
    public Optional<ChatSession> findSession(String sessionId) {
        return sessionRepository.findBySessionId(sessionId);
    }

    /**
     * 删除会话及其全部消息
     */
    @Transactional
    public boolean deleteSession(String sessionId) {
        Optional<ChatSession> session = sessionRepository.findBySessionId(sessionId);
        if (session.isEmpty()) {
            return false;
        }
        messageRepository.deleteBySessionId(sessionId);
        sessionRepository.delete(session.get());
        log.info("删除对话会话: {}", sessionId);
        return true;
    }

    /**
     * 以首条消息生成会话标题（截断过长文本）
     */
    private String buildTitle(String firstMessage) {
        if (firstMessage == null || firstMessage.isBlank()) {
            return "新对话";
        }
        String normalized = firstMessage.trim().replaceAll("\\s+", " ");
        return normalized.length() > SESSION_TITLE_MAX_LENGTH
                ? normalized.substring(0, SESSION_TITLE_MAX_LENGTH) + "…"
                : normalized;
    }
}
