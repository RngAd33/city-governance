package com.cityplatform.disaster.consumer;

import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.event.EventDomain;
import com.cityplatform.common.event.EventSource;
import com.cityplatform.common.mq.EventBusConfig;
import com.cityplatform.common.mq.EventPublisher;
import com.cityplatform.common.util.JsonUtil;
import com.cityplatform.disaster.model.FloodScenario;
import com.cityplatform.disaster.model.WaterStation;
import com.cityplatform.disaster.repository.FloodScenarioRepository;
import com.cityplatform.disaster.repository.WaterStationRepository;
import com.cityplatform.disaster.risk.FloodRiskAssessor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 灾害触发消费者
 * 监听灾害专属水位队列（disaster.water.queue，绑定 water.level.station），
 * 基于事件携带的真实水位/降水/长江径流量评估风险，
 * 当站点容量超阈值或水位达危险值时生成灾害场景并发布灾害模拟事件。
 * 说明：与 city-water 的水位记录消费者使用相互独立的队列，各自完整接收水位事件；
 * 本消费者只读取 water_station（共享库），写入 flood_scenario（本服务表）
 */
@Component
@Slf4j
public class DisasterWaterEventConsumer {

    /** 触发灾害模拟的容量阈值（80%） */
    private static final double SIMULATION_THRESHOLD = 80.0;

    /** 灾害场景最短生成间隔（小时），避免水位持续超标期间重复生成 */
    private static final long SCENARIO_COOLDOWN_HOURS = 6;

    /** 灾害模拟采用的预报时长（小时） */
    private static final int SIMULATION_FORECAST_HOURS = 24;

    @Resource
    private WaterStationRepository waterStationRepository;

    @Resource
    private FloodScenarioRepository floodScenarioRepository;

    @Resource
    private FloodRiskAssessor floodRiskAssessor;

    @Resource
    private EventPublisher eventPublisher;

    /**
     * 监听灾害水位队列
     */
    @RabbitListener(queues = EventBusConfig.DISASTER_WATER_QUEUE)
    public void handleWaterLevelEvent(String message) {
        log.info("接收到水位监测事件（灾害触发）: {}", message);

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
            Double flowRate = extractFlowRate(event);

            if (stationId == null || waterLevel == null) {
                log.warn("事件数据不完整，跳过处理: stationId={}, waterLevel={}",
                        stationId, waterLevel);
                return;
            }

            // 查找对应站点（只读）
            WaterStation station = waterStationRepository.findByStationId(stationId).orElse(null);
            if (station == null) {
                log.warn("未找到对应监测站点: {}", stationId);
                return;
            }

            // 以事件中的最新水位作为内存推演基线（不落库，水位写入由 city-water 负责）
            station.setCurrentLevel(waterLevel);
            double capacityPercentage = station.getCapacityPercentage();

            // 容量超过阈值或水位达到危险水位时评估是否触发灾害模拟
            if (capacityPercentage >= SIMULATION_THRESHOLD || waterLevel >= station.getDangerLevel()) {
                log.warn("站点[{}]容量已达{}%（危险水位{}m），评估是否触发洪涝灾害模拟",
                        stationId, String.format("%.1f", capacityPercentage),
                        station.getDangerLevel());

                if (isRecentlySimulated(stationId)) {
                    log.info("站点[{}]{}小时内已生成灾害场景，跳过本轮触发",
                            stationId, SCENARIO_COOLDOWN_HOURS);
                    return;
                }

                // 模拟参数采用真实数据：真实降水量、真实长江径流量与固定24小时预报时长
                double rainfallForecast = rainfall != null ? rainfall : 0.0;
                FloodScenario scenario = floodRiskAssessor.simulateFloodScenario(
                        station, rainfallForecast, SIMULATION_FORECAST_HOURS, flowRate);

                // 发布灾害模拟事件（携带真实数据来源）
                publishDisasterEvent(station, scenario, event);
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
     * 判断站点在冷却期内是否已生成过灾害场景（避免持续超标时重复生成）
     */
    private boolean isRecentlySimulated(String stationId) {
        return floodScenarioRepository.findTopByStationIdOrderByCreatedAtDesc(stationId)
                .map(scenario -> scenario.getCreatedAt() != null
                        && scenario.getCreatedAt().isAfter(
                                LocalDateTime.now().minusHours(SCENARIO_COOLDOWN_HOURS)))
                .orElse(false);
    }

    /**
     * 发布灾害模拟事件（城市与数据来源沿用触发事件的真实数据上下文）
     */
    private void publishDisasterEvent(WaterStation station, FloodScenario scenario, CityEvent triggerEvent) {
        CityEvent disasterEvent = new CityEvent(
                EventDomain.DISASTER,
                triggerEvent.getCity() != null ? triggerEvent.getCity() : "南京",
                "flood.scenario",
                scenario.getAffectedArea(),
                "km²",
                triggerEvent.getSource() != null ? triggerEvent.getSource() : EventSource.OPEN_METEO
        );
        disasterEvent.setEventId(UUID.randomUUID().toString());
        disasterEvent.setLocation(triggerEvent.getLocation());
        disasterEvent.setRawData(JsonUtil.toJson(scenario));

        eventPublisher.publishDisasterScenario(disasterEvent);
        log.info("灾害模拟事件已发布: 场景={}, 站点={}, 影响面积={}km²",
                scenario.getScenarioId(), station.getStationId(), scenario.getAffectedArea());
    }
}
