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
import java.util.List;
import java.util.Set;

/**
 * Open-Meteo 气象数据源
 * 通过免费开放的 Open-Meteo API 获取目标城市实时气象数据（无需密钥）
 * 文档: https://open-meteo.com/en/docs
 */
@Component
public class OpenMeteoDataSource implements EnvironmentDataSource {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoDataSource.class);

    private static final String NAME = "open-meteo";

    /** 城市本地时区 */
    private static final ZoneId CITY_ZONE = ZoneId.of("Asia/Shanghai");

    /** 时区名称（作为URI模板变量传入，避免斜杠被二次编码） */
    private static final String CITY_ZONE_NAME = "Asia/Shanghai";

    /** 请求的实况指标字段 */
    private static final String CURRENT_FIELDS =
            "temperature_2m,relative_humidity_2m,wind_speed_10m,precipitation";

    private final CollectorProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenMeteoDataSource(CollectorProperties properties,
                               ObjectMapper objectMapper,
                               RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        CollectorProperties.OpenMeteo config = properties.getSources().getOpenMeteo();
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
        return Set.of(DataCategory.WEATHER);
    }

    @Override
    public boolean isAvailable() {
        return properties.getSources().getOpenMeteo().isEnabled();
    }

    @Override
    public List<CityEvent> fetch() throws Exception {
        CollectorProperties.Sources sources = properties.getSources();
        CollectorProperties.OpenMeteo config = sources.getOpenMeteo();

        String body = restClient.get()
                .uri(config.getBaseUrl()
                                + "?latitude={lat}&longitude={lng}&current={fields}"
                                + "&timezone={tz}&wind_speed_unit=ms",
                        sources.getLatitude(), sources.getLongitude(), CURRENT_FIELDS, CITY_ZONE_NAME)
                .retrieve()
                .body(String.class);

        JsonNode current = objectMapper.readTree(body).path("current");
        if (current.isMissingNode() || current.isEmpty()) {
            throw new IllegalStateException("Open-Meteo 响应缺少 current 字段");
        }

        OffsetDateTime ts = parseTime(current.path("time").asText(null));
        List<CityEvent> events = new ArrayList<>();
        addIfPresent(events, current, "temperature_2m", "temperature", "°C", ts);
        addIfPresent(events, current, "relative_humidity_2m", "humidity", "%", ts);
        addIfPresent(events, current, "wind_speed_10m", "wind_speed", "m/s", ts);
        addIfPresent(events, current, "precipitation", "precipitation", "mm", ts);

        if (events.isEmpty()) {
            throw new IllegalStateException("Open-Meteo 响应无有效指标数据");
        }
        log.info("Open-Meteo 采集成功: {} 条气象指标, ts={}", events.size(), ts);
        return events;
    }

    /**
     * 字段存在时追加事件
     */
    private void addIfPresent(List<CityEvent> events, JsonNode current, String field,
                              String metric, String unit, OffsetDateTime ts) {
        JsonNode node = current.path(field);
        if (node.isMissingNode() || node.isNull() || !node.isNumber()) return;
        CityEvent event = new CityEvent(EventDomain.ENVIRONMENT,
                properties.getSources().getCity(), metric, node.asDouble(), unit, EventSource.OPEN_METEO);
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
            log.warn("Open-Meteo 时间解析失败: {}, 使用当前时间", time);
            return OffsetDateTime.now();
        }
    }
}
