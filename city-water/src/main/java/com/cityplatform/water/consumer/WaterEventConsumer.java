package com.cityplatform.water.consumer;

import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.mq.EventBusConfig;
import com.cityplatform.common.util.JsonUtil;
import com.cityplatform.water.model.WaterLevelRecord;
import com.cityplatform.water.model.WaterStation;
import com.cityplatform.water.repository.WaterLevelRecordRepository;
import com.cityplatform.water.repository.WaterStationRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 水位监测事件消费者
 * 监听水位监测队列，更新站点当前水位并持久化水位记录；
 * 灾害风险评估与场景生成已迁移至 city-disaster 服务（disaster.water.queue）
 */
@Component
@Slf4j
public class WaterEventConsumer {

    @Resource
    private WaterStationRepository waterStationRepository;

    @Resource
    private WaterLevelRecordRepository waterLevelRecordRepository;

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
            Double flowRate = extractFlowRate(event);

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

            // 计算容量百分比并确定风险等级
            double capacityPercentage = station.getCapacityPercentage();
            WaterLevelRecord.RiskLevel riskLevel = determineRiskLevel(capacityPercentage);

            // 保存水位记录
            WaterLevelRecord record = new WaterLevelRecord(
                    stationId,
                    LocalDateTime.now(),
                    waterLevel,
                    flowRate,
                    rainfall != null ? rainfall : 0.0,
                    riskLevel
            );
            waterLevelRecordRepository.save(record);
            log.info("水位记录已保存 [{}]: 水位={}m, 风险等级={}",
                    stationId, waterLevel, riskLevel);

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
}
