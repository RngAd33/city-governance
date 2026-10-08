package com.cityplatform.disaster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 灾害风险评估与模拟推演服务
 * 基于真实采集数据的洪涝态势、风险评估与灾害情景模拟
 */
@SpringBootApplication(scanBasePackages = "com.cityplatform")
public class DisasterApplication {

    public static void main(String[] args) {
        SpringApplication.run(DisasterApplication.class, args);
    }
}
