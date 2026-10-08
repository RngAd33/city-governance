package com.cityplatform.disaster.consumer;

import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.mq.EventBusConfig;
import com.cityplatform.common.util.JsonUtil;
import com.cityplatform.disaster.model.FloodForecast;
import com.cityplatform.disaster.repository.FloodForecastRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 洪水预报事件消费者
 * 监听洪水预报队列（water.flood.queue），将长江南京段真实径流量预报
 * （Open-Meteo Flood/GloFAS 采集）持久化到 flood_forecast 表；
 * 同一天的预报数据以最新一次采集结果覆盖，并清理已过期的历史预报
 */
@Component
@Slf4j
public class FloodForecastConsumer {

    /** 保留的历史预报天数（昨日数据仍保留，更早清理） */
    private static final long KEEP_PAST_DAYS = 1;

    @Resource
    private FloodForecastRepository floodForecastRepository;

    /**
     * 监听洪水预报队列
     */
    @RabbitListener(queues = EventBusConfig.WATER_FLOOD_QUEUE)
    public void handleFloodForecast(String message) {
        log.info("接收到洪水预报事件: {}", message);

        try {
            CityEvent event = JsonUtil.fromJson(message, CityEvent.class);
            if (event == null || event.getRawData() == null) {
                log.warn("洪水预报事件解析失败或缺少rawData，跳过处理");
                return;
            }

            Map<?, ?> raw = JsonUtil.fromJson(event.getRawData(), Map.class);
            if (raw == null) {
                log.warn("洪水预报rawData解析失败，跳过处理");
                return;
            }

            String stationName = asString(raw.get("station"));
            Object seriesObj = raw.get("series");
            if (stationName == null || !(seriesObj instanceof List<?> series)) {
                log.warn("洪水预报缺少 station/series 字段，跳过处理");
                return;
            }

            LocalDateTime fetchedAt = parseFetchedAt(raw.get("fetchedAt"));
            String source = event.getSource() != null ? event.getSource().name() : "OPEN_METEO";

            int saved = 0;
            for (Object item : series) {
                if (!(item instanceof Map<?, ?> row)) continue;
                if (saveSeriesRow(stationName, source, fetchedAt, row)) {
                    saved++;
                }
            }
            log.info("洪水预报已入库: station={}, 天数={}, fetchedAt={}",
                    stationName, saved, fetchedAt);

            cleanupExpired();
        } catch (Exception e) {
            log.error("处理洪水预报事件时发生错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 保存单日预报（同站同天覆盖更新）
     */
    private boolean saveSeriesRow(String stationName, String source,
                                  LocalDateTime fetchedAt, Map<?, ?> row) {
        String date = asString(row.get("date"));
        Double discharge = asDouble(row.get("discharge"));
        if (date == null || discharge == null) {
            return false;
        }

        LocalDate forecastDate;
        try {
            forecastDate = LocalDate.parse(date);
        } catch (Exception e) {
            log.warn("预报日期解析失败: {}", date);
            return false;
        }

        FloodForecast forecast = floodForecastRepository
                .findByStationNameAndForecastDate(stationName, forecastDate)
                .orElseGet(FloodForecast::new);
        forecast.setStationName(stationName);
        forecast.setForecastDate(forecastDate);
        forecast.setDischarge(discharge);
        forecast.setDischargeMax(asDouble(row.get("dischargeMax")));
        forecast.setDischargeMean(asDouble(row.get("dischargeMean")));
        forecast.setSource(source);
        forecast.setFetchedAt(fetchedAt);
        floodForecastRepository.save(forecast);
        return true;
    }

    /**
     * 清理已过期的历史预报（保留昨日数据用于对比）
     */
    private void cleanupExpired() {
        try {
            LocalDate cutoff = LocalDate.now().minusDays(KEEP_PAST_DAYS);
            List<FloodForecast> expired = floodForecastRepository.findByForecastDateBefore(cutoff);
            if (!expired.isEmpty()) {
                floodForecastRepository.deleteAll(expired);
                log.info("已清理{}条过期洪水预报（预报日期早于{}）", expired.size(), cutoff);
            }
        } catch (Exception e) {
            log.warn("清理过期洪水预报失败: {}", e.getMessage());
        }
    }

    private LocalDateTime parseFetchedAt(Object fetchedAtObj) {
        String fetchedAt = asString(fetchedAtObj);
        if (fetchedAt != null) {
            try {
                return OffsetDateTime.parse(fetchedAt).toLocalDateTime();
            } catch (Exception ignored) {
                // 继续按本地时间解析
            }
            try {
                return LocalDateTime.parse(fetchedAt);
            } catch (Exception ignored) {
                // 使用当前时间兜底
            }
        }
        return LocalDateTime.now();
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    private Double asDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }
}
