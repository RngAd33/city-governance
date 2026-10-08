package com.cityplatform.energy.consumer;

import com.cityplatform.common.event.CityEvent;
import com.cityplatform.common.event.EventDomain;
import com.cityplatform.common.event.EventSource;
import com.cityplatform.common.util.JsonUtil;
import com.cityplatform.energy.model.GridEnergyRecord;
import com.cityplatform.energy.repository.GridEnergyRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 电网指标事件消费者单元测试
 * 验证：年度数据值与时间戳均未变化时跳过重复写入，值变化时正常入库
 */
class GridEnergyEventConsumerTest {

    private GridEnergyEventConsumer consumer;
    private GridEnergyRecordRepository repository;

    @BeforeEach
    void setUp() {
        consumer = new GridEnergyEventConsumer();
        repository = mock(GridEnergyRecordRepository.class);
        ReflectionTestUtils.setField(consumer, "gridRepository", repository);
    }

    @Test
    void shouldSkipWhenValueAndTsUnchanged() {
        CityEvent event = gridEvent(525.34);
        // 模拟上一轮入库记录：ts 取 JSON 往返后的解析值（与消费者解析结果一致，年度数据时间戳固定）
        CityEvent parsed = JsonUtil.fromJson(JsonUtil.toJson(event), CityEvent.class);
        GridEnergyRecord last = new GridEnergyRecord();
        last.setValue(parsed.getValue());
        last.setTs(parsed.getTs());
        when(repository.findTopByZoneAndMetricOrderByTsDesc("CN", "grid_carbon_intensity"))
                .thenReturn(Optional.of(last));

        consumer.handleGridIndicatorEvent(JsonUtil.toJson(event));

        verify(repository, never()).save(any(GridEnergyRecord.class));
    }

    @Test
    void shouldSaveWhenValueChanged() {
        CityEvent event = gridEvent(500.0);
        CityEvent parsed = JsonUtil.fromJson(JsonUtil.toJson(event), CityEvent.class);
        GridEnergyRecord last = new GridEnergyRecord();
        last.setValue(525.34);
        last.setTs(parsed.getTs());
        when(repository.findTopByZoneAndMetricOrderByTsDesc("CN", "grid_carbon_intensity"))
                .thenReturn(Optional.of(last));

        consumer.handleGridIndicatorEvent(JsonUtil.toJson(event));

        verify(repository).save(any(GridEnergyRecord.class));
    }

    private CityEvent gridEvent(double value) {
        return new CityEvent(EventDomain.ENERGY, "CN",
                "grid_carbon_intensity", value, "gCO₂eq/kWh", EventSource.OWID);
    }
}
