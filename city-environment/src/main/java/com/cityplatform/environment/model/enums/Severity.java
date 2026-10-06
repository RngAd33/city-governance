package com.cityplatform.environment.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 告警严重级别枚举
 */
@Getter
@AllArgsConstructor
public enum Severity {
    INFO("信息"),
    WARNING("警告"),
    CRITICAL("严重");

    private final String description;
}
