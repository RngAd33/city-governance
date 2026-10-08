package com.cityplatform.disaster.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 水位监测记录实体（city-water 表的只读映射）
 * 灾害服务仅读取近期记录用于趋势分析，写入由 city-water 负责；
 * 字段定义须与 city-water 的同名实体保持一致，避免共享库表结构漂移
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "water_level_record")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WaterLevelRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "station_id", nullable = false, length = 32)
    private String stationId;

    @Column(name = "record_time", nullable = false)
    private LocalDateTime timestamp;

    /** 水位（米） */
    @Column(name = "water_level", nullable = false)
    private Double waterLevel;

    /** 流量（立方米/秒） */
    @Column(name = "flow_rate", nullable = false)
    private Double flowRate;

    /** 降雨量（毫米） */
    @Column(nullable = false)
    private Double rainfall;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 16)
    private RiskLevel riskLevel;

    /**
     * 风险等级枚举
     */
    @Getter
    @AllArgsConstructor
    public enum RiskLevel {
        LOW("低风险"),
        MEDIUM("中风险"),
        HIGH("高风险"),
        CRITICAL("严重风险");

        private final String description;
    }
}
