package com.cityplatform.traffic.optimizer;

import com.cityplatform.traffic.model.SignalPhase;
import com.cityplatform.traffic.model.TrafficFlowData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 遗传算法信号配时优化器
 *
 * 染色体编码：每个基因代表一个相位的绿灯时间(10-120秒)
 * 适应度函数：最小化总等待时间（M/D/1 排队模型，与 TrafficSimulator 同口径） + 约束惩罚
 * 约束条件：最小绿灯时间 >= 10s, 最大周期 <= 180s, 行人最小绿灯 >= 15s
 */
@Slf4j
@Component
public class GeneticAlgorithmOptimizer {

    // 遗传算法参数
    private static final int POPULATION_SIZE = 50;
    private static final int MAX_GENERATIONS = 100;
    private static final double MUTATION_RATE = 0.1;
    private static final double CROSSOVER_RATE = 0.8;

    // 约束条件
    private static final int MIN_GREEN_TIME = 10;
    private static final int MAX_GREEN_TIME = 120;
    private static final int MAX_CYCLE_LENGTH = 180;
    private static final int PEDESTRIAN_MIN_GREEN = 15;
    private static final int YELLOW_TIME = 3;

    // 惩罚系数
    private static final double PENALTY_CYCLE_VIOLATION = 10000.0;
    private static final double PENALTY_MIN_GREEN_VIOLATION = 5000.0;
    private static final double PENALTY_PEDESTRIAN_VIOLATION = 8000.0;
    // 饱和流量（辆/小时），与 TrafficSimulator 保持一致
    private static final double SATURATION_FLOW = 1800.0;
    // 过饱和排队基座（与 TrafficSimulator 一致），使过饱和段与稳态段平滑衔接
    private static final double OVERFLOW_QUEUE_BASE = 499.0;
    // 评估窗口（秒），与仿真时长 60 分钟同口径
    private static final double SIMULATION_SECONDS = 3600.0;

    private final Random random = new Random();

    /**
     * 优化信号配时方案
     *
     * @param currentPhases 当前信号相位配置
     * @param flowData      交通流数据
     * @return 优化后的信号相位列表
     */
    public List<SignalPhase> optimize(List<SignalPhase> currentPhases, List<TrafficFlowData> flowData) {
        if (currentPhases == null || currentPhases.isEmpty()) {
            log.warn("当前相位配置为空，无法进行优化");
            return currentPhases;
        }

        int phaseCount = currentPhases.size();
        log.info("开始遗传算法优化: {}个相位, {}条交通流数据", phaseCount, flowData != null ? flowData.size() : 0);

        // 按方向分组流量数据
        Map<String, List<TrafficFlowData>> flowByDirection = groupFlowByDirection(flowData);

        // 初始化种群
        List<Chromosome> population = initializePopulation(phaseCount);

        // 进化
        Chromosome bestChromosome = null;
        double bestFitness = Double.MAX_VALUE;

        for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
            // 评估适应度
            for (Chromosome chromosome : population) {
                chromosome.fitness = evaluateFitness(chromosome, currentPhases, flowByDirection);
            }

            // 排序(适应度越小越好)
            population.sort(Comparator.comparingDouble(c -> c.fitness));

            // 记录最优
            if (population.get(0).fitness < bestFitness) {
                bestFitness = population.get(0).fitness;
                bestChromosome = new Chromosome(population.get(0));
            }

            if (generation % 10 == 0) {
                log.info("第{}代: 最优适应度={}", generation, String.format("%.2f", bestFitness));
            }

            // 选择、交叉、变异产生新一代
            List<Chromosome> newPopulation = new ArrayList<>();

            // 精英保留: 保留前10%直接进入下一代
            int eliteCount = Math.max(1, POPULATION_SIZE / 10);
            for (int i = 0; i < eliteCount; i++) {
                newPopulation.add(new Chromosome(population.get(i)));
            }

            // 轮盘赌选择 + 交叉 + 变异
            while (newPopulation.size() < POPULATION_SIZE) {
                Chromosome parent1 = tournamentSelection(population);
                Chromosome parent2 = tournamentSelection(population);

                // 交叉
                Chromosome offspring;
                if (random.nextDouble() < CROSSOVER_RATE) {
                    offspring = crossover(parent1, parent2, phaseCount);
                } else {
                    offspring = new Chromosome(random.nextBoolean() ? parent1 : parent2);
                }

                // 变异
                mutate(offspring);

                // 修复不可行解
                repair(offspring);

                newPopulation.add(offspring);
            }

            population = newPopulation;
        }

