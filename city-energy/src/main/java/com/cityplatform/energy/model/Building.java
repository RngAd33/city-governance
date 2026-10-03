package com.cityplatform.energy.model;

import jakarta.persistence.*;

/**
 * 建筑实体
 * 存储城市建筑的基本信息与能耗参数
 */
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

    public Building() {}

    public Building(String buildingId, String name, BuildingType type, Double area,
                    Integer floors, Integer buildYear, Double lat, Double lng,
                    Double avgDailyConsumption, Double carbonFactor) {
        this.buildingId = buildingId;
        this.name = name;
        this.type = type;
        this.area = area;
        this.floors = floors;
        this.buildYear = buildYear;
        this.lat = lat;
        this.lng = lng;
        this.avgDailyConsumption = avgDailyConsumption;
        this.carbonFactor = carbonFactor;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBuildingId() { return buildingId; }
    public void setBuildingId(String buildingId) { this.buildingId = buildingId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BuildingType getType() { return type; }
    public void setType(BuildingType type) { this.type = type; }

    public Double getArea() { return area; }
    public void setArea(Double area) { this.area = area; }

    public Integer getFloors() { return floors; }
    public void setFloors(Integer floors) { this.floors = floors; }

    public Integer getBuildYear() { return buildYear; }
    public void setBuildYear(Integer buildYear) { this.buildYear = buildYear; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }

    public Double getAvgDailyConsumption() { return avgDailyConsumption; }
    public void setAvgDailyConsumption(Double avgDailyConsumption) { this.avgDailyConsumption = avgDailyConsumption; }

    public Double getCarbonFactor() { return carbonFactor; }
    public void setCarbonFactor(Double carbonFactor) { this.carbonFactor = carbonFactor; }

    @Override
    public String toString() {
        return "Building{" +
                "id=" + id +
                ", buildingId='" + buildingId + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", area=" + area +
                ", floors=" + floors +
                ", buildYear=" + buildYear +
                ", lat=" + lat +
                ", lng=" + lng +
                ", avgDailyConsumption=" + avgDailyConsumption +
                ", carbonFactor=" + carbonFactor +
                '}';
    }
}
