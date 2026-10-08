package com.cityplatform.collector.source;

import com.cityplatform.collector.config.CollectorProperties;
import com.cityplatform.collector.entity.EventRecord;
import com.cityplatform.collector.repository.EventRecordRepository;
import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.event.GeoLocation;
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
 * 电网数据采集编排服务
 * 采集国内公开电网数据（免费无密钥、无需注册）：
 * - Our World in Data（Ember 数据源）：中国年度碳强度与可再生占比（zone=CN，全国口径）
 * 经数据质量校验后发布到事件总线（energy.grid.queue）并持久化。
 * 与建筑能耗模拟器无关：本服务不产生模拟数据，数据源停用时静默跳过
 */
@Service
@Slf4j
public class EnergyGridCollectionService {

    @Resource
    private GridIndicatorSource chinaSource;

    @Resource
    private EventPublisher eventPublisher;

    @Resource
    private EventRecordRepository eventRecordRepository;

    @Resource
    private DataQualityValidator qualityValidator;

    @Resource
    private CollectorProperties properties;

    private final AtomicLong successCount = new AtomicLong();
    private final AtomicLong failureCount = new AtomicLong();
    private volatile OffsetDateTime lastFetchTime;
    private volatile Boolean lastSuccess;

    /**
     * 定时采集中国年度电力数据（默认每24小时，年度数据无需高频采集）
     */
    @Scheduled(fixedDelayString = "${collector.energy.china-interval-ms:86400000}",
            initialDelayString = "${collector.energy.china-initial-delay-ms:60000}")
    public void collectChinaGridIndicators() {
        if (!chinaSource.isAvailable()) {
            log.debug("电网数据源[{}]已停用，跳过本轮采集", chinaSource.getName());
            return;
        }
        try {
            int count = fetchPublishPersist(chinaSource);
            recordSuccess();
            log.info("电网数据采集完成: source={}, zone={}, count={}", chinaSource.getName(), chinaSource.getZone(), count);
        } catch (Exception e) {
            recordFailure();
            log.warn("电网数据源[{}]采集失败: {}", chinaSource.getName(), e.getMessage());
        }
    }

    /**
     * 手动触发一轮采集（供管理接口调用），返回发布的事件数
     */
    public int triggerCollect() throws Exception {
        if (!chinaSource.isAvailable()) {
            throw new IllegalStateException("电网数据源不可用：owid-china 已停用");
        }
        try {
            int count = fetchPublishPersist(chinaSource);
            recordSuccess();
            return count;
        } catch (Exception e) {
            recordFailure();
            throw new IllegalStateException("电网数据采集失败 - " + chinaSource.getName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * 采集→质量校验→发布→持久化，返回有效事件数；无有效数据时抛出异常
     */
    private int fetchPublishPersist(GridIndicatorSource source) throws Exception {
        List<CityEvent> valid = qualityValidator.filterInvalid(source.fetch());
        if (valid.isEmpty()) {
            throw new IllegalStateException("无有效数据返回");
        }
        publishAndPersist(valid);
        return valid.size();
    }

    /**
     * 数据源运行状态（供管理接口展示）
     */
    public List<SourceStatus> getStatuses() {
        return List.of(new SourceStatus(chinaSource.getName(),
                chinaSource.isAvailable(),
                chinaSource.getZone(),
                chinaSource.getBaseUrl(),
                lastFetchTime != null ? lastFetchTime.toString() : null,
                lastSuccess,
                successCount.get(),
                failureCount.get()));
    }

    /**
     * 补全事件元数据（eventId/ts/location），发布到事件总线并持久化
     */
    private void publishAndPersist(List<CityEvent> events) {
        CollectorProperties.Sources sources = properties.getSources();
        for (CityEvent event : events) {
            event.setEventId(UUID.randomUUID().toString());
            if (event.getTs() == null) event.setTs(OffsetDateTime.now());
            if (event.getLocation() == null) {
                event.setLocation(new GeoLocation(sources.getLatitude(), sources.getLongitude(), "中国电网(CN)"));
            }
            eventPublisher.publishGridIndicator(event);
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
            String zone,
            String baseUrl,
            String lastFetchTime,
            Boolean lastSuccess,
            long successCount,
            long failureCount
    ) {}
}
