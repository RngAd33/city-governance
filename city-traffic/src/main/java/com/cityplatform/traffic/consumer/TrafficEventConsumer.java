package com.cityplatform.traffic.consumer;

import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.event.EventDomain;
import com.cityplatform.common.event.EventSource;
import com.cityplatform.common.mq.EventPublisher;
import com.cityplatform.common.util.JsonUtil;
import com.cityplatform.traffic.model.Intersection;
import com.cityplatform.traffic.model.SignalPhase;
import com.cityplatform.traffic.model.TrafficFlowData;
import com.cityplatform.traffic.optimizer.GeneticAlgorithmOptimizer;
import com.cityplatform.traffic.simulator.TrafficSimulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 交通事件消费者
 * 监听交通流数据队列，触发信号优化并发布优化结果
 */
@Component
public class TrafficEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TrafficEventConsumer.class);

    @Autowired
    private GeneticAlgorithmOptimizer optimizer;

    @Autowired
    private TrafficSimulator simulator;

    @Autowired
    private EventPublisher eventPublisher;

    /**
     * 监听交通流数据队列
     * 接收交通流数据后触发信号优化
     */
    @RabbitListener(queues = "traffic.flow.queue")
    public void handleTrafficFlow(String message) {
        log.info("收到交通流数据: {}", message);

        try {
            // 解析交通流数据
            TrafficFlowData flowData = JsonUtil.fromJson(message, TrafficFlowData.class);
            if (flowData == null) {
                log.warn("交通流数据解析失败: {}", message);
                return;
            }

            // 构建路口配置(实际应从数据库或配置中心获取)
            Intersection intersection = buildIntersectionFromFlow(flowData);

            // 构建流量数据列表
            List<TrafficFlowData> flowDataList = new ArrayList<>();
            flowDataList.add(flowData);

            // 执行遗传算法优化
            List<SignalPhase> optimizedPhases = optimizer.optimize(
                    intersection.getPhases(), flowDataList);

            // 模拟优化后的效果
            TrafficSimulator.SimulationResult beforeResult = simulator.simulate(
                    intersection.getPhases(), 60);
            TrafficSimulator.SimulationResult afterResult = simulator.simulate(
                    optimizedPhases, 60);

            // 计算改善百分比
            double improvementPercent = calculateImprovement(beforeResult, afterResult);

            log.info("信号优化完成: 路口={}, 改善={}%", intersection.getIntersectionId(),
                    String.format("%.2f", improvementPercent));

            // 发布优化结果事件
            publishOptimizationEvent(intersection, optimizedPhases, improvementPercent);

        } catch (Exception e) {
            log.error("处理交通流数据异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 从流量数据构建路口配置
     * 实际应用中应从数据库或配置中心获取
     */
    private Intersection buildIntersectionFromFlow(TrafficFlowData flowData) {
        Intersection intersection = new Intersection();
        intersection.setIntersectionId(flowData.getIntersectionId());
        intersection.setName("路口-" + flowData.getIntersectionId());
        intersection.setLat(31.2304);
        intersection.setLng(121.4737);

        // 默认两相位配置
        SignalPhase phase1 = new SignalPhase("P1", "NS", 30, 3, 30);
        SignalPhase phase2 = new SignalPhase("P2", "EW", 30, 3, 30);
        intersection.addPhase(phase1);
        intersection.addPhase(phase2);

        return intersection;
    }

    /**
     * 计算优化改善百分比
     */
    private double calculateImprovement(TrafficSimulator.SimulationResult before,
                                        TrafficSimulator.SimulationResult after) {
        if (before.getTotalWaitTime() <= 0) {
            return 0.0;
        }
        double improvement = (before.getTotalWaitTime() - after.getTotalWaitTime())
                / before.getTotalWaitTime() * 100.0;
        return Math.max(0.0, improvement);
    }

    /**
     * 发布信号优化结果事件
     */
    private void publishOptimizationEvent(Intersection intersection,
                                         List<SignalPhase> optimizedPhases,
                                         double improvementPercent) {
        try {
            CityEvent event = new CityEvent();
            event.setDomain(EventDomain.TRAFFIC);
            event.setCity("上海市");
            event.setMetric("signal_optimization");
            event.setValue(improvementPercent);
            event.setUnit("percent");
            event.setSource(EventSource.SIMULATOR);
            event.setTs(OffsetDateTime.now());

            // 构建优化结果数据
            StringBuilder rawData = new StringBuilder();
            rawData.append("{");
            rawData.append("\"intersectionId\":\"").append(intersection.getIntersectionId()).append("\",");
            rawData.append("\"improvementPercent\":").append(String.format("%.2f", improvementPercent)).append(",");
            rawData.append("\"phases\":[");
            for (int i = 0; i < optimizedPhases.size(); i++) {
                SignalPhase p = optimizedPhases.get(i);
                rawData.append("{");
                rawData.append("\"phaseId\":\"").append(p.getPhaseId()).append("\",");
                rawData.append("\"direction\":\"").append(p.getDirection()).append("\",");
                rawData.append("\"greenTime\":").append(p.getGreenTime()).append(",");
                rawData.append("\"yellowTime\":").append(p.getYellowTime()).append(",");
                rawData.append("\"redTime\":").append(p.getRedTime());
                rawData.append("}");
                if (i < optimizedPhases.size() - 1) {
                    rawData.append(",");
                }
            }
            rawData.append("]}");
            event.setRawData(rawData.toString());

            eventPublisher.publishSignalOptimization(event);
            log.info("信号优化结果已发布: 路口={}, 改善={}%",
                    intersection.getIntersectionId(), String.format("%.2f", improvementPercent));

        } catch (Exception e) {
            log.error("发布信号优化结果异常: {}", e.getMessage(), e);
        }
    }
}
