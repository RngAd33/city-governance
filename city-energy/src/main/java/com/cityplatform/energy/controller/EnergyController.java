package com.cityplatform.energy.controller;

import com.cityplatform.energy.analyzer.BuildingEnergyAnalyzer;
import com.cityplatform.energy.analyzer.LoadPredictor;
import com.cityplatform.energy.model.Building;
import com.cityplatform.energy.model.EnergyConsumptionRecord;
import com.cityplatform.energy.model.GridEnergyRecord;
import com.cityplatform.energy.repository.BuildingRepository;
import com.cityplatform.energy.repository.EnergyConsumptionRecordRepository;
import com.cityplatform.energy.repository.GridEnergyRecordRepository;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 能耗分析REST控制器
 */
@RestController
@RequestMapping("/api/energy")
public class EnergyController {

    /** 电网指标名称清单（国内免费公开真实数据：OWID 中国年度） */
    private static final List<String> GRID_METRICS = List.of(
            "grid_carbon_intensity", "grid_renewable_percentage");

    /** 能耗记录以事件 UTC 墙钟时间入库（EnergyEventConsumer 取事件 ts 的 UTC 语义墙钟），查询锚点须同基准 */
    private static final ZoneOffset RECORD_OFFSET = ZoneOffset.UTC;
    /** 城市本地时区（南京），用于时间桶对齐到自然边界（整点/零点） */
    private static final ZoneId CITY_ZONE = ZoneId.of("Asia/Shanghai");
    /** 桶时间下发格式：UTC ISO（带 Z 后缀），前端 new Date() 自动转为本地时间展示 */
    private static final DateTimeFormatter BUCKET_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Resource
    private BuildingRepository buildingRepository;

    @Resource
    private EnergyConsumptionRecordRepository recordRepository;

    @Resource
    private GridEnergyRecordRepository gridRepository;

    @Resource
    private BuildingEnergyAnalyzer analyzer;

