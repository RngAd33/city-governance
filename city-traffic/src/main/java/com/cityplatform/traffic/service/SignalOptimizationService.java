package com.cityplatform.traffic.service;

import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.event.EventDomain;
import com.cityplatform.common.event.EventSource;
import com.cityplatform.common.mq.EventPublisher;
import com.cityplatform.common.util.JsonUtil;
import com.cityplatform.traffic.model.SignalPhase;
import com.cityplatform.traffic.model.TrafficFlowData;
import com.cityplatform.traffic.optimizer.GeneticAlgorithmOptimizer;
import com.cityplatform.traffic.optimizer.ReinforcementLearningOptimizer;
import com.cityplatform.traffic.optimizer.RuleBasedOptimizer;
import com.cityplatform.traffic.simulator.TrafficSimulator;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 信号优化核心服务
 * 流程：实时流量窗口 → 按策略执行配时优化（遗传算法/规则优化/强化学习） → 优化前后仿真对比 → 发布信号优化事件（traffic.signal.optimization）
 * 由前端"开始仿真"手动触发，优化结果经信号队列统一入库
 */
@Slf4j
@Service
public class SignalOptimizationService {

    /** 仿真时长（分钟） */
    private static final int SIMULATION_MINUTES = 60;

    @Resource
    private GeneticAlgorithmOptimizer geneticOptimizer;

    @Resource
    private RuleBasedOptimizer ruleOptimizer;

    @Resource
    private ReinforcementLearningOptimizer reinforcementOptimizer;

    @Resource
    private TrafficSimulator simulator;

    @Resource
    private TrafficDataWindow dataWindow;

    @Resource
    private EventPublisher eventPublisher;

    /** 优化结果（供接口响应与日志使用） */
    public record Outcome(String intersectionId,
                          String strategy,
                          List<SignalPhase> optimizedPhases,
                          TrafficSimulator.SimulationResult before,
                          TrafficSimulator.SimulationResult after,
                          double improvementPercent) {}

    /**
     * 执行一轮"优化 + 仿真对比 + 事件发布"
     *
     * @param intersectionId 路口编号
     * @param city           城市
     * @param currentPhases  当前配时方案
     * @param strategy       优化策略（genetic 遗传算法 / rule 规则优化 / reinforcement 强化学习）
     */
    public Outcome optimizeAndPublish(String intersectionId, String city,
                                      List<SignalPhase> currentPhases, String strategy) {
        String normalized = normalizeStrategy(strategy);
        List<TrafficFlowData> flowData = dataWindow.buildFlowData(intersectionId);

        long startAt = System.currentTimeMillis();
        List<SignalPhase> optimizedPhases = applyStrategy(normalized, currentPhases, flowData);
        TrafficSimulator.SimulationResult before =
                simulator.simulateWithFlowData(currentPhases, flowData, SIMULATION_MINUTES);
        TrafficSimulator.SimulationResult after =
                simulator.simulateWithFlowData(optimizedPhases, flowData, SIMULATION_MINUTES);
        long costMs = System.currentTimeMillis() - startAt;

        double improvement = before.getTotalWaitTime() > 0
                ? (before.getTotalWaitTime() - after.getTotalWaitTime()) / before.getTotalWaitTime() * 100.0
                : 0.0;

        Outcome outcome = new Outcome(intersectionId, normalized, optimizedPhases, before, after, round2(improvement));
        publishSignalEvent(city, outcome, costMs);
        return outcome;
    }

    /** 按策略分派优化算法 */
    private List<SignalPhase> applyStrategy(String strategy, List<SignalPhase> currentPhases,
                                            List<TrafficFlowData> flowData) {
        return switch (strategy) {
            case "rule" -> ruleOptimizer.optimize(currentPhases, flowData);
            case "reinforcement" -> reinforcementOptimizer.optimize(currentPhases, flowData);
            default -> geneticOptimizer.optimize(currentPhases, flowData);
        };
    }

    /** 规范化策略名：非法值回退为遗传算法 */
    private String normalizeStrategy(String strategy) {
        String value = strategy == null ? "" : strategy.trim().toLowerCase();
        return switch (value) {
            case "rule", "reinforcement" -> value;
            default -> "genetic";
        };
    }

    /** 发布信号优化结果事件（由 SignalOptimizationConsumer 统一入库） */
    private void publishSignalEvent(String city, Outcome outcome, long costMs) {
        try {
            Map<String, Object> raw = new LinkedHashMap<>();
            raw.put("intersectionId", outcome.intersectionId());
            raw.put("strategy", outcome.strategy());
            raw.put("improvementPercent", outcome.improvementPercent());
            raw.put("optimizedPhases", outcome.optimizedPhases());
            raw.put("before", outcome.before());
            raw.put("after", outcome.after());

            CityEvent event = new CityEvent();
            event.setDomain(EventDomain.TRAFFIC);
            event.setCity(city);
            event.setMetric("signal_optimization");
            event.setValue(outcome.improvementPercent());
            event.setUnit("percent");
            event.setSource(EventSource.SIMULATOR);
            event.setTs(OffsetDateTime.now());
            event.setRawData(JsonUtil.toJson(raw));

            eventPublisher.publishSignalOptimization(event);
            log.info("信号优化完成并发布: 路口={}, 策略={}, 改善={}%, 计算耗时={}ms",
                    outcome.intersectionId(), outcome.strategy(), outcome.improvementPercent(), costMs);
        } catch (Exception e) {
            log.error("发布信号优化结果失败: {}", e.getMessage(), e);
        }
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
