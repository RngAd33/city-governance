package com.cityplatform.common.event;

/**
 * 数据源枚举
 */
public enum EventSource {
    OPEN_METEO("open-meteo", "Open-Meteo气象API"),
    OPENAQ("openaq", "OpenAQ空气质量API"),
    QWEATHER("qweather", "和风天气"),
    OWID("owid", "Our World in Data开放数据"),
    DATASET("dataset", "公开数据集"),
    API("api", "政务开放API"),
    SIMULATOR("simulator", "模拟器");

    private final String code;
    private final String description;

    EventSource(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }
}
