package com.cityplatform.water.controller;

import com.cityplatform.water.model.FloodScenario;
import com.cityplatform.water.model.WaterLevelRecord;
import com.cityplatform.water.model.WaterStation;
import com.cityplatform.water.risk.DrainageAnalyzer;
import com.cityplatform.water.risk.FloodRiskAssessor;
import com.cityplatform.water.repository.FloodScenarioRepository;
import com.cityplatform.water.repository.WaterLevelRecordRepository;
import com.cityplatform.water.repository.WaterStationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 水务管理REST控制器
 * 提供水位监测、风险评估、灾害模拟等API
 */
@RestController
@RequestMapping("/api/water")
public class WaterController {

    private static final Logger log = LoggerFactory.getLogger(WaterController.class);

    private final WaterStationRepository waterStationRepository;
    private final WaterLevelRecordRepository waterLevelRecordRepository;
    private final FloodScenarioRepository floodScenarioRepository;
    private final FloodRiskAssessor floodRiskAssessor;
    private final DrainageAnalyzer drainageAnalyzer;

    public WaterController(WaterStationRepository waterStationRepository,
                           WaterLevelRecordRepository waterLevelRecordRepository,
                           FloodScenarioRepository floodScenarioRepository,
                           FloodRiskAssessor floodRiskAssessor,
                           DrainageAnalyzer drainageAnalyzer) {
        this.waterStationRepository = waterStationRepository;
        this.waterLevelRecordRepository = waterLevelRecordRepository;
        this.floodScenarioRepository = floodScenarioRepository;
        this.floodRiskAssessor = floodRiskAssessor;
        this.drainageAnalyzer = drainageAnalyzer;
    }

    /**
     * 获取水务系统整体状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        List<WaterStation> stations = waterStationRepository.findAll();

        long totalStations = stations.size();
        long criticalStations = stations.stream()
                .filter(s -> s.getCapacityPercentage() > 95.0)
                .count();
        long warningStations = stations.stream()
                .filter(s -> s.getCapacityPercentage() > 80.0
                        && s.getCapacityPercentage() <= 95.0)
                .count();
        long normalStations = stations.stream()
                .filter(s -> s.getCapacityPercentage() <= 80.0)
                .count();

        List<WaterStation> bottleneckStations = drainageAnalyzer.identifyBottlenecks(stations);

        Map<String, Object> status = new HashMap<>();
        status.put("totalStations", totalStations);
        status.put("normalStations", normalStations);
        status.put("warningStations", warningStations);
        status.put("criticalStations", criticalStations);
        status.put("bottleneckStations", bottleneckStations.size());
        status.put("systemStatus", criticalStations > 0 ? "CRITICAL"
                : warningStations > 0 ? "WARNING" : "NORMAL");

        return ResponseEntity.ok(status);
    }

    /**
     * 获取所有监测站点列表
     */
    @GetMapping("/stations")
    public ResponseEntity<List<WaterStation>> getAllStations() {
        List<WaterStation> stations = waterStationRepository.findAll();
        return ResponseEntity.ok(stations);
    }

    /**
     * 获取指定站点当前水位
     */
    @GetMapping("/stations/{id}/level")
    public ResponseEntity<Map<String, Object>> getCurrentLevel(@PathVariable Long id) {
        WaterStation station = waterStationRepository.findById(id)
                .orElse(null);

        if (station == null) {
            return ResponseEntity.notFound().build();
        }

        // 获取最新水位记录
        List<WaterLevelRecord> records = waterLevelRecordRepository
                .findTop5ByStationIdOrderByTimestampDesc(station.getStationId());

        Map<String, Object> result = new HashMap<>();
        result.put("stationId", station.getStationId());
        result.put("stationName", station.getName());
        result.put("currentLevel", station.getCurrentLevel());
        result.put("warningLevel", station.getWarningLevel());
        result.put("dangerLevel", station.getDangerLevel());
        result.put("maxCapacity", station.getMaxCapacity());
        result.put("capacityPercentage", station.getCapacityPercentage());
        result.put("recentRecords", records);

        return ResponseEntity.ok(result);
    }

    /**
     * 获取指定站点的风险评估
     */
    @GetMapping("/stations/{id}/risk")
    public ResponseEntity<FloodRiskAssessor.RiskAssessment> getRiskAssessment(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "0") double rainfallForecast,
            @RequestParam(required = false, defaultValue = "24") int forecastHours) {

        WaterStation station = waterStationRepository.findById(id)
                .orElse(null);

        if (station == null) {
            return ResponseEntity.notFound().build();
        }

        FloodRiskAssessor.RiskAssessment assessment = floodRiskAssessor.assessRisk(
                station, station.getCurrentLevel(), rainfallForecast, forecastHours);

        return ResponseEntity.ok(assessment);
    }

    /**
     * 运行洪涝模拟
     */
    @PostMapping("/simulate")
    public ResponseEntity<FloodScenario> runSimulation(@RequestBody Map<String, Object> request) {
        String stationId = (String) request.get("stationId");
        Double rainfallMm = ((Number) request.get("rainfallMm")).doubleValue();
        Integer durationHours = ((Number) request.get("durationHours")).intValue();

        WaterStation station = waterStationRepository.findByStationId(stationId)
                .orElse(null);

        if (station == null) {
            return ResponseEntity.notFound().build();
        }

        FloodScenario scenario = floodRiskAssessor.simulateFloodScenario(
                station, rainfallMm, durationHours);

        return ResponseEntity.ok(scenario);
    }

    /**
     * 获取所有洪涝模拟场景
     */
    @GetMapping("/scenarios")
    public ResponseEntity<List<FloodScenario>> getAllScenarios() {
        List<FloodScenario> scenarios = floodScenarioRepository.findAll();
        return ResponseEntity.ok(scenarios);
    }

    /**
     * 获取排水能力分析
     */
    @GetMapping("/drainage/analysis")
    public ResponseEntity<List<DrainageAnalyzer.DrainageAnalysis>> getDrainageAnalysis() {
        List<WaterStation> stations = waterStationRepository.findAll();

        List<DrainageAnalyzer.DrainageAnalysis> analyses = stations.stream()
                .map(station -> {
                    double inflow = station.getCurrentLevel() / station.getMaxCapacity() * 20.0;
                    return drainageAnalyzer.analyzeDrainageCapacity(station, inflow);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(analyses);
    }
}
