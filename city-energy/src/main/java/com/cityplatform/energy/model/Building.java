package com.cityplatform.energy.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 建筑实体
 * 存储城市建筑的基本信息与能耗参数
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "buildings")
public class Building {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "building_id", unique = true, nullable = false, length = 64)
    private String buildingId;

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BuildingType type;

    @Column(nullable = false)
    private Double area;

    @Column(nullable = false)
    private Integer floors;

    @Column(name = "build_year", nullable = false)
    private Integer buildYear;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Column(name = "avg_daily_consumption")
    private Double avgDailyConsumption;

    @Column(name = "carbon_factor")
    private Double carbonFactor;
}
