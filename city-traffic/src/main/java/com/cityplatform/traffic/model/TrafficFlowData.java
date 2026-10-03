package com.cityplatform.traffic.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/**
 * 交通流数据模型
 * 记录各路段的交通流量、速度和排队长度
 */
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

    public TrafficFlowData() {}

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

    // Getters and Setters
    public String getSegmentId() { return segmentId; }
    public void setSegmentId(String segmentId) { this.segmentId = segmentId; }

    public String getIntersectionId() { return intersectionId; }
    public void setIntersectionId(String intersectionId) { this.intersectionId = intersectionId; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public double getFlow() { return flow; }
    public void setFlow(double flow) { this.flow = flow; }

    public double getAvgSpeed() { return avgSpeed; }
    public void setAvgSpeed(double avgSpeed) { this.avgSpeed = avgSpeed; }

    public double getQueueLength() { return queueLength; }
    public void setQueueLength(double queueLength) { this.queueLength = queueLength; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "TrafficFlowData{" +
                "segmentId='" + segmentId + '\'' +
                ", intersectionId='" + intersectionId + '\'' +
                ", direction='" + direction + '\'' +
                ", flow=" + flow +
                ", avgSpeed=" + avgSpeed +
                ", queueLength=" + queueLength +
                ", timestamp=" + timestamp +
                '}';
    }
}
