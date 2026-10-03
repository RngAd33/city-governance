package com.cityplatform.traffic.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public OptimizationRecord() {
        this.createdAt = LocalDateTime.now();
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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

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
