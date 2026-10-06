package com.cityplatform.traffic.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 路口模型
 * 包含多个信号相位，可计算周期长度和绿信比
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Intersection {

    private String intersectionId;
    private String name;
    private double lat;
    private double lng;
    private List<SignalPhase> phases = new ArrayList<>();
    private int currentCycleLength;

    /**
     * 计算信号周期长度
     * 周期 = 所有相位的(绿灯+黄灯+红灯)之和
     */
    public int getCycleLength() {
        if (phases == null || phases.isEmpty()) {
            return 0;
        }
        int cycle = phases.stream()
                .mapToInt(SignalPhase::getPhaseDuration)
                .sum();
        this.currentCycleLength = cycle;
        return cycle;
    }

    /**
     * 获取指定相位的绿信比
     * 绿信比 = 绿灯时间 / 周期长度
     */
    public double getGreenRatio(SignalPhase phase) {
        int cycle = getCycleLength();
        if (cycle == 0) {
            return 0.0;
        }
        return (double) phase.getGreenTime() / cycle;
    }

    /**
     * 获取指定相位的绿信比(通过相位ID)
     */
    public double getGreenRatio(String phaseId) {
        return phases.stream()
                .filter(p -> p.getPhaseId().equals(phaseId))
                .findFirst()
                .map(this::getGreenRatio)
                .orElse(0.0);
    }

    /**
     * 添加相位
     */
    public void addPhase(SignalPhase phase) {
        this.phases.add(phase);
    }
}
