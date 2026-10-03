package com.cityplatform.collector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 城市数据采集服务
 * 模拟多源城市数据并发布到RabbitMQ事件总线
 */
@SpringBootApplication(scanBasePackages = "com.cityplatform")
@EnableScheduling
public class CollectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CollectorApplication.class, args);
    }
}
