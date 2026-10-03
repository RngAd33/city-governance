package com.cityplatform.environment.model;

import com.cityplatform.environment.model.enums.EnvironmentLevel;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * 环境监测记录实体
 * 存储各站点采集的环境指标数据
 */
@Entity
@Table(name = "environment_record")
public class EnvironmentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_id", unique = true, nullable = false, length = 64)
    private String recordId;

    @Column(nullable = false, length = 64)
    private String city;

    @Column(name = "station_name", nullable = false, length = 128)
    private String stationName;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Column(nullable = false, length = 32)
    private String metric;

    @Column(nullable = false)
    private Double value;

    @Column(nullable = false, length = 16)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EnvironmentLevel level;

    @Column(nullable = false)
    private OffsetDateTime ts;

    public EnvironmentRecord() {}

    public EnvironmentRecord(String recordId, String city, String stationName,
                            Double lat, Double lng, String metric,
                            Double value, String unit, EnvironmentLevel level) {
        this.recordId = recordId;
        this.city = city;
        this.stationName = stationName;
        this.lat = lat;
        this.lng = lng;
        this.metric = metric;
        this.value = value;
        this.unit = unit;
        this.level = level;
        this.ts = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }

    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public EnvironmentLevel getLevel() { return level; }
    public void setLevel(EnvironmentLevel level) { this.level = level; }

    public OffsetDateTime getTs() { return ts; }
    public void setTs(OffsetDateTime ts) { this.ts = ts; }

    @Override
    public String toString() {
        return "EnvironmentRecord{" +
                "id=" + id +
                ", recordId='" + recordId + '\'' +
                ", city='" + city + '\'' +
                ", stationName='" + stationName + '\'' +
                ", metric='" + metric + '\'' +
                ", value=" + value +
                ", unit='" + unit + '\'' +
                ", level=" + level +
                ", ts=" + ts +
                '}';
    }
}
