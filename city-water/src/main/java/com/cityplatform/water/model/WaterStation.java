package com.cityplatform.water.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 水文监测站点实体
 * 覆盖河流、湖泊、水库、排水管网四类监测点
 */
@Data
@NoArgsConstructor
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

    /**
     * 站点类型枚举
     */
    @Getter
    @AllArgsConstructor
    public enum StationType {
        RIVER("河流"),
        LAKE("湖泊"),
        RESERVOIR("水库"),
        DRAINAGE("排水管网");

        private final String description;
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

}
