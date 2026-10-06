package com.cityplatform.collector.simulator;

import com.cityplatform.collector.config.CollectorProperties;
import com.cityplatform.collector.entity.EventRecord;
import com.cityplatform.collector.repository.EventRecordRepository;
import com.cityplatform.collector.source.SimulatorEnvironmentSource;
import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.event.EventDomain;
import com.cityplatform.common.event.EventSource;
import com.cityplatform.common.event.GeoLocation;
import com.cityplatform.common.mq.EventPublisher;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 城市数据模拟器
 * 定时生成模拟城市数据并发布到事件总线
 */
@Service
@Slf4j
public class DataSimulatorService {

    private static final String CITY_NAME = "南京";
    private static final double CITY_LAT = 32.06;
    private static final double CITY_LNG = 118.79;
    private static final String CITY_ADDRESS = "江苏省南京市";

    /** 南京建筑清单（与 sql/init_fixed.sql 种子 B001-B008 一致）：能源模拟仅生成南京地区建筑数据。
     *  日均能耗基线 = 真实建筑面积 × 行业能耗强度（参考知识库 STD-003：办公 80-100 / 医疗 150 / 商业 120 / 教育 55 / 住宅 35 kWh/㎡·年） */
    private static final List<NanjingBuilding> NANJING_BUILDINGS = List.of(
            new NanjingBuilding("B001", "office", 6100),       // 南京市政务服务中心 2.8万㎡
            new NanjingBuilding("B002", "hospital", 197300),   // 江苏省人民医院 48万㎡
            new NanjingBuilding("B003", "commercial", 102900), // 德基广场 31.3万㎡
            new NanjingBuilding("B004", "residential", 70900), // 仁恒江湾城 74万㎡
            new NanjingBuilding("B005", "office", 71500),      // 紫峰大厦 26.1万㎡
            new NanjingBuilding("B006", "school", 20600),      // 南京外国语学校 13.7万㎡
            new NanjingBuilding("B007", "commercial", 78900),  // 南京国际博览中心 36万㎡
            new NanjingBuilding("B008", "office", 3000));      // 南京人民大会堂 1.2万㎡

    @Resource
    private EventPublisher eventPublisher;

    @Resource
    private EventRecordRepository eventRecordRepository;

    @Resource
    private SimulatorEnvironmentSource simulatorEnvironmentSource;

    @Resource
    private CollectorProperties collectorProperties;

    private final Random random = new Random();

    /**
     * 每10秒执行一次数据采集与发布
     */
    @Scheduled(fixedRate = 10000)
    public void collectAndPublish() {
        log.info("开始新一轮城市数据采集模拟...");

        List<CityEvent> events = new ArrayList<>();

        // 气象与空气质量数据：可通过 collector.simulator.environment-enabled 关闭，改由外部数据源采集
        if (collectorProperties.getSimulator().isEnvironmentEnabled()) {
            // 气象数据
            events.addAll(simulatorEnvironmentSource.generateWeatherData());
            // 空气质量数据
            events.addAll(simulatorEnvironmentSource.generateAirQualityData());
        }
        // 交通流数据
        events.addAll(generateTrafficFlowData());
        // 能源消耗数据
        events.addAll(generateEnergyConsumptionData());
        // 水务监测数据：由 WaterLevelCollectionService 以真实降水与长江径流量推算（非随机模拟）
        // 基础设施状态数据
        events.addAll(generateInfrastructureData());

        // 发布并持久化
        for (CityEvent event : events) {
            event.setEventId(UUID.randomUUID().toString());
            event.setTs(OffsetDateTime.now());
            event.setLocation(new GeoLocation(CITY_LAT, CITY_LNG, CITY_ADDRESS));

            // 发布到RabbitMQ
            publishEvent(event);

            // 持久化到数据库
            saveEventRecord(event);
        }

        log.info("本轮数据采集完成，共生成 {} 条事件", events.size());
    }

    /**
     * 生成交通流数据
     */
    private List<CityEvent> generateTrafficFlowData() {
        List<CityEvent> events = new ArrayList<>();

        double flow = random.nextDouble() * 2000.0;
        events.add(new CityEvent(EventDomain.TRAFFIC, CITY_NAME,
                "flow", round(flow, 0), "vehicles/h", EventSource.SIMULATOR));

        double speed = 10.0 + random.nextDouble() * 70.0;
        events.add(new CityEvent(EventDomain.TRAFFIC, CITY_NAME,
                "speed", round(speed, 1), "km/h", EventSource.SIMULATOR));

        double queueLength = random.nextDouble() * 500.0;
        events.add(new CityEvent(EventDomain.TRAFFIC, CITY_NAME,
                "queue_length", round(queueLength, 0), "m", EventSource.SIMULATOR));

        return events;
    }

