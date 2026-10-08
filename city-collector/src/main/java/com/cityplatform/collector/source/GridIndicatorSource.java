package com.cityplatform.collector.source;

import com.cityplatform.common.event.CityEvent;

import java.util.List;

/**
 * 电网指标数据源接口
 * 实现类均对接免费公开的电网数据源（无需密钥），
 * 由 EnergyGridCollectionService 统一编排采集与发布
 */
public interface GridIndicatorSource {

    /** 数据源名称（与配置项一致） */
    String getName();

    /** 电网分区代码（当前仅 CN 中国，全国年度口径） */
    String getZone();

    /** 数据源基础地址（供状态接口展示） */
    String getBaseUrl();

    /** 运行时是否可用（仅受配置开关控制） */
    boolean isAvailable();

    /** 采集一轮指标事件，失败时抛出异常 */
    List<CityEvent> fetch() throws Exception;
}
