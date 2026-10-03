package com.cityplatform.environment.model.enums;

/**
 * 环境指标等级枚举
 */
public enum EnvironmentLevel {
    NORMAL("正常"),
    WARNING("警告"),
    DANGER("危险");

    private final String description;

    EnvironmentLevel(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}
