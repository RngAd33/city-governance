package com.cityplatform.traffic.controller;

import com.cityplatform.traffic.model.Intersection;
import com.cityplatform.traffic.model.SignalPhase;
import com.cityplatform.traffic.model.TrafficFlowData;
import com.cityplatform.traffic.optimizer.GeneticAlgorithmOptimizer;
import com.cityplatform.traffic.repository.OptimizationRecordRepository;
import com.cityplatform.traffic.service.SignalOptimizationService;
import com.cityplatform.traffic.service.TrafficDataWindow;
import com.cityplatform.traffic.simulator.TrafficSimulator;
import com.cityplatform.traffic.entity.OptimizationRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * 交通信号优化REST控制器
 */
@RestController
@RequestMapping("/api/traffic")
@Slf4j
public class TrafficController {

    @Resource
    private GeneticAlgorithmOptimizer optimizer;

    @Resource
    private TrafficSimulator simulator;

    @Resource
    private TrafficDataWindow dataWindow;

    @Resource
    private SignalOptimizationService signalOptimizationService;

    @Resource
    private OptimizationRecordRepository optimizationRecordRepository;

    // 存储最近的模拟运行记录
    private final List<TrafficSimulator.SimulationResult> recentSimulations = new ArrayList<>();

    /** 路口基础信息（INT-001 为自动优化链路监控的主路口） */
    private static final String[][] INTERSECTIONS = {
            {"INT-001", "中山北路-中央路交叉口", "鼓楼区"},
            {"INT-002", "北京东路-太平北路交叉口", "玄武区"},
            {"INT-003", "汉中路-上海路交叉口", "秦淮区"}};

    /** 默认两相位配时（NS/EW 各 30s） */
    private List<SignalPhase> defaultPhases() {
        return List.of(new SignalPhase("P1", "NS", 30, 3, 30),
                new SignalPhase("P2", "EW", 30, 3, 30));
    }

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
     * 实时路口列表（前端 IntersectionSelector 数据源）
     * 流量取自采集窗口平均值并按路口系数展开，供选择与仿真使用
     */
    @GetMapping("/segments")
    public ResponseEntity<List<Map<String, Object>>> getSegments() {
        double baseFlow = dataWindow.avgFlow();
        double[] flowFactors = {1.0, 0.7, 1.35};
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < INTERSECTIONS.length; i++) {
            String[] info = INTERSECTIONS[i];
            double flow = Math.round(baseFlow * flowFactors[i]);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("intersectionId", info[0]);
            item.put("segmentId", info[0] + "-NS");
            item.put("name", info[1]);
            item.put("area", info[2]);
            item.put("direction", "NS");
            item.put("flow", flow);
            item.put("avgSpeed", dataWindow.avgSpeed());
            item.put("queueLength", dataWindow.avgQueue());
            item.put("congestionLevel", flow < 900 ? "smooth" : flow < 1400 ? "moderate" : "congested");
            item.put("phases", defaultPhases());
            list.add(item);
        }
        return ResponseEntity.ok(list);
    }

    /**
     * 运行仿真（前端"开始仿真"入口）
     * 接收当前配时方案与所选优化策略，执行对应算法的配时优化与优化前后仿真对比；
     * 响应契约：{intersectionId, strategy, optimizedPhases, beforeSimulation, afterSimulation, improvementPercent}
     */
    @PostMapping("/simulation/run")
    public ResponseEntity<Map<String, Object>> runSimulation(@RequestBody Map<String, Object> request) {
        String intersectionId = String.valueOf(request.getOrDefault("intersectionId", "INT-001"));
        String strategy = String.valueOf(request.getOrDefault("strategy", "genetic"));
        List<SignalPhase> phases = parsePhases((List<Map<String, Object>>) request.get("phases"));
        if (phases.isEmpty()) {
            phases = defaultPhases();
        }
        log.info("收到仿真请求: 路口={}, 相位数={}, 策略={}", intersectionId, phases.size(), strategy);

        try {
            SignalOptimizationService.Outcome outcome =
                    signalOptimizationService.optimizeAndPublish(intersectionId, "南京市", phases, strategy);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("intersectionId", intersectionId);
            result.put("strategy", outcome.strategy());
            result.put("optimizedPhases", outcome.optimizedPhases());
            result.put("beforeSimulation", outcome.before());
            result.put("afterSimulation", outcome.after());
            result.put("improvementPercent", outcome.improvementPercent());
            result.put("timestamp", OffsetDateTime.now());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("仿真请求处理异常: {}", e.getMessage(), e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "仿真失败");
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
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
    public ResponseEntity<Map<String, Object>> compareSimulation(@RequestBody Map<String, Object> request) {
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
     * 解析相位配置（兼容数值类型差异，缺省字段使用标准值）
     */
    private List<SignalPhase> parsePhases(List<Map<String, Object>> phasesData) {
        List<SignalPhase> phases = new ArrayList<>();
        if (phasesData == null) {
            return phases;
        }
        int index = 1;
        for (Map<String, Object> data : phasesData) {
            SignalPhase phase = new SignalPhase();
            phase.setPhaseId(data.get("phaseId") != null ? String.valueOf(data.get("phaseId")) : "P" + index);
            phase.setDirection(data.get("direction") != null ? String.valueOf(data.get("direction")) : "NS");
            phase.setGreenTime(toInt(data.get("greenTime"), 30));
            phase.setYellowTime(toInt(data.get("yellowTime"), 3));
            phase.setRedTime(toInt(data.get("redTime"), 30));
            phases.add(phase);
            index++;
        }
        return phases;
    }

    private int toInt(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
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
