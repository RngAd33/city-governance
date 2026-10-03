package com.cityplatform.energy.model;

/**
 * 建筑类型枚举
 */
public enum BuildingType {
    OFFICE("办公楼"),
    SCHOOL("学校"),
    HOSPITAL("医院"),
    RESIDENTIAL("住宅"),
    COMMERCIAL("商业");

    private final String description;

    BuildingType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
