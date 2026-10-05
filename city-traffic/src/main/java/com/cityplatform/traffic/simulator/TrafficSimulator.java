package com.cityplatform.traffic.simulator;

import com.cityplatform.traffic.model.SignalPhase;
import com.cityplatform.traffic.model.TrafficFlowData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 交通流模拟器
 * 基于M/D/1排队模型模拟路口交通流
 *
 * M/D/1模型: 到达过程为泊松过程(M)，服务时间为确定值(D)，单服务台(1)
 * 到达率 lambda = flow / 3600
 * 服务率 mu = greenRatio * saturationFlow / 3600
 */
@Component
public class TrafficSimulator {

    private static final Logger log = LoggerFactory.getLogger(TrafficSimulator.class);

    // 饱和流量(辆/小时/车道)，默认值1800
    private static final double DEFAULT_SATURATION_FLOW = 1800.0;
    // 过饱和排队基座：M/D/1 在 rho=0.999 处的稳态队列长度，使过饱和段与稳态段平滑衔接
    private static final double OVERFLOW_QUEUE_BASE = 499.0;
    // 自由流速度(km/h)
    private static final double FREE_FLOW_SPEED = 50.0;
    // 车辆平均长度(m)
    private static final double VEHICLE_LENGTH = 5.0;
    // 最小车头间距(m)
    private static final double MIN_HEADWAY = 2.0;

    /**
     * 模拟交通流
     *
     * @param phases        信号相位配置
     * @param durationMinutes 模拟时长(分钟)
     * @return 模拟结果
     */
    public SimulationResult simulate(List<SignalPhase> phases, int durationMinutes) {
        return simulate(phases, durationMinutes, DEFAULT_SATURATION_FLOW);
    }

