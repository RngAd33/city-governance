package com.cityplatform.govai.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI对话会话实体
 * 持久化存储多轮对话会话（会话标题、消息数、创建/更新时间）
 * 对话消息明细见 {@link ChatMessage}
 */
@Data
@Entity
@NoArgsConstructor
@Table(name = "chat_session")
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true, length = 64)
    private String sessionId;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(name = "message_count", nullable = false)
    private Integer messageCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 业务便利构造器：不含自增主键 id，消息数与时间自动初始化
     */
    public ChatSession(String sessionId, String title) {
        this.sessionId = sessionId;
        this.title = title;
        this.messageCount = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 精简输出
     */
    @Override
    public String toString() {
        return "ChatSession{" +
                "sessionId='" + sessionId + '\'' +
                ", title='" + title + '\'' +
                ", messageCount=" + messageCount +
                '}';
    }
}
