package com.cityplatform.water.risk;

import com.cityplatform.water.model.FloodScenario;
import com.cityplatform.water.model.WaterLevelRecord;
import com.cityplatform.water.model.WaterStation;
import com.cityplatform.water.repository.FloodScenarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 洪涝风险评估服务
 * 基于水量平衡模型评估洪水风险并生成灾害模拟场景
 */
@Service
public class FloodRiskAssessor {

    private static final Logger log = LoggerFactory.getLogger(FloodRiskAssessor.class);

    /** 径流系数（城市区域经验值） */
    private static final double RUNOFF_COEFFICIENT = 0.65;

    /** 排水速率（米/小时，城市排水管网平均排水能力） */
    private static final double DRAINAGE_RATE = 0.08;

    /** 风险等级阈值 */
    private static final double LOW_THRESHOLD = 60.0;
    private static final double MEDIUM_THRESHOLD = 80.0;
    private static final double HIGH_THRESHOLD = 95.0;

    private final FloodScenarioRepository floodScenarioRepository;

    public FloodRiskAssessor(FloodScenarioRepository floodScenarioRepository) {
        this.floodScenarioRepository = floodScenarioRepository;
    }

    /**
     * 风险评估结果
     */
    public static class RiskAssessment {
        private WaterLevelRecord.RiskLevel riskLevel;
        private double riskScore;
        private double affectedArea;
        private List<String> recommendedActions;
        private List<String> evacuationZones;

        public RiskAssessment() {}

        public RiskAssessment(WaterLevelRecord.RiskLevel riskLevel, double riskScore,
                               double affectedArea, List<String> recommendedActions,
                               List<String> evacuationZones) {
            this.riskLevel = riskLevel;
            this.riskScore = riskScore;
            this.affectedArea = affectedArea;
            this.recommendedActions = recommendedActions;
            this.evacuationZones = evacuationZones;
        }

        // Getters and Setters
        public WaterLevelRecord.RiskLevel getRiskLevel() { return riskLevel; }
        public void setRiskLevel(WaterLevelRecord.RiskLevel riskLevel) { this.riskLevel = riskLevel; }

        public double getRiskScore() { return riskScore; }
        public void setRiskScore(double riskScore) { this.riskScore = riskScore; }

        public double getAffectedArea() { return affectedArea; }
        public void setAffectedArea(double affectedArea) { this.affectedArea = affectedArea; }

        public List<String> getRecommendedActions() { return recommendedActions; }
        public void setRecommendedActions(List<String> recommendedActions) { this.recommendedActions = recommendedActions; }

        public List<String> getEvacuationZones() { return evacuationZones; }
        public void setEvacuationZones(List<String> evacuationZones) { this.evacuationZones = evacuationZones; }
    }

    /**
     * 评估洪水风险
     *
     * @param station           监测站点
     * @param currentLevel       当前水位（米）
     * @param rainfallForecast   预报降雨量（毫米）
     * @param forecastHours      预报时长（小时）
     * @return 风险评估结果
     */
    public RiskAssessment assessRisk(WaterStation station, double currentLevel,
                                      double rainfallForecast, int forecastHours) {
        // 水量平衡计算：水位变化 = 降雨 * 径流系数 - 排水速率
        double levelChange = (rainfallForecast * RUNOFF_COEFFICIENT) / 1000.0
                - (DRAINAGE_RATE * forecastHours);
        double projectedLevel = currentLevel + Math.max(0, levelChange);

        // 计算容量百分比
        double capacityPercentage = (projectedLevel / station.getMaxCapacity()) * 100.0;

        // 确定风险等级
        WaterLevelRecord.RiskLevel riskLevel = determineRiskLevel(capacityPercentage);

        // 计算风险评分（0-100）
        double riskScore = calculateRiskScore(capacityPercentage, rainfallForecast, forecastHours);

        // 估算影响面积（平方公里）
        double affectedArea = estimateAffectedArea(station, projectedLevel, riskLevel);

        // 生成建议措施
        List<String> recommendedActions = generateRecommendedActions(riskLevel, station);

        // 生成疏散区域
        List<String> evacuationZones = generateEvacuationZones(station, riskLevel);

        log.info("风险评估完成 [{}]: 风险等级={}, 评分={}/100, 影响面积={}km²",
                station.getStationId(), riskLevel, String.format("%.1f", riskScore),
                String.format("%.2f", affectedArea));

        return new RiskAssessment(riskLevel, riskScore, affectedArea,
                recommendedActions, evacuationZones);
    }

