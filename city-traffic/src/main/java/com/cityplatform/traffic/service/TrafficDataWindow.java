package com.cityplatform.traffic.service;

import com.cityplatform.traffic.model.TrafficFlowData;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * 交通实时数据滑动窗口
 * 累积 city-collector 上报的 flow/speed/queue_length 监测指标（最近5分钟），
 * 为信号优化与仿真提供真实流量输入
 */
@Component
public class TrafficDataWindow {

    /** 窗口时长：5分钟 */
    private static final long WINDOW_MS = 5 * 60 * 1000L;

    /** 无样本时的兜底值（与采集端数据量级一致） */
    private static final double DEFAULT_FLOW = 800.0;
    private static final double DEFAULT_SPEED = 35.0;
    private static final double DEFAULT_QUEUE = 50.0;

    private final Deque<Sample> flowWindow = new ArrayDeque<>();
    private final Deque<Sample> speedWindow = new ArrayDeque<>();
    private final Deque<Sample> queueWindow = new ArrayDeque<>();

    private record Sample(double value, long ts) {}

    public synchronized void addFlow(double value) { push(flowWindow, value); }

    public synchronized void addSpeed(double value) { push(speedWindow, value); }

    public synchronized void addQueue(double value) { push(queueWindow, value); }

    private void push(Deque<Sample> window, double value) {
        long now = System.currentTimeMillis();
        window.addLast(new Sample(value, now));
        while (!window.isEmpty() && now - window.peekFirst().ts() > WINDOW_MS) {
            window.pollFirst();
        }
    }

    public synchronized int flowSampleCount() {
        return flowWindow.size();
    }

    public synchronized double avgFlow() { return avg(flowWindow, DEFAULT_FLOW); }

    public synchronized double avgSpeed() { return avg(speedWindow, DEFAULT_SPEED); }

    public synchronized double avgQueue() { return avg(queueWindow, DEFAULT_QUEUE); }

    private double avg(Deque<Sample> window, double fallback) {
        if (window.isEmpty()) {
            return fallback;
        }
        return window.stream().mapToDouble(Sample::value).average().orElse(fallback);
    }

    /**
     * 构建双向（NS/EW）流量数据
     * 采集数据不含方向信息，按 55% / 45% 确定性拆分；无实时样本时使用兜底值
     */
    public synchronized List<TrafficFlowData> buildFlowData(String intersectionId) {
        double flow = avgFlow();
        double speed = avgSpeed();
        double queue = avgQueue();
        TrafficFlowData ns = new TrafficFlowData(intersectionId + "-NS", intersectionId, "NS",
                Math.round(flow * 0.55), speed, queue);
        TrafficFlowData ew = new TrafficFlowData(intersectionId + "-EW", intersectionId, "EW",
                Math.round(flow * 0.45), speed, queue);
        return List.of(ns, ew);
    }
}
