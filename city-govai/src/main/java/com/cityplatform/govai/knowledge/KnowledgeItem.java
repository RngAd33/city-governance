package com.cityplatform.govai.knowledge;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库条目（RAG知识源）
 * 持久化于 MySQL knowledge_items 表，以业务编号 item_id 作为主键（JSON 中仍序列化为 id，保持接口兼容）
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "knowledge_items")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KnowledgeItem {

    /** 业务编号（对应表 item_id 唯一键） */
    @Id
    @Column(name = "item_id", nullable = false, length = 64)
    private String id;

    @Column(nullable = false, length = 256)
    private String title;

    /** 分类：regulation/emergency_plan/dispatch_rule/standard 或 EMERGENCY/TRAFFIC 等业务域 */
    @Column(nullable = false, length = 64)
    private String category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 关键词（逗号分隔），用于增强检索命中 */
    @Column(length = 512)
    private String keywords;

    @Column(length = 256)
    private String source;

    /** 优先级（0-10），检索命中后微调排序 */
    @Column(nullable = false)
    private Integer priority = 0;

    /** 是否启用 */
    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /** 检索相关度得分（非持久化） */
    @Transient
    private double relevanceScore;

    /**
     * 精简输出（避免长正文污染日志）
     */
    @Override
    public String toString() {
        return "KnowledgeItem{" +
                "id='" + id + '\'' +
                ", category='" + category + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