    /**
     * 模拟洪涝灾害场景
     *
     * @param station       监测站点
     * @param rainfallMm     降雨强度（毫米）
     * @param durationHours  持续时长（小时）
     * @return 灾害模拟场景
     */
    public FloodScenario simulateFloodScenario(WaterStation station,
                                                double rainfallMm, int durationHours) {
        // 水量平衡：水位变化 = 降雨 * 径流系数 - 排水速率
        double totalRainfallM = rainfallMm / 1000.0;
        double levelChange = (totalRainfallM * RUNOFF_COEFFICIENT)
                - (DRAINAGE_RATE * durationHours);
        double projectedLevel = station.getCurrentLevel() + Math.max(0, levelChange);

        // 计算容量百分比
        double capacityPercentage = (projectedLevel / station.getMaxCapacity()) * 100.0;

        // 确定风险等级
        WaterLevelRecord.RiskLevel riskLevel = determineRiskLevel(capacityPercentage);

        // 估算影响面积
        double affectedArea = estimateAffectedArea(station, projectedLevel, riskLevel);

        // 估算受影响人口（基于影响面积与城市人口密度）
        int affectedPopulation = estimateAffectedPopulation(affectedArea, riskLevel);

        // 估算损失（万元）
        double estimatedDamage = estimateDamage(affectedArea, affectedPopulation, riskLevel);

        // 生成道路封闭建议
        String roadClosures = generateRoadClosures(station, riskLevel);

        // 创建场景
        String scenarioId = "FS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String scenarioName = String.format("%s洪涝场景-%s", station.getName(),
                LocalDateTime.now().toLocalDate());

        FloodScenario scenario = new FloodScenario(
                scenarioId,
                scenarioName,
                station.getStationId(),
                rainfallMm,
                durationHours,
                affectedArea,
                riskLevel,
                estimatedDamage,
                affectedPopulation,
                roadClosures,
                LocalDateTime.now()
        );

        floodScenarioRepository.save(scenario);
        log.info("洪涝模拟场景已生成 [{}]: 风险等级={}, 影响面积={}km², 预估损失={}万元",
                scenarioId, riskLevel, String.format("%.2f", affectedArea),
                String.format("%.2f", estimatedDamage));

