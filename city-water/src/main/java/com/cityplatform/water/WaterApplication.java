package com.cityplatform.water;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 水务风险与灾害模拟服务
 * 城市水位监测、洪涝风险评估与灾害模拟
 */
@SpringBootApplication(scanBasePackages = "com.cityplatform")
public class WaterApplication {

    public static void main(String[] args) {
        SpringApplication.run(WaterApplication.class, args);
    }
}
