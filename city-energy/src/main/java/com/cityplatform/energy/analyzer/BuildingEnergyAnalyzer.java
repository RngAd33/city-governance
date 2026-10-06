package com.cityplatform.energy.analyzer;

import com.cityplatform.energy.model.Building;
import com.cityplatform.energy.model.BuildingType;
import com.cityplatform.energy.model.EnergyConsumptionRecord;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 建筑能耗分析器
 * 提供建筑能耗分析、高能耗建筑识别、节能潜力计算等功能
 */
@Service
public class BuildingEnergyAnalyzer {

    private static final double ELECTRICITY_PRICE = 0.8;
    private static final double HEATING_PRICE = 0.5;
    private static final double COOLING_PRICE = 0.6;

    /**
     * 分析建筑能耗情况
     */
    public EnergyAnalysisResult analyzeBuilding(Building building, List<EnergyConsumptionRecord> records) {
        EnergyAnalysisResult result = new EnergyAnalysisResult();
        result.setBuildingId(building.getBuildingId());

        if (records == null || records.isEmpty()) {
            result.setAvgDailyConsumption(0.0);
            result.setPeakConsumption(0.0);
            result.setEnergyIntensity(0.0);
            result.setCarbonFootprint(0.0);
            result.setEfficiencyRating("N/A");
            result.setAnomalyDetected(false);
            result.setAnomalyDescription("无能耗数据");
            return result;
        }

        double totalConsumption = 0.0;
        double peakConsumption = 0.0;
        double totalCarbon = 0.0;

        for (EnergyConsumptionRecord record : records) {
            double total = record.getTotalKwh();
            totalConsumption += total;
            totalCarbon += record.getCarbonEmission();
            if (total > peakConsumption) {
                peakConsumption = total;
            }
        }

        int days = Math.max(1, records.size());
        double avgDaily = totalConsumption / days;
        double energyIntensity = (avgDaily * 365) / building.getArea();

        result.setAvgDailyConsumption(avgDaily);
        result.setPeakConsumption(peakConsumption);
        result.setEnergyIntensity(energyIntensity);
        result.setCarbonFootprint(totalCarbon);
        result.setEfficiencyRating(calculateEfficiencyRating(energyIntensity, building.getType()));

        boolean anomalyDetected = detectAnomaly(records, avgDaily);
        result.setAnomalyDetected(anomalyDetected);
        result.setAnomalyDescription(anomalyDetected ? generateAnomalyDescription(records, avgDaily) : "能耗正常");

        return result;
    }

    /**
     * 识别高能耗建筑
     */
    public List<Building> identifyHighConsumptionBuildings(List<Building> buildings, double threshold) {
        List<Building> highConsumptionBuildings = new ArrayList<>();
        for (Building building : buildings) {
            if (building.getAvgDailyConsumption() != null && building.getAvgDailyConsumption() > threshold) {
                highConsumptionBuildings.add(building);
            }
        }
        return highConsumptionBuildings;
    }

    /**
     * 计算节能潜力
     */
    public EnergySavingPotential calculateEnergySavingPotential(Building building) {
        EnergySavingPotential potential = new EnergySavingPotential();

        double currentConsumption = building.getAvgDailyConsumption() != null ? building.getAvgDailyConsumption() : 0.0;
        double savingRate = estimateSavingRate(building);
        double estimatedSavingKwh = currentConsumption * savingRate * 365;

        double estimatedSavingCost = estimatedSavingKwh * ELECTRICITY_PRICE;
        double estimatedCarbonReduction = estimatedSavingKwh * building.getCarbonFactor();

        potential.setEstimatedSavingKwh(estimatedSavingKwh);
        potential.setEstimatedSavingCost(estimatedSavingCost);
        potential.setEstimatedCarbonReduction(estimatedCarbonReduction);
        potential.setRecommendations(generateRecommendations(building, savingRate));

        return potential;
    }

    private String calculateEfficiencyRating(double energyIntensity, BuildingType type) {
        double thresholdA = getThresholdForType(type, 0);
        double thresholdB = getThresholdForType(type, 1);
        double thresholdC = getThresholdForType(type, 2);
        double thresholdD = getThresholdForType(type, 3);

        if (energyIntensity <= thresholdA) return "A";
        if (energyIntensity <= thresholdB) return "B";
        if (energyIntensity <= thresholdC) return "C";
        if (energyIntensity <= thresholdD) return "D";
        return "E";
    }

