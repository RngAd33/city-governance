package com.cityplatform.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据源枚举
 */
@Getter
@AllArgsConstructor
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
}
