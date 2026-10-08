package com.cityplatform.traffic.optimizer;

import com.cityplatform.traffic.model.SignalPhase;
import com.cityplatform.traffic.model.TrafficFlowData;
import com.cityplatform.traffic.simulator.TrafficSimulator;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 强化学习式优化器（仿真在环的策略迭代）
 * 以仿真器为交互环境、仿真总等待时间为奖励信号：
 * 每轮在当前策略的邻域扰动生成候选动作，依据仿真反馈更新策略（ε-贪心保留探索概率），
 * 探索幅度随轮次衰减（先大范围试探后精细收敛），输出获得最优奖励的配时方案
 */
@Slf4j
@Component
public class ReinforcementLearningOptimizer {

    /** 策略迭代轮数 */
    private static final int ROUNDS = 40;
    /** 每轮候选动作数 */
    private static final int CANDIDATES_PER_ROUND = 3;
    private static final int SIMULATION_MINUTES = 60;
    private static final int YELLOW_TIME = 3;
    private static final int MIN_GREEN_TIME = 10;
    private static final int MAX_GREEN_TIME = 120;
    /** 信号周期上限（与 GA 约束一致） */
    private static final int MAX_CYCLE = 180;

    @Resource
    private TrafficSimulator simulator;

    private final Random random = new Random();

    /**
     * 执行仿真在环的策略迭代优化
     */
    public List<SignalPhase> optimize(List<SignalPhase> currentPhases, List<TrafficFlowData> flowData) {
        if (currentPhases == null || currentPhases.isEmpty()) {
            log.warn("当前相位配置为空，无法进行强化学习优化");
            return currentPhases;
        }
        log.info("开始强化学习优化(仿真在环策略迭代): {}个相位, {}轮", currentPhases.size(), ROUNDS);

        List<SignalPhase> policy = copyOf(currentPhases);
        double policyReward = evaluate(policy, flowData);
        List<SignalPhase> best = copyOf(policy);
        double bestReward = policyReward;

        double epsilon = 0.35;
        for (int round = 0; round < ROUNDS; round++) {
            // 探索幅度随轮次衰减：早期大范围试探，后期精细收敛
            double sigma = 15.0 * (1.0 - (double) round / ROUNDS) + 3.0;
            for (int c = 0; c < CANDIDATES_PER_ROUND; c++) {
                List<SignalPhase> candidate = perturb(policy, sigma);
                double reward = evaluate(candidate, flowData);
                if (reward < policyReward) {
                    policy = candidate;
                    policyReward = reward;
                } else if (random.nextDouble() < epsilon) {
                    // ε-贪心探索：小概率接受次优动作，避免陷入局部最优
                    policy = candidate;
                    policyReward = reward;
                }
                if (reward < bestReward) {
                    best = candidate;
                    bestReward = reward;
                }
            }
            epsilon = Math.max(0.05, epsilon * 0.95);
        }

        log.info("强化学习优化完成: 最优总等待={}s", String.format("%.2f", bestReward));
        return best;
    }

    /** 以仿真总等待时间作为奖励评估（越小越好） */
    private double evaluate(List<SignalPhase> phases, List<TrafficFlowData> flowData) {
        return simulator.simulateWithFlowData(phases, flowData, SIMULATION_MINUTES).getTotalWaitTime();
    }

    /** 邻域扰动：对绿灯时长叠加高斯噪声，并满足最小绿、总周期约束 */
    private List<SignalPhase> perturb(List<SignalPhase> policy, double sigma) {
        int phaseCount = policy.size();
        int maxGreenSum = MAX_CYCLE - YELLOW_TIME * phaseCount;

        int[] greens = new int[phaseCount];
        int greenSum = 0;
        for (int i = 0; i < phaseCount; i++) {
            int green = (int) Math.round(policy.get(i).getGreenTime() + random.nextGaussian() * sigma);
            greens[i] = Math.max(MIN_GREEN_TIME, Math.min(MAX_GREEN_TIME, green));
            greenSum += greens[i];
        }
        // 总绿灯超限时逐个缩减
        while (greenSum > maxGreenSum) {
            for (int i = 0; i < phaseCount && greenSum > maxGreenSum; i++) {
                if (greens[i] > MIN_GREEN_TIME) {
                    greens[i]--;
                    greenSum--;
                }
            }
        }

        List<SignalPhase> result = new ArrayList<>(phaseCount);
        for (int i = 0; i < phaseCount; i++) {
            SignalPhase original = policy.get(i);
            int redTime = Math.max(0, greenSum + YELLOW_TIME * phaseCount - greens[i] - YELLOW_TIME);
            result.add(new SignalPhase(original.getPhaseId(), original.getDirection(),
                    greens[i], YELLOW_TIME, redTime));
        }
        return result;
    }

    private List<SignalPhase> copyOf(List<SignalPhase> phases) {
        List<SignalPhase> copy = new ArrayList<>(phases.size());
        for (SignalPhase phase : phases) {
            copy.add(new SignalPhase(phase.getPhaseId(), phase.getDirection(),
                    phase.getGreenTime(), phase.getYellowTime(), phase.getRedTime()));
        }
        return copy;
    }
}
