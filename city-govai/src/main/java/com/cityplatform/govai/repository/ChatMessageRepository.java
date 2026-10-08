package com.cityplatform.govai.repository;

import com.cityplatform.govai.model.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 对话消息数据访问层
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 会话内全部消息按序号升序（回放对话）
     */
    List<ChatMessage> findBySessionIdOrderBySeqAsc(String sessionId);

    /**
     * 会话内最近消息按序号降序（多轮上下文，调用方反转后使用）
     */
    List<ChatMessage> findBySessionIdOrderBySeqDesc(String sessionId, Pageable pageable);

    /**
     * 会话消息数
     */
    long countBySessionId(String sessionId);

    /**
     * 删除会话全部消息
     */
    void deleteBySessionId(String sessionId);
}
