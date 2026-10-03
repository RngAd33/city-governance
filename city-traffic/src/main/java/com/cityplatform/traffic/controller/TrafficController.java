package com.cityplatform.traffic.controller;

import com.cityplatform.traffic.model.Intersection;
import com.cityplatform.traffic.model.SignalPhase;
import com.cityplatform.traffic.model.TrafficFlowData;
import com.cityplatform.traffic.optimizer.GeneticAlgorithmOptimizer;
import com.cityplatform.traffic.repository.OptimizationRecordRepository;
import com.cityplatform.traffic.simulator.TrafficSimulator;
import com.cityplatform.traffic.entity.OptimizationRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 交通信号优化REST控制器
 */
@RestController
@RequestMapping("/api/traffic")
public class TrafficController {

    private static final Logger log = LoggerFactory.getLogger(TrafficController.class);

    @Autowired
    private GeneticAlgorithmOptimizer optimizer;

    @Autowired
    private TrafficSimulator simulator;

    @Autowired
    private OptimizationRecordRepository optimizationRecordRepository;

    // 存储最近的模拟运行记录
    private final List<TrafficSimulator.SimulationResult> recentSimulations = new ArrayList<>();

    /**
     * 获取交通服务状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("service", "city-traffic");
        status.put("status", "running");
        status.put("port", 8082);
        status.put("algorithm", "Genetic Algorithm");
        status.put("populationSize", 50);
        status.put("maxGenerations", 100);
        status.put("mutationRate", 0.1);
        status.put("crossoverRate", 0.8);
        status.put("timestamp", new Date());
        return ResponseEntity.ok(status);
    }

    /**
     * 执行信号优化
     * 接收路口数据，返回优化结果
     */
    @PostMapping("/optimize")
    public ResponseEntity<Map<String, Object>> optimize(@RequestBody Intersection intersection) {
        log.info("收到优化请求: 路口={}", intersection.getIntersectionId());

        try {
            // 构建模拟流量数据
            List<TrafficFlowData> flowData = buildDefaultFlowData(intersection);

            // 执行优化
            List<SignalPhase> optimizedPhases = optimizer.optimize(
                    intersection.getPhases(), flowData);

            // 模拟优化前后效果
            TrafficSimulator.SimulationResult beforeResult = simulator.simulate(
                    intersection.getPhases(), 60);
            TrafficSimulator.SimulationResult afterResult = simulator.simulate(
                    optimizedPhases, 60);

            // 计算改善百分比
            double improvementPercent = 0.0;
            if (beforeResult.getTotalWaitTime() > 0) {
                improvementPercent = (beforeResult.getTotalWaitTime() - afterResult.getTotalWaitTime())
                        / beforeResult.getTotalWaitTime() * 100.0;
            }

            // 保存优化记录
            saveOptimizationRecord(intersection, intersection.getPhases(),
                    optimizedPhases, improvementPercent);

            // 构建响应
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("intersectionId", intersection.getIntersectionId());
            result.put("optimizedPhases", optimizedPhases);
            result.put("beforeSimulation", beforeResult);
            result.put("afterSimulation", afterResult);
            result.put("improvementPercent", Math.round(improvementPercent * 100.0) / 100.0);
            result.put("timestamp", new Date());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("优化请求处理异常: {}", e.getMessage(), e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "优化失败");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 获取最近的模拟运行记录
     */
    @GetMapping("/simulation/runs")
    public ResponseEntity<List<TrafficSimulator.SimulationResult>> getRecentSimulations() {
        return ResponseEntity.ok(recentSimulations);
    }

    /**
     * 运行前后对比模拟
     */
    @PostMapping("/simulation/compare")
    public ResponseEntity<Map<String, Object>> compareSimulation(
            @RequestBody Map<String, Object> request) {

        log.info("收到模拟对比请求");

        try {
            // 解析请求中的相位配置
            List<Map<String, Object>> beforePhasesData =
                    (List<Map<String, Object>>) request.get("beforePhases");
            List<Map<String, Object>> afterPhasesData =
                    (List<Map<String, Object>>) request.get("afterPhases");
            int durationMinutes = (int) request.getOrDefault("durationMinutes", 60);

            List<SignalPhase> beforePhases = parsePhases(beforePhasesData);
            List<SignalPhase> afterPhases = parsePhases(afterPhasesData);

            // 执行模拟
            TrafficSimulator.SimulationResult beforeResult = simulator.simulate(
                    beforePhases, durationMinutes);
            TrafficSimulator.SimulationResult afterResult = simulator.simulate(
                    afterPhases, durationMinutes);

            // 保存模拟记录
            recentSimulations.add(beforeResult);
            recentSimulations.add(afterResult);
            if (recentSimulations.size() > 100) {
                recentSimulations.remove(0);
            }

            // 计算对比指标
            double waitTimeReduction = beforeResult.getTotalWaitTime() - afterResult.getTotalWaitTime();
            double speedImprovement = afterResult.getAvgSpeed() - beforeResult.getAvgSpeed();
            double queueReduction = beforeResult.getAvgQueueLength() - afterResult.getAvgQueueLength();

            Map<String, Object> comparison = new LinkedHashMap<>();
            comparison.put("before", beforeResult);
            comparison.put("after", afterResult);
            comparison.put("waitTimeReduction", Math.round(waitTimeReduction * 100.0) / 100.0);
            comparison.put("speedImprovement", Math.round(speedImprovement * 100.0) / 100.0);
            comparison.put("queueReduction", Math.round(queueReduction * 100.0) / 100.0);
            comparison.put("durationMinutes", durationMinutes);
            comparison.put("timestamp", new Date());

            return ResponseEntity.ok(comparison);

        } catch (Exception e) {
            log.error("模拟对比异常: {}", e.getMessage(), e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "模拟对比失败");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 构建默认流量数据
     */
    private List<TrafficFlowData> buildDefaultFlowData(Intersection intersection) {
        List<TrafficFlowData> flowData = new ArrayList<>();
        for (SignalPhase phase : intersection.getPhases()) {
            TrafficFlowData data = new TrafficFlowData();
            data.setSegmentId(intersection.getIntersectionId() + "-" + phase.getDirection());
            data.setIntersectionId(intersection.getIntersectionId());
            data.setDirection(phase.getDirection());
            data.setFlow(800.0); // 默认流量800辆/小时
            data.setAvgSpeed(35.0);
            data.setQueueLength(50.0);
            flowData.add(data);
        }
        return flowData;
    }

    /**
     * 解析相位配置
     */
    private List<SignalPhase> parsePhases(List<Map<String, Object>> phasesData) {
        List<SignalPhase> phases = new ArrayList<>();
        if (phasesData == null) {
            return phases;
        }
        for (Map<String, Object> data : phasesData) {
            SignalPhase phase = new SignalPhase();
            phase.setPhaseId((String) data.get("phaseId"));
            phase.setDirection((String) data.get("direction"));
            phase.setGreenTime((int) data.get("greenTime"));
            phase.setYellowTime((int) data.getOrDefault("yellowTime", 3));
            phase.setRedTime((int) data.get("redTime"));
            phases.add(phase);
        }
        return phases;
    }

    /**
     * 保存优化记录
     */
    private void saveOptimizationRecord(Intersection intersection,
                                        List<SignalPhase> beforePhases,
                                        List<SignalPhase> afterPhases,
                                        double improvementPercent) {
        try {
            OptimizationRecord record = new OptimizationRecord();
            record.setIntersectionId(intersection.getIntersectionId());
            record.setBeforeConfig(beforePhases.toString());
            record.setAfterConfig(afterPhases.toString());
            record.setImprovementPercent(Math.round(improvementPercent * 100.0) / 100.0);
            optimizationRecordRepository.save(record);
        } catch (Exception e) {
            log.warn("保存优化记录失败: {}", e.getMessage());
        }
    }
}
