package com.cityplatform.water.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 洪涝灾害模拟场景实体
 * 存储模拟参数与预测结果
 */
@Entity
@Table(name = "flood_scenario")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FloodScenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", unique = true, nullable = false, length = 32)
    private String scenarioId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "station_id", nullable = false, length = 32)
    private String stationId;

    /** 降雨强度（毫米） */
    @Column(name = "rainfall_mm", nullable = false)
    private Double rainfallMm;

    /** 持续时长（小时） */
    @Column(name = "duration_hours", nullable = false)
    private Integer durationHours;

    /** 影响面积（平方公里） */
    @Column(name = "affected_area", nullable = false)
    private Double affectedArea;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 16)
    private WaterLevelRecord.RiskLevel riskLevel;

    /** 预估损失（万元） */
    @Column(name = "estimated_damage", nullable = false)
    private Double estimatedDamage;

    /** 受影响人口 */
    @Column(name = "affected_population", nullable = false)
    private Integer affectedPopulation;

    /** 道路封闭建议（JSON数组格式） */
    @Column(name = "road_closures", columnDefinition = "TEXT")
    private String roadClosures;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public FloodScenario() {}

    public FloodScenario(String scenarioId, String name, String stationId,
                         Double rainfallMm, Integer durationHours, Double affectedArea,
                         WaterLevelRecord.RiskLevel riskLevel, Double estimatedDamage,
                         Integer affectedPopulation, String roadClosures,
                         LocalDateTime createdAt) {
        this.scenarioId = scenarioId;
        this.name = name;
        this.stationId = stationId;
        this.rainfallMm = rainfallMm;
        this.durationHours = durationHours;
        this.affectedArea = affectedArea;
        this.riskLevel = riskLevel;
        this.estimatedDamage = estimatedDamage;
        this.affectedPopulation = affectedPopulation;
        this.roadClosures = roadClosures;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getScenarioId() { return scenarioId; }
    public void setScenarioId(String scenarioId) { this.scenarioId = scenarioId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }

    public Double getRainfallMm() { return rainfallMm; }
    public void setRainfallMm(Double rainfallMm) { this.rainfallMm = rainfallMm; }

    public Integer getDurationHours() { return durationHours; }
    public void setDurationHours(Integer durationHours) { this.durationHours = durationHours; }

    public Double getAffectedArea() { return affectedArea; }
    public void setAffectedArea(Double affectedArea) { this.affectedArea = affectedArea; }

    public WaterLevelRecord.RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(WaterLevelRecord.RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public Double getEstimatedDamage() { return estimatedDamage; }
    public void setEstimatedDamage(Double estimatedDamage) { this.estimatedDamage = estimatedDamage; }

    public Integer getAffectedPopulation() { return affectedPopulation; }
    public void setAffectedPopulation(Integer affectedPopulation) { this.affectedPopulation = affectedPopulation; }

    public String getRoadClosures() { return roadClosures; }
    public void setRoadClosures(String roadClosures) { this.roadClosures = roadClosures; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "FloodScenario{" +
                "id=" + id +
                ", scenarioId='" + scenarioId + '\'' +
                ", name='" + name + '\'' +
                ", stationId='" + stationId + '\'' +
                ", rainfallMm=" + rainfallMm +
                ", durationHours=" + durationHours +
                ", affectedArea=" + affectedArea +
                ", riskLevel=" + riskLevel +
                ", estimatedDamage=" + estimatedDamage +
                ", affectedPopulation=" + affectedPopulation +
                ", roadClosures='" + roadClosures + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
