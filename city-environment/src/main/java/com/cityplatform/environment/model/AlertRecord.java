package com.cityplatform.environment.model;

import com.cityplatform.environment.model.enums.Severity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 环境告警记录实体
 * 存储触发的告警详情与处理状态
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "alert_record")
public class AlertRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id", unique = true, nullable = false, length = 64)
    private String alertId;

    @Column(name = "rule_id", nullable = false, length = 64)
    private String ruleId;

    @Column(nullable = false, length = 32)
    private String metric;

    @Column(nullable = false)
    private Double value;

    @Column(nullable = false)
    private Double threshold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Severity severity;

    @Column(nullable = false, length = 1024)
    private String message;

    @Column(nullable = false, length = 64)
    private String city;

    @Column(length = 256)
    private String location;

    @Column(nullable = false)
    private OffsetDateTime ts;

    @Column(nullable = false)
    private Boolean acknowledged = false;

    @Column(name = "acknowledged_at")
    private OffsetDateTime acknowledgedAt;

    @Column(name = "acknowledged_by", length = 64)
    private String acknowledgedBy;

    /**
     * 业务便利构造器：不含自增主键 id，时间戳与确认状态自动初始化
     */
    public AlertRecord(String alertId, String ruleId, String metric, Double value,
                       Double threshold, Severity severity, String message,
                       String city, String location) {
        this.alertId = alertId;
        this.ruleId = ruleId;
        this.metric = metric;
        this.value = value;
        this.threshold = threshold;
        this.severity = severity;
        this.message = message;
        this.city = city;
        this.location = location;
        this.ts = OffsetDateTime.now();
        this.acknowledged = false;
    }
}
