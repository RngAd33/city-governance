package com.cityplatform.collector.source;

import com.cityplatform.collector.config.CollectorProperties;
import com.cityplatform.collector.entity.EventRecord;
import com.cityplatform.collector.repository.EventRecordRepository;
import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.event.EventDomain;
import com.cityplatform.common.event.EventSource;
import com.cityplatform.common.event.GeoLocation;
import com.cityplatform.common.mq.EventPublisher;
import com.cityplatform.common.util.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 南京站点水位采集编排服务
 * 以真实数据推算各站点水位（非随机模拟）：
 * - 降水入流：南京最新真实降水（Open-Meteo 采集，读 event_record 最近 precipitation 记录）
 * - 径流调节：长江当前真实径流量（Open-Meteo Flood/GloFAS，FloodCollectionService 缓存）
 * - 水量平衡模型：入流 - 消退 + 径流调节，水位从上次采集值连续演进
 * 经质量校验后发布到事件总线（water.monitor.queue）并持久化；
 * 事件 metric 形如 "WS001:water_level"，rawData 携带站号/降水/径流量/趋势等元数据。
 * 本服务不产生随机数据，真实输入缺失时按无降水/无调节处理
 */
@Service
@Slf4j
public class WaterLevelCollectionService {

    /** 降水记录有效期（分钟），超出视为当前无有效降水 */
    private static final long RAINFALL_VALID_MINUTES = 120;

    /** 径流系数（降水量转化为径流深的比例） */
    private static final double RUNOFF_COEFFICIENT = 0.65;

    /** 河流站点径流调节系数（按长江径流量相对参照值的偏差线性调节，米/周期） */
    private static final double RIVER_ADJUST_COEFFICIENT = 0.02;

    /** 径流调节项绝对值上限（米/周期） */
    private static final double RIVER_ADJUST_LIMIT = 0.03;

    /** 水位趋势判定阈值（米） */
    private static final double TREND_THRESHOLD = 0.002;

    @Resource
    private FloodCollectionService floodCollectionService;

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
    private volatile int lastPublishedCount;

    /**
     * 定时推算并发布各站点水位（默认每10分钟，与气象采集同频）
     */
    @Scheduled(fixedDelayString = "${collector.water.interval-ms:600000}",
            initialDelayString = "${collector.water.initial-delay-ms:90000}")
    public void collectWaterLevels() {
        try {
            int count = fetchPublishPersist();
            recordSuccess(count);
            log.info("站点水位采集完成: 站点数={}", count);
        } catch (Exception e) {
            recordFailure();
            log.warn("站点水位采集失败: {}", e.getMessage());
        }
    }

    /**
     * 手动触发一轮采集（供管理接口调用），返回发布的事件数
     */
    public int triggerCollect() throws Exception {
        try {
            int count = fetchPublishPersist();
            recordSuccess(count);
            return count;
        } catch (Exception e) {
            recordFailure();
            throw new IllegalStateException("站点水位采集失败: " + e.getMessage(), e);
        }
    }

    /**
     * 以真实降水与径流量推算全部站点水位，经质量校验后发布并持久化
     */
    private int fetchPublishPersist() {
        List<CollectorProperties.Water.Station> stations = properties.getWater().getStations();
        if (stations.isEmpty()) {
            throw new IllegalStateException("未配置水文站点（collector.water.stations）");
        }

        // 真实输入1：南京最新降水（近15分钟，mm）
        double rainfallMm = resolveRainfallMm();
        // 真实输入2：长江当前径流量（m³/s）
        Double discharge = floodCollectionService.resolveCurrentDischarge();
        double referenceDischarge = properties.getWater().getRiverReferenceDischarge();

        List<CityEvent> events = new ArrayList<>();
        for (CollectorProperties.Water.Station station : stations) {
            try {
                events.add(buildStationEvent(station, rainfallMm, discharge, referenceDischarge));
            } catch (Exception e) {
                log.warn("站点[{}]水位推算失败: {}", station.getId(), e.getMessage());
            }
        }
        if (events.isEmpty()) {
            throw new IllegalStateException("所有站点水位推算失败");
        }

        List<CityEvent> valid = qualityValidator.filterInvalid(events);
        for (CityEvent event : valid) {
            event.setEventId(UUID.randomUUID().toString());
            eventPublisher.publishWaterLevel(event);
            saveEventRecord(event);
        }
        return valid.size();
    }

