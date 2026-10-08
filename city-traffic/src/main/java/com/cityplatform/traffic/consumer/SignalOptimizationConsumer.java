package com.cityplatform.traffic.consumer;

import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.mq.EventBusConfig;
import com.cityplatform.common.util.JsonUtil;
import com.cityplatform.traffic.entity.OptimizationRecord;
import com.cityplatform.traffic.repository.OptimizationRecordRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 信号优化结果消费者
 * 监听信号优化队列，将优化结果与前后仿真指标写入 optimization_record 表，
 * source 字段记录本次仿真所用的优化策略（genetic/rule/reinforcement）
 */
@Component
@Slf4j
public class SignalOptimizationConsumer {

    @Resource
    private OptimizationRecordRepository recordRepository;

    @RabbitListener(queues = EventBusConfig.TRAFFIC_SIGNAL_QUEUE)
    public void handleSignalOptimization(String message) {
        try {
            CityEvent event = JsonUtil.fromJson(message, CityEvent.class);
            if (event == null || !"signal_optimization".equals(event.getMetric())) {
                // 兼容历史版本误入本队列的监测指标消息（如 queue_length），直接忽略
                return;
            }

            Map<String, Object> raw = event.getRawData() != null
                    ? JsonUtil.fromJson(event.getRawData(), Map.class)
                    : null;
            if (raw == null || raw.get("intersectionId") == null) {
                log.warn("信号优化事件缺少必要字段，跳过入库");
                return;
            }

            OptimizationRecord record = new OptimizationRecord();
            record.setIntersectionId(String.valueOf(raw.get("intersectionId")));
            record.setImprovementPercent(event.getValue());
            record.setBeforeConfig(JsonUtil.toJson(raw.get("before")));
            record.setAfterConfig(JsonUtil.toJson(raw.get("after")));
            record.setOptimizedPhases(JsonUtil.toJson(raw.get("optimizedPhases")));
            Object strategy = raw.get("strategy") != null ? raw.get("strategy") : raw.get("source");
            record.setSource(strategy != null ? String.valueOf(strategy) : "genetic");
            recordRepository.save(record);

            log.info("信号优化记录已入库: 路口={}, 改善={}%", record.getIntersectionId(), event.getValue());
        } catch (Exception e) {
            log.error("处理信号优化事件异常: {}", e.getMessage(), e);
        }
    }
}
