package com.cityplatform.water.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 水位监测记录实体
 * 存储各站点的时间序列水位数据
 */
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

    public WaterLevelRecord() {}

    public WaterLevelRecord(String stationId, LocalDateTime timestamp,
                            Double waterLevel, Double flowRate,
                            Double rainfall, RiskLevel riskLevel) {
        this.stationId = stationId;
        this.timestamp = timestamp;
        this.waterLevel = waterLevel;
        this.flowRate = flowRate;
        this.rainfall = rainfall;
        this.riskLevel = riskLevel;
    }

    /**
     * 风险等级枚举
     */
    public enum RiskLevel {
        LOW("低风险"),
        MEDIUM("中风险"),
        HIGH("高风险"),
        CRITICAL("严重风险");

        private final String description;

        RiskLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Double getWaterLevel() { return waterLevel; }
    public void setWaterLevel(Double waterLevel) { this.waterLevel = waterLevel; }

    public Double getFlowRate() { return flowRate; }
    public void setFlowRate(Double flowRate) { this.flowRate = flowRate; }

    public Double getRainfall() { return rainfall; }
    public void setRainfall(Double rainfall) { this.rainfall = rainfall; }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    @Override
    public String toString() {
        return "WaterLevelRecord{" +
                "id=" + id +
                ", stationId='" + stationId + '\'' +
                ", timestamp=" + timestamp +
                ", waterLevel=" + waterLevel +
                ", flowRate=" + flowRate +
                ", rainfall=" + rainfall +
                ", riskLevel=" + riskLevel +
                '}';
    }
}
