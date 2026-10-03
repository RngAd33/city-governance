package com.cityplatform.environment.controller;

import com.cityplatform.environment.model.*;
import com.cityplatform.environment.model.enums.Severity;
import com.cityplatform.environment.monitor.AirQualityMonitor;
import com.cityplatform.environment.monitor.WeatherMonitor;
import com.cityplatform.environment.repository.AlertRecordRepository;
import com.cityplatform.environment.repository.AlertRuleRepository;
import com.cityplatform.environment.repository.EnvironmentRecordRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * 环境监测REST控制器
 * 提供环境数据查询、AQI获取、告警管理、趋势分析等接口
 */
@RestController
@RequestMapping("/api/environment")
public class EnvironmentController {

    private final EnvironmentRecordRepository environmentRecordRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final AlertRecordRepository alertRecordRepository;
    private final AirQualityMonitor airQualityMonitor;
    private final WeatherMonitor weatherMonitor;

    public EnvironmentController(EnvironmentRecordRepository environmentRecordRepository,
                                  AlertRuleRepository alertRuleRepository,
                                  AlertRecordRepository alertRecordRepository,
                                  AirQualityMonitor airQualityMonitor,
                                  WeatherMonitor weatherMonitor) {
        this.environmentRecordRepository = environmentRecordRepository;
        this.alertRuleRepository = alertRuleRepository;
        this.alertRecordRepository = alertRecordRepository;
        this.airQualityMonitor = airQualityMonitor;
        this.weatherMonitor = weatherMonitor;
    }

    /**
     * 获取环境监测服务状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("service", "city-environment");
        status.put("status", "UP");
        status.put("timestamp", OffsetDateTime.now());
        status.put("metrics", List.of("AQI", "PM2.5", "PM10", "NO2", "O3", "CO", "SO2",
                "temperature", "humidity", "wind_speed", "pressure"));
        return ResponseEntity.ok(status);
    }

    /**
     * 查询环境记录列表
     * 支持按城市、指标、时间范围过滤
     */
    @GetMapping("/records")
    public ResponseEntity<List<EnvironmentRecord>> getRecords(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String metric,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) {

        List<EnvironmentRecord> records;

        if (city != null && metric != null && startDate != null && endDate != null) {
            records = environmentRecordRepository
                    .findByCityAndMetricAndTsBetween(city, metric, startDate, endDate);
        } else if (city != null && metric != null) {
            records = environmentRecordRepository.findByCityAndMetric(city, metric);
        } else if (city != null && startDate != null && endDate != null) {
            records = environmentRecordRepository.findByCityAndTsBetween(city, startDate, endDate);
        } else if (city != null) {
            records = environmentRecordRepository.findByCity(city);
        } else if (metric != null) {
            records = environmentRecordRepository.findByMetric(metric);
        } else if (startDate != null && endDate != null) {
            records = environmentRecordRepository.findByTsBetween(startDate, endDate);
        } else {
            records = environmentRecordRepository.findAll();
        }

        return ResponseEntity.ok(records);
    }

    /**
     * 获取指定城市的当前AQI
     */
    @GetMapping("/aqi")
    public ResponseEntity<Map<String, Object>> getCurrentAQI(@RequestParam String city) {
        // 获取该城市最新的空气质量相关记录
        List<EnvironmentRecord> pm25Records = environmentRecordRepository
                .findByCityAndMetricOrderByTsDesc(city, "PM2.5");
        List<EnvironmentRecord> pm10Records = environmentRecordRepository
                .findByCityAndMetricOrderByTsDesc(city, "PM10");
        List<EnvironmentRecord> no2Records = environmentRecordRepository
                .findByCityAndMetricOrderByTsDesc(city, "NO2");
        List<EnvironmentRecord> o3Records = environmentRecordRepository
                .findByCityAndMetricOrderByTsDesc(city, "O3");
        List<EnvironmentRecord> coRecords = environmentRecordRepository
                .findByCityAndMetricOrderByTsDesc(city, "CO");
        List<EnvironmentRecord> so2Records = environmentRecordRepository
                .findByCityAndMetricOrderByTsDesc(city, "SO2");

        double pm25 = getLatestValue(pm25Records);
        double pm10 = getLatestValue(pm10Records);
        double no2 = getLatestValue(no2Records);
        double o3 = getLatestValue(o3Records);
        double co = getLatestValue(coRecords);
        double so2 = getLatestValue(so2Records);

        AirQualityMonitor.AQIResult aqiResult = airQualityMonitor.calculateAQI(pm25, pm10, no2, o3, co, so2);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("city", city);
        result.put("aqi", aqiResult.aqi());
        result.put("level", aqiResult.level().name());
        result.put("levelDescription", aqiResult.level().getDescription());
        result.put("color", aqiResult.level().getColor());
        result.put("primaryPollutant", aqiResult.primaryPollutant());
        result.put("timestamp", OffsetDateTime.now());
        result.put("pollutants", Map.of(
                "pm25", pm25,
                "pm10", pm10,
                "no2", no2,
                "o3", o3,
                "co", co,
                "so2", so2
        ));

        return ResponseEntity.ok(result);
    }

