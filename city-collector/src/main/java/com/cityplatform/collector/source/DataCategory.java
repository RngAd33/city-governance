package com.cityplatform.collector.source;

/**
 * 环境数据类别
 */
public enum DataCategory {

    WEATHER("weather", "气象"),
    AIR_QUALITY("airquality", "空气质量");

    private final String code;
    private final String description;

    DataCategory(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }

    public String getDescription() { return description; }

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
