package com.cityplatform.govai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 政务AI决策服务启动类
 * 提供基于模拟本地LLM + RAG的城市治理决策支持能力
 */
@SpringBootApplication(scanBasePackages = "com.cityplatform")
public class GovAIApplication {

    public static void main(String[] args) {
        SpringApplication.run(GovAIApplication.class, args);
    }
}
