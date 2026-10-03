package com.cityplatform.energy.analyzer;

import com.cityplatform.energy.model.Building;
import com.cityplatform.energy.model.BuildingType;
import org.springframework.stereotype.Component;

/**
 * 建筑负荷预测器
 * 基于度日法预测建筑采暖/制冷负荷
 */
@Component
public class LoadPredictor {

    private static final double BASE_LOAD_PER_M2 = 0.05;
    private static final double HUMIDITY_FACTOR = 0.001;

    /**
     * 预测建筑采暖/制冷负荷
     * 使用度日法: load = baseLoad + area * UValue * |targetTemp - outdoorTemp| / 1000
     *
     * @param building    建筑信息
     * @param outdoorTemp 室外温度 (摄氏度)
     * @param targetTemp  目标温度 (摄氏度)
     * @param humidity    相对湿度 (0-100)
     * @return 预测负荷 (kW)
     */
    public double predictLoad(Building building, double outdoorTemp, double targetTemp, double humidity) {
        double area = building.getArea();
        double uValue = getUValue(building);
        double baseLoad = area * BASE_LOAD_PER_M2;

        double tempDiff = Math.abs(targetTemp - outdoorTemp);
        double transmissionLoad = area * uValue * tempDiff / 1000.0;

        double humidityAdjustment = 1.0 + (humidity - 50.0) * HUMIDITY_FACTOR;

        double totalLoad = (baseLoad + transmissionLoad) * humidityAdjustment;

        return Math.round(totalLoad * 100.0) / 100.0;
    }

    /**
     * 根据建筑类型和年代获取U值 (W/m2·K)
     */
    private double getUValue(Building building) {
        int currentYear = java.time.Year.now().getValue();
        int buildingAge = currentYear - building.getBuildYear();

        double baseUValue;
        switch (building.getType()) {
            case OFFICE:
                baseUValue = 0.5;
                break;
            case SCHOOL:
                baseUValue = 0.6;
                break;
            case HOSPITAL:
                baseUValue = 0.4;
                break;
            case RESIDENTIAL:
                baseUValue = 0.7;
                break;
            case COMMERCIAL:
                baseUValue = 0.55;
                break;
            default:
                baseUValue = 0.6;
        }

        if (buildingAge > 30) {
            baseUValue *= 1.3;
        } else if (buildingAge > 20) {
            baseUValue *= 1.2;
        } else if (buildingAge > 10) {
            baseUValue *= 1.1;
        }

        return baseUValue;
    }
}