    /**
     * 模拟交通流(指定饱和流量)
     *
     * @param phases          信号相位配置
     * @param durationMinutes 模拟时长(分钟)
     * @param saturationFlow  饱和流量(辆/小时)
     * @return 模拟结果
     */
    public SimulationResult simulate(List<SignalPhase> phases, int durationMinutes, double saturationFlow) {
        if (phases == null || phases.isEmpty()) {
            return new SimulationResult(0, 0, 0, 0, 0);
        }

        // 计算周期长度（真实信号周期 = 各相位绿灯+黄灯之和；红灯为其他相位占用时间，不可重复计入）
        int cycleLength = phases.stream()
                .mapToInt(p -> p.getGreenTime() + p.getYellowTime())
                .sum();
        if (cycleLength == 0) {
            return new SimulationResult(0, 0, 0, 0, 0);
        }

        double durationSeconds = durationMinutes * 60.0;
        int cyclesInDuration = (int) Math.ceil(durationSeconds / cycleLength);

        double totalWaitTime = 0.0;
        double totalQueueLength = 0.0;
        double totalThroughput = 0.0;
        double totalSpeed = 0.0;
        int totalStopCount = 0;
        int measurementCount = 0;

        // 对每个相位分别模拟
        for (SignalPhase phase : phases) {
            double greenRatio = (double) phase.getGreenTime() / cycleLength;

            // 假设每个相位的流量为饱和流量的一个比例(简化模型)
            // 实际应用中应从TrafficFlowData获取
            double avgFlow = saturationFlow * 0.6; // 假设平均流量为饱和流量的60%

            // M/D/1排队模型
            double lambda = avgFlow / 3600.0; // 到达率(辆/秒)
            double mu = greenRatio * saturationFlow / 3600.0; // 服务率(辆/秒)

            if (mu <= 0 || lambda <= 0) {
                continue;
            }

            // 交通强度
            double rho = lambda / mu;

            // 平均排队长度：非饱和用 M/D/1 稳态解，过饱和用溢出外推（与优化器适应度同口径）
            double avgQueue;
            if (rho < 1.0) {
                // Lq = rho^2 / (2 * (1 - rho))
                avgQueue = (rho * rho) / (2.0 * (1.0 - rho));
            } else {
                // 溢出车队线性累积：overflow = (lambda - mu) * T，均值取半；基座保证与稳态段衔接
                avgQueue = OVERFLOW_QUEUE_BASE + (lambda - mu) * durationSeconds / 2.0;
            }

            // 平均等待时间(秒)
            // Wq = Lq / lambda
            double avgWaitTime = avgQueue / lambda;

            // 一个周期内的总等待时间
            double cycleWaitTime = avgWaitTime * avgFlow * (durationSeconds / 3600.0);

            // 周期内通过车辆数
            double cycleThroughput = mu * phase.getGreenTime() * cyclesInDuration;

            // 平均速度: 基于排队密度估算
            double jamDensity = 1.0 / (VEHICLE_LENGTH + MIN_HEADWAY); // 辆/m
            double queueDensity = avgQueue / Math.max(1.0, avgQueue * VEHICLE_LENGTH);
            double speedRatio = Math.max(0.1, 1.0 - queueDensity / jamDensity);
            double avgSpeed = FREE_FLOW_SPEED * speedRatio;

            // 停车次数估算: 红灯期间到达的车辆需要停车
            double redTime = phase.getRedTime() + phase.getYellowTime();
            double stopProbability = Math.min(1.0, redTime / cycleLength);
            double stopCount = lambda * redTime * cyclesInDuration * stopProbability;

            totalWaitTime += cycleWaitTime;
            totalQueueLength += avgQueue;
            totalThroughput += cycleThroughput;
            totalSpeed += avgSpeed;
            totalStopCount += (int) Math.round(stopCount);
            measurementCount++;
        }

        // 计算平均值
        double avgQueueLength = measurementCount > 0 ? totalQueueLength / measurementCount : 0;
        double avgSpeed = measurementCount > 0 ? totalSpeed / measurementCount : 0;

        SimulationResult result = new SimulationResult(
                round2(totalWaitTime),
                round2(avgQueueLength),
                round2(totalThroughput),
                round2(avgSpeed),
                totalStopCount
        );

        log.info("模拟完成: 周期={}s, 总等待={}s, 平均排队={}m, 通过量={}辆, 平均速度={}km/h, 停车={}次",
                cycleLength, result.getTotalWaitTime(), result.getAvgQueueLength(),
                result.getThroughput(), result.getAvgSpeed(), result.getStopCount());

        return result;
    }

