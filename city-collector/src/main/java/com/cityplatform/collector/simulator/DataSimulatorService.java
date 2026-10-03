package com.cityplatform.collector.simulator;

import com.cityplatform.collector.entity.EventRecord;
import com.cityplatform.collector.repository.EventRecordRepository;
import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.event.EventDomain;
import com.cityplatform.common.event.EventSource;
import com.cityplatform.common.event.GeoLocation;
import com.cityplatform.common.mq.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class DataSimulatorService {

    private static final Logger log = LoggerFactory.getLogger(DataSimulatorService.class);

    private static final String CITY_NAME = "南京";
    private static final double CITY_LAT = 32.06;
    private static final double CITY_LNG = 118.79;
    private static final String CITY_ADDRESS = "江苏省南京市";

    private final EventPublisher eventPublisher;
    private final EventRecordRepository eventRecordRepository;
    private final Random random = new Random();

    public DataSimulatorService(EventPublisher eventPublisher,
                                EventRecordRepository eventRecordRepository) {
        this.eventPublisher = eventPublisher;
        this.eventRecordRepository = eventRecordRepository;
    }

    /**
     * 每10秒执行一次数据采集与发布
     */
    @Scheduled(fixedRate = 10000)
    public void collectAndPublish() {
        log.info("开始新一轮城市数据采集模拟...");

        List<CityEvent> events = new ArrayList<>();

        // 气象数据
        events.addAll(generateWeatherData());
        // 空气质量数据
        events.addAll(generateAirQualityData());
        // 交通流数据
        events.addAll(generateTrafficFlowData());
        // 能源消耗数据
        events.addAll(generateEnergyConsumptionData());
        // 水务监测数据
        events.addAll(generateWaterLevelData());
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
     * 生成气象数据
     */
    private List<CityEvent> generateWeatherData() {
        List<CityEvent> events = new ArrayList<>();

        double temperature = 15.0 + random.nextDouble() * 20.0;
        events.add(new CityEvent(EventDomain.ENVIRONMENT, CITY_NAME,
                "temperature", round(temperature, 1), "°C", EventSource.SIMULATOR));

        double humidity = 30.0 + random.nextDouble() * 60.0;
        events.add(new CityEvent(EventDomain.ENVIRONMENT, CITY_NAME,
                "humidity", round(humidity, 1), "%", EventSource.SIMULATOR));

        double windSpeed = random.nextDouble() * 15.0;
        events.add(new CityEvent(EventDomain.ENVIRONMENT, CITY_NAME,
                "wind_speed", round(windSpeed, 1), "m/s", EventSource.SIMULATOR));

        double precipitation = random.nextDouble() * 50.0;
        events.add(new CityEvent(EventDomain.ENVIRONMENT, CITY_NAME,
                "precipitation", round(precipitation, 1), "mm", EventSource.SIMULATOR));

        return events;
    }

    /**
     * 生成空气质量数据
     */
    private List<CityEvent> generateAirQualityData() {
        List<CityEvent> events = new ArrayList<>();

        double pm25 = random.nextDouble() * 200.0;
        events.add(new CityEvent(EventDomain.ENVIRONMENT, CITY_NAME,
                "pm25", round(pm25, 1), "μg/m³", EventSource.SIMULATOR));

        double pm10 = random.nextDouble() * 300.0;
        events.add(new CityEvent(EventDomain.ENVIRONMENT, CITY_NAME,
                "pm10", round(pm10, 1), "μg/m³", EventSource.SIMULATOR));

        double no2 = random.nextDouble() * 100.0;
        events.add(new CityEvent(EventDomain.ENVIRONMENT, CITY_NAME,
                "no2", round(no2, 1), "μg/m³", EventSource.SIMULATOR));

        double o3 = random.nextDouble() * 200.0;
        events.add(new CityEvent(EventDomain.ENVIRONMENT, CITY_NAME,
                "o3", round(o3, 1), "μg/m³", EventSource.SIMULATOR));

        return events;
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
     * 生成能源消耗数据
     */
    private List<CityEvent> generateEnergyConsumptionData() {
        List<CityEvent> events = new ArrayList<>();

        String[] buildingTypes = {"residential", "commercial", "industrial", "public"};
        String buildingType = buildingTypes[random.nextInt(buildingTypes.length)];

        double consumption = 100.0 + random.nextDouble() * 4900.0;
        events.add(new CityEvent(EventDomain.ENERGY, CITY_NAME,
                "electricity_consumption_" + buildingType, round(consumption, 1), "kWh", EventSource.SIMULATOR));

        return events;
    }

    /**
     * 生成水务监测数据
     */
    private List<CityEvent> generateWaterLevelData() {
        List<CityEvent> events = new ArrayList<>();

        double waterLevel = random.nextDouble() * 10.0;
        events.add(new CityEvent(EventDomain.WATER, CITY_NAME,
                "water_level", round(waterLevel, 2), "m", EventSource.SIMULATOR));

        double flowRate = random.nextDouble() * 1000.0;
        events.add(new CityEvent(EventDomain.WATER, CITY_NAME,
                "flow_rate", round(flowRate, 1), "m³/s", EventSource.SIMULATOR));

        return events;
    }

    /**
     * 生成基础设施状态数据
     */
    private List<CityEvent> generateInfrastructureData() {
        List<CityEvent> events = new ArrayList<>();

        String[] facilities = {"bridge", "tunnel", "road", "pipeline", "power_grid"};
        String facility = facilities[random.nextInt(facilities.length)];

        // 设施状态评分 (0-100)
        double statusScore = 60.0 + random.nextDouble() * 40.0;
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
                if (event.getMetric().contains("signal") || event.getMetric().contains("queue")) {
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
}
