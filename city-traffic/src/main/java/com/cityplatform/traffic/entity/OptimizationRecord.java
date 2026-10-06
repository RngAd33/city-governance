package com.cityplatform.traffic.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 信号优化记录实体
 * 存储每次优化的前后配置和改善百分比
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "optimization_record")
public class OptimizationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "intersection_id", nullable = false, length = 64)
    private String intersectionId;

    @Column(name = "before_config", columnDefinition = "TEXT")
    private String beforeConfig;

    @Column(name = "after_config", columnDefinition = "TEXT")
    private String afterConfig;

    @Column(name = "improvement_percent")
    private Double improvementPercent;

    /** 优化后的相位配置（JSON） */
    @Column(name = "optimized_phases", columnDefinition = "TEXT")
    private String optimizedPhases;

    /** 触发来源：auto 自动优化链路 / manual 前端仿真 */
    @Column(name = "source", length = 16)
    private String source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    /**
     * 精简输出（避免长配置文本污染日志）
     */
    @Override
    public String toString() {
        return "OptimizationRecord{" +
                "id=" + id +
                ", intersectionId='" + intersectionId + '\'' +
                ", improvementPercent=" + improvementPercent +
                ", createdAt=" + createdAt +
                '}';
    }
}
