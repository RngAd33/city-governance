package com.cityplatform.energy.consumer;

import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.mq.EventBusConfig;
import com.cityplatform.common.util.JsonUtil;
import com.cityplatform.energy.model.GridEnergyRecord;
import com.cityplatform.energy.repository.GridEnergyRecordRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * 电网指标事件消费者
 * 接收国内免费公开电网数据源事件（OWID 中国年度：碳强度/可再生占比），
 * 写入 grid_energy_records 表；值与时间戳均未变化时跳过重复写入
 */
@Component
@Slf4j
public class GridEnergyEventConsumer {

    @Resource
    private GridEnergyRecordRepository gridRepository;

    @RabbitListener(queues = EventBusConfig.ENERGY_GRID_QUEUE)
    public void handleGridIndicatorEvent(String message) {
        try {
            CityEvent event = JsonUtil.fromJson(message, CityEvent.class);
            log.info("接收到电网指标事件: {} = {} {}", event.getMetric(), event.getValue(), event.getUnit());

            GridEnergyRecord record = new GridEnergyRecord();
            record.setZone(event.getCity());
            record.setMetric(event.getMetric());
            record.setValue(event.getValue());
            record.setUnit(event.getUnit());
            record.setSource(event.getSource() != null ? event.getSource().getCode() : null);
            // rawData 可携带估算标记（estimated=true 为模型估算值；OWID 统计值非估算）
            record.setEstimated(event.getRawData() != null && event.getRawData().contains("estimated=true"));
            record.setTs(event.getTs() != null ? event.getTs() : OffsetDateTime.now());

            // 年度指标值与时间戳均未变化时跳过：避免每轮采集追加重复行、历史曲线出现重复点
            boolean unchanged = gridRepository.findTopByZoneAndMetricOrderByTsDesc(record.getZone(), record.getMetric())
                    .filter(last -> Objects.equals(last.getValue(), record.getValue())
                            && Objects.equals(last.getTs(), record.getTs()))
                    .isPresent();
            if (unchanged) {
                log.info("电网指标无变化，跳过重复写入: zone={}, metric={}, value={}",
                        record.getZone(), record.getMetric(), record.getValue());
                return;
            }

            gridRepository.save(record);
            log.info("电网指标已保存: zone={}, metric={}, value={}",
                    record.getZone(), record.getMetric(), record.getValue());
        } catch (Exception e) {
            log.error("处理电网指标事件失败: {}", e.getMessage(), e);
        }
    }
}
