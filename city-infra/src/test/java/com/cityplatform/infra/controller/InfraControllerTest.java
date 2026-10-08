package com.cityplatform.infra.controller;

import com.cityplatform.infra.model.FacilityStatusRecord;
import com.cityplatform.infra.repository.FacilityStatusRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 基础设施运维控制器测试：聚焦历史趋势接口的按小时降采样
 */
class InfraControllerTest {

    private static final ZoneOffset CST = ZoneOffset.ofHours(8);

    private static OffsetDateTime at(int hour, int minute) {
        return OffsetDateTime.of(2026, 10, 6, hour, minute, 0, 0, CST);
    }

    private static FacilityStatusRecord record(long id, OffsetDateTime ts, double value) {
        FacilityStatusRecord record = new FacilityStatusRecord(
                "bridge", "facility_status", value, "score", ts, "南京", "SIMULATOR");
        record.setId(id);
        return record;
    }

    @Test
    void downsampleByHourAveragesWithinEachNaturalHour() {
        List<FacilityStatusRecord> raw = List.of(
                record(1, at(10, 5), 80.0),
                record(2, at(10, 35), 90.0),
                record(3, at(11, 10), 70.0),
                record(4, at(11, 50), 75.4));

        List<FacilityStatusRecord> points = InfraController.downsampleByHour(raw);

        assertEquals(2, points.size());

        FacilityStatusRecord first = points.get(0);
        assertEquals(85.0, first.getValue(), 1e-9);
        assertEquals(at(10, 0), first.getTimestamp());
        assertEquals(2L, first.getId());
        assertEquals("score", first.getUnit());
        assertEquals("南京", first.getCity());
        assertEquals("bridge", first.getFacility());

        FacilityStatusRecord second = points.get(1);
        assertEquals(72.7, second.getValue(), 1e-9);
        assertEquals(at(11, 0), second.getTimestamp());
        assertEquals(4L, second.getId());
    }

    @Test
    void downsampleByHourKeepsSingleRecordValue() {
        List<FacilityStatusRecord> points =
                InfraController.downsampleByHour(List.of(record(9, at(15, 30), 66.6)));

        assertEquals(1, points.size());
        assertEquals(66.6, points.get(0).getValue(), 1e-9);
        assertEquals(at(15, 0), points.get(0).getTimestamp());
    }

    @Test
    void downsampleByHourReturnsEmptyForEmptyInput() {
        assertTrue(InfraController.downsampleByHour(List.of()).isEmpty());
    }

    @Test
    void getHistoryReturnsRawRecordsWithinRawWindow() {
        FacilityStatusRecordRepository repository = mock(FacilityStatusRecordRepository.class);
        List<FacilityStatusRecord> raw = List.of(
                record(1, at(9, 5), 80.0),
                record(2, at(9, 35), 90.0));
        when(repository.findByFacilityAndMetricTypeAndTimestampAfterOrderByTimestampAsc(
                eq("bridge"), eq("facility_status"), any(OffsetDateTime.class))).thenReturn(raw);

        InfraController controller = new InfraController();
        ReflectionTestUtils.setField(controller, "recordRepository", repository);

        ResponseEntity<List<FacilityStatusRecord>> response = controller.getHistory("bridge", 24);

        assertEquals(2, response.getBody().size());
        assertEquals(80.0, response.getBody().get(0).getValue(), 1e-9);
    }

    @Test
    void getHistoryDownsamplesBeyondRawWindow() {
        FacilityStatusRecordRepository repository = mock(FacilityStatusRecordRepository.class);
        List<FacilityStatusRecord> raw = List.of(
                record(1, at(8, 5), 80.0),
                record(2, at(8, 35), 90.0));
        when(repository.findByFacilityAndMetricTypeAndTimestampAfterOrderByTimestampAsc(
                eq("bridge"), eq("facility_status"), any(OffsetDateTime.class))).thenReturn(raw);

        InfraController controller = new InfraController();
        ReflectionTestUtils.setField(controller, "recordRepository", repository);

        ResponseEntity<List<FacilityStatusRecord>> response = controller.getHistory("bridge", 168);

        assertEquals(1, response.getBody().size());
        assertEquals(85.0, response.getBody().get(0).getValue(), 1e-9);
        assertEquals(at(8, 0), response.getBody().get(0).getTimestamp());
    }
}
