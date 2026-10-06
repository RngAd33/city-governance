package com.cityplatform.traffic.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 交通流数据模型
 * 记录各路段的交通流量、速度和排队长度
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrafficFlowData {

    private String segmentId;

    private String intersectionId;
    /** 行驶方向：NS(南北) 或 EW(东西) */
    private String direction;
    /** 流量(辆/小时) */
    private double flow;
    /** 平均速度(km/h) */
    private double avgSpeed;
    /** 排队长度(m) */
    private double queueLength;
    /** 数据采集时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * 业务便利构造器：采集时间自动填充为当前时间
     */
    public TrafficFlowData(String segmentId, String intersectionId, String direction,
                          double flow, double avgSpeed, double queueLength) {
        this.segmentId = segmentId;
        this.intersectionId = intersectionId;
        this.direction = direction;
        this.flow = flow;
        this.avgSpeed = avgSpeed;
        this.queueLength = queueLength;
        this.timestamp = LocalDateTime.now();
    }

}