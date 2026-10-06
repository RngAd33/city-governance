package com.cityplatform.energy.consumer;

import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.util.JsonUtil;
import com.cityplatform.energy.model.EnergyConsumptionRecord;
import com.cityplatform.energy.repository.EnergyConsumptionRecordRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 建筑能耗事件消费者
 * 监听能耗事件队列，处理南京建筑能耗数据；
 * buildingId 优先取事件携带的南京建筑ID（rawData: buildingId=Bxxx），缺失时回落「城市_指标」合成ID
 */
@Component
@Slf4j
public class EnergyEventConsumer {

    /** 事件rawData中建筑ID前缀（如 buildingId=B001） */
    private static final String BUILDING_ID_PREFIX = "buildingId=";

    @Resource
    private EnergyConsumptionRecordRepository recordRepository;

    @RabbitListener(queues = "energy.building.queue")
    public void handleEnergyConsumptionEvent(String message) {
        try {
            CityEvent event = JsonUtil.fromJson(message, CityEvent.class);
            log.info("接收到能耗事件: {} = {} {}", event.getMetric(), event.getValue(), event.getUnit());

            EnergyConsumptionRecord record = convertToRecord(event);
            recordRepository.save(record);

            log.info("能耗记录已保存: buildingId={}, totalKwh={}", record.getBuildingId(), record.getTotalKwh());
        } catch (Exception e) {
            log.error("处理能耗事件失败: {}", e.getMessage(), e);
        }
    }

    private EnergyConsumptionRecord convertToRecord(CityEvent event) {
        EnergyConsumptionRecord record = new EnergyConsumptionRecord();
        record.setBuildingId(resolveBuildingId(event));

        if (event.getTs() != null) {
            record.setTimestamp(event.getTs().toLocalDateTime());
        } else {
            record.setTimestamp(LocalDateTime.now());
        }

        // 电/热/冷耗按指标前缀归类：electricity_consumption_{类型} 亦计入电耗
        double electricity = metricMatches(event.getMetric(), "electricity") ? event.getValue() : 0.0;
        double heating = metricMatches(event.getMetric(), "heating") ? event.getValue() : 0.0;
        double cooling = metricMatches(event.getMetric(), "cooling") ? event.getValue() : 0.0;
        double total = electricity + heating + cooling;

        record.setElectricityKwh(electricity);
        record.setHeatingKwh(heating);
        record.setCoolingKwh(cooling);
        record.setTotalKwh(total);
        record.setCarbonEmission(total * 0.58);
        record.setCost(electricity * 0.8 + heating * 0.5 + cooling * 0.6);

        return record;
    }

    /**
     * 指标匹配：等于基础指标名或为带 "_" 后缀的派生指标（如 electricity_consumption_office）
     */
    private boolean metricMatches(String metric, String base) {
        return metric != null && (metric.equals(base) || metric.startsWith(base + "_"));
    }

    /**
     * 建筑ID：优先取事件携带的南京建筑ID（rawData: buildingId=Bxxx），缺失时回落「城市_指标」合成ID
     */
    private String resolveBuildingId(CityEvent event) {
        String rawData = event.getRawData();
        if (rawData != null && rawData.startsWith(BUILDING_ID_PREFIX)) {
            String buildingId = rawData.substring(BUILDING_ID_PREFIX.length()).trim();
            if (!buildingId.isEmpty()) {
                return buildingId;
            }
        }
        return event.getCity() + "_" + event.getMetric();
    }
}
