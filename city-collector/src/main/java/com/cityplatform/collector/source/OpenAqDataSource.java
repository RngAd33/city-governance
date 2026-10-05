package com.cityplatform.collector.source;

import com.cityplatform.collector.config.CollectorProperties;
import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.event.EventDomain;
import com.cityplatform.common.event.EventSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * OpenAQ 空气质量数据源
 * 通过 OpenAQ v3 API 获取城市周边监测站点的 pm25/pm10/no2/o3 实况浓度，
 * 超过最大陈旧时长的测量值视为站点停更数据、自动跳过（避免写入历史陈旧值）
 * 需在 https://explore.openaq.org 免费注册获取 API Key，文档: https://docs.openaq.org
 */
@Component
public class OpenAqDataSource implements EnvironmentDataSource {

    private static final Logger log = LoggerFactory.getLogger(OpenAqDataSource.class);

    private static final String NAME = "openaq";

    /** 城市本地时区 */
    private static final ZoneId CITY_ZONE = ZoneId.of("Asia/Shanghai");

    /** 目标污染物参数（与模拟器指标命名保持一致） */
    private static final List<String> TARGET_PARAMETERS = List.of("pm25", "pm10", "no2", "o3");

    /** 默认浓度单位（µg/m³，OpenAQ 常用 µ 符号 U+00B5，统一规范为 μ U+03BC） */
    private static final String DEFAULT_UNIT = "\u03BCg/m\u00B3";

    /** 测量值最大陈旧时长（小时），超过则视为站点已停更、数据无效 */
    private static final long MAX_MEASUREMENT_AGE_HOURS = 24;

    private final CollectorProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenAqDataSource(CollectorProperties properties,
                            ObjectMapper objectMapper,
                            RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        CollectorProperties.OpenAq config = properties.getSources().getOpenaq();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(config.getTimeoutMs());
        requestFactory.setReadTimeout(config.getTimeoutMs());
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public EventSource getSource() {
        return EventSource.OPENAQ;
    }

    @Override
    public Set<DataCategory> getCategories() {
        return Set.of(DataCategory.AIR_QUALITY);
    }

    @Override
    public boolean isAvailable() {
        CollectorProperties.OpenAq config = properties.getSources().getOpenaq();
        return config.isEnabled() && config.getApiKey() != null && !config.getApiKey().isBlank();
    }

    @Override
    public List<CityEvent> fetch() throws Exception {
        CollectorProperties.Sources sources = properties.getSources();
        CollectorProperties.OpenAq config = sources.getOpenaq();

        // 1. 查询城市周边监测站点，建立 参数名 -> 传感器ID 映射
        Map<String, Long> sensorIds = resolveSensorIds(config, sources.getLatitude(), sources.getLongitude());
        if (sensorIds.isEmpty()) {
            throw new IllegalStateException("OpenAQ 未找到包含目标污染物的监测站点");
        }

        // 2. 逐个传感器查询最新测量值
        List<CityEvent> events = new ArrayList<>();
        for (Map.Entry<String, Long> entry : sensorIds.entrySet()) {
            try {
                CityEvent event = fetchLatestMeasurement(config, entry.getKey(), entry.getValue());
                if (event != null) events.add(event);
            } catch (Exception e) {
                log.warn("OpenAQ 传感器[{}]测量值获取失败: {}", entry.getValue(), e.getMessage());
            }
        }

        if (events.isEmpty()) {
            throw new IllegalStateException("OpenAQ 未获取到任何有效测量值");
        }
        log.info("OpenAQ 采集成功: {} 条空气质量指标", events.size());
        return events;
    }

    /**
     * 查询周边站点并解析各目标参数对应的传感器ID
     */
    private Map<String, Long> resolveSensorIds(CollectorProperties.OpenAq config, double lat, double lng) throws Exception {
        String body = restClient.get()
                .uri(config.getBaseUrl() + "/locations?coordinates={lat},{lng}&radius={radius}&limit={limit}",
                        lat, lng, config.getRadiusMeters(), config.getMaxLocations())
                .header("X-API-Key", config.getApiKey())
                .retrieve()
                .body(String.class);

        JsonNode results = objectMapper.readTree(body).path("results");
        Map<String, Long> sensorIds = new LinkedHashMap<>();
        for (JsonNode location : results) {
            for (JsonNode sensor : location.path("sensors")) {
                String parameter = sensor.path("parameter").path("name").asText(null);
                if (parameter == null || !TARGET_PARAMETERS.contains(parameter)) continue;
                if (sensor.hasNonNull("id")) sensorIds.putIfAbsent(parameter, sensor.get("id").asLong());
            }
        }
        return sensorIds;
    }

    /**
     * 查询单个传感器的最新测量值并转换为城市事件
     */
    private CityEvent fetchLatestMeasurement(CollectorProperties.OpenAq config,
                                             String parameter, long sensorId) throws Exception {
        String body = restClient.get()
                .uri(config.getBaseUrl() + "/sensors/{id}/measurements?limit=1", sensorId)
                .header("X-API-Key", config.getApiKey())
                .retrieve()
                .body(String.class);

        JsonNode first = objectMapper.readTree(body).path("results").path(0);
        if (first.isMissingNode() || !first.path("value").isNumber()) return null;

        // 拒绝陈旧测量值：站点停更时会一直返回最后一笔历史数据，避免写入历史陈旧值
        OffsetDateTime ts = parseTime(first);
        if (ts.isBefore(OffsetDateTime.now().minusHours(MAX_MEASUREMENT_AGE_HOURS))) {
            log.warn("OpenAQ 传感器[{}]最新测量值过于陈旧({}), 视为站点已停更并跳过", sensorId, ts);
            return null;
        }

        CityEvent event = new CityEvent(EventDomain.ENVIRONMENT,
                properties.getSources().getCity(), parameter, first.get("value").asDouble(),
                normalizeUnit(first.path("parameter").path("units").asText(null)),
                EventSource.OPENAQ);
        event.setTs(ts);
        return event;
    }

    /**
     * 解析测量时间：优先 period.datetimeTo.local/utc，兼容顶层 datetime（对象或字符串）
     */
    private OffsetDateTime parseTime(JsonNode measurement) {
        JsonNode datetime = measurement.path("period").path("datetimeTo");
        if (datetime.isMissingNode()) datetime = measurement.path("datetime");

        String text = null;
        if (datetime.isObject()) {
            text = datetime.path("local").asText(null);
            if (text == null) text = datetime.path("utc").asText(null);
        } else if (datetime.isTextual()) {
            text = datetime.asText();
        }
        if (text == null || text.isBlank()) return OffsetDateTime.now();

        try {
            return OffsetDateTime.parse(text);
        } catch (Exception ignored) {
            // 本地时间无偏移场景，按城市时区补全
        }
        try {
            return LocalDateTime.parse(text).atZone(CITY_ZONE).toOffsetDateTime();
        } catch (Exception e) {
            log.warn("OpenAQ 时间解析失败: {}, 使用当前时间", text);
            return OffsetDateTime.now();
        }
    }

    /**
     * 单位规范化：µg/m³ -> μg/m³，缺失时使用默认值
     */
    private String normalizeUnit(String unit) {
        if (unit == null || unit.isBlank()) return DEFAULT_UNIT;
        return unit.replace("\u00B5", "\u03BC");
    }
}
