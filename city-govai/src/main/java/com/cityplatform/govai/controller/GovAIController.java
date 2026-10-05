package com.cityplatform.govai.controller;

import com.cityplatform.govai.ai.AlertExplainer;
import com.cityplatform.govai.ai.LocalLLMService;
import com.cityplatform.govai.ai.PlanRetriever;
import com.cityplatform.govai.ai.ReportGenerator;
import com.cityplatform.govai.ai.model.EmergencyPlan;
import com.cityplatform.govai.ai.model.Explanation;
import com.cityplatform.govai.ai.model.Report;
import com.cityplatform.govai.entity.DecisionRecord;
import com.cityplatform.govai.knowledge.KnowledgeBase;
import com.cityplatform.govai.knowledge.KnowledgeItem;
import com.cityplatform.govai.repository.DecisionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 政务AI决策支持REST控制器
 * 提供状态查询、自然语言问答、告警解释、报告生成、预案检索和知识库搜索等接口
 */
@RestController
@RequestMapping("/api/govai")
public class GovAIController {

    private static final Logger log = LoggerFactory.getLogger(GovAIController.class);

    private final LocalLLMService llmService;
    private final AlertExplainer alertExplainer;
    private final ReportGenerator reportGenerator;
    private final PlanRetriever planRetriever;
    private final KnowledgeBase knowledgeBase;
    private final DecisionRecordRepository decisionRecordRepository;

    public GovAIController(LocalLLMService llmService,
                           AlertExplainer alertExplainer,
                           ReportGenerator reportGenerator,
                           PlanRetriever planRetriever,
                           KnowledgeBase knowledgeBase,
                           DecisionRecordRepository decisionRecordRepository) {
        this.llmService = llmService;
        this.alertExplainer = alertExplainer;
        this.reportGenerator = reportGenerator;
        this.planRetriever = planRetriever;
        this.knowledgeBase = knowledgeBase;
        this.decisionRecordRepository = decisionRecordRepository;
    }

