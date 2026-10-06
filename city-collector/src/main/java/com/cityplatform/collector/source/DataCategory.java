package com.cityplatform.collector.source;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 环境数据类别
 */
@Getter
@AllArgsConstructor
public enum DataCategory {

    WEATHER("weather", "气象"),
    AIR_QUALITY("airquality", "空气质量");

    private final String code;
    private final String description;

    /**
     * 根据编码解析类别，无法识别时返回 null
     */
    public static DataCategory fromCode(String code) {
        if (code == null) return null;
        for (DataCategory category : values()) {
            if (category.code.equalsIgnoreCase(code)) return category;
        }
        return null;
    }
}
