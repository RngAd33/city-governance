package com.cityplatform.infra.consumer;

import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.mq.EventBusConfig;
import com.cityplatform.common.util.JsonUtil;
import com.cityplatform.infra.model.FacilityStatusRecord;
import com.cityplatform.infra.repository.FacilityStatusRecordRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * 基础设施状态事件消费者
 * 监听基础设施监控队列，将设施状态评分/告警级别写入 infra_status_record 表
 */
@Component
@Slf4j
public class InfraEventConsumer {

    /** 状态评分指标前缀（facility_status_{facility}） */
    private static final String STATUS_PREFIX = "facility_status_";

    /** 告警级别指标前缀（alert_level_{facility}） */
    private static final String ALERT_PREFIX = "alert_level_";

    @Resource
    private FacilityStatusRecordRepository recordRepository;

    /**
     * 监听基础设施监控队列
     */
    @RabbitListener(queues = EventBusConfig.INFRA_MONITOR_QUEUE)
    public void handleInfraEvent(String message) {
        try {
            CityEvent event = JsonUtil.fromJson(message, CityEvent.class);
            if (event == null || event.getMetric() == null || event.getValue() == null) {
                log.warn("基础设施事件数据不完整，跳过处理");
                return;
            }

            // 解析指标：facility_status_{facility} 或 alert_level_{facility}
            String metric = event.getMetric();
            String metricType;
            String facility;
            if (metric.startsWith(STATUS_PREFIX)) {
                metricType = "facility_status";
                facility = metric.substring(STATUS_PREFIX.length());
            } else if (metric.startsWith(ALERT_PREFIX)) {
                metricType = "alert_level";
                facility = metric.substring(ALERT_PREFIX.length());
            } else {
                log.warn("未知的基础设施指标类型，跳过处理: {}", metric);
                return;
            }

            OffsetDateTime ts = event.getTs() != null ? event.getTs() : OffsetDateTime.now();
            FacilityStatusRecord record = new FacilityStatusRecord(
                    facility,
                    metricType,
                    event.getValue(),
                    event.getUnit(),
                    ts,
                    event.getCity(),
                    event.getSource() != null ? event.getSource().name() : "UNKNOWN");
            recordRepository.save(record);
            log.info("基础设施记录已保存: facility={}, metric={}, value={}",
                    facility, metricType, event.getValue());
        } catch (Exception e) {
            log.error("处理基础设施事件时发生错误: {}", e.getMessage(), e);
        }
    }
}
