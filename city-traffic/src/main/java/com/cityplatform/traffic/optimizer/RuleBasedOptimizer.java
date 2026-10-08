package com.cityplatform.traffic.optimizer;

import com.cityplatform.traffic.model.SignalPhase;
import com.cityplatform.traffic.model.TrafficFlowData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 规则优化器（Webster 经典配时法）
 * 依据交通工程经验公式计算最优周期并按流量比分配绿灯时长：
 * 周期 C = (1.5L + 5) / (1 - Y)，L 为相位总损失时间，Y 为各相位流量比之和；
 * 绿灯 g_i = (C - L) * y_i / Y，满足最小绿灯与最大周期约束
 */
@Slf4j
@Component
public class RuleBasedOptimizer {

    /** 饱和流量（辆/小时），与 TrafficSimulator 保持一致 */
    private static final double SATURATION_FLOW = 1800.0;
    private static final int YELLOW_TIME = 3;
    private static final int MIN_GREEN_TIME = 10;
    private static final int MIN_CYCLE = 40;
    private static final int MAX_CYCLE = 180;
    /** 流量比之和上限（防止分母过小导致周期发散） */
    private static final double MAX_FLOW_RATIO_SUM = 0.92;
    /** 无流量数据时的兜底流量（与采集窗口兜底一致） */
    private static final double DEFAULT_FLOW = 800.0;

    /**
     * 执行 Webster 规则优化
     */
    public List<SignalPhase> optimize(List<SignalPhase> currentPhases, List<TrafficFlowData> flowData) {
        if (currentPhases == null || currentPhases.isEmpty()) {
            log.warn("当前相位配置为空，无法进行规则优化");
            return currentPhases;
        }
        int phaseCount = currentPhases.size();
        log.info("开始规则优化(Webster法): {}个相位", phaseCount);

        // 各相位流量比 y_i = q_i / 饱和流量
        double[] ratios = new double[phaseCount];
        double ratioSum = 0.0;
        for (int i = 0; i < phaseCount; i++) {
            double flow = directionFlow(flowData, currentPhases.get(i).getDirection());
            ratios[i] = flow / SATURATION_FLOW;
            ratioSum += ratios[i];
        }
        ratioSum = Math.min(MAX_FLOW_RATIO_SUM, Math.max(0.1, ratioSum));

        // Webster 最优周期
        double lostTime = YELLOW_TIME * (double) phaseCount;
        int cycle = (int) Math.round((1.5 * lostTime + 5.0) / (1.0 - ratioSum));
        cycle = Math.max(MIN_CYCLE, Math.min(MAX_CYCLE, cycle));

        // 按流量比分配有效绿灯时长
        double effectiveGreen = cycle - lostTime;
        int[] greens = new int[phaseCount];
        int greenSum = 0;
        for (int i = 0; i < phaseCount; i++) {
            greens[i] = Math.max(MIN_GREEN_TIME, (int) Math.round(effectiveGreen * ratios[i] / ratioSum));
            greenSum += greens[i];
        }

        List<SignalPhase> optimized = new ArrayList<>(phaseCount);
        for (int i = 0; i < phaseCount; i++) {
            SignalPhase original = currentPhases.get(i);
            int redTime = Math.max(0, greenSum + YELLOW_TIME * phaseCount - greens[i] - YELLOW_TIME);
            optimized.add(new SignalPhase(original.getPhaseId(), original.getDirection(),
                    greens[i], YELLOW_TIME, redTime));
        }

        log.info("规则优化完成: 周期={}s, 绿灯={}s", greenSum + YELLOW_TIME * phaseCount, greenSum);
        return optimized;
    }

    /** 获取指定方向的平均流量 */
    private double directionFlow(List<TrafficFlowData> flowData, String direction) {
        if (flowData == null || flowData.isEmpty()) {
            return DEFAULT_FLOW;
        }
        double avg = flowData.stream()
                .filter(f -> direction.equals(f.getDirection()))
                .mapToDouble(TrafficFlowData::getFlow)
                .average()
                .orElse(DEFAULT_FLOW);
        return avg > 0 ? avg : DEFAULT_FLOW;
    }
}
