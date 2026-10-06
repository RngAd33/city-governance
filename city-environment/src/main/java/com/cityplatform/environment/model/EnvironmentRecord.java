package com.cityplatform.environment.model;

import com.cityplatform.environment.model.enums.EnvironmentLevel;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 环境监测记录实体
 * 存储各站点采集的环境指标数据
 */
@Data
@NoArgsConstructor
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
}
