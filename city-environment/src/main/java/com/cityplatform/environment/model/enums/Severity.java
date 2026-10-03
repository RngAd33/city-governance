package com.cityplatform.environment.model.enums;

/**
 * 告警严重级别枚举
 */
public enum Severity {
    INFO("信息"),
    WARNING("警告"),
    CRITICAL("严重");

    private final String description;

    Severity(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
