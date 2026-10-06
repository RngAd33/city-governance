package com.cityplatform.environment.controller;

import com.cityplatform.environment.model.AlertRecord;
import com.cityplatform.environment.model.AlertRule;
import com.cityplatform.environment.model.EnvironmentRecord;
import com.cityplatform.environment.model.enums.Severity;
import com.cityplatform.environment.monitor.AirQualityMonitor;
import com.cityplatform.environment.monitor.WeatherMonitor;
import com.cityplatform.environment.repository.AlertRecordRepository;
import com.cityplatform.environment.repository.AlertRuleRepository;
import com.cityplatform.environment.repository.EnvironmentRecordRepository;
import jakarta.annotation.Resource;
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

    @Resource
    private EnvironmentRecordRepository environmentRecordRepository;

    @Resource
    private AlertRuleRepository alertRuleRepository;

    @Resource
    private AlertRecordRepository alertRecordRepository;

    @Resource
    private AirQualityMonitor airQualityMonitor;

    @Resource
    private WeatherMonitor weatherMonitor;

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
     * hours 参数用于前端 24小时/7天/30天 选项（24/168/720），优先于 startDate/endDate
     */
    @GetMapping("/records")
    public ResponseEntity<List<EnvironmentRecord>> getRecords(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String metric,
            @RequestParam(required = false) Integer hours,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) {

        // 指标名归一化：前端可能传 "PM2.5" 等展示名，统一转换为存储规范的小写名称
        metric = normalizeMetric(metric);

        // hours 转换：[now-hours, now] 查询窗口
        if (hours != null && hours > 0) {
            OffsetDateTime now = OffsetDateTime.now();
            startDate = now.minusHours(hours);
            endDate = now;
        }

        List<EnvironmentRecord> records;

        if (city != null && metric != null && startDate != null && endDate != null) {
            records = environmentRecordRepository
                    .findByCityAndMetricAndTsBetween(city, metric, startDate, endDate);
        } else if (city != null && metric != null) {
            records = environmentRecordRepository.findByCityAndMetric(city, metric);
        } else if (city != null && startDate != null && endDate != null) {
            records = environmentRecordRepository.findByCityAndTsBetween(city, startDate, endDate);
        } else if (metric != null && startDate != null && endDate != null) {
            records = environmentRecordRepository.findByMetricAndTsBetween(metric, startDate, endDate);
        } else if (city != null) {
            records = environmentRecordRepository.findByCity(city);
        } else if (metric != null) {
            records = environmentRecordRepository.findByMetric(metric);
        } else if (startDate != null && endDate != null) {
            records = environmentRecordRepository.findByTsBetween(startDate, endDate);
        } else {
            records = environmentRecordRepository.findAll();
        }

        // 统一按时间升序返回，保证前端趋势图按时间顺序绘制
        records = new ArrayList<>(records);
        records.sort(Comparator.comparing(EnvironmentRecord::getTs));

        return ResponseEntity.ok(records);
    }

    /**
     * 获取指定城市的当前AQI
     */
    @GetMapping("/aqi")
    public ResponseEntity<Map<String, Object>> getCurrentAQI(@RequestParam String city) {
        // 获取该城市最新的空气质量相关记录
        // 注意：数据库存储的指标名为小写规范（pm25/pm10/no2/o3/co/so2），
        // 与采集端事件命名保持一致，查询时必须使用小写名称
        List<EnvironmentRecord> pm25Records = environmentRecordRepository
                .findByCityAndMetricOrderByTsDesc(city, "pm25");
        List<EnvironmentRecord> pm10Records = environmentRecordRepository
                .findByCityAndMetricOrderByTsDesc(city, "pm10");
        List<EnvironmentRecord> no2Records = environmentRecordRepository
                .findByCityAndMetricOrderByTsDesc(city, "no2");
        List<EnvironmentRecord> o3Records = environmentRecordRepository
                .findByCityAndMetricOrderByTsDesc(city, "o3");
        List<EnvironmentRecord> coRecords = environmentRecordRepository
                .findByCityAndMetricOrderByTsDesc(city, "co");
        List<EnvironmentRecord> so2Records = environmentRecordRepository
                .findByCityAndMetricOrderByTsDesc(city, "so2");

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

        // 指标名归一化，兼容前端传入的展示名变体
        metric = normalizeMetric(metric);

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

        // 指标名归一化，兼容前端传入的展示名变体
        metric = normalizeMetric(metric);

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

    /**
     * 指标名归一化
     * 将前端传入的展示名变体（如 "PM2.5"、"PM 2.5"）统一转换为
     * 数据库中存储的规范小写名称（如 pm25）
     */
    private String normalizeMetric(String metric) {
        if (metric == null || metric.isBlank()) return metric;
        return metric.toLowerCase(Locale.ROOT).replace(" ", "").replace(".", "");
    }
}
