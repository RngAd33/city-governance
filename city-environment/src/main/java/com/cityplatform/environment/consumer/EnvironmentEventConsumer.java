package com.cityplatform.environment.consumer;

import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.util.JsonUtil;
import com.cityplatform.environment.model.AlertRecord;
import com.cityplatform.environment.model.AlertRule;
import com.cityplatform.environment.model.EnvironmentRecord;
import com.cityplatform.environment.model.enums.EnvironmentLevel;
import com.cityplatform.environment.monitor.AirQualityMonitor;
import com.cityplatform.environment.repository.AlertRecordRepository;
import com.cityplatform.environment.repository.AlertRuleRepository;
import com.cityplatform.environment.repository.EnvironmentRecordRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 环境事件消费者
 * 监听气象和空气质量事件队列，处理事件并检查告警规则
 */
@Component
@Slf4j
public class EnvironmentEventConsumer {

    @Resource
    private EnvironmentRecordRepository environmentRecordRepository;

    @Resource
    private AlertRuleRepository alertRuleRepository;

    @Resource
    private AlertRecordRepository alertRecordRepository;

    @Resource
    private AirQualityMonitor airQualityMonitor;

    /**
     * 监听气象事件队列
     */
    @RabbitListener(queues = {"env.weather.queue"})
    @Transactional
    public void handleWeatherEvent(String message) {
        try {
            CityEvent event = JsonUtil.fromJson(message, CityEvent.class);
            if (event == null) {
                log.warn("气象事件反序列化失败: {}", message);
                return;
            }
            log.info("接收到气象事件: {} = {} {}", event.getMetric(), event.getValue(), event.getUnit());

            // 保存环境记录
            EnvironmentRecord record = saveEnvironmentRecord(event);

            // 检查告警规则
            checkAndTriggerAlerts(event, record);

        } catch (Exception e) {
            log.error("处理气象事件异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 监听空气质量事件队列
     */
    @RabbitListener(queues = {"env.airquality.queue"})
    @Transactional
    public void handleAirQualityEvent(String message) {
        try {
            CityEvent event = JsonUtil.fromJson(message, CityEvent.class);
            if (event == null) {
                log.warn("空气质量事件反序列化失败: {}", message);
                return;
            }
            log.info("接收到空气质量事件: {} = {} {}", event.getMetric(), event.getValue(), event.getUnit());

            // 保存环境记录
            EnvironmentRecord record = saveEnvironmentRecord(event);

            // 检查告警规则
            checkAndTriggerAlerts(event, record);

        } catch (Exception e) {
            log.error("处理空气质量事件异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 保存环境记录到数据库
     */
    private EnvironmentRecord saveEnvironmentRecord(CityEvent event) {
        EnvironmentLevel level = determineLevel(event.getMetric(), event.getValue());

        EnvironmentRecord record = new EnvironmentRecord();
        record.setRecordId(event.getEventId() != null ? event.getEventId() : UUID.randomUUID().toString());
        record.setCity(event.getCity());
        record.setStationName(event.getSource() != null ? event.getSource().getCode() : "unknown");
        record.setLat(event.getLocation() != null ? event.getLocation().getLat() : 0.0);
        record.setLng(event.getLocation() != null ? event.getLocation().getLng() : 0.0);
        record.setMetric(event.getMetric());
        record.setValue(event.getValue());
        record.setUnit(event.getUnit());
        record.setLevel(level);
        record.setTs(event.getTs() != null ? event.getTs() : java.time.OffsetDateTime.now());

        EnvironmentRecord saved = environmentRecordRepository.save(record);
        log.info("环境记录已保存: id={}, metric={}, value={}", saved.getId(), saved.getMetric(), saved.getValue());
        return saved;
    }

    /**
     * 检查告警规则并触发告警
     */
    private void checkAndTriggerAlerts(CityEvent event, EnvironmentRecord record) {
        List<AlertRule> rules = alertRuleRepository.findByEnabledTrue();
        if (rules == null || rules.isEmpty()) return;

        List<AlertRecord> triggeredAlerts = airQualityMonitor.checkAlertRules(
                rules, event.getMetric(), event.getValue());

        for (AlertRecord alert : triggeredAlerts) {
            alert.setCity(event.getCity());
            if (event.getLocation() != null) {
                alert.setLocation(String.format("%.6f,%.6f",
                        event.getLocation().getLat(), event.getLocation().getLng()));
            }
            alertRecordRepository.save(alert);
            log.warn("告警已触发: {} - {} = {} (阈值: {})",
                    alert.getSeverity(), alert.getMetric(), alert.getValue(), alert.getThreshold());
        }
    }

    /**
     * 根据指标值判断环境等级
     */
    private EnvironmentLevel determineLevel(String metric, Double value) {
        if (value == null) return EnvironmentLevel.NORMAL;

        String lowerMetric = metric.toLowerCase();
        if (lowerMetric.contains("aqi")) {
            return airQualityMonitor.getEnvironmentLevel(value.intValue());
        }
        if (lowerMetric.contains("pm25") || lowerMetric.contains("pm10")) {
            if (value <= 75) return EnvironmentLevel.NORMAL;
            if (value <= 150) return EnvironmentLevel.WARNING;
            return EnvironmentLevel.DANGER;
        }
        if (lowerMetric.contains("temperature") || lowerMetric.contains("temp")) {
            if (value < -10 || value > 40) return EnvironmentLevel.DANGER;
            if (value < 0 || value > 35) return EnvironmentLevel.WARNING;
            return EnvironmentLevel.NORMAL;
        }
        if (lowerMetric.contains("humidity")) {
            if (value < 20 || value > 90) return EnvironmentLevel.WARNING;
            return EnvironmentLevel.NORMAL;
        }
        if (lowerMetric.contains("wind")) {
            if (value > 50) return EnvironmentLevel.DANGER;
            if (value > 30) return EnvironmentLevel.WARNING;
            return EnvironmentLevel.NORMAL;
        }
        return EnvironmentLevel.NORMAL;
    }
}
