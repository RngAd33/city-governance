package com.cityplatform.collector.source;

import com.cityplatform.collector.config.CollectorProperties;
import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.event.EventDomain;
import com.cityplatform.common.event.EventSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Open-Meteo 空气质量数据源
 * 通过免费开放的 Open-Meteo Air Quality API 获取目标城市实况空气质量数据（无需密钥），
 * 文档: https://open-meteo.com/en/docs/air-quality-api
 */
@Component
@Slf4j
public class OpenMeteoAirQualityDataSource implements EnvironmentDataSource {

    private static final String NAME = "open-meteo-aq";

    /** 城市本地时区 */
    private static final ZoneId CITY_ZONE = ZoneId.of("Asia/Shanghai");

    /** 时区名称（作为URI模板变量传入，避免斜杠被二次编码） */
    private static final String CITY_ZONE_NAME = "Asia/Shanghai";

    /** 请求的实况指标字段 */
    private static final String CURRENT_FIELDS =
            "pm2_5,pm10,nitrogen_dioxide,ozone,sulphur_dioxide,carbon_monoxide";

    /** 浓度单位（µ 统一规范为 μ U+03BC，与 OpenAQ 源保持一致） */
    private static final String CONCENTRATION_UNIT = "\u03BCg/m\u00B3";

    /** CO 浓度单位：平台按 mg/m³ 口径存储（与AQI计算标准一致） */
    private static final String CO_UNIT = "mg/m\u00B3";

    @Resource
    private CollectorProperties properties;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private RestClient.Builder restClientBuilder;

    private RestClient restClient;

    /**
     * 按配置超时构建 HTTP 客户端（依赖注入完成后执行）
     */
    @PostConstruct
    void initRestClient() {
        CollectorProperties.OpenMeteoAirQuality config = properties.getSources().getOpenMeteoAirQuality();
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
        return EventSource.OPEN_METEO;
    }

    @Override
    public Set<DataCategory> getCategories() {
        return Set.of(DataCategory.AIR_QUALITY);
    }

    @Override
    public boolean isAvailable() {
        return properties.getSources().getOpenMeteoAirQuality().isEnabled();
    }

    @Override
    public List<CityEvent> fetch() throws Exception {
        CollectorProperties.Sources sources = properties.getSources();
        CollectorProperties.OpenMeteoAirQuality config = sources.getOpenMeteoAirQuality();

        String body = restClient.get()
                .uri(config.getBaseUrl()
                                + "?latitude={lat}&longitude={lng}&current={fields}"
                                + "&timezone={tz}",
                        sources.getLatitude(), sources.getLongitude(), CURRENT_FIELDS, CITY_ZONE_NAME)
                .retrieve()
                .body(String.class);

        JsonNode current = objectMapper.readTree(body).path("current");
        if (current.isMissingNode() || current.isEmpty()) {
            throw new IllegalStateException("Open-Meteo 空气质量响应缺少 current 字段");
        }

        OffsetDateTime ts = parseTime(current.path("time").asText(null));
        List<CityEvent> events = new ArrayList<>();
        addIfPresent(events, current, "pm2_5", "pm25", ts);
        addIfPresent(events, current, "pm10", "pm10", ts);
        addIfPresent(events, current, "nitrogen_dioxide", "no2", ts);
        addIfPresent(events, current, "ozone", "o3", ts);
        addIfPresent(events, current, "sulphur_dioxide", "so2", ts);
        // CO：Open-Meteo 返回 μg/m³，平台统一换算为 mg/m³ 存储
        addIfPresent(events, current, "carbon_monoxide", "co", CO_UNIT, 0.001, ts);

        if (events.isEmpty()) {
            throw new IllegalStateException("Open-Meteo 空气质量响应无有效指标数据");
        }
        log.info("Open-Meteo 空气质量采集成功: {} 条指标, ts={}", events.size(), ts);
        return events;
    }

    /**
     * 字段存在时追加事件（默认浓度单位，不换算）
     */
    private void addIfPresent(List<CityEvent> events, JsonNode current, String field,
                              String metric, OffsetDateTime ts) {
        addIfPresent(events, current, field, metric, CONCENTRATION_UNIT, 1.0, ts);
    }

    /**
     * 字段存在时追加事件（可指定单位与换算系数）
     */
    private void addIfPresent(List<CityEvent> events, JsonNode current, String field,
                              String metric, String unit, double scale, OffsetDateTime ts) {
        JsonNode node = current.path(field);
        if (node.isMissingNode() || node.isNull() || !node.isNumber()) return;
        CityEvent event = new CityEvent(EventDomain.ENVIRONMENT,
                properties.getSources().getCity(), metric, node.asDouble() * scale,
                unit, EventSource.OPEN_METEO);
        event.setTs(ts);
        events.add(event);
    }

    /**
     * Open-Meteo 返回本地时间（无时区偏移），按城市时区解析
     */
    private OffsetDateTime parseTime(String time) {
        if (time == null || time.isBlank()) return OffsetDateTime.now();
        try {
            return LocalDateTime.parse(time).atZone(CITY_ZONE).toOffsetDateTime();
        } catch (Exception e) {
            log.warn("Open-Meteo 空气质量时间解析失败: {}, 使用当前时间", time);
            return OffsetDateTime.now();
        }
    }
}
