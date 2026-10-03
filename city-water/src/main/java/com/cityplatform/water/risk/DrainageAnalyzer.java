package com.cityplatform.water.risk;

import com.cityplatform.water.model.WaterStation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 排水能力分析服务
 * 评估城市排水管网承载能力，识别排水瓶颈
 */
@Service
public class DrainageAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(DrainageAnalyzer.class);

    /** 排水管网设计排水速率（立方米/秒/公里管道） */
    private static final double DESIGN_DRAINAGE_RATE = 2.5;

    /** 排水能力充足阈值（排水能力/入流 >= 1.2为充足） */
    private static final double ADEQUATE_RATIO = 1.2;

    /** 排水能力不足阈值（排水能力/入流 < 0.8为不足） */
    private static final double INADEQUATE_RATIO = 0.8;

    /**
     * 排水能力分析结果
     */
    public static class DrainageAnalysis {
        private String stationId;
        private String stationName;
        private double inflow;
        private double drainageCapacity;
        private double adequacyRatio;
        private AdequacyLevel adequacyLevel;
        private List<String> recommendations;

        public enum AdequacyLevel {
            SUFFICIENT("充足"),
            ADEQUATE("基本满足"),
            INADEQUATE("不足"),
            CRITICAL("严重不足");

            private final String description;

            AdequacyLevel(String description) {
                this.description = description;
            }

            public String getDescription() {
                return description;
            }
        }

        public DrainageAnalysis() {}

        public DrainageAnalysis(String stationId, String stationName, double inflow,
                              double drainageCapacity, double adequacyRatio,
                              AdequacyLevel adequacyLevel, List<String> recommendations) {
            this.stationId = stationId;
            this.stationName = stationName;
            this.inflow = inflow;
            this.drainageCapacity = drainageCapacity;
            this.adequacyRatio = adequacyRatio;
            this.adequacyLevel = adequacyLevel;
            this.recommendations = recommendations;
        }

        // Getters and Setters
        public String getStationId() { return stationId; }
        public void setStationId(String stationId) { this.stationId = stationId; }

        public String getStationName() { return stationName; }
        public void setStationName(String stationName) { this.stationName = stationName; }

        public double getInflow() { return inflow; }
        public void setInflow(double inflow) { this.inflow = inflow; }

        public double getDrainageCapacity() { return drainageCapacity; }
        public void setDrainageCapacity(double drainageCapacity) { this.drainageCapacity = drainageCapacity; }

        public double getAdequacyRatio() { return adequacyRatio; }
        public void setAdequacyRatio(double adequacyRatio) { this.adequacyRatio = adequacyRatio; }

        public AdequacyLevel getAdequacyLevel() { return adequacyLevel; }
        public void setAdequacyLevel(AdequacyLevel adequacyLevel) { this.adequacyLevel = adequacyLevel; }

        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    }

    /**
     * 分析排水能力
     *
     * @param station 监测站点
     * @param inflow  入流量（立方米/秒）
     * @return 排水能力分析结果
     */
    public DrainageAnalysis analyzeDrainageCapacity(WaterStation station, double inflow) {
        // 根据站点类型估算排水能力
        double drainageCapacity = estimateDrainageCapacity(station);

        // 计算排水充足率
        double adequacyRatio = inflow > 0 ? drainageCapacity / inflow : Double.MAX_VALUE;

        // 确定充足等级
        DrainageAnalysis.AdequacyLevel level = determineAdequacyLevel(adequacyRatio);

        // 生成建议
        List<String> recommendations = generateRecommendations(level, station, inflow, drainageCapacity);

        log.info("排水能力分析 [{}]: 入流={}m³/s, 排水能力={}m³/s, 充足率={}",
                station.getStationId(), String.format("%.2f", inflow),
                String.format("%.2f", drainageCapacity),
                String.format("%.2f", adequacyRatio));

        return new DrainageAnalysis(
                station.getStationId(),
                station.getName(),
                inflow,
                drainageCapacity,
                adequacyRatio,
                level,
                recommendations
        );
    }

    /**
     * 识别排水瓶颈站点
     *
     * @param stations 所有监测站点列表
     * @return 瓶颈站点列表
     */
    public List<WaterStation> identifyBottlenecks(List<WaterStation> stations) {
        List<WaterStation> bottlenecks = new ArrayList<>();

        for (WaterStation station : stations) {
            // 估算当前入流量（基于水位与流量的经验关系）
            double estimatedInflow = estimateInflow(station);

            // 获取排水能力
            double drainageCapacity = estimateDrainageCapacity(station);

            // 计算充足率
            double adequacyRatio = estimatedInflow > 0
                    ? drainageCapacity / estimatedInflow : Double.MAX_VALUE;

            // 充足率低于阈值则为瓶颈
            if (adequacyRatio < INADEQUATE_RATIO) {
                bottlenecks.add(station);
                log.warn("识别排水瓶颈 [{}]: 充足率={}", station.getStationId(),
                        String.format("%.2f", adequacyRatio));
            }
        }

        log.info("排水瓶颈识别完成: 共发现{}个瓶颈站点", bottlenecks.size());
        return bottlenecks;
    }

    /**
     * 根据站点类型估算排水能力
     */
    private double estimateDrainageCapacity(WaterStation station) {
        double baseCapacity;
        switch (station.getType()) {
            case RIVER:
                // 河流排水能力与水位差相关
                baseCapacity = station.getMaxCapacity() * 0.5;
                break;
            case LAKE:
                // 湖泊排水能力较低
                baseCapacity = station.getMaxCapacity() * 0.2;
                break;
            case RESERVOIR:
                // 水库有闸门控制，排水能力较强
                baseCapacity = station.getMaxCapacity() * 0.8;
                break;
            case DRAINAGE:
                // 排水管网按设计标准
                baseCapacity = DESIGN_DRAINAGE_RATE * 10;
                break;
            default:
                baseCapacity = 5.0;
        }

        // 根据当前水位调整（水位越高，排水压力越大，有效排水能力下降）
        double levelFactor = 1.0 - (station.getCurrentLevel() / station.getMaxCapacity()) * 0.3;
        return baseCapacity * Math.max(0.5, levelFactor);
    }

    /**
     * 估算当前入流量
     */
    private double estimateInflow(WaterStation station) {
        // 基于水位变化率与降雨的经验估算
        double levelRatio = station.getCurrentLevel() / station.getMaxCapacity();

        // 基础入流与水位成正比
        double baseInflow = levelRatio * 20.0;

        // 排水管网类型站点入流更大
        if (station.getType() == WaterStation.StationType.DRAINAGE) {
            baseInflow *= 1.5;
        }

        return baseInflow;
    }

    /**
     * 确定排水充足等级
     */
    private DrainageAnalysis.AdequacyLevel determineAdequacyLevel(double adequacyRatio) {
        if (adequacyRatio >= ADEQUATE_RATIO) {
            return DrainageAnalysis.AdequacyLevel.SUFFICIENT;
        } else if (adequacyRatio >= 1.0) {
            return DrainageAnalysis.AdequacyLevel.ADEQUATE;
        } else if (adequacyRatio >= INADEQUATE_RATIO) {
            return DrainageAnalysis.AdequacyLevel.INADEQUATE;
        }
        return DrainageAnalysis.AdequacyLevel.CRITICAL;
    }

    /**
     * 生成排水改善建议
     */
    private List<String> generateRecommendations(DrainageAnalysis.AdequacyLevel level,
                                                  WaterStation station,
                                                  double inflow,
                                                  double drainageCapacity) {
        List<String> recommendations = new ArrayList<>();

        switch (level) {
            case SUFFICIENT:
                recommendations.add("排水能力充足，保持现有维护频率");
                recommendations.add("定期清理管道淤积，维持排水效率");
                break;
            case ADEQUATE:
                recommendations.add("排水能力基本满足，建议加强日常巡查");
                recommendations.add("关注极端天气下的排水压力变化");
                recommendations.add("制定应急预案，预置排水设备");
                break;
            case INADEQUATE:
                recommendations.add("排水能力不足，建议进行管网扩容改造");
                recommendations.add("增加临时排水泵站，提升应急排水能力");
                recommendations.add("加强管道清淤频率，确保排水畅通");
                recommendations.add("优化排水调度方案，合理分配排水负荷");
                break;
            case CRITICAL:
                recommendations.add("排水能力严重不足，需立即采取工程措施");
                recommendations.add("紧急增设排水泵站，提升排水能力");
                recommendations.add("实施雨污分流改造，减少系统负荷");
                recommendations.add("建设调蓄设施，缓解峰值排水压力");
                recommendations.add("启动应急响应，防范内涝风险");
                break;
        }

        return recommendations;
    }
}
