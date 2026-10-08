package com.cityplatform.govai.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI对话消息实体
 * 持久化存储会话中的每条消息（用户提问 / 助手回答）
 * 记录RAG命中的知识条目与生成回答的模型提供方，便于溯源
 */
@Data
@Entity
@NoArgsConstructor
@Table(name = "chat_message")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    /** 消息角色：user / assistant / system */
    @Column(nullable = false, length = 16)
    private String role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** RAG命中的知识条目标题（分号分隔，最多5条） */
    @Column(name = "knowledge_hits", length = 1024)
    private String knowledgeHits;

    /** 生成该回答的模型提供方标识 */
    @Column(length = 64)
    private String provider;

    /** 会话内消息序号（从1开始） */
    @Column(nullable = false)
    private Integer seq;

    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 业务便利构造器：不含自增主键 id，创建时间自动初始化
     */
    public ChatMessage(String sessionId, String role, String content, Integer seq) {
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.seq = seq;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 精简输出（避免长正文污染日志）
     */
    @Override
    public String toString() {
        return "ChatMessage {" +
                "sessionId='" + sessionId + '\'' +
                ", role='" + role + '\'' +
                ", seq=" + seq +
                ", contentLength=" + (content != null ? content.length() : 0) +
                '}';
    }
}
