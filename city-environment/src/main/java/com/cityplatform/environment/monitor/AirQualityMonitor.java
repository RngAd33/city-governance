package com.cityplatform.environment.monitor;

import com.cityplatform.environment.model.AlertRecord;
import com.cityplatform.environment.model.AlertRule;
import com.cityplatform.environment.model.enums.AQILevel;
import com.cityplatform.environment.model.enums.EnvironmentLevel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 空气质量监测服务
 * 负责AQI计算、空气质量等级判定、告警规则匹配
 */
@Service
public class AirQualityMonitor {

    /**
     * 空气质量指数计算结果
     */
    public record AQIResult(int aqi, AQILevel level, String primaryPollutant) {}

    /**
     * 根据六项污染物浓度计算AQI
     * 采用标准AQI分段线性插值法，取各污染物IAQI最大值作为最终AQI
     *
     * @param pm25 PM2.5浓度 (ug/m3)
     * @param pm10  PM10浓度 (ug/m3)
     * @param no2   NO2浓度 (ug/m3)
     * @param o3    O3浓度 (ug/m3)
     * @param co    CO浓度 (mg/m3)
     * @param so2   SO2浓度 (ug/m3)
     * @return AQI计算结果
     */
    public AQIResult calculateAQI(double pm25, double pm10, double no2,
                                   double o3, double co, double so2) {
        int iaqiPm25 = calculateIAQI_PM25(pm25);
        int iaqiPm10 = calculateIAQI_PM10(pm10);
        int iaqiNo2 = calculateIAQI_NO2(no2);
        int iaqiO3 = calculateIAQI_O3(o3);
        int iaqiCo = calculateIAQI_CO(co);
        int iaqiSo2 = calculateIAQI_SO2(so2);

        int maxIaqi = iaqiPm25;
        String primary = "PM2.5";

        if (iaqiPm10 > maxIaqi) { maxIaqi = iaqiPm10; primary = "PM10"; }
        if (iaqiNo2 > maxIaqi) { maxIaqi = iaqiNo2; primary = "NO2"; }
        if (iaqiO3 > maxIaqi) { maxIaqi = iaqiO3; primary = "O3"; }
        if (iaqiCo > maxIaqi) { maxIaqi = iaqiCo; primary = "CO"; }
        if (iaqiSo2 > maxIaqi) { maxIaqi = iaqiSo2; primary = "SO2"; }

        AQILevel level = AQILevel.fromValue(maxIaqi);
        return new AQIResult(maxIaqi, level, primary);
    }

    /**
     * 根据AQI值获取环境等级
     */
    public EnvironmentLevel getEnvironmentLevel(int aqi) {
        if (aqi <= 100) return EnvironmentLevel.NORMAL;
        if (aqi <= 200) return EnvironmentLevel.WARNING;
        return EnvironmentLevel.DANGER;
    }

    /**
     * 检查告警规则，返回触发的告警记录列表
     *
     * @param rules  启用的告警规则列表
     * @param metric 指标名称
     * @param value  当前值
     * @return 触发的告警记录列表
     */
    public List<AlertRecord> checkAlertRules(List<AlertRule> rules, String metric, double value) {
        List<AlertRecord> alerts = new ArrayList<>();
        if (rules == null || rules.isEmpty()) return alerts;

        for (AlertRule rule : rules) {
            if (!Boolean.TRUE.equals(rule.getEnabled())) continue;
            // 指标名归一化后对比，兼容规则使用 "PM2.5" 等展示名而事件使用小写规范名（pm25）的情况
            if (!normalizeMetric(rule.getMetric()).equals(normalizeMetric(metric))) continue;

            if (rule.getOperator().evaluate(value, rule.getThreshold())) {
                String message = formatAlertMessage(rule, value);
                AlertRecord alert = new AlertRecord(
                        UUID.randomUUID().toString(),
                        rule.getRuleId(),
                        metric,
                        value,
                        rule.getThreshold(),
                        rule.getSeverity(),
                        message,
                        null,
                        null
                );
                alerts.add(alert);
            }
        }
        return alerts;
    }

    /**
     * 指标名归一化
     * 将展示名变体（如 "PM2.5"、"PM 2.5"）统一转换为存储规范小写名称（如 pm25），
     * 用于规则指标与事件指标的匹配
     */
    private String normalizeMetric(String metric) {
        if (metric == null) return null;
        return metric.toLowerCase(Locale.ROOT).replace(" ", "").replace(".", "");
    }

    /**
     * 格式化告警消息
     */
    private String formatAlertMessage(AlertRule rule, double value) {
        String template = rule.getMessageTemplate();
        if (template == null || template.isBlank()) {
            return String.format("指标 %s 当前值 %.2f 触发规则 %s (阈值 %.2f)",
                    rule.getMetric(), value, rule.getName(), rule.getThreshold());
        }
        return template.replace("{metric}", rule.getMetric())
                .replace("{value}", String.format("%.2f", value))
                .replace("{threshold}", String.format("%.2f", rule.getThreshold()))
                .replace("{operator}", rule.getOperator().getSymbol())
                .replace("{ruleName}", rule.getName());
    }

