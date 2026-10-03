package com.cityplatform.common.mq;

import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 城市事件发布器
 * 各服务通过此发布器将事件发送到RabbitMQ事件总线
 */
@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public EventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发布事件到指定routing key
     */
    public void publish(String routingKey, CityEvent event) {
        try {
            String json = JsonUtil.toJson(event);
            rabbitTemplate.convertAndSend(routingKey, json);
            log.info("事件已发布 [{}]: {} = {} {}", routingKey, event.getMetric(), event.getValue(), event.getUnit());
        } catch (Exception e) {
            log.error("事件发布失败 [{}]: {}", routingKey, e.getMessage());
        }
    }

    /**
     * 发布气象事件
     */
    public void publishWeather(CityEvent event) {
        publish(EventBusConfig.RK_ENV_WEATHER, event);
    }

    /**
     * 发布空气质量事件
     */
    public void publishAirQuality(CityEvent event) {
        publish(EventBusConfig.RK_ENV_AIRQUALITY, event);
    }

    /**
     * 发布交通流事件
     */
    public void publishTrafficFlow(CityEvent event) {
        publish(EventBusConfig.RK_TRAFFIC_FLOW, event);
    }

    /**
     * 发布信号优化结果
     */
    public void publishSignalOptimization(CityEvent event) {
        publish(EventBusConfig.RK_TRAFFIC_SIGNAL, event);
    }

    /**
     * 发布建筑能耗事件
     */
    public void publishEnergyConsumption(CityEvent event) {
        publish(EventBusConfig.RK_ENERGY_BUILDING, event);
    }

    /**
     * 发布水位监测事件
     */
    public void publishWaterLevel(CityEvent event) {
        publish(EventBusConfig.RK_WATER_MONITOR, event);
    }

    /**
     * 发布灾害模拟事件
     */
    public void publishDisasterScenario(CityEvent event) {
        publish(EventBusConfig.RK_DISASTER_SIM, event);
    }

    /**
     * 发布政务AI决策建议
     */
    public void publishDecisionSuggestion(CityEvent event) {
        publish(EventBusConfig.RK_GOVAI_DECISION, event);
    }
}
