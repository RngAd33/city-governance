package com.cityplatform.collector.source;

import com.cityplatform.common.event.CityEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据质量校验器
 * 对环境监测数据进行清洗：过滤缺失值与超出合理范围的异常值
 */
@Component
public class DataQualityValidator {

    private static final Logger log = LoggerFactory.getLogger(DataQualityValidator.class);

    /**
     * 各指标合理取值范围 [最小值, 最大值]（单位统一后的口径）
     */
    private static final Map<String, double[]> METRIC_RANGES = Map.of(
            "temperature", new double[]{-60, 60},
            "humidity", new double[]{0, 100},
            "wind_speed", new double[]{0, 120},
            "precipitation", new double[]{0, 500},
            "pm25", new double[]{0, 1000},
            "pm10", new double[]{0, 2000},
            "no2", new double[]{0, 2000},
            "o3", new double[]{0, 2000}
    );

    /**
     * 过滤无效事件，返回通过校验的数据
     */
    public List<CityEvent> filterInvalid(List<CityEvent> events) {
        List<CityEvent> valid = new ArrayList<>();
        if (events == null) return valid;

        for (CityEvent event : events) {
            if (isValid(event)) {
                valid.add(event);
            } else {
                log.warn("数据清洗过滤异常值: metric={}, value={}, unit={}",
                        event.getMetric(), event.getValue(), event.getUnit());
            }
        }
        return valid;
    }

    /**
     * 校验单个事件的指标值是否在合理范围内
     */
    private boolean isValid(CityEvent event) {
        if (event == null || event.getMetric() == null || event.getValue() == null) return false;
        if (event.getValue().isNaN() || event.getValue().isInfinite()) return false;

        double[] range = METRIC_RANGES.get(event.getMetric());
        if (range == null) return true;
        return event.getValue() >= range[0] && event.getValue() <= range[1];
    }
}
