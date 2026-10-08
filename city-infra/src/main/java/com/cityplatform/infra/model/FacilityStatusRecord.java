package com.cityplatform.infra.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 基础设施状态记录实体
 * 存储各类设施的时间序列状态数据（状态评分 / 告警级别）
 */
@Entity
@Data
@Table(name = "infra_status_record")
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FacilityStatusRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 设施类型（bridge/tunnel/road/pipeline/power_grid） */
    @Column(nullable = false, length = 32)
    private String facility;

    /** 指标类型（facility_status 状态评分 / alert_level 告警级别） */
    @Column(name = "metric_type", nullable = false, length = 32)
    private String metricType;

    /** 指标值（评分 0-100 / 告警级别 0-3） */
    @Column(nullable = false)
    private Double value;

    /** 单位（score / level） */
    @Column(length = 16)
    private String unit;

    /** 采集时间（Hibernate 按 UTC 写入 datetime 列，读出/查询带时区，与平台其他模块一致） */
    @Column(name = "record_time", nullable = false)
    private OffsetDateTime timestamp;

    /** 城市 */
    @Column(length = 64)
    private String city;

    /** 数据来源代码 */
    @Column(length = 32)
    private String source;

    /**
     * 业务便利构造器：不含自增主键 id
     */
    public FacilityStatusRecord(String facility, String metricType, Double value, String unit, OffsetDateTime ts, String city, String source) {
        this.facility = facility;
        this.metricType = metricType;
        this.value = value;
        this.unit = unit;
        this.timestamp = ts;
        this.city = city;
        this.source = source;
    }
}
