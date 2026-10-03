package com.cityplatform.traffic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 交通信号配时优化服务
 * 基于遗传算法的城市交通信号配时优化
 */
@SpringBootApplication(scanBasePackages = "com.cityplatform")
public class TrafficApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrafficApplication.class, args);
    }
}
