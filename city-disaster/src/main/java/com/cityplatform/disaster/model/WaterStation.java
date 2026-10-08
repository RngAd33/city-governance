package com.cityplatform.disaster.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 水文监测站点实体（city-water 表的只读映射）
 * 灾害服务仅读取站点定义与当前水位用于风险评估，写入由 city-water 负责；
 * 字段定义须与 city-water 的同名实体保持一致，避免共享库表结构漂移
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
