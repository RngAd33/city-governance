package com.cityplatform.environment.monitor;

import com.cityplatform.environment.model.EnvironmentRecord;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 气象监测服务
 * 负责气象趋势分析、异常检测
 */
@Service
public class WeatherMonitor {

    /**
     * 趋势分析结果
     */
    public record TrendAnalysis(
            String metric,
            String city,
            double avgValue,
            double minValue,
            double maxValue,
            double stdDeviation,
            TrendDirection direction,
            double changeRate,
            String summary
    ) {}

    /**
     * 异常检测结果
     */
    public record AnomalyResult(
            String metric,
            String city,
            double value,
            double expectedValue,
            double deviation,
            String description
    ) {}

    /**
     * 趋势方向枚举
     */
    public enum TrendDirection {
        RISING("上升"),
        FALLING("下降"),
        STABLE("平稳"),
        FLUCTUATING("波动");

        private final String description;

        TrendDirection(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }

    /**
     * 分析气象趋势
     * 对同一城市同一指标的时间序列数据进行统计分析
     *
     * @param records 环境记录列表（需按时间排序）
     * @return 趋势分析结果列表
     */
    public List<TrendAnalysis> analyzeWeatherTrend(List<EnvironmentRecord> records) {
        List<TrendAnalysis> results = new ArrayList<>();
        if (records == null || records.isEmpty()) return results;

        // 按城市和指标分组
        Map<String, List<EnvironmentRecord>> grouped = new LinkedHashMap<>();
        for (EnvironmentRecord record : records) {
            String key = record.getCity() + ":" + record.getMetric();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
        }

        for (Map.Entry<String, List<EnvironmentRecord>> entry : grouped.entrySet()) {
            List<EnvironmentRecord> group = entry.getValue();
            if (group.size() < 2) continue;

            // 按时间排序
            group.sort(Comparator.comparing(EnvironmentRecord::getTs));

            String[] parts = entry.getKey().split(":", 2);
            String city = parts[0];
            String metric = parts[1];

            double[] values = group.stream().mapToDouble(EnvironmentRecord::getValue).toArray();
            double avg = Arrays.stream(values).average().orElse(0);
            double min = Arrays.stream(values).min().orElse(0);
            double max = Arrays.stream(values).max().orElse(0);
            double stdDev = calculateStdDeviation(values, avg);

            // 计算趋势方向
            TrendDirection direction = determineTrendDirection(values);

            // 计算变化率
            double changeRate = calculateChangeRate(values);

            String summary = String.format(
                    "%s %s趋势: 均值=%.2f, 范围=[%.2f, %.2f], 标准差=%.2f, 变化率=%.2f%%",
                    city, direction.getDescription(), avg, min, max, stdDev, changeRate * 100
            );

            results.add(new TrendAnalysis(
                    metric, city, avg, min, max, stdDev, direction, changeRate, summary
            ));
        }
        return results;
    }

    /**
     * 检测异常读数
     * 使用3-sigma原则检测偏离均值的异常值
     *
     * @param records 环境记录列表
     * @return 异常检测结果列表
     */
    public List<AnomalyResult> detectAnomalies(List<EnvironmentRecord> records) {
        List<AnomalyResult> anomalies = new ArrayList<>();
        if (records == null || records.isEmpty()) return anomalies;

        // 按城市和指标分组
        Map<String, List<EnvironmentRecord>> grouped = new LinkedHashMap<>();
        for (EnvironmentRecord record : records) {
            String key = record.getCity() + ":" + record.getMetric();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
        }

        for (Map.Entry<String, List<EnvironmentRecord>> entry : grouped.entrySet()) {
            List<EnvironmentRecord> group = entry.getValue();
            if (group.size() < 3) continue;

            String[] parts = entry.getKey().split(":", 2);
            String city = parts[0];
            String metric = parts[1];

            double[] values = group.stream().mapToDouble(EnvironmentRecord::getValue).toArray();
            double avg = Arrays.stream(values).average().orElse(0);
            double stdDev = calculateStdDeviation(values, avg);

            if (stdDev == 0) continue;

            for (EnvironmentRecord record : group) {
                double deviation = Math.abs(record.getValue() - avg) / stdDev;
                if (deviation > 3.0) {
                    String description = String.format(
                            "%s %s异常: 当前值=%.2f, 均值=%.2f, 偏离=%.2fσ",
                            city, metric, record.getValue(), avg, deviation
                    );
                    anomalies.add(new AnomalyResult(
                            metric, city, record.getValue(), avg, deviation, description
                    ));
                }
            }
        }
        return anomalies;
    }

    /**
     * 计算标准差
     */
    private double calculateStdDeviation(double[] values, double mean) {
        if (values.length < 2) return 0;
        double sumSquaredDiff = 0;
        for (double v : values) {
            sumSquaredDiff += Math.pow(v - mean, 2);
        }
        return Math.sqrt(sumSquaredDiff / (values.length - 1));
    }

    /**
     * 判断趋势方向
     */
    private TrendDirection determineTrendDirection(double[] values) {
        if (values.length < 3) return TrendDirection.STABLE;

        // 使用线性回归斜率判断趋势
        int n = values.length;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += values[i];
            sumXY += i * values[i];
            sumX2 += i * i;
        }
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);

        // 计算变异系数判断波动性
        double mean = sumY / n;
        double stdDev = calculateStdDeviation(values, mean);
        double cv = mean != 0 ? stdDev / Math.abs(mean) : 0;

        if (cv > 0.3) return TrendDirection.FLUCTUATING;

        double threshold = Math.abs(mean) * 0.05;
        if (slope > threshold) return TrendDirection.RISING;
        if (slope < -threshold) return TrendDirection.FALLING;
        return TrendDirection.STABLE;
    }

    /**
     * 计算变化率（首尾比较）
     */
    private double calculateChangeRate(double[] values) {
        if (values.length < 2) return 0;
        double first = values[0];
        double last = values[values.length - 1];
        if (first == 0) return last == 0 ? 0 : 1.0;
        return (last - first) / Math.abs(first);
    }
}
