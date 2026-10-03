package com.cityplatform.environment.model;

import com.cityplatform.environment.model.enums.Severity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * 环境告警记录实体
 * 存储触发的告警详情与处理状态
 */
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

    public AlertRecord() {}

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }

    public Double getThreshold() { return threshold; }
    public void setThreshold(Double threshold) { this.threshold = threshold; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public OffsetDateTime getTs() { return ts; }
    public void setTs(OffsetDateTime ts) { this.ts = ts; }

    public Boolean getAcknowledged() { return acknowledged; }
    public void setAcknowledged(Boolean acknowledged) { this.acknowledged = acknowledged; }

    public OffsetDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(OffsetDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }

    public String getAcknowledgedBy() { return acknowledgedBy; }
    public void setAcknowledgedBy(String acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }

    @Override
    public String toString() {
        return "AlertRecord{" +
                "id=" + id +
                ", alertId='" + alertId + '\'' +
                ", ruleId='" + ruleId + '\'' +
                ", metric='" + metric + '\'' +
                ", value=" + value +
                ", threshold=" + threshold +
                ", severity=" + severity +
                ", city='" + city + '\'' +
                ", acknowledged=" + acknowledged +
                ", ts=" + ts +
                '}';
    }
}
