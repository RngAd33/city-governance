package com.cityplatform.collector.source;

import com.cityplatform.collector.config.CollectorProperties;
import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.event.EventDomain;
import com.cityplatform.common.event.EventSource;
import com.cityplatform.common.event.GeoLocation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Open-Meteo 洪水预报数据源（GloFAS 全球洪水模型）
 * 通过免费开放的 Open-Meteo Flood API 获取南京长江段真实径流量（日粒度，含7天预报），
 * 采样点取南京长江大桥江段（覆盖长江干流格点，免费接口无需密钥）；
 * 文档: https://open-meteo.com/en/docs/flood-api
 */
@Component
@Slf4j
public class OpenMeteoFloodDataSource {

    private static final String NAME = "open-meteo-flood";

    /** 采样河段名称 */
    private static final String STATION_NAME = "南京长江大桥江段";

    /** 请求的日粒度径流量字段 */
    private static final String DAILY_FIELDS = "river_discharge,river_discharge_max,river_discharge_mean";

    /** 时区名称（作为URI模板变量传入，避免斜杠被二次编码） */
    private static final String CITY_ZONE_NAME = "Asia/Shanghai";

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
        CollectorProperties.OpenMeteoFlood config = properties.getSources().getOpenMeteoFlood();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(config.getTimeoutMs());
        requestFactory.setReadTimeout(config.getTimeoutMs());
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    }

    public String getName() {
        return NAME;
    }

    public boolean isAvailable() {
        return properties.getSources().getOpenMeteoFlood().isEnabled();
    }

    public String getStationName() {
        return STATION_NAME;
    }

    public String getBaseUrl() {
        return properties.getSources().getOpenMeteoFlood().getBaseUrl();
    }

    /**
     * 采集长江径流量预报（过去1天 + 未来7天，日粒度）
     */
    public FloodForecast fetchForecast() throws Exception {
        CollectorProperties.OpenMeteoFlood config = properties.getSources().getOpenMeteoFlood();

        String body = restClient.get()
                .uri(config.getBaseUrl()
                                + "?latitude={lat}&longitude={lng}&daily={fields}"
                                + "&forecast_days=7&past_days=1&timezone={tz}",
                        config.getLatitude(), config.getLongitude(), DAILY_FIELDS, CITY_ZONE_NAME)
                .retrieve()
                .body(String.class);

        JsonNode daily = objectMapper.readTree(body).path("daily");
        JsonNode times = daily.path("time");
        if (daily.isMissingNode() || !times.isArray() || times.isEmpty()) {
            throw new IllegalStateException("Open-Meteo Flood 响应缺少 daily 数据");
        }

        List<DailyFlow> series = new ArrayList<>();
        for (int i = 0; i < times.size(); i++) {
            series.add(new DailyFlow(
                    times.get(i).asText(),
                    numberOrNull(daily.path("river_discharge"), i),
                    numberOrNull(daily.path("river_discharge_max"), i),
                    numberOrNull(daily.path("river_discharge_mean"), i)));
        }

        DailyFlow current = pickCurrent(series);
        if (current == null || current.discharge() == null) {
            throw new IllegalStateException("Open-Meteo Flood 响应无有效径流量数据");
        }
        log.info("Open-Meteo Flood 采集成功: station={}, current={}m³/s, 序列天数={}",
                STATION_NAME, current.discharge(), series.size());
        return new FloodForecast(STATION_NAME, series, current, OffsetDateTime.now());
    }

    /**
     * 构造径流量事件（供事件总线发布与持久化）
     */
    public CityEvent toEvent(FloodForecast forecast) throws Exception {
        CollectorProperties.OpenMeteoFlood config = properties.getSources().getOpenMeteoFlood();

        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("station", forecast.stationName());
        raw.put("stationLat", config.getLatitude());
        raw.put("stationLng", config.getLongitude());
        raw.put("series", forecast.series());
        raw.put("fetchedAt", forecast.fetchedAt().toString());

        CityEvent event = new CityEvent(EventDomain.WATER,
                properties.getSources().getCity(), "river_discharge",
                forecast.current().discharge(), "m³/s", EventSource.OPEN_METEO);
        event.setLocation(new GeoLocation(config.getLatitude(), config.getLongitude(), forecast.stationName()));
        event.setRawData(objectMapper.writeValueAsString(raw));
        return event;
    }

    /**
     * 选取当前值：优先今日，否则取序列中最后一个有值的日期
     */
    private DailyFlow pickCurrent(List<DailyFlow> series) {
        String today = LocalDate.now().toString();
        DailyFlow lastWithValue = null;
        for (DailyFlow flow : series) {
            if (flow.discharge() != null) lastWithValue = flow;
            if (today.equals(flow.date()) && flow.discharge() != null) return flow;
        }
        return lastWithValue;
    }

    private Double numberOrNull(JsonNode arrayNode, int index) {
        JsonNode node = arrayNode.path(index);
        return node.isNumber() ? node.asDouble() : null;
    }

    /**
     * 单日径流量（GloFAS 原始日粒度值，可能为 null）
     */
    public record DailyFlow(String date, Double discharge, Double dischargeMax, Double dischargeMean) {}

    /**
     * 长江径流量预报（过去1天 + 未来7天日粒度序列）
     */
    public record FloodForecast(String stationName, List<DailyFlow> series, DailyFlow current,
                                OffsetDateTime fetchedAt) {

        /** 序列最大径流量（无值时取当前值） */
        public double maxDischarge() {
            return series.stream()
                    .filter(flow -> flow.discharge() != null)
                    .mapToDouble(DailyFlow::discharge)
                    .max()
                    .orElse(current.discharge());
        }

        /** 序列均值径流量（无值时取当前值） */
        public double meanDischarge() {
            return series.stream()
                    .filter(flow -> flow.discharge() != null)
                    .mapToDouble(DailyFlow::discharge)
                    .average()
                    .orElse(current.discharge());
        }
    }
}
