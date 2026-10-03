package com.cityplatform.environment.model;

import com.cityplatform.environment.model.enums.Operator;
import com.cityplatform.environment.model.enums.Severity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * 环境告警规则实体
 * 定义各环境指标的阈值与告警级别
 */
@Entity
@Table(name = "alert_rule")
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_id", unique = true, nullable = false, length = 64)
    private String ruleId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 32)
    private String metric;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private Operator operator;

    @Column(nullable = false)
    private Double threshold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Severity severity;

    @Column(name = "message_template", nullable = false, length = 512)
    private String messageTemplate;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public AlertRule() {}

    public AlertRule(String ruleId, String name, String metric, Operator operator,
                    Double threshold, Severity severity, String messageTemplate) {
        this.ruleId = ruleId;
        this.name = name;
        this.metric = metric;
        this.operator = operator;
        this.threshold = threshold;
        this.severity = severity;
        this.messageTemplate = messageTemplate;
        this.enabled = true;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }

    public Operator getOperator() { return operator; }
    public void setOperator(Operator operator) { this.operator = operator; }

    public Double getThreshold() { return threshold; }
    public void setThreshold(Double threshold) { this.threshold = threshold; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public String getMessageTemplate() { return messageTemplate; }
    public void setMessageTemplate(String messageTemplate) { this.messageTemplate = messageTemplate; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "AlertRule{" +
                "id=" + id +
                ", ruleId='" + ruleId + '\'' +
                ", name='" + name + '\'' +
                ", metric='" + metric + '\'' +
                ", operator=" + operator +
                ", threshold=" + threshold +
                ", severity=" + severity +
                ", enabled=" + enabled +
                '}';
    }
}