        return scenario;
    }

    /**
     * 根据容量百分比确定风险等级
     */
    private WaterLevelRecord.RiskLevel determineRiskLevel(double capacityPercentage) {
        if (capacityPercentage > HIGH_THRESHOLD) {
            return WaterLevelRecord.RiskLevel.CRITICAL;
        } else if (capacityPercentage > MEDIUM_THRESHOLD) {
            return WaterLevelRecord.RiskLevel.HIGH;
        } else if (capacityPercentage > LOW_THRESHOLD) {
            return WaterLevelRecord.RiskLevel.MEDIUM;
        }
        return WaterLevelRecord.RiskLevel.LOW;
    }

    /**
     * 计算综合风险评分（0-100）
     */
    private double calculateRiskScore(double capacityPercentage,
                                      double rainfallForecast, int forecastHours) {
        // 容量占比权重60%，降雨强度权重25%，时长权重15%
        double capacityScore = Math.min(capacityPercentage, 100.0) * 0.6;
        double rainfallScore = Math.min(rainfallForecast / 200.0 * 100.0, 100.0) * 0.25;
        double durationScore = Math.min(forecastHours / 72.0 * 100.0, 100.0) * 0.15;

        return Math.min(capacityScore + rainfallScore + durationScore, 100.0);
    }

    /**
     * 估算影响面积（平方公里）
     */
    private double estimateAffectedArea(WaterStation station,
                                        double projectedLevel,
                                        WaterLevelRecord.RiskLevel riskLevel) {
        double overflowRatio = Math.max(0, projectedLevel - station.getWarningLevel())
                / station.getMaxCapacity();

        double baseArea;
        switch (riskLevel) {
            case CRITICAL:
                baseArea = 15.0;
                break;
            case HIGH:
                baseArea = 8.0;
                break;
            case MEDIUM:
                baseArea = 3.0;
                break;
            default:
                baseArea = 0.5;
        }

        return baseArea * (1 + overflowRatio * 10);
    }

    /**
     * 估算受影响人口
     */
    private int estimateAffectedPopulation(double affectedArea,
                                           WaterLevelRecord.RiskLevel riskLevel) {
        // 城市人口密度约5000人/km²
        double densityFactor;
        switch (riskLevel) {
            case CRITICAL:
                densityFactor = 8000.0;
                break;
            case HIGH:
                densityFactor = 6000.0;
                break;
            case MEDIUM:
                densityFactor = 4000.0;
                break;
            default:
                densityFactor = 2000.0;
        }
        return (int) (affectedArea * densityFactor);
    }

    /**
     * 估算损失（万元）
     */
    private double estimateDamage(double affectedArea, int affectedPopulation,
                                  WaterLevelRecord.RiskLevel riskLevel) {
        double baseDamagePerKm2;
        switch (riskLevel) {
            case CRITICAL:
                baseDamagePerKm2 = 5000.0;
                break;
            case HIGH:
                baseDamagePerKm2 = 2000.0;
                break;
            case MEDIUM:
                baseDamagePerKm2 = 500.0;
                break;
            default:
                baseDamagePerKm2 = 100.0;
        }
        return affectedArea * baseDamagePerKm2 + affectedPopulation * 0.5;
    }

    /**
     * 生成建议措施
     */
    private List<String> generateRecommendedActions(WaterLevelRecord.RiskLevel riskLevel,
                                                     WaterStation station) {
        List<String> actions = new ArrayList<>();

        switch (riskLevel) {
            case CRITICAL:
                actions.add("立即启动I级应急响应，通知所有相关部门");
                actions.add("组织低洼地区居民紧急疏散");
                actions.add("关闭地下空间及地下交通设施");
                actions.add("调集抢险救援队伍待命");
                actions.add("开启所有排水泵站满负荷运行");
                actions.add("发布红色预警信息，通过全媒体渠道推送");
                break;
            case HIGH:
                actions.add("启动II级应急响应");
                actions.add("加强水位监测频率至每15分钟一次");
                actions.add("预置抢险物资至重点区域");
                actions.add("通知低洼地区居民做好转移准备");
                actions.add("检查排水管网运行状态");
                actions.add("发布橙色预警信息");
                break;
            case MEDIUM:
                actions.add("启动III级应急响应");
                actions.add("加强巡查，关注水位变化趋势");
                actions.add("清理排水口杂物，确保排水畅通");
                actions.add("通知相关部门进入待命状态");
                actions.add("发布黄色预警信息");
                break;
            default:
                actions.add("保持常规监测");
                actions.add("关注气象预报变化");
                actions.add("定期检查排水设施");
                break;
        }

        return actions;
    }

    /**
     * 生成疏散区域列表
     */
    private List<String> generateEvacuationZones(WaterStation station,
                                                 WaterLevelRecord.RiskLevel riskLevel) {
        List<String> zones = new ArrayList<>();

        switch (riskLevel) {
            case CRITICAL:
                zones.add(station.getName() + "周边500米范围");
                zones.add(station.getName() + "下游1公里范围");
                zones.add("邻近低洼居民区");
                zones.add("地下商业设施");
                break;
            case HIGH:
                zones.add(station.getName() + "周边300米范围");
                zones.add(station.getName() + "下游500米范围");
                zones.add("邻近低洼居民区");
                break;
            case MEDIUM:
                zones.add(station.getName() + "周边100米范围");
                zones.add("邻近低洼地带");
                break;
            default:
                zones.add("暂无需疏散区域");
                break;
        }

        return zones;
    }

    /**
     * 生成道路封闭建议（JSON格式）
     */
    private String generateRoadClosures(WaterStation station,
                                         WaterLevelRecord.RiskLevel riskLevel) {
        List<String> roads = new ArrayList<>();

        switch (riskLevel) {
            case CRITICAL:
                roads.add(station.getName() + "沿岸道路");
                roads.add("下穿隧道");
                roads.add("地下通道");
                break;
            case HIGH:
                roads.add(station.getName() + "低洼路段");
                roads.add("下穿隧道");
                break;
            case MEDIUM:
                roads.add(station.getName() + "部分低洼路段");
                break;
            default:
                roads.add("暂无道路封闭建议");
                break;
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < roads.size(); i++) {
            sb.append("\"").append(roads.get(i)).append("\"");
            if (i < roads.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
