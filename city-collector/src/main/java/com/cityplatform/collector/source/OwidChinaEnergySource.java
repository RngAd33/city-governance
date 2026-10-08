package com.cityplatform.collector.source;

import com.cityplatform.collector.config.CollectorProperties;
import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.event.EventDomain;
import com.cityplatform.common.event.EventSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Our World in Data 中国电力数据源（数据源 Ember，免费无密钥）
 * 通过 Grapher CSV 直链下载并解析中国年度数据（zone=CN，公开统计数据整理值）：
 * - grid_carbon_intensity: 电力碳强度（gCO2eq/kWh）
 * - grid_renewable_percentage: 可再生电力占比（%）
 * 数据: https://ourworldindata.org/grapher/carbon-intensity-electricity
 */
@Component
@Slf4j
public class OwidChinaEnergySource implements GridIndicatorSource {

    private static final String NAME = "owid-china";

    /** 电网分区代码（中国，国家级口径） */
    private static final String ZONE = "CN";

    /** OWID 数据中中国的国家代码 */
    private static final String CODE_CHINA = "CHN";

    /** 碳强度数据集（列: Entity,Code,Year,Carbon intensity） */
    private static final String CARBON_CSV = "carbon-intensity-electricity.csv";

    /** 可再生占比数据集（列: Entity,Code,Year,Renewables） */
    private static final String RENEWABLES_CSV = "share-electricity-renewables.csv";

    private static final String CARBON_INTENSITY_UNIT = "gCO\u2082eq/kWh";

    private static final String PERCENT_UNIT = "%";

    @Resource
    private CollectorProperties properties;

    @Resource
    private RestClient.Builder restClientBuilder;

    private RestClient restClient;

    /**
     * 按配置超时构建 HTTP 客户端（依赖注入完成后执行）
     */
    @PostConstruct
    void initRestClient() {
        CollectorProperties.OwidChina config = properties.getSources().getOwidChina();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(config.getTimeoutMs());
        requestFactory.setReadTimeout(config.getTimeoutMs());
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    }

    public String getName() {
        return NAME;
    }

    public String getZone() {
        return ZONE;
    }

    public String getBaseUrl() {
        return properties.getSources().getOwidChina().getBaseUrl();
    }

    /**
     * 运行时是否可用（开放数据无密钥，仅受开关控制）
     */
    public boolean isAvailable() {
        return properties.getSources().getOwidChina().isEnabled();
    }

    /**
     * 采集一轮中国年度数据：碳强度 + 可再生占比（取 CSV 中最新年份的中国值）
     */
    public List<CityEvent> fetch() throws Exception {
        CollectorProperties.OwidChina config = properties.getSources().getOwidChina();

        CsvValue carbon = latestChinaValue(config.getBaseUrl() + "/" + CARBON_CSV);
        CsvValue renewables = latestChinaValue(config.getBaseUrl() + "/" + RENEWABLES_CSV);

        List<CityEvent> events = new ArrayList<>();
        addIfPresent(events, carbon, "grid_carbon_intensity", CARBON_INTENSITY_UNIT);
        addIfPresent(events, renewables, "grid_renewable_percentage", PERCENT_UNIT);

        if (events.isEmpty()) {
            throw new IllegalStateException("OWID CSV 未解析到中国有效数据");
        }
        log.info("OWID 中国电力数据采集成功: zone={}, 指标数={}, 碳强度年份={}, 可再生年份={}",
                ZONE, events.size(), carbon != null ? carbon.year : null, renewables != null ? renewables.year : null);
        return events;
    }

    /**
     * 下载 Grapher CSV 并解析中国（CHN）最新年份的数值，
     * 行格式: Entity,Code,Year,Value；空值行/表头自动跳过
     */
    private CsvValue latestChinaValue(String url) throws Exception {
        String csv = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);
        if (csv == null || csv.isBlank()) return null;

        CsvValue latest = null;
        for (String line : csv.split("\n")) {
            String[] parts = line.split(",");
            if (parts.length < 4 || !CODE_CHINA.equals(parts[1].trim())) continue;
            try {
                int year = Integer.parseInt(parts[2].trim());
                double value = Double.parseDouble(parts[3].trim());
                if (latest == null || year > latest.year) {
                    latest = new CsvValue(year, value);
                }
            } catch (NumberFormatException ignore) {
                // 跳过表头或空值行
            }
        }
        return latest;
    }

    private void addIfPresent(List<CityEvent> events, CsvValue value, String metric, String unit) {
        if (value == null) return;
        CityEvent event = new CityEvent(EventDomain.ENERGY, ZONE, metric, value.value, unit, EventSource.OWID);
        // 年度数据：时间取该年 1 月 1 日 UTC，rawData 携带数据年份
        event.setTs(OffsetDateTime.of(value.year, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        event.setRawData("year=" + value.year);
        events.add(event);
    }

    /** CSV 中解析出的年度值 */
    private record CsvValue(int year, double value) {
    }
}
