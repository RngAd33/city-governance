package com.cityplatform.collector.source;

import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.event.EventSource;

import java.util.List;
import java.util.Set;

/**
 * 环境监测数据源抽象
 * 各实现负责从外部（API/数据集/模拟器）拉取数据并转换为统一城市事件，
 * eventId、ts、location 等元数据由采集服务统一补全
 */
public interface EnvironmentDataSource {

    /**
     * 数据源名称（配置引用与REST展示用）
     */
    String getName();

    /**
     * 统一事件来源枚举
     */
    EventSource getSource();

    /**
     * 数据源支持的数据类别
     */
    Set<DataCategory> getCategories();

    /**
     * 运行时是否可用（如API Key缺失、数据源关闭时不可用）
     */
    boolean isAvailable();

    /**
     * 是否为兜底数据源（真实数据源全部失败时使用）
     */
    default boolean isFallback() {
        return false;
    }

    /**
     * 采集一轮数据，失败时抛出异常由采集服务降级处理
     */
    List<CityEvent> fetch() throws Exception;
}
