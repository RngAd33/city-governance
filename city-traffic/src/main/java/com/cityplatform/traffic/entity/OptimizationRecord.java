package com.cityplatform.traffic.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * 信号优化记录实体
 * 存储每次优化的前后配置和改善百分比
 */
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
    private OffsetDateTime createdAt;

    public OptimizationRecord() {
        this.createdAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIntersectionId() { return intersectionId; }
    public void setIntersectionId(String intersectionId) { this.intersectionId = intersectionId; }

    public String getBeforeConfig() { return beforeConfig; }
    public void setBeforeConfig(String beforeConfig) { this.beforeConfig = beforeConfig; }

    public String getAfterConfig() { return afterConfig; }
    public void setAfterConfig(String afterConfig) { this.afterConfig = afterConfig; }

    public Double getImprovementPercent() { return improvementPercent; }
    public void setImprovementPercent(Double improvementPercent) { this.improvementPercent = improvementPercent; }

    public String getOptimizedPhases() { return optimizedPhases; }
    public void setOptimizedPhases(String optimizedPhases) { this.optimizedPhases = optimizedPhases; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

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
