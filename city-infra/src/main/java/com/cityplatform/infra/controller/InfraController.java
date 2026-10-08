package com.cityplatform.infra.controller;

import com.cityplatform.infra.model.FacilityStatusRecord;
import com.cityplatform.infra.model.WorkOrder;
import com.cityplatform.infra.repository.FacilityStatusRecordRepository;
import com.cityplatform.infra.repository.WorkOrderRepository;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基础设施运维REST控制器
 * 提供设施实时状态总览与状态趋势查询
 */
@RestController
@RequestMapping("/api/infra")
public class InfraController {

    /** 设施类型清单（与采集端南京重点设施清单一致） */
    private static final List<String> FACILITY_ORDER =
            List.of("bridge", "tunnel", "road", "pipeline", "power_grid");

    /** 设施类型名称映射：对应南京市重点设施 */
    private static final Map<String, String> FACILITY_NAMES = Map.of(
            "bridge", "南京长江大桥",
            "tunnel", "定淮门长江隧道",
            "road", "玄武大道",
            "pipeline", "江北新区综合管廊",
            "power_grid", "新街口片区电网");

    private static final String METRIC_STATUS = "facility_status";
    private static final String METRIC_ALERT = "alert_level";

    @Resource
    private FacilityStatusRecordRepository recordRepository;

    @Resource
    private WorkOrderRepository workOrderRepository;

    /**
     * 设施实时状态总览（每类设施取最新状态评分与告警级别）
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        List<Map<String, Object>> facilities = new ArrayList<>();
        double scoreSum = 0;
        int scoreCount = 0;
        int normalCount = 0;
        int attentionCount = 0;
        int warningCount = 0;
        int dangerCount = 0;
        OffsetDateTime lastUpdateTime = null;

        for (String facility : FACILITY_ORDER) {
            FacilityStatusRecord status = recordRepository
                    .findTop1ByFacilityAndMetricTypeOrderByTimestampDesc(facility, METRIC_STATUS)
                    .orElse(null);
            FacilityStatusRecord alert = recordRepository
                    .findTop1ByFacilityAndMetricTypeOrderByTimestampDesc(facility, METRIC_ALERT)
                    .orElse(null);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("facility", facility);
            item.put("facilityName", FACILITY_NAMES.get(facility));
            item.put("statusScore", status != null ? status.getValue() : null);
            item.put("statusTime", status != null ? status.getTimestamp() : null);
            item.put("alertLevel", alert != null ? alert.getValue().intValue() : null);
            item.put("alertTime", alert != null ? alert.getTimestamp() : null);
            facilities.add(item);

            if (status != null) {
                scoreSum += status.getValue();
                scoreCount++;
                if (lastUpdateTime == null || status.getTimestamp().isAfter(lastUpdateTime)) {
                    lastUpdateTime = status.getTimestamp();
                }
            }
            if (alert != null) {
                int level = alert.getValue().intValue();
                switch (level) {
                    case 0 -> normalCount++;
                    case 1 -> attentionCount++;
                    case 2 -> warningCount++;
                    default -> dangerCount++;
                }
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("facilityCount", FACILITY_ORDER.size());
        summary.put("avgScore", scoreCount > 0 ? Math.round(scoreSum / scoreCount * 10) / 10.0 : null);
        summary.put("normalCount", normalCount);
        summary.put("attentionCount", attentionCount);
        summary.put("warningCount", warningCount);
        summary.put("dangerCount", dangerCount);
        summary.put("lastUpdateTime", lastUpdateTime);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("facilities", facilities);
        result.put("summary", summary);
        return ResponseEntity.ok(result);
    }

    /** 原始点直出窗口上限（小时）：超过该窗口按自然小时降采样，控制返回数据量 */
    private static final int RAW_WINDOW_HOURS = 48;

    /**
     * 设施状态评分趋势（默认桥梁，默认近24小时，最多90天）
     * 窗口超过48小时按自然小时聚合：每点为该小时均值，避免长跨度返回海量原始点
     */
    @GetMapping("/history")
    public ResponseEntity<List<FacilityStatusRecord>> getHistory(
            @RequestParam(defaultValue = "bridge") String facility,
            @RequestParam(defaultValue = "24") int hours) {
        int effectiveHours = Math.max(1, Math.min(hours, 2160));
        OffsetDateTime start = OffsetDateTime.now().minusHours(effectiveHours);
        List<FacilityStatusRecord> records = recordRepository
                .findByFacilityAndMetricTypeAndTimestampAfterOrderByTimestampAsc(
                        facility, METRIC_STATUS, start);
        if (effectiveHours > RAW_WINDOW_HOURS) {
            records = downsampleByHour(records);
        }
        return ResponseEntity.ok(records);
    }

    /**
     * 按自然小时降采样：同一小时的记录聚合为单点
     * value 取均值（保留1位小数），timestamp 取小时整点，
     * id/unit/city/source 等保留组内最后一条（保持响应结构与原始点一致）
     */
    static List<FacilityStatusRecord> downsampleByHour(List<FacilityStatusRecord> records) {
        Map<OffsetDateTime, List<FacilityStatusRecord>> byHour = records.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getTimestamp().truncatedTo(ChronoUnit.HOURS),
                        LinkedHashMap::new, Collectors.toList()));

        List<FacilityStatusRecord> points = new ArrayList<>(byHour.size());
        for (Map.Entry<OffsetDateTime, List<FacilityStatusRecord>> entry : byHour.entrySet()) {
            List<FacilityStatusRecord> hourRecords = entry.getValue();
            FacilityStatusRecord last = hourRecords.get(hourRecords.size() - 1);
            double avg = hourRecords.stream()
                    .mapToDouble(FacilityStatusRecord::getValue)
                    .average()
                    .orElse(last.getValue());
            FacilityStatusRecord point = new FacilityStatusRecord(
                    last.getFacility(), last.getMetricType(), Math.round(avg * 10) / 10.0,
                    last.getUnit(), entry.getKey(), last.getCity(), last.getSource());
            point.setId(last.getId());
            points.add(point);
        }
        return points;
    }

    /**
     * 南京地区基础设施运维工单列表（按创建时间倒序）
     */
    @GetMapping("/work-orders")
    public ResponseEntity<List<WorkOrder>> getWorkOrders() {
        return ResponseEntity.ok(workOrderRepository.findAllByOrderByCreatedAtDesc());
    }
}