    /**
     * 模拟交通流(使用实际流量数据)
     *
     * @param phases        信号相位配置
     * @param flowDataList   交通流数据列表
     * @param durationMinutes 模拟时长(分钟)
     * @return 模拟结果
     */
    public SimulationResult simulateWithFlowData(List<SignalPhase> phases,
                                                  List<TrafficFlowData> flowDataList,
                                                  int durationMinutes) {
        if (phases == null || phases.isEmpty()) {
            return new SimulationResult(0, 0, 0, 0, 0);
        }

        // 真实信号周期 = 各相位绿灯+黄灯之和（红灯为其他相位占用时间，不可重复计入）
        int cycleLength = phases.stream()
                .mapToInt(p -> p.getGreenTime() + p.getYellowTime())
                .sum();
        if (cycleLength == 0) {
            return new SimulationResult(0, 0, 0, 0, 0);
        }

        double durationSeconds = durationMinutes * 60.0;
        int cyclesInDuration = (int) Math.ceil(durationSeconds / cycleLength);

        double totalWaitTime = 0.0;
        double totalQueueLength = 0.0;
        double totalThroughput = 0.0;
        double totalSpeed = 0.0;
        int totalStopCount = 0;
        int measurementCount = 0;

        for (SignalPhase phase : phases) {
            double greenRatio = (double) phase.getGreenTime() / cycleLength;

            // 从流量数据中获取该方向的实际流量
            double avgFlow = flowDataList.stream()
                    .filter(f -> f.getDirection().equals(phase.getDirection()))
                    .mapToDouble(TrafficFlowData::getFlow)
                    .average()
                    .orElse(DEFAULT_SATURATION_FLOW * 0.5);

            double lambda = avgFlow / 3600.0;
            double mu = greenRatio * DEFAULT_SATURATION_FLOW / 3600.0;

            if (mu <= 0 || lambda <= 0) {
                continue;
            }

            double rho = lambda / mu;

            // M/D/1模型：非饱和稳态解，过饱和溢出外推（与优化器适应度同口径，避免指标拉平）
            double avgQueue;
            if (rho < 1.0) {
                avgQueue = (rho * rho) / (2.0 * (1.0 - rho));
            } else {
                avgQueue = OVERFLOW_QUEUE_BASE + (lambda - mu) * durationSeconds / 2.0;
            }
            double avgWaitTime = avgQueue / lambda;
            double cycleWaitTime = avgWaitTime * avgFlow * (durationSeconds / 3600.0);
            double cycleThroughput = mu * phase.getGreenTime() * cyclesInDuration;

            double jamDensity = 1.0 / (VEHICLE_LENGTH + MIN_HEADWAY);
            double queueDensity = avgQueue / Math.max(1.0, avgQueue * VEHICLE_LENGTH);
            double speedRatio = Math.max(0.1, 1.0 - queueDensity / jamDensity);
            double avgSpeed = FREE_FLOW_SPEED * speedRatio;

            double redTime = phase.getRedTime() + phase.getYellowTime();
            double stopProbability = Math.min(1.0, redTime / cycleLength);
            double stopCount = lambda * redTime * cyclesInDuration * stopProbability;

            totalWaitTime += cycleWaitTime;
            totalQueueLength += avgQueue;
            totalThroughput += cycleThroughput;
            totalSpeed += avgSpeed;
            totalStopCount += (int) Math.round(stopCount);
            measurementCount++;
        }

        double avgQueueLength = measurementCount > 0 ? totalQueueLength / measurementCount : 0;
        double avgSpeed = measurementCount > 0 ? totalSpeed / measurementCount : 0;

        return new SimulationResult(
                round2(totalWaitTime),
                round2(avgQueueLength),
                round2(totalThroughput),
                round2(avgSpeed),
                totalStopCount
        );
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * 模拟结果
     */
    public static class SimulationResult {
        private double totalWaitTime;
        private double avgQueueLength;
        private double throughput;
        private double avgSpeed;
        private int stopCount;

        public SimulationResult(double totalWaitTime, double avgQueueLength,
                                double throughput, double avgSpeed, int stopCount) {
            this.totalWaitTime = totalWaitTime;
            this.avgQueueLength = avgQueueLength;
            this.throughput = throughput;
            this.avgSpeed = avgSpeed;
            this.stopCount = stopCount;
        }

        public double getTotalWaitTime() { return totalWaitTime; }
        public void setTotalWaitTime(double totalWaitTime) { this.totalWaitTime = totalWaitTime; }

        public double getAvgQueueLength() { return avgQueueLength; }
        public void setAvgQueueLength(double avgQueueLength) { this.avgQueueLength = avgQueueLength; }

        public double getThroughput() { return throughput; }
        public void setThroughput(double throughput) { this.throughput = throughput; }

        public double getAvgSpeed() { return avgSpeed; }
        public void setAvgSpeed(double avgSpeed) { this.avgSpeed = avgSpeed; }

        public int getStopCount() { return stopCount; }
        public void setStopCount(int stopCount) { this.stopCount = stopCount; }

        @Override
        public String toString() {
            return "SimulationResult{" +
                    "totalWaitTime=" + totalWaitTime +
                    ", avgQueueLength=" + avgQueueLength +
                    ", throughput=" + throughput +
                    ", avgSpeed=" + avgSpeed +
                    ", stopCount=" + stopCount +
                    '}';
        }
    }
}
