package com.cityplatform.water.controller;

import com.cityplatform.water.model.WaterLevelRecord;
import com.cityplatform.water.model.WaterStation;
import com.cityplatform.water.risk.DrainageAnalyzer;
import com.cityplatform.water.repository.WaterLevelRecordRepository;
import com.cityplatform.water.repository.WaterStationRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 水务监测REST控制器
 * 提供水位监测与排水能力分析API（灾害评估与模拟接口已迁移至 city-disaster）
 */
@RestController
@RequestMapping("/api/water")
@Slf4j
public class WaterController {

    @Resource
    private WaterStationRepository waterStationRepository;

    @Resource
    private WaterLevelRecordRepository waterLevelRecordRepository;

    @Resource
    private DrainageAnalyzer drainageAnalyzer;

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
