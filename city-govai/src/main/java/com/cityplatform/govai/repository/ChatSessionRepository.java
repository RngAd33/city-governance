package com.cityplatform.govai.repository;

import com.cityplatform.govai.model.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 对话会话数据访问层
 */
@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    /**
     * 按会话业务ID查找
     */
    Optional<ChatSession> findBySessionId(String sessionId);

    /**
     * 全部会话按最近更新倒序（历史会话列表）
     */
    List<ChatSession> findAllByOrderByUpdatedAtDesc();

    /**
     * 会话是否存在
     */
    boolean existsBySessionId(String sessionId);
}
