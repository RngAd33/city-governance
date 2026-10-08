package com.cityplatform.disaster.controller;

import com.cityplatform.disaster.model.FloodForecast;
import com.cityplatform.disaster.model.FloodScenario;
import com.cityplatform.disaster.model.WaterStation;
import com.cityplatform.disaster.repository.FloodForecastRepository;
import com.cityplatform.disaster.repository.FloodScenarioRepository;
import com.cityplatform.disaster.repository.WaterStationRepository;
import com.cityplatform.disaster.risk.DisasterSimulationService;
import com.cityplatform.disaster.risk.FloodRiskAssessor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 灾害模拟REST控制器
 * 提供灾害态势、灾害模拟与风险评估API（数据均来自真实采集，非随机模拟）：
 * - GET  /api/disaster/current               当前灾害态势（站点水位/长江径流量/降水）
 * - POST /api/disaster/simulate              以真实站点状态为起点的灾害情景模拟
 * - GET  /api/disaster/scenarios             历史灾害模拟场景列表
 * - GET  /api/disaster/stations/{id}/risk    单站点风险评估（含真实长江径流量调节）
 */
@RestController
@RequestMapping("/api/disaster")
@Slf4j
public class DisasterController {

    @Resource
    private DisasterSimulationService disasterSimulationService;

    @Resource
    private WaterStationRepository waterStationRepository;

    @Resource
    private FloodScenarioRepository floodScenarioRepository;

    @Resource
    private FloodForecastRepository floodForecastRepository;

    @Resource
    private FloodRiskAssessor floodRiskAssessor;

    /**
     * 当前灾害态势（真实数据）
     */
    @GetMapping("/current")
    public ResponseEntity<DisasterSimulationService.CurrentDisasterStatus> getCurrentStatus() {
        return ResponseEntity.ok(disasterSimulationService.getCurrentStatus());
    }

    /**
     * 灾害情景模拟：基于站点真实当前水位，输入降雨量/时长推演灾害影响
     * 请求体可选字段：stationId（默认选容量占比最高站点）、rainfallMm（默认50）、durationHours（默认24）
     */
    @PostMapping("/simulate")
    public ResponseEntity<Object> simulate(@RequestBody(required = false) Map<String, Object> request) {
        String stationId = request != null && request.get("stationId") != null
                ? request.get("stationId").toString() : null;
        Double rainfallMm = request != null ? asDouble(request.get("rainfallMm")) : null;
        Integer durationHours = request != null ? asInt(request.get("durationHours")) : null;

        log.info("灾害模拟请求: stationId={}, rainfallMm={}, durationHours={}",
                stationId, rainfallMm, durationHours);
        try {
            DisasterSimulationService.SimulationResult result =
                    disasterSimulationService.simulate(stationId, rainfallMm, durationHours);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("灾害模拟请求参数无效: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 历史灾害模拟场景列表（按入库顺序）
     */
    @GetMapping("/scenarios")
    public ResponseEntity<List<FloodScenario>> getAllScenarios() {
        return ResponseEntity.ok(floodScenarioRepository.findAll());
    }

    /**
     * 单站点风险评估（含真实长江径流量调节）
     */
    @GetMapping("/stations/{id}/risk")
    public ResponseEntity<FloodRiskAssessor.RiskAssessment> getRiskAssessment(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "0") double rainfallForecast,
            @RequestParam(required = false, defaultValue = "24") int forecastHours) {

        WaterStation station = waterStationRepository.findByStationId(id).orElse(null);
        if (station == null) {
            return ResponseEntity.notFound().build();
        }

        // 读取最新长江径流量预报（GloFAS），驱动径流调节项
        Double riverDischarge = floodForecastRepository
                .findTopByForecastDateLessThanEqualOrderByForecastDateDesc(LocalDate.now())
                .map(FloodForecast::getDischarge)
                .orElse(null);

        FloodRiskAssessor.RiskAssessment assessment = floodRiskAssessor.assessRisk(
                station, station.getCurrentLevel(), rainfallForecast, forecastHours, riverDischarge);

        return ResponseEntity.ok(assessment);
    }

    private Double asDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private Integer asInt(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }
}
