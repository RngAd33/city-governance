package com.cityplatform.common;

import com.cityplatform.common.event.*;
import com.cityplatform.common.mq.EventBusConfig;
import com.cityplatform.common.util.JsonUtil;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 事件总线集成测试
 */
public class EventBusIntegrationTest {

    @Test
    public void testCityEventCreation() {
        CityEvent event = new CityEvent(
                EventDomain.ENVIRONMENT,
                "南京",
                "pm25",
                45.2,
                "μg/m³",
                EventSource.SIMULATOR
        );
        event.setEventId(UUID.randomUUID().toString());
        event.setLocation(new GeoLocation(32.06, 118.79));
        event.setTs(OffsetDateTime.now());

        assertNotNull(event.getEventId());
        assertEquals("南京", event.getCity());
        assertEquals(45.2, event.getValue());
        assertEquals("μg/m³", event.getUnit());
    }

    @Test
    public void testEventSerialization() {
        CityEvent event = new CityEvent(
                EventDomain.TRAFFIC,
                "南京",
                "flow",
                1200.0,
                "vehicles/h",
                EventSource.DATASET
        );
        event.setEventId(UUID.randomUUID().toString());
        event.setLocation(new GeoLocation(32.06, 118.79, "中山北路"));

        String json = JsonUtil.toJson(event);
        assertNotNull(json);
        assertTrue(json.contains("TRAFFIC"));
        assertTrue(json.contains("南京"));
        assertTrue(json.contains("flow"));

        CityEvent deserialized = JsonUtil.fromJson(json, CityEvent.class);
        assertNotNull(deserialized);
        assertEquals(event.getDomain(), deserialized.getDomain());
        assertEquals(event.getCity(), deserialized.getCity());
        assertEquals(event.getValue(), deserialized.getValue());
    }

    @Test
    public void testAllEventDomains() {
        for (EventDomain domain : EventDomain.values()) {
            CityEvent event = new CityEvent(domain, "南京", "test_metric", 1.0, "unit", EventSource.API);
            assertNotNull(event);
            assertEquals(domain, event.getDomain());
        }
    }

    @Test
    public void testAllEventSources() {
        for (EventSource source : EventSource.values()) {
            CityEvent event = new CityEvent(EventDomain.ENERGY, "南京", "electricity", 100.0, "kWh", source);
            assertNotNull(event);
            assertEquals(source, event.getSource());
        }
    }

    @Test
    public void testRoutingKeys() {
        assertEquals("env.weather.city", EventBusConfig.RK_ENV_WEATHER);
        assertEquals("env.airquality.city", EventBusConfig.RK_ENV_AIRQUALITY);
        assertEquals("traffic.flow.segment", EventBusConfig.RK_TRAFFIC_FLOW);
        assertEquals("traffic.signal.optimization", EventBusConfig.RK_TRAFFIC_SIGNAL);
        assertEquals("energy.building.consumption", EventBusConfig.RK_ENERGY_BUILDING);
        assertEquals("water.level.station", EventBusConfig.RK_WATER_MONITOR);
        assertEquals("disaster.flood.scenario", EventBusConfig.RK_DISASTER_SIM);
        assertEquals("govai.decision.suggest", EventBusConfig.RK_GOVAI_DECISION);
    }

    @Test
    public void testGeoLocation() {
        GeoLocation loc = new GeoLocation(32.06, 118.79, "南京市鼓楼区");
        assertEquals(32.06, loc.getLat());
        assertEquals(118.79, loc.getLng());
        assertEquals("南京市鼓楼区", loc.getAddress());
    }
}
