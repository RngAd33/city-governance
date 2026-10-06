package com.cityplatform.energy.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 能耗记录实体
 * 存储建筑的分项能耗数据
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "energy_consumption_records")
public class EnergyConsumptionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "building_id", nullable = false, length = 64)
    private String buildingId;

    @Column(name = "record_time", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "electricity_kwh", nullable = false)
    private Double electricityKwh;

    @Column(name = "heating_kwh", nullable = false)
    private Double heatingKwh;

    @Column(name = "cooling_kwh", nullable = false)
    private Double coolingKwh;

    @Column(name = "total_kwh", nullable = false)
    private Double totalKwh;

    @Column(name = "carbon_emission", nullable = false)
    private Double carbonEmission;

    @Column(nullable = false)
    private Double cost;
}
