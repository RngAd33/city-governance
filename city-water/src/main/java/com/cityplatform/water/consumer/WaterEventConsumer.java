package com.cityplatform.water.consumer;

import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.event.EventDomain;
import com.cityplatform.common.event.EventSource;
import com.cityplatform.common.mq.EventBusConfig;
import com.cityplatform.common.mq.EventPublisher;
import com.cityplatform.water.model.FloodScenario;
import com.cityplatform.water.model.WaterLevelRecord;
import com.cityplatform.water.model.WaterStation;
import com.cityplatform.water.risk.FloodRiskAssessor;
import com.cityplatform.water.repository.FloodScenarioRepository;
import com.cityplatform.water.repository.WaterLevelRecordRepository;
import com.cityplatform.water.repository.WaterStationRepository;
import com.cityplatform.common.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 水位监测事件消费者
 * 监听水位监测队列，评估洪水风险并触发灾害模拟
 */
@Component
public class WaterEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(WaterEventConsumer.class);

    /** 触发灾害模拟的容量阈值（80%） */
    private static final double SIMULATION_THRESHOLD = 80.0;

    private final WaterStationRepository waterStationRepository;
    private final WaterLevelRecordRepository waterLevelRecordRepository;
    private final FloodScenarioRepository floodScenarioRepository;
    private final FloodRiskAssessor floodRiskAssessor;
    private final EventPublisher eventPublisher;

    public WaterEventConsumer(WaterStationRepository waterStationRepository,
                              WaterLevelRecordRepository waterLevelRecordRepository,
                              FloodScenarioRepository floodScenarioRepository,
                              FloodRiskAssessor floodRiskAssessor,
                              EventPublisher eventPublisher) {
        this.waterStationRepository = waterStationRepository;
        this.waterLevelRecordRepository = waterLevelRecordRepository;
        this.floodScenarioRepository = floodScenarioRepository;
        this.floodRiskAssessor = floodRiskAssessor;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 监听水位监测队列
     */
    @RabbitListener(queues = EventBusConfig.WATER_MONITOR_QUEUE)
    public void handleWaterLevelEvent(String message) {
        log.info("接收到水位监测事件: {}", message);

        try {
            CityEvent event = JsonUtil.fromJson(message, CityEvent.class);
            if (event == null) {
                log.warn("事件解析失败，跳过处理");
                return;
            }

            // 提取事件数据
            String stationId = extractStationId(event);
            Double waterLevel = event.getValue();
            Double rainfall = extractRainfall(event);

            if (stationId == null || waterLevel == null) {
                log.warn("事件数据不完整，跳过处理: stationId={}, waterLevel={}",
                        stationId, waterLevel);
                return;
            }

            // 查找对应站点
            WaterStation station = waterStationRepository.findByStationId(stationId).orElse(null);
            if (station == null) {
                log.warn("未找到对应监测站点: {}", stationId);
                return;
            }

            // 更新站点当前水位
            station.setCurrentLevel(waterLevel);
            waterStationRepository.save(station);

            // 计算容量百分比
            double capacityPercentage = station.getCapacityPercentage();

            // 确定风险等级
            WaterLevelRecord.RiskLevel riskLevel = determineRiskLevel(capacityPercentage);

            // 保存水位记录
            WaterLevelRecord record = new WaterLevelRecord(
                    stationId,
                    LocalDateTime.now(),
                    waterLevel,
                    extractFlowRate(event),
                    rainfall != null ? rainfall : 0.0,
                    riskLevel
            );
            waterLevelRecordRepository.save(record);
            log.info("水位记录已保存 [{}]: 水位={}m, 风险等级={}",
                    stationId, waterLevel, riskLevel);

            // 风险评估
            double rainfallForecast = rainfall != null ? rainfall : 0.0;
            FloodRiskAssessor.RiskAssessment assessment = floodRiskAssessor.assessRisk(
                    station, waterLevel, rainfallForecast, 24);

            // 如果容量超过阈值，触发灾害模拟
            if (capacityPercentage >= SIMULATION_THRESHOLD) {
                log.warn("站点[{}]容量已达{}%，触发洪涝灾害模拟",
                        stationId, String.format("%.1f", capacityPercentage));

                double simulationRainfall = Math.max(rainfallForecast * 2, 50.0);
                int simulationDuration = capacityPercentage > 95 ? 72 : 48;

                FloodScenario scenario = floodRiskAssessor.simulateFloodScenario(
                        station, simulationRainfall, simulationDuration);

                // 发布灾害模拟事件
                publishDisasterEvent(station, scenario);
            }

        } catch (Exception e) {
            log.error("处理水位监测事件时发生错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 从事件中提取站点ID
     */
    private String extractStationId(CityEvent event) {
        if (event.getMetric() != null && event.getMetric().contains(":")) {
            return event.getMetric().split(":")[0];
        }
        if (event.getRawData() != null) {
            try {
                var rawData = JsonUtil.fromJson(event.getRawData(), java.util.Map.class);
                if (rawData != null && rawData.containsKey("stationId")) {
                    return (String) rawData.get("stationId");
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * 从事件中提取降雨量
     */
    private Double extractRainfall(CityEvent event) {
        if (event.getRawData() != null) {
            try {
                var rawData = JsonUtil.fromJson(event.getRawData(), java.util.Map.class);
                if (rawData != null && rawData.containsKey("rainfall")) {
                    Object rainfallObj = rawData.get("rainfall");
                    if (rainfallObj instanceof Number) {
                        return ((Number) rainfallObj).doubleValue();
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * 从事件中提取流量
     */
    private Double extractFlowRate(CityEvent event) {
        if (event.getRawData() != null) {
            try {
                var rawData = JsonUtil.fromJson(event.getRawData(), java.util.Map.class);
                if (rawData != null && rawData.containsKey("flowRate")) {
                    Object flowObj = rawData.get("flowRate");
                    if (flowObj instanceof Number) {
                        return ((Number) flowObj).doubleValue();
                    }
                }
            } catch (Exception ignored) {}
        }
        return 0.0;
    }

    /**
     * 根据容量百分比确定风险等级
     */
    private WaterLevelRecord.RiskLevel determineRiskLevel(double capacityPercentage) {
        if (capacityPercentage > 95.0) {
            return WaterLevelRecord.RiskLevel.CRITICAL;
        } else if (capacityPercentage > 80.0) {
            return WaterLevelRecord.RiskLevel.HIGH;
        } else if (capacityPercentage > 60.0) {
            return WaterLevelRecord.RiskLevel.MEDIUM;
        }
        return WaterLevelRecord.RiskLevel.LOW;
    }

    /**
     * 发布灾害模拟事件
     */
    private void publishDisasterEvent(WaterStation station, FloodScenario scenario) {
        CityEvent disasterEvent = new CityEvent(
                EventDomain.DISASTER,
                "city",
                "flood.scenario",
                scenario.getAffectedArea(),
                "km²",
                EventSource.SIMULATOR
        );
        disasterEvent.setEventId(UUID.randomUUID().toString());
        disasterEvent.setRawData(JsonUtil.toJson(scenario));

        eventPublisher.publishDisasterScenario(disasterEvent);
        log.info("灾害模拟事件已发布: 场景={}, 影响面积={}km²",
                scenario.getScenarioId(), scenario.getAffectedArea());
    }
}