    /**
     * 读取南京最新真实降水量（mm）；无记录或记录过期时按无降水处理
     */
    private double resolveRainfallMm() {
        try {
            EventRecord record = eventRecordRepository
                    .findTopByMetricOrderByTsDesc("precipitation")
                    .orElse(null);
            if (record == null || record.getValue() == null) {
                return 0.0;
            }
            OffsetDateTime ts = record.getTs();
            boolean fresh = ts != null
                    && ChronoUnit.MINUTES.between(ts, OffsetDateTime.now()) <= RAINFALL_VALID_MINUTES;
            if (!fresh) {
                log.debug("最近降水记录已过期: ts={}, 按无降水处理", ts);
                return 0.0;
            }
            return Math.max(0.0, record.getValue());
        } catch (Exception e) {
            log.warn("读取最近降水记录失败: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * 单站点水位推算：入流（真实降水）- 消退（向基线回落）+ 径流调节（真实长江径流量），
     * 以上次采集水位为起点保持连续演进
     */
    private CityEvent buildStationEvent(CollectorProperties.Water.Station station,
                                        double rainfallMm, Double discharge, double referenceDischarge) {
        String metric = station.getId() + ":water_level";
        double baseline = station.getBaselineLevel();

        double lastLevel = eventRecordRepository.findTopByMetricOrderByTsDesc(metric)
                .map(EventRecord::getValue)
                .orElse(baseline);

        double inflow = rainfallMm * RUNOFF_COEFFICIENT * station.getCatchmentFactor() / 1000.0;
        double recession = station.getRecessionRate() * Math.max(0.0, lastLevel - baseline);

        double riverAdjust = 0.0;
        boolean riverStation = "RIVER".equalsIgnoreCase(station.getType());
        if (riverStation && discharge != null && referenceDischarge > 0) {
            riverAdjust = RIVER_ADJUST_COEFFICIENT * (discharge / referenceDischarge - 1.0);
            riverAdjust = Math.max(-RIVER_ADJUST_LIMIT, Math.min(RIVER_ADJUST_LIMIT, riverAdjust));
        }

        double newLevel = lastLevel + inflow - recession + riverAdjust;
        newLevel = Math.max(baseline * 0.5, Math.min(station.getMaxLevel(), newLevel));
        newLevel = round(newLevel, 2);

        String trend = newLevel - lastLevel > TREND_THRESHOLD ? "rising"
                : lastLevel - newLevel > TREND_THRESHOLD ? "falling" : "stable";

        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("stationId", station.getId());
        raw.put("stationName", station.getName());
        raw.put("stationType", station.getType());
        raw.put("rainfall", round(rainfallMm, 2));
        if (riverStation && discharge != null) {
            raw.put("flowRate", round(discharge, 1));
        }
        raw.put("trend", trend);
        raw.put("baselineLevel", baseline);
        raw.put("maxLevel", station.getMaxLevel());

        CityEvent event = new CityEvent(EventDomain.WATER, properties.getSources().getCity(),
                metric, newLevel, "m", EventSource.OPEN_METEO);
        event.setLocation(new GeoLocation(station.getLatitude(), station.getLongitude(), station.getName()));
        event.setRawData(JsonUtil.toJson(raw));
        return event;
    }

    /**
     * 各站点最新水位（供管理接口展示）
     */
    public List<StationStatus> getStationStatuses() {
        return properties.getWater().getStations().stream().map(station -> {
            Double level = eventRecordRepository
                    .findTopByMetricOrderByTsDesc(station.getId() + ":water_level")
                    .map(EventRecord::getValue)
                    .orElse(null);
            return new StationStatus(station.getId(), station.getName(), station.getType(),
                    station.getLatitude(), station.getLongitude(), level,
                    station.getBaselineLevel(), station.getMaxLevel());
        }).toList();
    }

    /**
     * 服务运行状态（供管理接口展示）
     */
    public RuntimeStatus getRuntimeStatus() {
        return new RuntimeStatus(
                lastFetchTime != null ? lastFetchTime.toString() : null,
                lastSuccess,
                successCount.get(),
                failureCount.get(),
                lastPublishedCount);
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

    private void recordSuccess(int count) {
        this.lastFetchTime = OffsetDateTime.now();
        this.lastSuccess = true;
        this.lastPublishedCount = count;
        successCount.incrementAndGet();
    }

    private void recordFailure() {
        this.lastFetchTime = OffsetDateTime.now();
        this.lastSuccess = false;
        failureCount.incrementAndGet();
    }

    private double round(double value, int decimalPlaces) {
        double factor = Math.pow(10, decimalPlaces);
        return Math.round(value * factor) / factor;
    }

    /**
     * 站点水位概览
     */
    public record StationStatus(
            String stationId,
            String name,
            String type,
            double latitude,
            double longitude,
            Double currentLevel,
            double baselineLevel,
            double maxLevel
    ) {}

    /**
     * 服务运行状态
     */
    public record RuntimeStatus(
            String lastFetchTime,
            Boolean lastSuccess,
            long successCount,
            long failureCount,
            int lastPublishedCount
    ) {}
}
