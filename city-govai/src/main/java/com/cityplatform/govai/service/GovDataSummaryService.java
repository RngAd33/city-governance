package com.cityplatform.govai.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.cityplatform.govai.constant.ReportConstants.*;

/**
 * 政务数据摘要服务
 * 从各业务数据表（事件流/水务/工单/告警/环境等）动态提取最新指标，
 * 生成紧凑的「实时政务数据快照」注入大模型提示词，实现政务数据与大模型的对接
 * 各数据域独立容错：任一表查询失败或为空不影响整体摘要生成
 */
@Service
@Slf4j
public class GovDataSummaryService {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 生成实时政务数据快照文本
     */
    public String buildSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("【实时政务数据快照】(生成时间 ")
          .append(LocalDateTime.now().format(TIME_FORMAT)).append(")\n");
        appendDomainMetrics(sb);
        appendWaterStations(sb);
        appendWorkOrders(sb);
        appendAlerts(sb);
        appendEventStats(sb);
        return sb.toString();
    }

    /**
     * 各数据域最新指标（每个 domain+metric 取最新一条）
     */
    private void appendDomainMetrics(StringBuilder sb) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT x.domain, x.metric, MAX(x.value) AS value, MAX(x.unit) AS unit FROM (" +
                    "  SELECT e.domain, e.metric, e.value, e.unit FROM event_record e " +
                    "  JOIN (SELECT domain, metric, MAX(ts) mt FROM event_record GROUP BY domain, metric) t " +
                    "    ON e.domain = t.domain AND e.metric = t.metric AND e.ts = t.mt" +
                    ") x GROUP BY x.domain, x.metric ORDER BY x.domain, x.metric LIMIT 60");
            if (rows.isEmpty()) {
                return;
            }
            Map<String, List<String>> grouped = new LinkedHashMap<>();
            for (Map<String, Object> row : rows) {
                String domain = str(row.get("domain"));
                String metric = str(row.get("metric"));
                String display = METRIC_NAMES.getOrDefault(metric, metric);
                String unit = row.get("unit") == null ? "" : str(row.get("unit"));
                grouped.computeIfAbsent(domain, k -> new ArrayList<>())
                       .add(display + "=" + formatNumber(row.get("value")) + unit);
            }
            for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
                sb.append(DOMAIN_NAMES.getOrDefault(entry.getKey(), entry.getKey()))
                  .append(": ").append(String.join("; ", entry.getValue())).append("\n");
            }
        } catch (Exception e) {
            log.warn("生成数据域指标摘要失败: {}", e.getMessage());
        }
    }

    /**
     * 水务监测站最新批次水位（最高3站）
     */
    private void appendWaterStations(StringBuilder sb) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT w.station_id, s.name, w.water_level, w.risk_level FROM water_level_record w " +
                    "LEFT JOIN water_station s ON s.station_id = w.station_id " +
                    "WHERE w.record_time = (SELECT MAX(record_time) FROM water_level_record) " +
                    "ORDER BY w.water_level DESC LIMIT 3");
            if (rows.isEmpty()) {
                return;
            }
            List<String> parts = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                String name = row.get("name") != null ? str(row.get("name")) : str(row.get("station_id"));
                parts.add(name + " " + formatNumber(row.get("water_level")) + "m("
                        + str(row.get("risk_level")) + ")");
            }
            sb.append("水务站点(最新批次): ").append(String.join("，", parts)).append("\n");
        } catch (Exception e) {
            log.warn("生成水务站点摘要失败: {}", e.getMessage());
        }
    }

    /**
     * 运维工单统计（总量/处理中/已完成/逾期）
     */
    private void appendWorkOrders(StringBuilder sb) {
        try {
            Map<String, Object> stats = jdbcTemplate.queryForMap(
                    "SELECT COUNT(*) total, " +
                    "COALESCE(SUM(status = 'PENDING'), 0) pending, " +
                    "COALESCE(SUM(status = 'IN_PROGRESS'), 0) inProgress, " +
                    "COALESCE(SUM(status = 'COMPLETED'), 0) completed, " +
                    "COALESCE(SUM(due_date < CURDATE() AND status NOT IN ('COMPLETED', 'CANCELLED')), 0) overdue " +
                    "FROM work_orders");
            sb.append("运维工单: 共").append(formatNumber(stats.get("total"))).append("单（待处理")
              .append(formatNumber(stats.get("pending"))).append("，进行中")
              .append(formatNumber(stats.get("inProgress"))).append("，已完成")
              .append(formatNumber(stats.get("completed"))).append("，逾期未完成")
              .append(formatNumber(stats.get("overdue"))).append("）\n");
        } catch (Exception e) {
            log.warn("生成工单统计摘要失败: {}", e.getMessage());
        }
    }

    /**
     * 告警概况（未确认数量与最近告警）
     */
    private void appendAlerts(StringBuilder sb) {
        try {
            Integer unack = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM alert_record WHERE acknowledged = 0", Integer.class);
            if (unack == null || unack == 0) {
                sb.append("告警: 当前无未确认告警\n");
                return;
            }
            List<Map<String, Object>> recent = jdbcTemplate.queryForList(
                    "SELECT severity, metric, message FROM alert_record ORDER BY ts DESC LIMIT 3");
            List<String> parts = new ArrayList<>();
            for (Map<String, Object> row : recent) {
                parts.add("[" + str(row.get("severity")) + "]" + str(row.get("message")));
            }
            sb.append("告警: 未确认 ").append(unack).append(" 条；最近: ")
              .append(String.join("；", parts)).append("\n");
        } catch (Exception e) {
            log.warn("生成告警摘要失败: {}", e.getMessage());
        }
    }

    /**
     * 近24小时事件流统计（按数据域）
     */
    private void appendEventStats(StringBuilder sb) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT domain, COUNT(*) c FROM event_record " +
                    "WHERE ts >= DATE_SUB(NOW(), INTERVAL 24 HOUR) GROUP BY domain ORDER BY c DESC");
            if (rows.isEmpty()) {
                sb.append("事件(近24h): 无\n");
                return;
            }
            List<String> parts = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                String domain = str(row.get("domain"));
                parts.add(DOMAIN_NAMES.getOrDefault(domain, domain) + formatNumber(row.get("c")) + "条");
            }
            sb.append("事件(近24h): ").append(String.join("，", parts)).append("\n");
        } catch (Exception e) {
            log.warn("生成事件统计摘要失败: {}", e.getMessage());
        }
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String formatNumber(Object value) {
        if (value == null) {
            return "--";
        }
        double d;
        if (value instanceof BigDecimal bd) {
            d = bd.doubleValue();
        } else if (value instanceof Number n) {
            d = n.doubleValue();
        } else {
            try {
                d = Double.parseDouble(String.valueOf(value));
            } catch (NumberFormatException e) {
                return String.valueOf(value);
            }
        }
        return new DecimalFormat("#,##0.##").format(d);
    }
}
