package com.cityplatform.environment.model.enums;

/**
 * 告警规则比较运算符枚举
 */
public enum Operator {
    GT(">"),
    LT("<"),
    GTE(">="),
    LTE("<=");

    private final String symbol;

    Operator(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() { return symbol; }

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
