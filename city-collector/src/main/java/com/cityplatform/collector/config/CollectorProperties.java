package com.cityplatform.collector.config;

import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 采集服务配置
 * 对应 application.yml 中 collector.* 配置前缀
 */
@Getter
@Component
@ConfigurationProperties(prefix = "collector")
public class CollectorProperties {

    /** 模拟器配置 */
    private final Simulator simulator = new Simulator();

    /** 环境数据源配置 */
    private final Sources sources = new Sources();

    /** 环境采集调度配置 */
    private final Environment environment = new Environment();

    /** 能源电网数据采集调度配置 */
    private final Energy energy = new Energy();

    /** 洪水预报（长江径流量）采集调度配置 */
    private final Flood flood = new Flood();

    /** 水文站点水位采集调度配置 */
    private final Water water = new Water();

    /**
     * 模拟器配置
     */
    @Data
    public static class Simulator {

        /** 模拟器是否生成环境（气象/空气质量）数据，关闭后环境数据完全由外部数据源提供 */
        private boolean environmentEnabled = true;
    }

    /**
     * 环境数据源配置
     */
    @Data
    public static class Sources {

        /** 目标城市名称 */
        private String city = "南京";

        /** 目标城市纬度 */
        private double latitude = 32.06;

        /** 目标城市经度 */
        private double longitude = 118.79;

        /** 目标城市地址描述 */
        private String address = "江苏省南京市";

        private final OpenMeteo openMeteo = new OpenMeteo();

        private final OpenMeteoAirQuality openMeteoAirQuality = new OpenMeteoAirQuality();

        private final OpenAq openaq = new OpenAq();

        private final OwidChina owidChina = new OwidChina();

        private final OpenMeteoFlood openMeteoFlood = new OpenMeteoFlood();
    }

    /**
     * Open-Meteo 气象 API 配置
     * 免费开放接口，无需密钥，文档: https://open-meteo.com/en/docs
     */
    @Data
    public static class OpenMeteo {

        /** 是否启用 */
        private boolean enabled = true;

        /** API 基础地址 */
        private String baseUrl = "https://api.open-meteo.com/v1/forecast";

        /** HTTP 超时（毫秒） */
        private int timeoutMs = 10000;
    }

    /**
     * Open-Meteo 空气质量 API 配置
     * 免费开放接口，无需密钥，文档: https://open-meteo.com/en/docs/air-quality-api
     */
    @Data
    public static class OpenMeteoAirQuality {

        /** 是否启用 */
        private boolean enabled = true;

        /** API 基础地址 */
        private String baseUrl = "https://air-quality-api.open-meteo.com/v1/air-quality";

        /** HTTP 超时（毫秒） */
        private int timeoutMs = 10000;
    }

    /**
     * OpenAQ 空气质量 API 配置
     * 需要 API Key，可在 https://explore.openaq.org 免费注册
     */
    @Data
    public static class OpenAq {

        /** 是否启用 */
        private boolean enabled = true;

        /** API 基础地址 */
        private String baseUrl = "https://api.openaq.org/v3";

        /** API Key，未配置时该数据源不可用 */
        private String apiKey = "";

        /** 站点搜索半径（米，OpenAQ 上限 25000） */
        private int radiusMeters = 25000;

        /** 站点查询数量上限 */
        private int maxLocations = 10;

        /** HTTP 超时（毫秒） */
        private int timeoutMs = 10000;
    }

    /**
     * Our World in Data 中国电力数据配置（数据源 Ember，免费无密钥）
     * 年度粒度数据（碳强度/可再生占比），CSV 直链下载解析，
     * 数据: https://ourworldindata.org/grapher/carbon-intensity-electricity
     */
    @Data
    public static class OwidChina {

        /** 是否启用 */
        private boolean enabled = true;

        /** Grapher CSV 基础地址 */
        private String baseUrl = "https://ourworldindata.org/grapher";

        /** HTTP 超时（毫秒，CSV 文件较大需放宽） */
        private int timeoutMs = 30000;
    }

    /**
     * Open-Meteo 洪水预报 API 配置（GloFAS 全球洪水模型，免费开放无需密钥）
     * 提供目标坐标所在河段的日粒度径流量（含7天预报），
     * 南京采样点取长江大桥江段（覆盖长江干流格点）；
     * 文档: https://open-meteo.com/en/docs/flood-api
     */
    @Data
    public static class OpenMeteoFlood {

        /** 是否启用 */
        private boolean enabled = true;

        /** API 基础地址 */
        private String baseUrl = "https://flood-api.open-meteo.com/v1/flood";

        /** 长江南京段采样纬度（南京长江大桥江段） */
        private double latitude = 32.116;

        /** 长江南京段采样经度（南京长江大桥江段） */
        private double longitude = 118.736;

        /** HTTP 超时（毫秒） */
        private int timeoutMs = 10000;
    }

    /**
     * 环境数据采集调度配置
     */
    @Data
    public static class Environment {

        /** 气象数据采集间隔（毫秒，默认10分钟） */
        private long weatherIntervalMs = 600_000L;

        /** 空气质量数据采集间隔（毫秒，默认30分钟） */
        private long airQualityIntervalMs = 1_800_000L;
    }

    /**
     * 能源电网数据采集调度配置
     */
    @Data
    public static class Energy {

        /** 中国 OWID 数据采集间隔（毫秒，默认24小时，年度数据无需高频采集） */
        private long chinaIntervalMs = 86_400_000L;

        /** 中国 OWID 首次采集延迟（毫秒） */
        private long chinaInitialDelayMs = 60_000L;
    }

    /**
     * 洪水预报（长江径流量）采集调度配置
     */
    @Data
    public static class Flood {

        /** 采集间隔（毫秒，默认1小时；GloFAS为日粒度预报无需更高频） */
        private long intervalMs = 3_600_000L;

        /** 首次采集延迟（毫秒） */
        private long initialDelayMs = 60_000L;
    }

    /**
     * 水文站点水位采集调度配置
     * 南京各站点水位由真实降水量与长江径流量经水量平衡模型推算，非随机模拟
     */
    @Data
    public static class Water {

        /** 采集间隔（毫秒，默认10分钟） */
        private long intervalMs = 600_000L;

        /** 首次采集延迟（毫秒，晚于洪水预报首采，便于使用最新径流量） */
        private long initialDelayMs = 90_000L;

        /** 长江径流量参照值（m³/s，枯水~丰水年内的典型中位水平，用于水位径流调节项） */
        private double riverReferenceDischarge = 30000.0;

        /** 南京水文站点配置（与 water_station 种子一致，水位由真实降水量与长江径流量推算） */
        private List<Station> stations = new ArrayList<>();

        /**
         * 水文站点定义
         */
        @Data
        public static class Station {

            /** 站号（与 city-water 库 water_station.station_id 一致，如 WS001） */
            private String id;

            /** 站点名称 */
            private String name;

            /** 站点类型（RIVER/LAKE/RESERVOIR/DRAINAGE） */
            private String type = "RIVER";

            /** 纬度 */
            private double latitude;

            /** 经度 */
            private double longitude;

            /** 基线水位（米，枯水常态水位） */
            private double baselineLevel;

            /** 最大容量水位（米，与 water_station.max_capacity 一致） */
            private double maxLevel;

            /** 汇水系数（站点汇水区相对影响倍数，反映集水面积大小） */
            private double catchmentFactor = 1.0;

            /** 消退速率（每周期水位向基线回落的比例） */
            private double recessionRate = 0.02;
        }
    }
}
