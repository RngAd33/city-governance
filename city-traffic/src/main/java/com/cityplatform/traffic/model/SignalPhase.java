package com.cityplatform.traffic.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 交通信号相位模型
 * 每个相位包含方向、绿灯时间、黄灯时间和红灯时间
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SignalPhase {

    private String phaseId;
    /** 方向：NS(南北) 或 EW(东西) */
    private String direction;
    /** 绿灯时间(秒) */
    private int greenTime;
    /** 黄灯时间(秒)，默认3秒 */
    private int yellowTime = 3;
    /** 红灯时间(秒) */
    private int redTime;

    public SignalPhase() {}

    public SignalPhase(String phaseId, String direction, int greenTime, int redTime) {
        this.phaseId = phaseId;
        this.direction = direction;
        this.greenTime = greenTime;
        this.yellowTime = 3;
        this.redTime = redTime;
    }

    public SignalPhase(String phaseId, String direction, int greenTime, int yellowTime, int redTime) {
        this.phaseId = phaseId;
        this.direction = direction;
        this.greenTime = greenTime;
        this.yellowTime = yellowTime;
        this.redTime = redTime;
    }

    /**
     * 获取该相位的总时长(绿+黄+红)
     */
    public int getPhaseDuration() {
        return greenTime + yellowTime + redTime;
    }

    // Getters and Setters
    public String getPhaseId() { return phaseId; }
    public void setPhaseId(String phaseId) { this.phaseId = phaseId; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public int getGreenTime() { return greenTime; }
    public void setGreenTime(int greenTime) { this.greenTime = greenTime; }

    public int getYellowTime() { return yellowTime; }
    public void setYellowTime(int yellowTime) { this.yellowTime = yellowTime; }

    public int getRedTime() { return redTime; }
    public void setRedTime(int redTime) { this.redTime = redTime; }

    @Override
    public String toString() {
        return "SignalPhase{" +
                "phaseId='" + phaseId + '\'' +
                ", direction='" + direction + '\'' +
                ", greenTime=" + greenTime +
                ", yellowTime=" + yellowTime +
                ", redTime=" + redTime +
                '}';
    }
}
