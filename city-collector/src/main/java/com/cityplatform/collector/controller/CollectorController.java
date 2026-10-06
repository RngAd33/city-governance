package com.cityplatform.collector.controller;

import com.cityplatform.collector.entity.EventRecord;
import com.cityplatform.collector.repository.EventRecordRepository;
import com.cityplatform.collector.simulator.DataSimulatorService;
import com.cityplatform.collector.source.EnergyGridCollectionService;
import com.cityplatform.collector.source.FloodCollectionService;
import com.cityplatform.collector.source.WaterLevelCollectionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据采集服务REST接口
 */
@RestController
@RequestMapping("/api/collector")
@Slf4j
public class CollectorController {

    @Resource
    private DataSimulatorService dataSimulatorService;

    @Resource
    private EventRecordRepository eventRecordRepository;

    @Resource
    private EnergyGridCollectionService energyGridCollectionService;

    @Resource
    private FloodCollectionService floodCollectionService;

    @Resource
    private WaterLevelCollectionService waterLevelCollectionService;

    /**
     * 获取服务状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "city-collector");
        status.put("status", "running");
        status.put("timestamp", OffsetDateTime.now().toString());
        status.put("city", "南京");
        status.put("location", Map.of("lat", 32.06, "lng", 118.79));
        status.put("scheduler", "active (fixedRate=10000ms)");
        return ResponseEntity.ok(status);
    }

    /**
     * 手动触发一轮数据采集
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerCollection() {
        log.info("手动触发数据采集请求");
        try {
            dataSimulatorService.collectAndPublish();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "数据采集已触发");
            result.put("timestamp", OffsetDateTime.now().toString());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("手动触发数据采集失败: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "数据采集触发失败: " + e.getMessage());
            result.put("timestamp", OffsetDateTime.now().toString());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 获取最近的事件记录
     */
    @GetMapping("/events/recent")
    public ResponseEntity<List<EventRecord>> getRecentEvents(
            @RequestParam(required = false, defaultValue = "100") int limit) {
        log.info("查询最近 {} 条事件记录", limit);
        List<EventRecord> events = eventRecordRepository.findTop100ByOrderByTsDesc();
        // 限制返回数量
        if (events.size() > limit) {
            events = events.subList(0, limit);
        }
        return ResponseEntity.ok(events);
    }

    /**
     * 电网数据源状态（国内 OWID 中国年度数据，免费公开）
     */
    @GetMapping("/energy/grid/status")
    public ResponseEntity<Map<String, Object>> getGridSourceStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", OffsetDateTime.now().toString());
        result.put("sources", energyGridCollectionService.getStatuses());
        return ResponseEntity.ok(result);
    }

    /**
     * 手动触发一轮电网数据采集
     */
    @PostMapping("/energy/grid/fetch")
    public ResponseEntity<Map<String, Object>> triggerGridFetch() {
        log.info("手动触发电网数据采集请求");
        try {
            int count = energyGridCollectionService.triggerCollect();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("eventCount", count);
            result.put("message", "电网数据采集完成");
            result.put("timestamp", OffsetDateTime.now().toString());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.warn("手动触发电网数据采集失败: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "电网数据采集失败: " + e.getMessage());
            result.put("timestamp", OffsetDateTime.now().toString());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 洪水预报与站点水位采集状态（真实数据源：Open-Meteo GloFAS + 南京站点）
     */
    @GetMapping("/water/status")
    public ResponseEntity<Map<String, Object>> getWaterCollectionStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", OffsetDateTime.now().toString());
        result.put("floodSources", floodCollectionService.getStatuses());
        result.put("waterRuntime", waterLevelCollectionService.getRuntimeStatus());
        result.put("stations", waterLevelCollectionService.getStationStatuses());
        return ResponseEntity.ok(result);
    }

    /**
     * 手动触发一轮洪水预报 + 站点水位采集（先采径流量再推算水位）
     */
    @PostMapping("/water/fetch")
    public ResponseEntity<Map<String, Object>> triggerWaterFetch() {
        log.info("手动触发洪水/水位数据采集请求");
        Map<String, Object> result = new HashMap<>();
        try {
            int floodCount = floodCollectionService.triggerCollect();
            result.put("floodEventCount", floodCount);
        } catch (Exception e) {
            log.warn("洪水预报采集失败: {}", e.getMessage());
            result.put("floodEventCount", 0);
            result.put("floodError", e.getMessage());
        }
        try {
            int waterCount = waterLevelCollectionService.triggerCollect();
            result.put("waterEventCount", waterCount);
            result.put("success", true);
            result.put("message", "洪水/水位数据采集完成");
        } catch (Exception e) {
            log.warn("站点水位采集失败: {}", e.getMessage());
            result.put("waterEventCount", 0);
            result.put("success", false);
            result.put("message", "站点水位采集失败: " + e.getMessage());
        }
        result.put("timestamp", OffsetDateTime.now().toString());
        return ResponseEntity.ok(result);
    }
}
