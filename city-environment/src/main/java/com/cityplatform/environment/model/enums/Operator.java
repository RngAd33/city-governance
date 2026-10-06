package com.cityplatform.environment.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 告警规则比较运算符枚举
 */
@Getter
@AllArgsConstructor
public enum Operator {
    GT(">"),
    LT("<"),
    GTE(">="),
    LTE("<=");

    private final String symbol;

    /**
     * 根据运算符比较两个值
     */
    public boolean evaluate(double value, double threshold) {
        return switch (this) {
            case GT -> value > threshold;
            case LT -> value < threshold;
            case GTE -> value >= threshold;
            case LTE -> value <= threshold;
        };
    }
}
