package com.cityplatform.water;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 水务监测服务
 * 城市水位监测记录与排水能力分析（灾害评估与模拟已迁移至 city-disaster）
 */
@SpringBootApplication(scanBasePackages = "com.cityplatform")
public class WaterApplication {

    public static void main(String[] args) {
        SpringApplication.run(WaterApplication.class, args);
    }
}
