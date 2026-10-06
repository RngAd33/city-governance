package com.cityplatform.environment.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AQI空气质量指数等级枚举
 */
@Getter
@AllArgsConstructor
public enum AQILevel {

    GOOD(0, 50, "优", "绿色"),
    MODERATE(51, 100, "良", "黄色"),
    UNHEALTHY_SENSITIVE(101, 150, "轻度污染", "橙色"),
    UNHEALTHY(151, 200, "中度污染", "红色"),
    VERY_UNHEALTHY(201, 300, "重度污染", "紫色"),
    HAZARDOUS(301, Integer.MAX_VALUE, "严重污染", "褐红色");

    private final int min;
    private final int max;
    private final String description;
    private final String color;

    /**
     * 根据AQI值获取对应等级
     */
    public static AQILevel fromValue(int aqi) {
        for (AQILevel level : values()) {
            if (aqi >= level.min && aqi <= level.max) {
                return level;
            }
        }
        return HAZARDOUS;
    }

}