package com.cityplatform.energy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 城市能耗分析服务
 * 提供建筑能耗分析、节能建议、负荷预测等功能
 */
@SpringBootApplication(scanBasePackages = "com.cityplatform")
public class EnergyApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnergyApplication.class, args);
    }
}