    @Resource
    private LoadPredictor loadPredictor;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "city-energy");
        status.put("status", "running");
        status.put("port", 8083);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/buildings")
    public ResponseEntity<List<Building>> getAllBuildings() {
        List<Building> buildings = buildingRepository.findAll();
        return ResponseEntity.ok(buildings);
    }

    @GetMapping("/buildings/{id}/analysis")
    public ResponseEntity<BuildingEnergyAnalyzer.EnergyAnalysisResult> getBuildingAnalysis(@PathVariable String id) {
        Building building = buildingRepository.findByBuildingId(id)
                .orElseThrow(() -> new RuntimeException("建筑不存在: " + id));
        List<EnergyConsumptionRecord> records = recordRepository.findByBuildingId(id);
        BuildingEnergyAnalyzer.EnergyAnalysisResult result = analyzer.analyzeBuilding(building, records);
        return ResponseEntity.ok(result);
    }

    /**
     * 建筑能耗历史（按时间桶聚合 total_kwh 均值，供前端绘制能耗趋势）
     * hours 查询窗口小时数（1~8760）；bucketHours 桶粒度小时数（1~24），桶边界按城市本地时区对齐；
     * 返回 [{bucketTime: UTC ISO, avgTotalKwh, sampleCount}] 按时间升序，无样本的桶不下发
     */
    @GetMapping("/buildings/{id}/history")
    public ResponseEntity<List<Map<String, Object>>> getBuildingHistory(
            @PathVariable String id,
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "1") int bucketHours) {
        buildingRepository.findByBuildingId(id)
                .orElseThrow(() -> new RuntimeException("建筑不存在: " + id));

        int safeHours = Math.min(Math.max(hours, 1), 8760);
        int safeBucket = Math.min(Math.max(bucketHours, 1), 24);

        // 记录时间为 UTC 墙钟，查询锚点须用同基准的当前时刻
        LocalDateTime endUtc = LocalDateTime.now(RECORD_OFFSET);
        LocalDateTime startUtc = endUtc.minusHours(safeHours);
        List<EnergyConsumptionRecord> records = recordRepository
                .findByBuildingIdAndTimestampBetween(id, startUtc, endUtc);

        // 分桶聚合：桶键为对齐后的 UTC 墙钟时间，TreeMap 保证升序输出
        Map<LocalDateTime, List<Double>> buckets = new TreeMap<>();
        for (EnergyConsumptionRecord record : records) {
            if (record.getTimestamp() == null) {
                continue;
            }
            LocalDateTime bucketKey = alignToBucket(record.getTimestamp(), safeBucket);
            buckets.computeIfAbsent(bucketKey, k -> new ArrayList<>()).add(record.getTotalKwh());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<LocalDateTime, List<Double>> entry : buckets.entrySet()) {
            List<Double> values = entry.getValue();
            double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            Map<String, Object> bucket = new LinkedHashMap<>();
            bucket.put("bucketTime", entry.getKey().format(BUCKET_TIME_FORMATTER));
            bucket.put("avgTotalKwh", Math.round(mean * 10.0) / 10.0);
            bucket.put("sampleCount", values.size());
            result.add(bucket);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 将记录时间（UTC 墙钟）对齐到时间桶的自然边界：
     * 1h → 本地整点；24h → 本地零点；其余 → 本地「零点 + 桶粒度整倍」时刻。
     * 先还原为瞬时点转城市本地时区对齐，再转回 UTC 墙钟作桶键，
     * 保证并行查询的各建筑桶边界严格一致
     */
    private LocalDateTime alignToBucket(LocalDateTime recordTimeUtc, int bucketHours) {
        ZonedDateTime local = recordTimeUtc.atOffset(RECORD_OFFSET).atZoneSameInstant(CITY_ZONE);
        ZonedDateTime localBucket;
        if (bucketHours >= 24) {
            localBucket = local.truncatedTo(ChronoUnit.DAYS);
        } else if (bucketHours == 1) {
            localBucket = local.truncatedTo(ChronoUnit.HOURS);
        } else {
            localBucket = local.truncatedTo(ChronoUnit.DAYS)
                    .plusHours((local.getHour() / bucketHours) * bucketHours);
        }
        return localBucket.withZoneSameInstant(RECORD_OFFSET).toLocalDateTime();
    }

    @GetMapping("/high-consumption")
    public ResponseEntity<List<Building>> getHighConsumptionBuildings(
            @RequestParam(defaultValue = "1000") double threshold) {
        List<Building> buildings = buildingRepository.findAll();
        List<Building> highConsumption = analyzer.identifyHighConsumptionBuildings(buildings, threshold);
        return ResponseEntity.ok(highConsumption);
    }

    @PostMapping("/predict")
    public ResponseEntity<Map<String, Object>> predictLoad(@RequestBody Map<String, Object> request) {
        String buildingId = (String) request.get("buildingId");
        double outdoorTemp = ((Number) request.get("outdoorTemp")).doubleValue();
        double targetTemp = ((Number) request.get("targetTemp")).doubleValue();
        double humidity = ((Number) request.getOrDefault("humidity", 50.0)).doubleValue();

        Building building = buildingRepository.findByBuildingId(buildingId)
                .orElseThrow(() -> new RuntimeException("建筑不存在: " + buildingId));

        double predictedLoad = loadPredictor.predictLoad(building, outdoorTemp, targetTemp, humidity);

        Map<String, Object> response = new HashMap<>();
        response.put("buildingId", buildingId);
        response.put("outdoorTemp", outdoorTemp);
        response.put("targetTemp", targetTemp);
        response.put("humidity", humidity);
        response.put("predictedLoadKw", predictedLoad);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/saving-potential/{buildingId}")
    public ResponseEntity<BuildingEnergyAnalyzer.EnergySavingPotential> getSavingPotential(@PathVariable String buildingId) {
        Building building = buildingRepository.findByBuildingId(buildingId)
                .orElseThrow(() -> new RuntimeException("建筑不存在: " + buildingId));
        BuildingEnergyAnalyzer.EnergySavingPotential potential = analyzer.calculateEnergySavingPotential(building);
        return ResponseEntity.ok(potential);
    }

    /**
     * 电网指标最新值（逐指标取最新一条；真实数据，OWID 中国年度）
     */
    @GetMapping("/grid/latest")
    public ResponseEntity<List<GridEnergyRecord>> getGridLatest(
            @RequestParam(defaultValue = "CN") String zone) {
        List<GridEnergyRecord> latest = new ArrayList<>();
        for (String metric : GRID_METRICS) {
            gridRepository.findTopByZoneAndMetricOrderByTsDesc(zone, metric).ifPresent(latest::add);
        }
        return ResponseEntity.ok(latest);
    }

    /**
     * 电网指标历史序列（默认碳强度，默认近24小时，最多90天）
     */
    @GetMapping("/grid/history")
    public ResponseEntity<List<GridEnergyRecord>> getGridHistory(
            @RequestParam(defaultValue = "CN") String zone,
            @RequestParam(defaultValue = "grid_carbon_intensity") String metric,
            @RequestParam(defaultValue = "24") int hours) {
        OffsetDateTime end = OffsetDateTime.now();
        OffsetDateTime start = end.minusHours(Math.max(1, Math.min(hours, 2160)));
        return ResponseEntity.ok(
                gridRepository.findByZoneAndMetricAndTsBetweenOrderByTsAsc(zone, metric, start, end));
    }
}
