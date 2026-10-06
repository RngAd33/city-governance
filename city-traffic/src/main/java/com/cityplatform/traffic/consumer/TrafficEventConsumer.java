package com.cityplatform.traffic.consumer;

import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.event.EventDomain;
import com.cityplatform.common.mq.EventBusConfig;
import com.cityplatform.common.util.JsonUtil;
import com.cityplatform.traffic.service.TrafficDataWindow;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 交通流事件消费者
 * 监听交通流队列（消息为 CityEvent JSON），将实时监测指标（flow/speed/queue_length）
 * 累积到滑动窗口，供 /api/traffic/segments 等查询接口提供实时流量输入；
 * 信号仿真由前端手动触发（/api/traffic/simulation/run），本消费者不执行自动优化
 */
@Component
@Slf4j
public class TrafficEventConsumer {

    @Resource
    private TrafficDataWindow dataWindow;

    /**
     * 监听交通流数据队列，仅累积窗口数据
     */
    @RabbitListener(queues = EventBusConfig.TRAFFIC_FLOW_QUEUE)
    public void handleTrafficFlow(String message) {
        try {
            CityEvent event = JsonUtil.fromJson(message, CityEvent.class);
            if (event == null || event.getDomain() != EventDomain.TRAFFIC
                    || event.getMetric() == null || event.getValue() == null) {
                return;
            }
            switch (event.getMetric()) {
                case "flow" -> dataWindow.addFlow(event.getValue());
                case "speed" -> dataWindow.addSpeed(event.getValue());
                case "queue_length" -> dataWindow.addQueue(event.getValue());
                default -> log.debug("忽略非监测指标: {}", event.getMetric());
            }
        } catch (Exception e) {
            log.error("处理交通流数据异常: {}", e.getMessage(), e);
        }
    }
}
