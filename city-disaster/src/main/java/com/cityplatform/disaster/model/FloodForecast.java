package com.cityplatform.disaster.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 洪水预报实体
 * 存储长江南京段真实径流量预报（Open-Meteo Flood/GloFAS 采集，日粒度含7天预报）
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "flood_forecast",
        uniqueConstraints = @UniqueConstraint(name = "uk_flood_forecast_station_date",
                columnNames = {"station_name", "forecast_date"}))
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FloodForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 河段名称（如 南京长江大桥江段） */
    @Column(name = "station_name", nullable = false, length = 128)
    private String stationName;

    /** 预报日期（日粒度） */
    @Column(name = "forecast_date", nullable = false)
    private LocalDate forecastDate;

    /** 日径流量（m³/s） */
    @Column(nullable = false)
    private Double discharge;

    /** 日最大径流量（m³/s） */
    @Column(name = "discharge_max")
    private Double dischargeMax;

    /** 日平均径流量（m³/s） */
    @Column(name = "discharge_mean")
    private Double dischargeMean;

    /** 数据来源（如 OPEN_METEO） */
    @Column(nullable = false, length = 32)
    private String source;

    /** 采集时间 */
    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;
}
