package com.cityplatform.govai.consumer;

import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.util.JsonUtil;
import com.cityplatform.govai.ai.LocalLLMService;
import com.cityplatform.govai.model.entity.DecisionRecord;
import com.cityplatform.govai.repository.DecisionRecordRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 决策建议消费者
 * 监听govai.decision.queue队列，处理其他服务发布的决策建议事件
 * 使用LLM服务分析事件并生成决策支持信息
 */
@Component
@Slf4j
public class DecisionConsumer {

    @Resource
    private LocalLLMService llmService;

    @Resource
    private DecisionRecordRepository decisionRecordRepository;

    /**
     * 监听决策建议队列
     */
    @RabbitListener(queues = "govai.decision.queue")
    public void handleDecisionSuggestion(String message) {
        log.info("收到决策建议消息: {}", message);

        try {
            CityEvent event = JsonUtil.fromJson(message, CityEvent.class);
            if (event == null) {
                log.warn("消息反序列化失败，跳过处理");
                return;
            }

            // 构建prompt
            String prompt = buildPrompt(event);
            String context = buildContext(event);

            // 调用LLM生成响应
            String response = llmService.generateResponse(prompt, context);

            // 保存决策记录
            DecisionRecord record = new DecisionRecord();
            record.setQueryType("decision_suggestion");
            record.setInput(message);
            record.setOutput(response);
            record.setConfidence(0.85);
            decisionRecordRepository.save(record);

            log.info("决策建议处理完成 - eventId: {}, domain: {}, metric: {}",
                    event.getEventId(), event.getDomain(), event.getMetric());

        } catch (Exception e) {
            log.error("处理决策建议消息时发生错误: {}", e.getMessage(), e);
        }
    }

    private String buildPrompt(CityEvent event) {
        return String.format("分析以下城市事件并提供决策建议：领域=%s, 城市=%s, 指标=%s, 数值=%s%s",
                event.getDomain(), event.getCity(), event.getMetric(),
                event.getValue(), event.getUnit() != null ? event.getUnit() : "");
    }

    private String buildContext(CityEvent event) {
        StringBuilder context = new StringBuilder();
        context.append("事件来源: ").append(event.getSource()).append("\n");
        context.append("事件时间: ").append(event.getTs()).append("\n");
        if (event.getLocation() != null) {
            context.append("位置: ").append(event.getLocation()).append("\n");
        }
        if (event.getRawData() != null) {
            context.append("原始数据: ").append(event.getRawData());
        }
        return context.toString();
    }
}
