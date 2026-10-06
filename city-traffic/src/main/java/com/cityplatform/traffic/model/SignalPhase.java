package com.cityplatform.traffic.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 交通信号相位模型
 * 每个相位包含方向、绿灯时间、黄灯时间和红灯时间
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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

    /**
     * 获取该相位的总时长(绿+黄+红)
     */
    public int getPhaseDuration() {
        return greenTime + yellowTime + redTime;
    }

}