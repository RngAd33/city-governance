package com.cityplatform.collector.source;

import com.cityplatform.collector.entity.EventRecord;
import com.cityplatform.collector.repository.EventRecordRepository;
import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.mq.EventPublisher;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 洪水预报数据采集编排服务
 * 定时采集长江南京段真实径流量预报（Open-Meteo Flood / GloFAS，免费无密钥，日粒度含7天预报），
 * 经数据质量校验后发布到事件总线（water.flood.queue）并持久化；
 * 内存缓存最近一次预报，供水位采集服务推算站点水位使用。
 * 本服务不产生模拟数据，数据源停用时静默跳过
 */
@Service
@Slf4j
public class FloodCollectionService {

    @Resource
    private OpenMeteoFloodDataSource floodSource;

    @Resource
    private EventPublisher eventPublisher;

    @Resource
    private EventRecordRepository eventRecordRepository;

    @Resource
    private DataQualityValidator qualityValidator;

    private final AtomicLong successCount = new AtomicLong();
    private final AtomicLong failureCount = new AtomicLong();
    private volatile OffsetDateTime lastFetchTime;
    private volatile Boolean lastSuccess;

    /** 最近一次采集的径流量预报（供水位采集服务使用） */
    private volatile OpenMeteoFloodDataSource.FloodForecast latestForecast;

    /**
     * 定时采集长江径流量预报（默认每1小时，GloFAS为日粒度预报无需更高频）
     */
    @Scheduled(fixedDelayString = "${collector.flood.interval-ms:3600000}",
            initialDelayString = "${collector.flood.initial-delay-ms:60000}")
    public void collectFloodForecast() {
        if (!floodSource.isAvailable()) {
            log.debug("洪水预报数据源[{}]已停用，跳过本轮采集", floodSource.getName());
            return;
        }
        try {
            int count = fetchPublishPersist();
            recordSuccess();
            log.info("洪水预报采集完成: source={}, station={}, count={}",
                    floodSource.getName(), floodSource.getStationName(), count);
        } catch (Exception e) {
            recordFailure();
            log.warn("洪水预报数据源[{}]采集失败: {}", floodSource.getName(), e.getMessage());
        }
    }

    /**
     * 手动触发一轮采集（供管理接口调用），返回发布的事件数
     */
    public int triggerCollect() throws Exception {
        if (!floodSource.isAvailable()) {
            throw new IllegalStateException("洪水预报数据源不可用：open-meteo-flood 已停用");
        }
        try {
            int count = fetchPublishPersist();
            recordSuccess();
            return count;
        } catch (Exception e) {
            recordFailure();
            throw new IllegalStateException("洪水预报采集失败: " + e.getMessage(), e);
        }
    }

    /**
     * 采集→质量校验→发布→缓存，返回有效事件数；无有效数据时抛出异常
     */
    private int fetchPublishPersist() throws Exception {
        OpenMeteoFloodDataSource.FloodForecast forecast = floodSource.fetchForecast();
        CityEvent event = floodSource.toEvent(forecast);

        List<CityEvent> valid = qualityValidator.filterInvalid(List.of(event));
        if (valid.isEmpty()) {
            throw new IllegalStateException("无有效数据返回");
        }
        CityEvent validEvent = valid.get(0);
        validEvent.setEventId(UUID.randomUUID().toString());
        eventPublisher.publishFloodForecast(validEvent);
        saveEventRecord(validEvent);

        this.latestForecast = forecast;
        return valid.size();
    }

    /**
     * 最近一次采集的径流量预报（供水位采集服务使用），无缓存时返回 null
     */
    public OpenMeteoFloodDataSource.FloodForecast getLatestForecast() {
        return latestForecast;
    }

    /**
     * 解析长江当前径流量（m³/s）：优先内存缓存，其次最近持久化事件，均无时返回 null
     */
    public Double resolveCurrentDischarge() {
        OpenMeteoFloodDataSource.FloodForecast forecast = latestForecast;
        if (forecast != null && forecast.current() != null) {
            return forecast.current().discharge();
        }
        return eventRecordRepository.findTopByMetricOrderByTsDesc("river_discharge")
                .map(EventRecord::getValue)
                .orElse(null);
    }

    /**
     * 数据源运行状态（供管理接口展示）
     */
    public List<SourceStatus> getStatuses() {
        OpenMeteoFloodDataSource.FloodForecast forecast = latestForecast;
        return List.of(new SourceStatus(
                floodSource.getName(),
                floodSource.isAvailable(),
                floodSource.getStationName(),
                floodSource.getBaseUrl(),
                lastFetchTime != null ? lastFetchTime.toString() : null,
                lastSuccess,
                successCount.get(),
                failureCount.get(),
                forecast != null ? forecast.current().discharge() : null,
                forecast != null ? forecast.current().date() : null));
    }

    /**
     * 持久化事件记录到数据库
     */
    private void saveEventRecord(CityEvent event) {
        try {
            EventRecord record = new EventRecord(
                    event.getEventId(),
                    event.getDomain().name(),
                    event.getCity(),
                    event.getMetric(),
                    event.getValue(),
                    event.getUnit(),
                    event.getSource().name(),
                    event.getTs()
            );
            eventRecordRepository.save(record);
        } catch (Exception e) {
            log.error("事件持久化失败: {}", e.getMessage());
        }
    }

    private void recordSuccess() {
        this.lastFetchTime = OffsetDateTime.now();
        this.lastSuccess = true;
        successCount.incrementAndGet();
    }

    private void recordFailure() {
        this.lastFetchTime = OffsetDateTime.now();
        this.lastSuccess = false;
        failureCount.incrementAndGet();
    }

    /**
     * 数据源运行状态
     */
    public record SourceStatus(
            String name,
            boolean available,
            String stationName,
            String baseUrl,
            String lastFetchTime,
            Boolean lastSuccess,
            long successCount,
            long failureCount,
            Double currentDischarge,
            String currentDate
    ) {}
}
