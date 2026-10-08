package com.cityplatform.govai.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云百炼（DashScope）大模型配置
 */
@Component
@ConfigurationProperties(prefix = "spring.ai.dashscope")
@Data
public class DashScopeProperties {

    /** 是否启用云端大模型，关闭或未配置API Key时降级为本地模板生成 */
    @Value("${spring.ai.dashscope.enabled}")
    private boolean enabled = true;

    /** 阿里云百炼 API Key，建议通过环境变量 DASHSCOPE_API_KEY 注入 */
    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    /** 模型名称（qwen-plus / qwen-turbo 等） */
    @Value("${spring.ai.dashscope.chat.options.model}")
    private String model;

    /** 单次调用超时时间（秒） */
    private int timeoutSeconds = 60;

}