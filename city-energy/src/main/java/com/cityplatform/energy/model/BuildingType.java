package com.cityplatform.energy.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 建筑类型枚举
 */
@Getter
@AllArgsConstructor
public enum BuildingType {
    OFFICE("办公楼"),
    SCHOOL("学校"),
    HOSPITAL("医院"),
    RESIDENTIAL("住宅"),
    COMMERCIAL("商业");

    private final String description;
}