    /**
     * 服务状态检查
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("service", "city-govai");
        status.put("status", "running");
        status.put("timestamp", new Date());
        status.put("version", "1.0.0");
        status.put("aiProvider", llmService.getProviderName());
        status.put("capabilities", Arrays.asList(
                "natural_language_query", "alert_explanation",
                "report_generation", "plan_retrieval", "knowledge_search"));
        return ResponseEntity.ok(status);
    }

    /**
     * 自然语言查询
     */
    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> query(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "查询内容不能为空"));
        }

        log.info("处理自然语言查询: {}", query);
        String answer = llmService.naturalLanguageQuery(query);

        // 保存决策记录
        DecisionRecord record = new DecisionRecord();
        record.setQueryType("natural_language_query");
        record.setInput(query);
        record.setOutput(answer);
        record.setConfidence(0.80);
        decisionRecordRepository.save(record);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", query);
        result.put("answer", answer);
        result.put("recordId", record.getId());
        return ResponseEntity.ok(result);
    }

    /**
     * 告警解释
     */
    @PostMapping("/alert/explain")
    public ResponseEntity<Map<String, Object>> explainAlert(@RequestBody Map<String, Object> request) {
        String alertType = (String) request.get("alertType");
        String city = (String) request.get("city");

        if (alertType == null || city == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "缺少必要参数: alertType, city"));
        }

        log.info("解释告警 - type: {}, city: {}", alertType, city);

        Explanation explanation;
        String lowerType = alertType.toLowerCase();

        if (lowerType.contains("air") || lowerType.contains("空气") || lowerType.contains("aqi")) {
            double pm25 = getDoubleValue(request, "pm25", 150);
            int aqi = getIntValue(request, "aqi", 180);
            explanation = alertExplainer.explainAirQualityAlert(city, pm25, aqi);
        } else if (lowerType.contains("traffic") || lowerType.contains("交通") || lowerType.contains("拥堵")) {
            String location = (String) request.getOrDefault("location", "市中心");
            double congestion = getDoubleValue(request, "congestionLevel", 7.5);
            explanation = alertExplainer.explainTrafficAlert(city, location, congestion);
        } else if (lowerType.contains("energy") || lowerType.contains("能耗") || lowerType.contains("电力")) {
            String building = (String) request.getOrDefault("building", "市政大楼");
            double consumption = getDoubleValue(request, "consumption", 130);
            explanation = alertExplainer.explainEnergyAlert(city, building, consumption);
        } else if (lowerType.contains("water") || lowerType.contains("水") || lowerType.contains("洪涝")) {
            String station = (String) request.getOrDefault("station", "中心水文站");
            double waterLevel = getDoubleValue(request, "waterLevel", 2.5);
            explanation = alertExplainer.explainWaterAlert(city, station, waterLevel);
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "不支持的告警类型: " + alertType));
        }

        // 保存决策记录
        DecisionRecord record = new DecisionRecord();
        record.setQueryType("alert_explanation");
        record.setInput(alertType + " - " + city);
        record.setOutput(explanation.getSummary());
        record.setConfidence(explanation.getConfidence());
        decisionRecordRepository.save(record);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("explanation", explanation);
        result.put("recordId", record.getId());
        return ResponseEntity.ok(result);
    }

    /**
     * 报告生成
     */
    @PostMapping("/report/generate")
    public ResponseEntity<Map<String, Object>> generateReport(@RequestBody Map<String, String> request) {
        String reportType = request.get("reportType");
        String city = request.get("city");
        String date = request.get("date");

        if (reportType == null || city == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "缺少必要参数: reportType, city"));
        }

        log.info("生成报告 - type: {}, city: {}, date: {}", reportType, city, date);

        Report report;
        String lowerType = reportType.toLowerCase();

        if (lowerType.contains("daily") || lowerType.contains("日报")) {
            report = reportGenerator.generateDailyReport(city, date != null ? date : "今日");
        } else if (lowerType.contains("traffic") || lowerType.contains("交通")) {
            report = reportGenerator.generateTrafficBrief(city, date != null ? date : "本周");
        } else if (lowerType.contains("flood") || lowerType.contains("洪涝") || lowerType.contains("水")) {
            report = reportGenerator.generateFloodRiskAlert(city, date != null ? date : "今日");
        } else if (lowerType.contains("energy") || lowerType.contains("能耗")) {
            report = reportGenerator.generateEnergyWeeklyReport(city, date != null ? date : "本周");
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "不支持的报告类型: " + reportType));
        }

        // 保存决策记录
        DecisionRecord record = new DecisionRecord();
        record.setQueryType("report_generation");
        record.setInput(reportType + " - " + city);
        record.setOutput(report.getSummary());
        record.setConfidence(0.88);
        decisionRecordRepository.save(record);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("report", report);
        result.put("formattedText", reportGenerator.formatReportAsText(report));
        result.put("recordId", record.getId());
        return ResponseEntity.ok(result);
    }

    /**
     * 应急预案检索
     */
    @PostMapping("/plan/retrieve")
    public ResponseEntity<Map<String, Object>> retrievePlan(@RequestBody Map<String, String> request) {
        String eventType = request.get("eventType");
        String severity = request.get("severity");

        if (eventType == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "缺少必要参数: eventType"));
        }

        log.info("检索应急预案 - eventType: {}, severity: {}", eventType, severity);

        EmergencyPlan plan = planRetriever.retrievePlan(eventType, severity != null ? severity : "III");

        // 保存决策记录
        DecisionRecord record = new DecisionRecord();
        record.setQueryType("plan_retrieval");
        record.setInput(eventType + " - " + severity);
        record.setOutput(plan.getPlanName());
        record.setConfidence(0.90);
        decisionRecordRepository.save(record);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("plan", plan);
        result.put("recordId", record.getId());
        return ResponseEntity.ok(result);
    }

    /**
     * 知识库搜索
     */
    @GetMapping("/knowledge/search")
    public ResponseEntity<Map<String, Object>> searchKnowledge(
            @RequestParam String query,
            @RequestParam(required = false) String category) {

        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "搜索关键词不能为空"));
        }

        log.info("搜索知识库 - query: {}, category: {}", query, category);

        List<KnowledgeItem> results;
        if (category != null && !category.isEmpty()) {
            results = knowledgeBase.getByCategory(category);
        } else {
            results = knowledgeBase.search(query);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("query", query);
        response.put("total", results.size());
        response.put("results", results);
        return ResponseEntity.ok(response);
    }

    // ==================== 辅助方法 ====================

    private double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
}