    /**
     * 生成能源消耗数据（仅南京地区建筑）：随机选取一栋南京建筑，
     * 以日均能耗为基线波动，事件携带建筑ID（rawData）供 city-energy 按建筑归档
     */
    private List<CityEvent> generateEnergyConsumptionData() {
        List<CityEvent> events = new ArrayList<>();

        NanjingBuilding building = NANJING_BUILDINGS.get(random.nextInt(NANJING_BUILDINGS.size()));
        double consumption = building.avgDailyKwh() / 24.0 * (0.7 + random.nextDouble() * 0.6);
        CityEvent event = new CityEvent(EventDomain.ENERGY, CITY_NAME,
                "electricity_consumption_" + building.type(), round(consumption, 1), "kWh", EventSource.SIMULATOR);
        event.setRawData("buildingId=" + building.id());
        events.add(event);

        return events;
    }

    /**
     * 生成南京重点设施状态数据
     * 设施ID与南京市重点设施对应：bridge=南京长江大桥 / tunnel=定淮门长江隧道 /
     * road=玄武大道 / pipeline=江北新区综合管廊 / power_grid=新街口片区电网
     */
    private List<CityEvent> generateInfrastructureData() {
        List<CityEvent> events = new ArrayList<>();

        String[] facilities = {"bridge", "tunnel", "road", "pipeline", "power_grid"};
        String facility = facilities[random.nextInt(facilities.length)];

        // 设施状态评分 (0-100)：按各设施监测波动特征取基线随机值
        double base;
        double deviation;
        switch (facility) {
            case "bridge" -> { base = 84.0; deviation = 8.0; }     // 南京长江大桥
            case "tunnel" -> { base = 79.0; deviation = 9.0; }     // 定淮门长江隧道
            case "road" -> { base = 86.0; deviation = 7.0; }       // 玄武大道
            case "pipeline" -> { base = 75.0; deviation = 10.0; }  // 江北新区综合管廊
            default -> { base = 88.0; deviation = 6.0; }           // 新街口片区电网
        }
        double statusScore = Math.max(40.0, Math.min(100.0,
                base + (random.nextDouble() * 2 - 1) * deviation));
        events.add(new CityEvent(EventDomain.INFRA, CITY_NAME,
                "facility_status_" + facility, round(statusScore, 1), "score", EventSource.SIMULATOR));

        // 告警级别 (0=正常, 1=注意, 2=警告, 3=严重)
        int alertLevel = random.nextInt(4);
        events.add(new CityEvent(EventDomain.INFRA, CITY_NAME,
                "alert_level_" + facility, (double) alertLevel, "level", EventSource.SIMULATOR));

        return events;
    }

    /**
     * 根据领域发布事件到对应的routing key
     */
    private void publishEvent(CityEvent event) {
        switch (event.getDomain()) {
            case ENVIRONMENT:
                if (isAirQualityMetric(event.getMetric())) {
                    eventPublisher.publishAirQuality(event);
                } else {
                    eventPublisher.publishWeather(event);
                }
                break;
            case TRAFFIC:
                // 仅信号优化结果进入信号优化队列；车流/排队长度等监测指标统一走交通流队列
                if (event.getMetric().contains("signal")) {
                    eventPublisher.publishSignalOptimization(event);
                } else {
                    eventPublisher.publishTrafficFlow(event);
                }
                break;
            case ENERGY:
                eventPublisher.publishEnergyConsumption(event);
                break;
            case WATER:
                eventPublisher.publishWaterLevel(event);
                break;
            case DISASTER:
                eventPublisher.publishDisasterScenario(event);
                break;
            case INFRA:
                eventPublisher.publishInfrastructure(event);
                break;
            default:
                log.warn("未知领域，使用默认发布方式: {}", event.getDomain());
                eventPublisher.publish("city.event." + event.getDomain().getCode(), event);
        }
    }

    /**
     * 判断是否为空气质量指标
     */
    private boolean isAirQualityMetric(String metric) {
        return metric.equals("pm25") || metric.equals("pm10")
                || metric.equals("no2") || metric.equals("o3");
    }

    /**
     * 持久化事件记录到数据库
     */
    private void saveEventRecord(CityEvent event) {
        try {
            EventRecord record = new EventRecord(
                    event.getEventId(),
                    event.getDomain().name(),
                    event.getCity(),
                    event.getMetric(),
                    event.getValue(),
                    event.getUnit(),
                    event.getSource().name(),
                    event.getTs()
            );
            eventRecordRepository.save(record);
        } catch (Exception e) {
            log.error("事件持久化失败: {}", e.getMessage());
        }
    }

    /**
     * 四舍五入到指定小数位
     */
    private double round(double value, int decimalPlaces) {
        double factor = Math.pow(10, decimalPlaces);
        return Math.round(value * factor) / factor;
    }

    /**
     * 南京建筑（id=建筑ID，type=用电类型后缀，avgDailyKwh=日均能耗基线）
     */
    private record NanjingBuilding(String id, String type, double avgDailyKwh) {
    }
}
