package com.cityplatform.environment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 环境监测服务启动类
 * 负责空气质量监测、气象趋势分析、环境告警管理
 */
@SpringBootApplication(scanBasePackages = "com.cityplatform")
public class EnvironmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnvironmentApplication.class, args);
    }
}
