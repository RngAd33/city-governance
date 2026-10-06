package com.cityplatform.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 城市事件领域枚举
 */
@Getter
@AllArgsConstructor
public enum EventDomain {
    TRAFFIC("traffic", "交通流"),
    ENVIRONMENT("environment", "环境监测"),
    ENERGY("energy", "能源"),
    WATER("water", "水务管理"),
    DISASTER("disaster", "灾害模拟"),
    INFRA("infra", "基础设施");

    private final String code;
    private final String description;
}