        // 最终评估
        for (Chromosome chromosome : population) {
            chromosome.fitness = evaluateFitness(chromosome, currentPhases, flowByDirection);
        }
        population.sort(Comparator.comparingDouble(c -> c.fitness));
        if (population.get(0).fitness < bestFitness) {
            bestFitness = population.get(0).fitness;
            bestChromosome = new Chromosome(population.get(0));
        }

        log.info("遗传算法优化完成: 最终适应度={}", String.format("%.2f", bestFitness));

        // 将最优染色体转换为信号相位配置
        return convertToPhases(bestChromosome, currentPhases);
    }

    /**
     * 初始化种群
     */
    private List<Chromosome> initializePopulation(int phaseCount) {
        List<Chromosome> population = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            Chromosome chromosome = new Chromosome(phaseCount);
            for (int j = 0; j < phaseCount; j++) {
                chromosome.genes[j] = MIN_GREEN_TIME + random.nextInt(MAX_GREEN_TIME - MIN_GREEN_TIME + 1);
            }
            repair(chromosome);
            population.add(chromosome);
        }
        return population;
    }

    /**
     * 适应度评估
     * 目标: 最小化 1 小时总等待时间（与 TrafficSimulator 的 M/D/1 排队模型同口径，
     * 避免优化方向与仿真验证不一致）+ 约束惩罚
     */
    private double evaluateFitness(Chromosome chromosome, List<SignalPhase> currentPhases,
                                   Map<String, List<TrafficFlowData>> flowByDirection) {
        int phaseCount = chromosome.genes.length;

        // 计算周期长度
        int cycleLength = 0;
        for (int i = 0; i < phaseCount; i++) {
            cycleLength += chromosome.genes[i] + YELLOW_TIME;
        }

        // 计算基础等待时间（M/D/1 排队模型，与仿真器一致）
        double totalWaitTime = 0.0;
        for (int i = 0; i < phaseCount; i++) {
            SignalPhase phase = currentPhases.get(i);
            String direction = phase.getDirection();
            double greenRatio = (double) chromosome.genes[i] / cycleLength;

            // 获取该方向的流量
            double flow = 0.0;
            List<TrafficFlowData> dirFlows = flowByDirection.get(direction);
            if (dirFlows != null && !dirFlows.isEmpty()) {
                flow = dirFlows.stream().mapToDouble(TrafficFlowData::getFlow).average().orElse(0.0);
            }
            if (flow <= 0) {
                continue;
            }

            // lambda 到达率、mu 服务率（辆/秒）；排队模型与仿真器同口径：
            // 非饱和用 M/D/1 稳态解，过饱和用溢出外推，避免截断拉平
            double lambda = flow / 3600.0;
            double mu = greenRatio * SATURATION_FLOW / 3600.0;
            double rho = mu > 0 ? lambda / mu : 2.0;
            double avgQueue;
            if (rho < 1.0) {
                avgQueue = (rho * rho) / (2.0 * (1.0 - rho));
            } else {
                avgQueue = OVERFLOW_QUEUE_BASE + (lambda - mu) * SIMULATION_SECONDS / 2.0;
            }
            double waitSec = avgQueue / lambda;
            // 1 小时窗口总等待（辆·秒），与仿真 durationMinutes=60 同量纲
            totalWaitTime += flow * waitSec;
        }

        // 约束惩罚
        double penalty = 0.0;

        // 约束1: 周期长度不超过最大值
        if (cycleLength > MAX_CYCLE_LENGTH) {
            penalty += PENALTY_CYCLE_VIOLATION * (cycleLength - MAX_CYCLE_LENGTH);
        }

        // 约束2: 最小绿灯时间
        for (int i = 0; i < phaseCount; i++) {
            if (chromosome.genes[i] < MIN_GREEN_TIME) {
                penalty += PENALTY_MIN_GREEN_VIOLATION * (MIN_GREEN_TIME - chromosome.genes[i]);
            }
        }

        // 约束3: 行人最小绿灯时间(假设每个相位都需要满足行人最小绿灯)
        for (int i = 0; i < phaseCount; i++) {
            if (chromosome.genes[i] < PEDESTRIAN_MIN_GREEN) {
                penalty += PENALTY_PEDESTRIAN_VIOLATION * (PEDESTRIAN_MIN_GREEN - chromosome.genes[i]);
            }
        }

        return totalWaitTime + penalty;
    }

    /**
     * 锦标赛选择
     */
    private Chromosome tournamentSelection(List<Chromosome> population) {
        int tournamentSize = 3;
        Chromosome best = null;
        double bestFitness = Double.MAX_VALUE;

        for (int i = 0; i < tournamentSize; i++) {
            Chromosome candidate = population.get(random.nextInt(population.size()));
            if (candidate.fitness < bestFitness) {
                bestFitness = candidate.fitness;
                best = candidate;
            }
        }
        return best;
    }

    /**
     * 单点交叉
     */
    private Chromosome crossover(Chromosome parent1, Chromosome parent2, int phaseCount) {
        Chromosome offspring = new Chromosome(phaseCount);
        int crossoverPoint = 1 + random.nextInt(phaseCount - 1);

        for (int i = 0; i < phaseCount; i++) {
            if (i < crossoverPoint) {
                offspring.genes[i] = parent1.genes[i];
            } else {
                offspring.genes[i] = parent2.genes[i];
            }
        }
        return offspring;
    }

    /**
     * 变异操作
     * 对每个基因以变异概率进行高斯变异
     */
    private void mutate(Chromosome chromosome) {
        for (int i = 0; i < chromosome.genes.length; i++) {
            if (random.nextDouble() < MUTATION_RATE) {
                // 高斯变异: 均值为0，标准差为10
                double delta = random.nextGaussian() * 10;
                chromosome.genes[i] = (int) Math.round(chromosome.genes[i] + delta);
            }
        }
    }

    /**
     * 修复不可行解
     * 将基因值限制在[MIN_GREEN_TIME, MAX_GREEN_TIME]范围内
     */
    private void repair(Chromosome chromosome) {
        for (int i = 0; i < chromosome.genes.length; i++) {
            if (chromosome.genes[i] < MIN_GREEN_TIME) {
                chromosome.genes[i] = MIN_GREEN_TIME;
            }
            if (chromosome.genes[i] > MAX_GREEN_TIME) {
                chromosome.genes[i] = MAX_GREEN_TIME;
            }
        }
    }

    /**
     * 将染色体转换为信号相位配置
     */
    private List<SignalPhase> convertToPhases(Chromosome chromosome, List<SignalPhase> currentPhases) {
        List<SignalPhase> optimizedPhases = new ArrayList<>();
        int cycleLength = 0;

        // 先计算周期
        for (int i = 0; i < chromosome.genes.length; i++) {
            cycleLength += chromosome.genes[i] + YELLOW_TIME;
        }

        for (int i = 0; i < chromosome.genes.length; i++) {
            SignalPhase original = currentPhases.get(i);
            int greenTime = chromosome.genes[i];
            // 红灯时间 = 周期 - 本相位绿灯 - 黄灯
            int redTime = cycleLength - greenTime - YELLOW_TIME;
            if (redTime < 0) redTime = 0;

            SignalPhase optimized = new SignalPhase(
                    original.getPhaseId(),
                    original.getDirection(),
                    greenTime,
                    YELLOW_TIME,
                    redTime
            );
            optimizedPhases.add(optimized);
        }

        return optimizedPhases;
    }

    /**
     * 按方向分组流量数据
     */
    private Map<String, List<TrafficFlowData>> groupFlowByDirection(List<TrafficFlowData> flowData) {
        if (flowData == null || flowData.isEmpty()) {
            return Collections.emptyMap();
        }
        return flowData.stream()
                .collect(Collectors.groupingBy(TrafficFlowData::getDirection));
    }

    /**
     * 染色体内部类
     */
    private static class Chromosome {
        int[] genes;
        double fitness;

        Chromosome(int length) {
            this.genes = new int[length];
            this.fitness = Double.MAX_VALUE;
        }

        Chromosome(Chromosome other) {
            this.genes = Arrays.copyOf(other.genes, other.genes.length);
            this.fitness = other.fitness;
        }
    }
}
