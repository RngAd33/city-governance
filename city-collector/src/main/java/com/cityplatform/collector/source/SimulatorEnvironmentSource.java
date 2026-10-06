package com.cityplatform.collector.source;

import com.cityplatform.collector.config.CollectorProperties;
import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.event.EventDomain;
import com.cityplatform.common.event.EventSource;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 模拟器环境数据源
 * 生成模拟气象与空气质量数据，作为真实数据源不可用时的兜底
 */
@Component
public class SimulatorEnvironmentSource implements EnvironmentDataSource {

    private static final String NAME = "simulator";

    @Resource
    private CollectorProperties properties;

    private final Random random = new Random();

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public EventSource getSource() {
        return EventSource.SIMULATOR;
    }

    @Override
    public Set<DataCategory> getCategories() {
        return EnumSet.allOf(DataCategory.class);
    }

    @Override
    public boolean isAvailable() {
        // 与模拟器环境数据开关联动：关闭时视为不可用（含状态接口展示）
        return properties.getSimulator().isEnvironmentEnabled();
    }

    @Override
    public boolean isFallback() {
        return true;
    }

    @Override
    public List<CityEvent> fetch() {
        List<CityEvent> events = new ArrayList<>();
        events.addAll(generateWeatherData());
        events.addAll(generateAirQualityData());
        return events;
    }

    /**
     * 按类别生成模拟数据
     */
    public List<CityEvent> generateByCategory(DataCategory category) {
        return category == DataCategory.AIR_QUALITY ? generateAirQualityData() : generateWeatherData();
    }

    /**
     * 生成气象数据
     */
    public List<CityEvent> generateWeatherData() {
        String city = properties.getSources().getCity();
        List<CityEvent> events = new ArrayList<>();

        double temperature = 15.0 + random.nextDouble() * 20.0;
        events.add(new CityEvent(EventDomain.ENVIRONMENT, city,
                "temperature", round(temperature, 1), "°C", EventSource.SIMULATOR));

        double humidity = 30.0 + random.nextDouble() * 60.0;
        events.add(new CityEvent(EventDomain.ENVIRONMENT, city,
                "humidity", round(humidity, 1), "%", EventSource.SIMULATOR));

        double windSpeed = random.nextDouble() * 15.0;
        events.add(new CityEvent(EventDomain.ENVIRONMENT, city,
                "wind_speed", round(windSpeed, 1), "m/s", EventSource.SIMULATOR));

        double precipitation = random.nextDouble() * 50.0;
        events.add(new CityEvent(EventDomain.ENVIRONMENT, city,
                "precipitation", round(precipitation, 1), "mm", EventSource.SIMULATOR));

        return events;
    }

    /**
     * 生成空气质量数据
     */
    public List<CityEvent> generateAirQualityData() {
        String city = properties.getSources().getCity();
        List<CityEvent> events = new ArrayList<>();

        double pm25 = random.nextDouble() * 200.0;
        events.add(new CityEvent(EventDomain.ENVIRONMENT, city,
                "pm25", round(pm25, 1), "μg/m³", EventSource.SIMULATOR));

        double pm10 = random.nextDouble() * 300.0;
        events.add(new CityEvent(EventDomain.ENVIRONMENT, city,
                "pm10", round(pm10, 1), "μg/m³", EventSource.SIMULATOR));

        double no2 = random.nextDouble() * 100.0;
        events.add(new CityEvent(EventDomain.ENVIRONMENT, city,
                "no2", round(no2, 1), "μg/m³", EventSource.SIMULATOR));

        double o3 = random.nextDouble() * 200.0;
        events.add(new CityEvent(EventDomain.ENVIRONMENT, city,
                "o3", round(o3, 1), "μg/m³", EventSource.SIMULATOR));

        return events;
    }

    /**
     * 四舍五入到指定小数位
     */
    private double round(double value, int decimalPlaces) {
        double factor = Math.pow(10, decimalPlaces);
        return Math.round(value * factor) / factor;
    }
}
