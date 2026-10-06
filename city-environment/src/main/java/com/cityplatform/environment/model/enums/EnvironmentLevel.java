package com.cityplatform.environment.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 环境指标等级枚举
 */
@Getter
@AllArgsConstructor
public enum EnvironmentLevel {
    NORMAL("正常"),
    WARNING("警告"),
    DANGER("危险");

    private final String description;
}
