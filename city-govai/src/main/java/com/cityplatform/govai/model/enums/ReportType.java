package com.cityplatform.govai.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 政务报告类型
 * 支持中文名称与英文代码双向交互（生成/筛选请求传中文或英文均可）
 */
@Getter
@AllArgsConstructor
public enum ReportType {

    DAILY("日报", "DAILY"),
    WEEKLY("周报", "WEEKLY"),
    BRIEF("简报", "BRIEF"),
    RISK_ALERT("风险提示", "RISK_ALERT");

    /** 中文标准名称（入库与展示） */
    private final String label;

    /** 英文代码（前端枚举交互） */
    private final String code;

    /**
     * 规范化解析：支持英文代码（忽略大小写）与中文名称（含模糊包含）
     *
     * @param raw 请求中的类型值，可为 null
     * @return 匹配的报告类型；无法识别时返回 null
     */
    public static ReportType from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        // 先精确匹配：英文代码忽略大小写 / 中文名称完全一致
        for (ReportType type : values()) {
            if (type.code.equalsIgnoreCase(value) || type.label.equals(value)) {
                return type;
            }
        }
        // 再模糊匹配：如"城市运行日报"→日报、"风险预警提示"→风险提示
        for (ReportType type : values()) {
            if (value.contains(type.label)) {
                return type;
            }
        }
        if (value.contains("风险") || value.contains("预警")) {
            return RISK_ALERT;
        }
        return null;
    }
}