    // ==================== IAQI分段线性插值计算 ====================

    private int calculateIAQI_PM25(double concentration) {
        if (concentration <= 0) return 0;
        if (concentration <= 35) return linearInterpolate(concentration, 0, 35, 0, 50);
        if (concentration <= 75) return linearInterpolate(concentration, 35, 75, 50, 100);
        if (concentration <= 115) return linearInterpolate(concentration, 75, 115, 100, 150);
        if (concentration <= 150) return linearInterpolate(concentration, 115, 150, 150, 200);
        if (concentration <= 250) return linearInterpolate(concentration, 150, 250, 200, 300);
        if (concentration <= 350) return linearInterpolate(concentration, 250, 350, 300, 400);
        if (concentration <= 500) return linearInterpolate(concentration, 350, 500, 400, 500);
        return 500;
    }

    private int calculateIAQI_PM10(double concentration) {
        if (concentration <= 0) return 0;
        if (concentration <= 50) return linearInterpolate(concentration, 0, 50, 0, 50);
        if (concentration <= 150) return linearInterpolate(concentration, 50, 150, 50, 100);
        if (concentration <= 250) return linearInterpolate(concentration, 150, 250, 100, 150);
        if (concentration <= 350) return linearInterpolate(concentration, 250, 350, 150, 200);
        if (concentration <= 420) return linearInterpolate(concentration, 350, 420, 200, 300);
        if (concentration <= 500) return linearInterpolate(concentration, 420, 500, 300, 400);
        if (concentration <= 600) return linearInterpolate(concentration, 500, 600, 400, 500);
        return 500;
    }

    private int calculateIAQI_NO2(double concentration) {
        if (concentration <= 0) return 0;
        if (concentration <= 40) return linearInterpolate(concentration, 0, 40, 0, 50);
        if (concentration <= 80) return linearInterpolate(concentration, 40, 80, 50, 100);
        if (concentration <= 180) return linearInterpolate(concentration, 80, 180, 100, 150);
        if (concentration <= 280) return linearInterpolate(concentration, 180, 280, 150, 200);
        if (concentration <= 565) return linearInterpolate(concentration, 280, 565, 200, 300);
        if (concentration <= 750) return linearInterpolate(concentration, 565, 750, 300, 400);
        if (concentration <= 940) return linearInterpolate(concentration, 750, 940, 400, 500);
        return 500;
    }

    private int calculateIAQI_O3(double concentration) {
        if (concentration <= 0) return 0;
        if (concentration <= 100) return linearInterpolate(concentration, 0, 100, 0, 50);
        if (concentration <= 160) return linearInterpolate(concentration, 100, 160, 50, 100);
        if (concentration <= 215) return linearInterpolate(concentration, 160, 215, 100, 150);
        if (concentration <= 265) return linearInterpolate(concentration, 215, 265, 150, 200);
        if (concentration <= 800) return linearInterpolate(concentration, 265, 800, 200, 300);
        return 500;
    }

    private int calculateIAQI_CO(double concentration) {
        if (concentration <= 0) return 0;
        if (concentration <= 2) return linearInterpolate(concentration, 0, 2, 0, 50);
        if (concentration <= 4) return linearInterpolate(concentration, 2, 4, 50, 100);
        if (concentration <= 14) return linearInterpolate(concentration, 4, 14, 100, 150);
        if (concentration <= 24) return linearInterpolate(concentration, 14, 24, 150, 200);
        if (concentration <= 36) return linearInterpolate(concentration, 24, 36, 200, 300);
        if (concentration <= 48) return linearInterpolate(concentration, 36, 48, 300, 400);
        if (concentration <= 60) return linearInterpolate(concentration, 48, 60, 400, 500);
        return 500;
    }

    private int calculateIAQI_SO2(double concentration) {
        if (concentration <= 0) return 0;
        if (concentration <= 50) return linearInterpolate(concentration, 0, 50, 0, 50);
        if (concentration <= 150) return linearInterpolate(concentration, 50, 150, 50, 100);
        if (concentration <= 475) return linearInterpolate(concentration, 150, 475, 100, 150);
        if (concentration <= 800) return linearInterpolate(concentration, 475, 800, 150, 200);
        if (concentration <= 1600) return linearInterpolate(concentration, 800, 1600, 200, 300);
        if (concentration <= 2100) return linearInterpolate(concentration, 1600, 2100, 300, 400);
        if (concentration <= 2620) return linearInterpolate(concentration, 2100, 2620, 400, 500);
        return 500;
    }

    /**
     * 分段线性插值
     * IAQI = (IAQI_high - IAQI_low) / (BP_high - BP_low) * (C - BP_low) + IAQI_low
     */
    private int linearInterpolate(double c, double bpLow, double bpHigh,
                                   int iaqiLow, int iaqiHigh) {
        if (bpHigh == bpLow) return iaqiHigh;
        double iaqi = (double) (iaqiHigh - iaqiLow) / (bpHigh - bpLow) * (c - bpLow) + iaqiLow;
        return (int) Math.round(iaqi);
    }
}
