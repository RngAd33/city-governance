package com.cityplatform.infra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 基础设施运维服务
 * 消费基础设施状态事件并对外提供实时监控接口
 */
@SpringBootApplication(scanBasePackages = "com.cityplatform")
public class InfraApplication {

    public static void main(String[] args) {
        SpringApplication.run(InfraApplication.class, args);
    }
}
