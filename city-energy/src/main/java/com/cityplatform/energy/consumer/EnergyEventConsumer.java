package com.cityplatform.energy.consumer;

import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.util.JsonUtil;
import com.cityplatform.energy.model.EnergyConsumptionRecord;
import com.cityplatform.energy.repository.EnergyConsumptionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * 建筑能耗事件消费者
 * 监听能耗事件队列，处理建筑能耗数据
 */
@Component
public class EnergyEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EnergyEventConsumer.class);

    private final EnergyConsumptionRecordRepository recordRepository;

    public EnergyEventConsumer(EnergyConsumptionRecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

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
        record.setBuildingId(event.getCity() + "_" + event.getMetric());

        if (event.getTs() != null) {
            record.setTimestamp(event.getTs().toLocalDateTime());
        } else {
            record.setTimestamp(LocalDateTime.now());
        }

        double electricity = "electricity".equals(event.getMetric()) ? event.getValue() : 0.0;
        double heating = "heating".equals(event.getMetric()) ? event.getValue() : 0.0;
        double cooling = "cooling".equals(event.getMetric()) ? event.getValue() : 0.0;
        double total = electricity + heating + cooling;

        record.setElectricityKwh(electricity);
        record.setHeatingKwh(heating);
        record.setCoolingKwh(cooling);
        record.setTotalKwh(total);
        record.setCarbonEmission(total * 0.58);
        record.setCost(electricity * 0.8 + heating * 0.5 + cooling * 0.6);

        return record;
    }
}
