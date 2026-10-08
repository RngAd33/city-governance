package com.cityplatform.energy.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 电网指标记录实体
 * 存储 OWID（Ember 数据源）中国年度真实数据（碳强度/可再生占比），zone=CN 为全国口径
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "grid_energy_records", indexes = {
        @Index(name = "idx_grid_zone_metric_ts", columnList = "zone, metric, ts")
})
public class GridEnergyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 电网分区（中国为 CN） */
    @Column(nullable = false, length = 16)
    private String zone;

    /** 指标名（grid_carbon_intensity / grid_renewable_percentage） */
    @Column(nullable = false, length = 64)
    private String metric;

    /** 指标值 */
    @Column(nullable = false)
    private Double value;

    /** 单位（gCO₂eq/kWh、%、MW） */
    @Column(length = 32)
    private String unit;

    /** 是否为模型估算值（OWID 统计值非估算，恒为 false；保留列兼容历史数据） */
    @Column(name = "is_estimated")
    private Boolean estimated;

    /** 数据来源代码（如 owid） */
    @Column(length = 32)
    private String source;

    /** 数据时间（带时区偏移） */
    @Column(nullable = false)
    private OffsetDateTime ts;
}
