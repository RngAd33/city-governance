package com.cityplatform.energy.consumer;

import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.event.EventDomain;
import com.cityplatform.common.event.EventSource;
import com.cityplatform.common.util.JsonUtil;
import com.cityplatform.energy.model.EnergyConsumptionRecord;
import com.cityplatform.energy.repository.EnergyConsumptionRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 建筑能耗事件消费者单元测试
 * 验证：派生指标（electricity_consumption_*）正确计入电耗；事件携带的南京建筑ID正确归档
 */
class EnergyEventConsumerTest {

    private EnergyEventConsumer consumer;
    private EnergyConsumptionRecordRepository repository;

    @BeforeEach
    void setUp() {
        consumer = new EnergyEventConsumer();
        repository = mock(EnergyConsumptionRecordRepository.class);
        ReflectionTestUtils.setField(consumer, "recordRepository", repository);
    }

    @Test
    void shouldPersistValuesWithBuildingIdFromRawData() {
        CityEvent event = new CityEvent(EventDomain.ENERGY, "南京",
                "electricity_consumption_office", 150.0, "kWh", EventSource.SIMULATOR);
        event.setRawData("buildingId=B001");

        consumer.handleEnergyConsumptionEvent(JsonUtil.toJson(event));

        EnergyConsumptionRecord saved = captureSaved();
        assertEquals("B001", saved.getBuildingId());
        assertEquals(150.0, saved.getElectricityKwh(), 1e-9);
        assertEquals(0.0, saved.getHeatingKwh(), 1e-9);
        assertEquals(0.0, saved.getCoolingKwh(), 1e-9);
        assertEquals(150.0, saved.getTotalKwh(), 1e-9);
        assertEquals(87.0, saved.getCarbonEmission(), 1e-6);
        assertEquals(120.0, saved.getCost(), 1e-6);
    }

    @Test
    void shouldFallbackToSyntheticBuildingIdWithoutRawData() {
        CityEvent event = new CityEvent(EventDomain.ENERGY, "南京",
                "electricity_consumption_residential", 80.0, "kWh", EventSource.SIMULATOR);

        consumer.handleEnergyConsumptionEvent(JsonUtil.toJson(event));

        EnergyConsumptionRecord saved = captureSaved();
        assertEquals("南京_electricity_consumption_residential", saved.getBuildingId());
        assertEquals(80.0, saved.getTotalKwh(), 1e-9);
        assertTrue(saved.getCarbonEmission() > 0);
    }

    @Test
    void shouldMapHeatingMetricToHeatingField() {
        CityEvent event = new CityEvent(EventDomain.ENERGY, "南京",
                "heating", 100.0, "kWh", EventSource.SIMULATOR);

        consumer.handleEnergyConsumptionEvent(JsonUtil.toJson(event));

        EnergyConsumptionRecord saved = captureSaved();
        assertEquals(0.0, saved.getElectricityKwh(), 1e-9);
        assertEquals(100.0, saved.getHeatingKwh(), 1e-9);
        assertEquals(100.0, saved.getTotalKwh(), 1e-9);
    }

    private EnergyConsumptionRecord captureSaved() {
        ArgumentCaptor<EnergyConsumptionRecord> captor = ArgumentCaptor.forClass(EnergyConsumptionRecord.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }
}