    /**
     * 获取活跃告警列表
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<AlertRecord>> getActiveAlerts(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String severity) {

        List<AlertRecord> alerts;

        if (city != null && severity != null) {
            alerts = alertRecordRepository
                    .findByAcknowledgedFalseAndCityAndSeverity(city, Severity.valueOf(severity));
        } else if (city != null) {
            alerts = alertRecordRepository.findByAcknowledgedFalseAndCity(city);
        } else if (severity != null) {
            alerts = alertRecordRepository.findByAcknowledgedFalseAndSeverity(Severity.valueOf(severity));
        } else {
            alerts = alertRecordRepository.findByAcknowledgedFalse();
        }

        return ResponseEntity.ok(alerts);
    }

    /**
     * 确认告警
     */
    @PostMapping("/alerts/{id}/acknowledge")
    public ResponseEntity<Map<String, Object>> acknowledgeAlert(@PathVariable Long id) {
        Optional<AlertRecord> alertOpt = alertRecordRepository.findById(id);
        if (alertOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        AlertRecord alert = alertOpt.get();
        alert.setAcknowledged(true);
        alert.setAcknowledgedAt(OffsetDateTime.now());
        alert.setAcknowledgedBy("system");
        alertRecordRepository.save(alert);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("alertId", alert.getAlertId());
        result.put("acknowledged", true);
        result.put("acknowledgedAt", alert.getAcknowledgedAt());
        return ResponseEntity.ok(result);
    }

    /**
     * 获取趋势分析
     */
    @GetMapping("/trends")
    public ResponseEntity<List<WeatherMonitor.TrendAnalysis>> getTrends(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String metric,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) {

        List<EnvironmentRecord> records;

        if (city != null && metric != null && startDate != null && endDate != null) {
            records = environmentRecordRepository
                    .findByCityAndMetricAndTsBetween(city, metric, startDate, endDate);
        } else if (city != null && metric != null) {
            records = environmentRecordRepository.findByCityAndMetric(city, metric);
        } else if (city != null) {
            records = environmentRecordRepository.findByCity(city);
        } else if (metric != null) {
            records = environmentRecordRepository.findByMetric(metric);
        } else {
            records = environmentRecordRepository.findAll();
        }

        List<WeatherMonitor.TrendAnalysis> trends = weatherMonitor.analyzeWeatherTrend(records);
        return ResponseEntity.ok(trends);
    }

    /**
     * 获取异常检测
     */
    @GetMapping("/anomalies")
    public ResponseEntity<List<WeatherMonitor.AnomalyResult>> getAnomalies(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String metric) {

        List<EnvironmentRecord> records;

        if (city != null && metric != null) {
            records = environmentRecordRepository.findByCityAndMetric(city, metric);
        } else if (city != null) {
            records = environmentRecordRepository.findByCity(city);
        } else if (metric != null) {
            records = environmentRecordRepository.findByMetric(metric);
        } else {
            records = environmentRecordRepository.findAll();
        }

        List<WeatherMonitor.AnomalyResult> anomalies = weatherMonitor.detectAnomalies(records);
        return ResponseEntity.ok(anomalies);
    }

    /**
     * 获取所有告警规则
     */
    @GetMapping("/rules")
    public ResponseEntity<List<AlertRule>> getAlertRules() {
        return ResponseEntity.ok(alertRuleRepository.findAll());
    }

    /**
     * 创建告警规则
     */
    @PostMapping("/rules")
    public ResponseEntity<AlertRule> createAlertRule(@RequestBody AlertRule rule) {
        AlertRule saved = alertRuleRepository.save(rule);
        return ResponseEntity.ok(saved);
    }

    /**
     * 获取最新值
     */
    private double getLatestValue(List<EnvironmentRecord> records) {
        if (records == null || records.isEmpty()) return 0;
        return records.get(0).getValue() != null ? records.get(0).getValue() : 0;
    }
}