    private double getThresholdForType(BuildingType type, int level) {
        double base;
        switch (type) {
            case OFFICE: base = 80; break;
            case SCHOOL: base = 50; break;
            case HOSPITAL: base = 150; break;
            case RESIDENTIAL: base = 30; break;
            case COMMERCIAL: base = 100; break;
            default: base = 60;
        }
        return base * (1 + level * 0.5);
    }

    private boolean detectAnomaly(List<EnergyConsumptionRecord> records, double avgDaily) {
        if (records.size() < 3) return false;
        double variance = 0.0;
        for (EnergyConsumptionRecord record : records) {
            double diff = record.getTotalKwh() - avgDaily;
            variance += diff * diff;
        }
        double stdDev = Math.sqrt(variance / records.size());
        for (EnergyConsumptionRecord record : records) {
            if (Math.abs(record.getTotalKwh() - avgDaily) > 3 * stdDev) {
                return true;
            }
        }
        return false;
    }

    private String generateAnomalyDescription(List<EnergyConsumptionRecord> records, double avgDaily) {
        int anomalyCount = 0;
        for (EnergyConsumptionRecord record : records) {
            if (record.getTotalKwh() > avgDaily * 1.5) {
                anomalyCount++;
            }
        }
        return String.format("检测到%d条异常高能耗记录，超过均值50%%", anomalyCount);
    }

    private double estimateSavingRate(Building building) {
        int currentYear = java.time.Year.now().getValue();
        int buildingAge = currentYear - building.getBuildYear();
        double baseRate = 0.15;
        if (buildingAge > 30) baseRate += 0.10;
        else if (buildingAge > 20) baseRate += 0.07;
        else if (buildingAge > 10) baseRate += 0.05;
        if (building.getType() == BuildingType.HOSPITAL) baseRate += 0.05;
        return Math.min(baseRate, 0.40);
    }

    private List<String> generateRecommendations(Building building, double savingRate) {
        List<String> recommendations = new ArrayList<>();
        int currentYear = java.time.Year.now().getValue();
        int buildingAge = currentYear - building.getBuildYear();

        if (buildingAge > 20) {
            recommendations.add("建议进行建筑围护结构保温改造，降低采暖和制冷能耗");
        }
        if (building.getType() == BuildingType.OFFICE || building.getType() == BuildingType.COMMERCIAL) {
            recommendations.add("建议升级LED照明系统并安装智能照明控制，预计照明节能30%以上");
            recommendations.add("建议优化空调系统运行策略，实施分时段温度控制");
        }
        if (building.getType() == BuildingType.HOSPITAL) {
            recommendations.add("建议对医疗设备进行能耗监测与管理，优化设备运行时间表");
            recommendations.add("建议采用热回收技术，回收空调废热用于生活热水预热");
        }
        if (building.getType() == BuildingType.RESIDENTIAL) {
            recommendations.add("建议推广使用高效节能家电，引导居民错峰用电");
        }
        if (building.getType() == BuildingType.SCHOOL) {
            recommendations.add("建议加强师生节能意识教育，建立校园能源管理制度");
        }
        if (savingRate > 0.20) {
            recommendations.add("建议安装建筑能源管理系统(BEMS)，实现能耗实时监测与优化控制");
            recommendations.add("建议进行能源审计，识别具体节能改造机会");
        }
        recommendations.add("建议定期开展能耗数据分析，持续优化能源管理策略");

        return recommendations;
    }

    /**
     * 能耗分析结果
     */
    @Data
    public static class EnergyAnalysisResult {
        private String buildingId;
        private Double avgDailyConsumption;
        private Double peakConsumption;
        private Double energyIntensity;
        private Double carbonFootprint;
        private String efficiencyRating;
        private Boolean anomalyDetected;
        private String anomalyDescription;
    }

    /**
     * 节能潜力
     */
    @Data
    public static class EnergySavingPotential {
        private Double estimatedSavingKwh;
        private Double estimatedSavingCost;
        private Double estimatedCarbonReduction;
        private List<String> recommendations;
    }
}
