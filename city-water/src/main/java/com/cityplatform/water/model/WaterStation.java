package com.cityplatform.water.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;

/**
 * 水文监测站点实体
 * 覆盖河流、湖泊、水库、排水管网四类监测点
 */
@Entity
@Table(name = "water_station")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WaterStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "station_id", unique = true, nullable = false, length = 32)
    private String stationId;

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private StationType type;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    /** 预警水位（米） */
    @Column(name = "warning_level", nullable = false)
    private Double warningLevel;

    /** 危险水位（米） */
    @Column(name = "danger_level", nullable = false)
    private Double dangerLevel;

    /** 当前水位（米） */
    @Column(name = "current_level", nullable = false)
    private Double currentLevel;

    /** 最大容量（米） */
    @Column(name = "max_capacity", nullable = false)
    private Double maxCapacity;

    public WaterStation() {}

    public WaterStation(String stationId, String name, StationType type,
                        Double lat, Double lng, Double warningLevel,
                        Double dangerLevel, Double currentLevel, Double maxCapacity) {
        this.stationId = stationId;
        this.name = name;
        this.type = type;
        this.lat = lat;
        this.lng = lng;
        this.warningLevel = warningLevel;
        this.dangerLevel = dangerLevel;
        this.currentLevel = currentLevel;
        this.maxCapacity = maxCapacity;
    }

    /**
     * 站点类型枚举
     */
    public enum StationType {
        RIVER("河流"),
        LAKE("湖泊"),
        RESERVOIR("水库"),
        DRAINAGE("排水管网");

        private final String description;

        StationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 计算当前水位占最大容量的百分比
     */
    public double getCapacityPercentage() {
        if (maxCapacity == null || maxCapacity == 0) {
            return 0.0;
        }
        return (currentLevel / maxCapacity) * 100.0;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStationId() { return stationId; }
    public void setStationId(String stationId) { this.stationId = stationId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public StationType getType() { return type; }
    public void setType(StationType type) { this.type = type; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }

    public Double getWarningLevel() { return warningLevel; }
    public void setWarningLevel(Double warningLevel) { this.warningLevel = warningLevel; }

    public Double getDangerLevel() { return dangerLevel; }
    public void setDangerLevel(Double dangerLevel) { this.dangerLevel = dangerLevel; }

    public Double getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(Double currentLevel) { this.currentLevel = currentLevel; }

    public Double getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(Double maxCapacity) { this.maxCapacity = maxCapacity; }

    @Override
    public String toString() {
        return "WaterStation{" +
                "id=" + id +
                ", stationId='" + stationId + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", lat=" + lat +
                ", lng=" + lng +
                ", warningLevel=" + warningLevel +
                ", dangerLevel=" + dangerLevel +
                ", currentLevel=" + currentLevel +
                ", maxCapacity=" + maxCapacity +
                '}';
    }
}
