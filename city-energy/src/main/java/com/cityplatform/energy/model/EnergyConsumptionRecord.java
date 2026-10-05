package com.cityplatform.energy.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 能耗记录实体
 * 存储建筑的分项能耗数据
 */
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

    public EnergyConsumptionRecord() {}

    public EnergyConsumptionRecord(String buildingId, LocalDateTime timestamp,
                                   Double electricityKwh, Double heatingKwh, Double coolingKwh,
                                   Double totalKwh, Double carbonEmission, Double cost) {
        this.buildingId = buildingId;
        this.timestamp = timestamp;
        this.electricityKwh = electricityKwh;
        this.heatingKwh = heatingKwh;
        this.coolingKwh = coolingKwh;
        this.totalKwh = totalKwh;
        this.carbonEmission = carbonEmission;
        this.cost = cost;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBuildingId() { return buildingId; }
    public void setBuildingId(String buildingId) { this.buildingId = buildingId; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Double getElectricityKwh() { return electricityKwh; }
    public void setElectricityKwh(Double electricityKwh) { this.electricityKwh = electricityKwh; }

    public Double getHeatingKwh() { return heatingKwh; }
    public void setHeatingKwh(Double heatingKwh) { this.heatingKwh = heatingKwh; }

    public Double getCoolingKwh() { return coolingKwh; }
    public void setCoolingKwh(Double coolingKwh) { this.coolingKwh = coolingKwh; }

    public Double getTotalKwh() { return totalKwh; }
    public void setTotalKwh(Double totalKwh) { this.totalKwh = totalKwh; }

    public Double getCarbonEmission() { return carbonEmission; }
    public void setCarbonEmission(Double carbonEmission) { this.carbonEmission = carbonEmission; }

    public Double getCost() { return cost; }
    public void setCost(Double cost) { this.cost = cost; }

    @Override
    public String toString() {
        return "EnergyConsumptionRecord{" +
                "id=" + id +
                ", buildingId='" + buildingId + '\'' +
                ", timestamp=" + timestamp +
                ", electricityKwh=" + electricityKwh +
                ", heatingKwh=" + heatingKwh +
                ", coolingKwh=" + coolingKwh +
                ", totalKwh=" + totalKwh +
                ", carbonEmission=" + carbonEmission +
                ", cost=" + cost +
                '}';
    }
}
