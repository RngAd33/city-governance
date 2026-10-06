package com.cityplatform.govai.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 决策记录实体
 * 用于持久化存储AI决策支持过程中的查询和响应记录
 */
@Data
@Entity
@NoArgsConstructor
@Table(name = "decision_record")
public class DecisionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "query_type", nullable = false, length = 50)
    private String queryType;

    @Column(name = "input", nullable = false, columnDefinition = "TEXT")
    private String input;

    @Column(name = "output", columnDefinition = "TEXT")
    private String output;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "created_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 精简输出（避免长输入/输出污染日志）
     */
    @Override
    public String toString() {
        return "DecisionRecord{" +
                "id=" + id +
                ", queryType='" + queryType + '\'' +
                ", confidence=" + confidence +
                ", createdAt=" + createdAt +
                '}';
    }
}
