package com.cityplatform.collector.source;

import com.cityplatform.collector.config.CollectorProperties;
import com.cityplatform.collector.entity.EventRecord;
import com.cityplatform.collector.repository.EventRecordRepository;
import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.event.GeoLocation;
import com.cityplatform.common.mq.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 环境数据采集编排服务
 * 按配置周期从外部数据源（Open-Meteo/OpenAQ等）采集环境数据，
 * 经数据清洗后发布到RabbitMQ事件总线并持久化；
 * 真实数据源全部失败时自动降级使用模拟器兜底，保证数据流不中断
 */
@Service
public class EnvironmentCollectionService {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentCollectionService.class);

    /** 空气质量指标集合 */
    private static final List<String> AIR_QUALITY_METRICS = List.of("pm25", "pm10", "no2", "o3");

    private final List<EnvironmentDataSource> allSources;
    private final List<EnvironmentDataSource> realSources;
    private final SimulatorEnvironmentSource fallbackSource;
    private final EventPublisher eventPublisher;
    private final EventRecordRepository eventRecordRepository;
    private final DataQualityValidator qualityValidator;
    private final CollectorProperties properties;

    /** 数据源运行时状态 */
    private final Map<String, SourceRuntimeStatus> runtimeStatus = new ConcurrentHashMap<>();

    public EnvironmentCollectionService(List<EnvironmentDataSource> dataSources,
                                        EventPublisher eventPublisher,
                                        EventRecordRepository eventRecordRepository,
                                        DataQualityValidator qualityValidator,
                                        CollectorProperties properties) {
        this.allSources = List.copyOf(dataSources);
        this.realSources = dataSources.stream().filter(source -> !source.isFallback()).toList();
        this.fallbackSource = dataSources.stream()
                .filter(SimulatorEnvironmentSource.class::isInstance)
                .map(SimulatorEnvironmentSource.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("缺少模拟器兜底数据源"));
        this.eventPublisher = eventPublisher;
        this.eventRecordRepository = eventRecordRepository;
        this.qualityValidator = qualityValidator;
        this.properties = properties;
        log.info("环境数据源初始化完成: 真实数据源={}, 兜底数据源={}",
                realSources.stream().map(EnvironmentDataSource::getName).toList(),
                fallbackSource.getName());
    }

    /**
     * 定时采集气象数据（默认每10分钟）
     */
    @Scheduled(fixedDelayString = "${collector.environment.weather-interval-ms:600000}",
            initialDelayString = "${collector.environment.initial-delay-ms:30000}")
    public void collectWeather() {
        collectCategory(DataCategory.WEATHER);
    }

    /**
     * 定时采集空气质量数据（默认每30分钟）
     */
    @Scheduled(fixedDelayString = "${collector.environment.air-quality-interval-ms:1800000}",
            initialDelayString = "${collector.environment.initial-delay-ms:30000}")
    public void collectAirQuality() {
        collectCategory(DataCategory.AIR_QUALITY);
    }

    /**
     * 手动触发全量环境数据采集
     */
    public List<CollectionResult> collectAll() {
        return List.of(collectCategory(DataCategory.WEATHER), collectCategory(DataCategory.AIR_QUALITY));
    }

    /**
     * 采集指定类别的环境数据：优先真实数据源，全部失败时降级模拟器兜底
     */
    public CollectionResult collectCategory(DataCategory category) {
        for (EnvironmentDataSource source : realSources) {
            if (!source.getCategories().contains(category)) continue;
            if (!source.isAvailable()) {
                log.debug("数据源[{}]不可用（未启用或缺少密钥），跳过", source.getName());
                continue;
            }
            try {
                List<CityEvent> valid = qualityValidator.filterInvalid(source.fetch());
                if (valid.isEmpty()) {
                    recordFailure(source);
                    log.warn("数据源[{}]无有效数据，尝试下一数据源", source.getName());
                    continue;
                }
                publishAndPersist(valid);
                recordSuccess(source);
                log.info("环境数据采集完成: category={}, source={}, count={}",
                        category.getCode(), source.getName(), valid.size());
                return new CollectionResult(category.getCode(), source.getName(), valid.size(),
                        false, metricsOf(valid), "采集成功");
            } catch (Exception e) {
                recordFailure(source);
                log.warn("数据源[{}]采集失败: {}，尝试下一数据源", source.getName(), e.getMessage());
            }
        }

        // 兜底：模拟器数据
        List<CityEvent> valid = qualityValidator.filterInvalid(fallbackSource.generateByCategory(category));
        publishAndPersist(valid);
        recordSuccess(fallbackSource);
        log.info("真实数据源全部不可用，已使用模拟器兜底: category={}, count={}", category.getCode(), valid.size());
        return new CollectionResult(category.getCode(), fallbackSource.getName(), valid.size(),
                true, metricsOf(valid), "真实数据源不可用，已使用模拟数据兜底");
    }

    /**
     * 查询所有数据源信息与运行时状态
     */
    public List<SourceInfo> getSourceInfoList() {
        List<SourceInfo> infos = new ArrayList<>();
        for (EnvironmentDataSource source : allSources) {
            SourceRuntimeStatus status = runtimeStatus.get(source.getName());
            infos.add(new SourceInfo(
                    source.getName(),
                    source.getSource().name(),
                    source.getCategories().stream().map(DataCategory::getCode).sorted().toList(),
                    source.isAvailable(),
                    source.isFallback(),
                    status != null && status.lastFetchTime != null ? status.lastFetchTime.toString() : null,
                    status != null ? status.lastSuccess : null,
                    status != null ? status.successCount.get() : 0L,
                    status != null ? status.failureCount.get() : 0L));
        }
        return infos;
    }

    /**
     * 补全事件元数据（eventId/ts/location），按类别发布到事件总线并持久化
     */
    private void publishAndPersist(List<CityEvent> events) {
        for (CityEvent event : events) {
            event.setEventId(UUID.randomUUID().toString());
            if (event.getTs() == null) event.setTs(OffsetDateTime.now());
            if (event.getLocation() == null) {
                CollectorProperties.Sources sources = properties.getSources();
                event.setLocation(new GeoLocation(sources.getLatitude(), sources.getLongitude(), sources.getAddress()));
            }

            if (AIR_QUALITY_METRICS.contains(event.getMetric())) {
                eventPublisher.publishAirQuality(event);
            } else {
                eventPublisher.publishWeather(event);
            }
            saveEventRecord(event);
        }
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

    private List<String> metricsOf(List<CityEvent> events) {
        return events.stream().map(CityEvent::getMetric).toList();
    }

    private void recordSuccess(EnvironmentDataSource source) {
        runtimeStatus.computeIfAbsent(source.getName(), key -> new SourceRuntimeStatus()).record(true);
    }

    private void recordFailure(EnvironmentDataSource source) {
        runtimeStatus.computeIfAbsent(source.getName(), key -> new SourceRuntimeStatus()).record(false);
    }

    /**
     * 单次采集结果
     */
    public record CollectionResult(
            String category,
            String sourceUsed,
            int eventCount,
            boolean fallback,
            List<String> metrics,
            String message
    ) {}

    /**
     * 数据源信息与运行时状态
     */
    public record SourceInfo(
            String name,
            String source,
            List<String> categories,
            boolean available,
            boolean fallback,
            String lastFetchTime,
            Boolean lastSuccess,
            long successCount,
            long failureCount
    ) {}

    /**
     * 数据源运行时状态（可变，线程安全）
     */
    private static final class SourceRuntimeStatus {
        private volatile OffsetDateTime lastFetchTime;
        private volatile Boolean lastSuccess;
        private final AtomicLong successCount = new AtomicLong();
        private final AtomicLong failureCount = new AtomicLong();

        void record(boolean success) {
            this.lastFetchTime = OffsetDateTime.now();
            this.lastSuccess = success;
            if (success) {
                successCount.incrementAndGet();
            } else {
                failureCount.incrementAndGet();
            }
        }
    }
}
