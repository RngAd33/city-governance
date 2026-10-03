package com.cityplatform.collector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 采集服务配置
 * 对应 application.yml 中 collector.* 配置前缀
 */
@Component
@ConfigurationProperties(prefix = "collector")
public class CollectorProperties {

    /** 模拟器配置 */
    private final Simulator simulator = new Simulator();

    /** 环境数据源配置 */
    private final Sources sources = new Sources();

    /** 环境采集调度配置 */
    private final Environment environment = new Environment();

    public Simulator getSimulator() { return simulator; }

    public Sources getSources() { return sources; }

    public Environment getEnvironment() { return environment; }

    /**
     * 模拟器配置
     */
    public static class Simulator {

        /** 模拟器是否生成环境（气象/空气质量）数据，关闭后环境数据完全由外部数据源提供 */
        private boolean environmentEnabled = true;

        public boolean isEnvironmentEnabled() { return environmentEnabled; }

        public void setEnvironmentEnabled(boolean environmentEnabled) { this.environmentEnabled = environmentEnabled; }
    }

    /**
     * 环境数据源配置
     */
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

        private final OpenAq openaq = new OpenAq();

        public String getCity() { return city; }

        public void setCity(String city) { this.city = city; }

        public double getLatitude() { return latitude; }

        public void setLatitude(double latitude) { this.latitude = latitude; }

        public double getLongitude() { return longitude; }

        public void setLongitude(double longitude) { this.longitude = longitude; }

        public String getAddress() { return address; }

        public void setAddress(String address) { this.address = address; }

        public OpenMeteo getOpenMeteo() { return openMeteo; }

        public OpenAq getOpenaq() { return openaq; }
    }

    /**
     * Open-Meteo 气象 API 配置
     * 免费开放接口，无需密钥，文档: https://open-meteo.com/en/docs
     */
    public static class OpenMeteo {

        /** 是否启用 */
        private boolean enabled = true;

        /** API 基础地址 */
        private String baseUrl = "https://api.open-meteo.com/v1/forecast";

        /** HTTP 超时（毫秒） */
        private int timeoutMs = 10000;

        public boolean isEnabled() { return enabled; }

        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getBaseUrl() { return baseUrl; }

        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public int getTimeoutMs() { return timeoutMs; }

        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    }

    /**
     * OpenAQ 空气质量 API 配置
     * 需要 API Key，可在 https://explore.openaq.org 免费注册
     */
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

        public boolean isEnabled() { return enabled; }

        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getBaseUrl() { return baseUrl; }

        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public String getApiKey() { return apiKey; }

        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public int getRadiusMeters() { return radiusMeters; }

        public void setRadiusMeters(int radiusMeters) { this.radiusMeters = radiusMeters; }

        public int getMaxLocations() { return maxLocations; }

        public void setMaxLocations(int maxLocations) { this.maxLocations = maxLocations; }

        public int getTimeoutMs() { return timeoutMs; }

        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    }

    /**
     * 环境数据采集调度配置
     */
    public static class Environment {

        /** 气象数据采集间隔（毫秒，默认10分钟） */
        private long weatherIntervalMs = 600_000L;

        /** 空气质量数据采集间隔（毫秒，默认30分钟） */
        private long airQualityIntervalMs = 1_800_000L;

        public long getWeatherIntervalMs() { return weatherIntervalMs; }

        public void setWeatherIntervalMs(long weatherIntervalMs) { this.weatherIntervalMs = weatherIntervalMs; }

        public long getAirQualityIntervalMs() { return airQualityIntervalMs; }

        public void setAirQualityIntervalMs(long airQualityIntervalMs) { this.airQualityIntervalMs = airQualityIntervalMs; }
    }
}
