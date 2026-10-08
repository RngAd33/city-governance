package com.cityplatform.disaster.risk;

import com.cityplatform.disaster.model.FloodScenario;
import com.cityplatform.disaster.model.WaterLevelRecord;
import com.cityplatform.disaster.model.WaterStation;
import com.cityplatform.disaster.repository.FloodScenarioRepository;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 洪涝风险评估服务
 * 基于水量平衡模型评估洪水风险并生成灾害模拟场景
 */
@Service
@Slf4j
public class FloodRiskAssessor {

    /** 径流系数（城市区域经验值） */
    private static final double RUNOFF_COEFFICIENT = 0.65;

    /** 排水速率（米/小时，城市排水管网平均排水能力） */
    private static final double DRAINAGE_RATE = 0.08;

    /** 汇水放大系数（降雨径流经汇水区汇集至监测断面的放大效应） */
    private static final double CATCHMENT_FACTOR = 2.0;

    /** 排水抵消上限（排水最多抵消入流的60%，超出部分形成积水累积） */
    private static final double DRAINAGE_OFFSET_LIMIT = 0.6;

    /** 长江径流量参照值（m³/s，与采集服务配置一致） */
    private static final double RIVER_REFERENCE_DISCHARGE = 30000.0;

    /** 径流调节系数（长江径流量相对参照值每偏差1倍、持续24小时对应的水位调节，米） */
    private static final double RIVER_ADJUST_COEFFICIENT = 0.3;

    /** 站点类型对长江径流量的敏感度（河流最敏感，排水管网基本不受干流顶托影响） */
    private static final Map<WaterStation.StationType, Double> RIVER_SENSITIVITY = Map.of(
            WaterStation.StationType.RIVER, 1.0,
            WaterStation.StationType.LAKE, 0.5,
            WaterStation.StationType.RESERVOIR, 0.3,
            WaterStation.StationType.DRAINAGE, 0.1
    );

    /** 风险等级阈值 */
    private static final double LOW_THRESHOLD = 60.0;
    private static final double MEDIUM_THRESHOLD = 80.0;
    private static final double HIGH_THRESHOLD = 95.0;

    @Resource
    private FloodScenarioRepository floodScenarioRepository;

    /**
     * 风险评估结果
     */
    @Data
    @AllArgsConstructor
    public static class RiskAssessment {
        private WaterLevelRecord.RiskLevel riskLevel;
        private double riskScore;
        private double affectedArea;
        private List<String> recommendedActions;
        private List<String> evacuationZones;
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
        return assessRisk(station, currentLevel, rainfallForecast, forecastHours, null);
    }

    /**
     * 评估洪水风险（含真实长江径流量调节）
     *
     * @param riverDischarge 长江当前径流量（m³/s，可为空）
     */
    public RiskAssessment assessRisk(WaterStation station, double currentLevel,
                                      double rainfallForecast, int forecastHours,
                                      Double riverDischarge) {
        // 水量平衡计算：降雨入流 - 排水抵消 + 径流调节
        double projectedLevel = projectLevel(currentLevel, station, rainfallForecast, forecastHours, riverDischarge);

        // 计算容量百分比
        double capacityPercentage = (projectedLevel / station.getMaxCapacity()) * 100.0;

        // 确定风险等级（综合库容占比与预警/危险水位阈值）
        WaterLevelRecord.RiskLevel riskLevel = determineRiskLevel(station, projectedLevel);

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
        return simulateFloodScenario(station, rainfallMm, durationHours, null);
    }

    /**
     * 模拟洪涝灾害场景（含真实长江径流量调节）
     *
     * @param riverDischarge 长江当前径流量（m³/s，可为空）
     */
    public FloodScenario simulateFloodScenario(WaterStation station,
                                                double rainfallMm, int durationHours,
                                                Double riverDischarge) {
        // 水量平衡推演：预测水位
        double projectedLevel = projectLevel(station, rainfallMm, durationHours, riverDischarge);

        // 计算容量百分比
        double capacityPercentage = (projectedLevel / station.getMaxCapacity()) * 100.0;

        // 确定风险等级（综合库容占比与预警/危险水位阈值）
        WaterLevelRecord.RiskLevel riskLevel = determineRiskLevel(station, projectedLevel);

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
     * 水量平衡推演预测水位（以站点当前水位为起点，含真实长江径流量调节）
     * 降雨入流 = 降雨量 * 径流系数 * 汇水放大系数；
     * 排水抵消 = min(排水速率 * 时长, 入流的60%)，超出部分形成积水累积；
     * 径流调节 = 长江径流量相对参照值偏差 * 站点类型敏感度（按天累计）
     */
    public double projectLevel(WaterStation station, double rainfallMm, int durationHours,
                               Double riverDischarge) {
        return projectLevel(station.getCurrentLevel(), station, rainfallMm, durationHours, riverDischarge);
    }

    /**
     * 水量平衡推演预测水位（不含径流调节）
     */
    public double projectLevel(WaterStation station, double rainfallMm, int durationHours) {
        return projectLevel(station, rainfallMm, durationHours, null);
    }

    private double projectLevel(double currentLevel, WaterStation station, double rainfallMm,
                                int durationHours, Double riverDischarge) {
        double inflow = rainfallMm / 1000.0 * RUNOFF_COEFFICIENT * CATCHMENT_FACTOR;
        double drainage = Math.min(DRAINAGE_RATE * durationHours, DRAINAGE_OFFSET_LIMIT * inflow);
        double levelChange = inflow - drainage;

        if (riverDischarge != null && riverDischarge > 0) {
            double sensitivity = RIVER_SENSITIVITY.getOrDefault(station.getType(), 0.5);
            double ratio = riverDischarge / RIVER_REFERENCE_DISCHARGE - 1.0;
            levelChange += RIVER_ADJUST_COEFFICIENT * sensitivity * ratio * (durationHours / 24.0);
        }
        return currentLevel + Math.max(0.0, levelChange);
    }

    /**
     * 综合风险等级：危险/预警水位阈值与库容占比取较高级别
     */
    public WaterLevelRecord.RiskLevel determineRiskLevel(WaterStation station, double level) {
        double capacity = station.getMaxCapacity() != null && station.getMaxCapacity() > 0
                ? level / station.getMaxCapacity() * 100.0 : 0.0;
        WaterLevelRecord.RiskLevel capacityRisk = determineRiskLevel(capacity);
        if (station.getDangerLevel() != null && level >= station.getDangerLevel()) {
            return WaterLevelRecord.RiskLevel.CRITICAL;
        }
        if (capacityRisk == WaterLevelRecord.RiskLevel.CRITICAL) {
            return WaterLevelRecord.RiskLevel.CRITICAL;
        }
        if (station.getWarningLevel() != null && level >= station.getWarningLevel()) {
            return WaterLevelRecord.RiskLevel.HIGH;
        }
        return capacityRisk;
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
     * 估算受影响人口（供灾害模拟接口复用）
     */
    public int estimateAffectedPopulation(double affectedArea,
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
     * 估算损失（万元，供灾害模拟接口复用）
     */
    public double estimateDamage(double affectedArea, int affectedPopulation,
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
