package com.cityplatform.energy.controller;

import com.cityplatform.energy.analyzer.BuildingEnergyAnalyzer;
import com.cityplatform.energy.analyzer.LoadPredictor;
import com.cityplatform.energy.model.Building;
import com.cityplatform.energy.model.EnergyConsumptionRecord;
import com.cityplatform.energy.model.GridEnergyRecord;
import com.cityplatform.energy.repository.BuildingRepository;
import com.cityplatform.energy.repository.EnergyConsumptionRecordRepository;
import com.cityplatform.energy.repository.GridEnergyRecordRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 能耗分析REST控制器
 */
@RestController
@RequestMapping("/api/energy")
public class EnergyController {

    /** 电网指标名称清单（国内免费公开真实数据：OWID 中国年度） */
    private static final List<String> GRID_METRICS = List.of(
            "grid_carbon_intensity", "grid_renewable_percentage");

    private final BuildingRepository buildingRepository;
    private final EnergyConsumptionRecordRepository recordRepository;
    private final GridEnergyRecordRepository gridRepository;
    private final BuildingEnergyAnalyzer analyzer;
    private final LoadPredictor loadPredictor;

    public EnergyController(BuildingRepository buildingRepository,
                           EnergyConsumptionRecordRepository recordRepository,
                           GridEnergyRecordRepository gridRepository,
                           BuildingEnergyAnalyzer analyzer,
                           LoadPredictor loadPredictor) {
        this.buildingRepository = buildingRepository;
        this.recordRepository = recordRepository;
        this.gridRepository = gridRepository;
        this.analyzer = analyzer;
        this.loadPredictor = loadPredictor;
    }

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
