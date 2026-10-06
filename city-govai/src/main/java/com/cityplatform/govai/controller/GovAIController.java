package com.cityplatform.govai.controller;

import com.cityplatform.govai.ai.LocalLLMService;
import com.cityplatform.govai.knowledge.KnowledgeBase;
import com.cityplatform.govai.knowledge.KnowledgeItem;
import com.cityplatform.govai.model.entity.ChatMessage;
import com.cityplatform.govai.model.entity.ChatSession;
import com.cityplatform.govai.model.entity.DecisionRecord;
import com.cityplatform.govai.model.entity.Report;
import com.cityplatform.govai.model.enums.ReportType;
import com.cityplatform.govai.repository.DecisionRecordRepository;
import com.cityplatform.govai.service.ChatHistoryService;
import com.cityplatform.govai.service.ReportPdfService;
import com.cityplatform.govai.service.ReportService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.cityplatform.govai.constant.ReportConstants.HISTORY_LIMIT;
import static com.cityplatform.govai.constant.ReportConstants.HITS_SUMMARY_MAX;

/**
 * 政务AI决策支持REST控制器
 * 内容生成接口统一为：系统提示词 + 前端输入（自然语言或参数自然语言化） + 知识库检索上下文 → DashScope大模型生成
 */
@RestController
@RequestMapping("/api/govai")
@Slf4j
public class GovAIController {

    @Resource
    private LocalLLMService llmService;

    @Resource
    private KnowledgeBase knowledgeBase;

    @Resource
    private DecisionRecordRepository decisionRecordRepository;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ReportService reportService;

    @Resource
    private ReportPdfService reportPdfService;

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
        status.put("knowledgeBase", "mysql(knowledge_items)");
        status.put("chatPersistence", "mysql(chat_session/chat_message)");
        status.put("capabilities", Arrays.asList(
                "natural_language_query", "alert_explanation", "report_generation",
                "plan_retrieval", "knowledge_search", "multi_turn_chat", "conversation_history"));
        return ResponseEntity.ok(status);
    }

    /**
     * 自然语言问答（兼容原一次性查询）：RAG知识库 + 实时政务数据 + 多轮上下文，
     * 问答过程持久化到 MySQL 会话（chat_session/chat_message），并保留决策记录
     */
    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> query(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "查询内容不能为空"));
        }

        log.info("处理自然语言查询: {}", query);
        Map<String, Object> conversation = handleConversation(query, request.get("sessionId"));
        Long recordId = saveRecord("natural_language_query", query, str(conversation.get("answer")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", query);
        result.putAll(conversation);
        result.put("recordId", recordId);
        return ResponseEntity.ok(result);
    }

    /**
     * 多轮对话问答：会话持久化 + 多轮上下文 + RAG知识库 + 实时政务数据快照
     * 请求：{ "message": "...", "sessionId": "可选，不传则创建新会话" }
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            message = request.get("query");
        }
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "消息内容不能为空"));
        }

        log.info("多轮对话 - sessionId: {}, message: {}", request.get("sessionId"), message);
        return ResponseEntity.ok(handleConversation(message, request.get("sessionId")));
    }

    /**
     * 历史会话列表（最近更新在前）
     */
    @GetMapping("/sessions")
    public ResponseEntity<Map<String, Object>> listSessions() {
        List<ChatSession> sessions = chatHistoryService.listSessions();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", sessions.size());
        response.put("sessions", sessions);
        return ResponseEntity.ok(response);
    }

    /**
     * 会话消息回放（按序号升序）
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<Map<String, Object>> sessionMessages(@PathVariable String sessionId) {
        Optional<ChatSession> session = chatHistoryService.findSession(sessionId);
        if (session.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<ChatMessage> messages = chatHistoryService.getMessages(sessionId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sessionId", sessionId);
        response.put("title", session.get().getTitle());
        response.put("total", messages.size());
        response.put("messages", messages);
        return ResponseEntity.ok(response);
    }

    /**
     * 删除会话及其全部消息
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Map<String, Object>> deleteSession(@PathVariable String sessionId) {
        boolean deleted = chatHistoryService.deleteSession(sessionId);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sessionId", sessionId);
        response.put("deleted", true);
        return ResponseEntity.ok(response);
    }

    /**
     * 告警解释：告警类型/城市及任意补充参数自然语言化后交由大模型分析
     */
    @PostMapping("/alert/explain")
    public ResponseEntity<Map<String, Object>> explainAlert(@RequestBody Map<String, Object> request) {
        String alertType = str(request.get("alertType"));
        String city = str(request.get("city"));

        if (alertType == null || city == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "缺少必要参数: alertType, city"));
        }

        String details = buildDetails(request);
        log.info("解释告警 - type: {}, city: {}, details: {}", alertType, city, details);

        String explanation = llmService.alertExplanation(alertType, city, details);
        Long recordId = saveRecord("alert_explanation", alertType + " - " + city, explanation);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("alertType", alertType);
        result.put("city", city);
        result.put("explanation", explanation);
        result.put("recordId", recordId);
        return ResponseEntity.ok(result);
    }

    /**
     * 政务报告生成（报告中心"生成报告"入口）：大模型撰写后入库，自动出现在报告列表中
     * 请求：{ "type": "日报/周报/简报/风险提示 或 DAILY/WEEKLY/BRIEF/RISK_ALERT",
     *        "city": "可选，默认南京市", "date": "可选周期，默认今日" }
     * 兼容 type/reportType、date/period 字段名
     */
    @PostMapping("/report/generate")
    public ResponseEntity<Map<String, Object>> generateReport(@RequestBody Map<String, String> request) {
        ReportType type = ReportType.from(firstNonBlank(request.get("type"), request.get("reportType")));
        if (type == null) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "缺少或无法识别的报告类型 type：支持 日报/周报/简报/风险提示（或 DAILY/WEEKLY/BRIEF/RISK_ALERT）"));
        }
        String city = firstNonBlank(request.get("city"), "南京市");
        String period = firstNonBlank(request.get("date"), request.get("period"), "今日");
        log.info("生成报告 - type: {}, city: {}, period: {}", type.getLabel(), city, period);

        Report report = reportService.generate(type, city, period);
        Long recordId = saveRecord("report_generation", type.getLabel() + " - " + city, report.getContent());

        Map<String, Object> result = toReportDetail(report);
        // 兼容历史响应字段（reportType/report/recordId）
        result.put("reportType", report.getType());
        result.put("report", report.getContent());
        result.put("recordId", recordId);
        return ResponseEntity.ok(result);
    }

    /**
     * 报告列表（报告中心）：可选 type 筛选（日报/周报/简报/风险提示，或英文代码），创建时间倒序，不含正文
     */
    @GetMapping("/report/list")
    public ResponseEntity<Map<String, Object>> listReports(@RequestParam(required = false) String type) {
        ReportType reportType = null;
        if (type != null && !type.isBlank()) {
            reportType = ReportType.from(type);
            if (reportType == null) {
                return ResponseEntity.badRequest().body(Map.of("error",
                        "无法识别的报告类型 type：支持 日报/周报/简报/风险提示（或 DAILY/WEEKLY/BRIEF/RISK_ALERT）"));
            }
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (Report report : reportService.listByType(reportType)) {
            items.add(toReportSummary(report));
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", items.size());
        response.put("reports", items);
        return ResponseEntity.ok(response);
    }

    /**
     * 报告详情（报告中心"点击预览"）：返回完整正文（Markdown）
     */
    @GetMapping("/report/{id:\\d+}")
    public ResponseEntity<Map<String, Object>> reportDetail(@PathVariable Long id) {
        Optional<Report> report = reportService.findById(id);
        if (report.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toReportDetail(report.get()));
    }

    /**
     * 报告 PDF 导出：将指定报告以 PDF 文件返回（浏览器下载/新窗口预览）
     * 200: application/pdf（Content-Disposition attachment，文件名含报告标题）
     * 404: 报告不存在；500: 生成失败（如缺少中文字体）
     */
    @GetMapping("/report/{id:\\d+}/pdf")
    public ResponseEntity<?> exportReportPdf(@PathVariable Long id) {
        Optional<Report> report = reportService.findById(id);
        if (report.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            byte[] pdf = reportPdfService.render(report.get());
            String encodedName = URLEncoder.encode(report.get().getTitle() + ".pdf", StandardCharsets.UTF_8)
                    .replace("+", "%20");
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"report-" + id + ".pdf\"; filename*=UTF-8''" + encodedName)
                    .body(pdf);
        } catch (ReportPdfService.FontUnavailableException e) {
            log.error("报告PDF导出失败(字体不可用): {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("报告PDF导出失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "报告PDF生成失败: " + e.getMessage()));
        }
    }

    /**
     * 应急预案检索：知识库候选预案作为上下文，由大模型整理输出
     */
    @PostMapping("/plan/retrieve")
    public ResponseEntity<Map<String, Object>> retrievePlan(@RequestBody Map<String, String> request) {
        String eventType = request.get("eventType");

        if (eventType == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "缺少必要参数: eventType"));
        }

        String severity = request.getOrDefault("severity", "III");
        log.info("检索应急预案 - eventType: {}, severity: {}", eventType, severity);

        String plan = llmService.planRetrieval(eventType, severity);
        Long recordId = saveRecord("plan_retrieval", eventType + " - " + severity, plan);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("eventType", eventType);
        result.put("severity", severity);
        result.put("plan", plan);
        result.put("recordId", recordId);
        return ResponseEntity.ok(result);
    }

    /**
     * 知识库搜索（RAG知识源查询）
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

    /**
     * 知识库条目列表（MySQL knowledge_items，启用条目按优先级倒序）
     */
    @GetMapping("/knowledge")
    public ResponseEntity<Map<String, Object>> listKnowledge() {
        List<KnowledgeItem> items = knowledgeBase.getAll();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", items.size());
        response.put("source", "mysql:knowledge_items");
        response.put("items", items);
        return ResponseEntity.ok(response);
    }

    // ==================== 辅助方法 ====================

    /**
     * 会话化问答核心流程：持久化用户消息 → 组装多轮上下文 + RAG检索 + 实时数据 → 大模型生成 → 持久化回答
     */
    private Map<String, Object> handleConversation(String message, String sessionId) {
        ChatSession session = chatHistoryService.getOrCreateSession(sessionId, message);
        List<ChatMessage> history = chatHistoryService.getRecentHistory(session.getSessionId(), HISTORY_LIMIT);
        chatHistoryService.appendMessage(session, "user", message, null, null);

        List<KnowledgeItem> hits = knowledgeBase.search(message);
        String answer = llmService.conversationalQuery(message, hits, history);
        chatHistoryService.appendMessage(session, "assistant", answer, summarizeHits(hits), llmService.getProviderName());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", session.getSessionId());
        result.put("title", session.getTitle());
        result.put("answer", answer);
        result.put("messageCount", session.getMessageCount());
        result.put("knowledgeHits", hitSummaries(hits));
        result.put("provider", llmService.getProviderName());
        result.put("timestamp", new Date());
        return result;
    }

    /**
     * RAG命中条目标题摘要（分号分隔，最多5条），用于随消息持久化
     */
    private String summarizeHits(List<KnowledgeItem> hits) {
        if (hits == null || hits.isEmpty()) {
            return null;
        }
        String summary = hits.stream().limit(5)
                .map(KnowledgeItem::getTitle)
                .collect(Collectors.joining("; "));
        return summary.length() > HITS_SUMMARY_MAX ? summary.substring(0, HITS_SUMMARY_MAX) : summary;
    }

    /**
     * RAG命中条目精简列表（供前端展示引用来源）
     */
    private List<Map<String, Object>> hitSummaries(List<KnowledgeItem> hits) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (hits == null) {
            return list;
        }
        for (KnowledgeItem item : hits) {
            Map<String, Object> hit = new LinkedHashMap<>();
            hit.put("id", item.getId());
            hit.put("title", item.getTitle());
            hit.put("category", item.getCategory());
            hit.put("source", item.getSource());
            hit.put("relevanceScore", Math.round(item.getRelevanceScore() * 100) / 100.0);
            list.add(hit);
        }
        return list;
    }

    /**
     * 将请求中除 alertType/city 外的键值对合并为自然语言补充描述，
     * 如 {pm25: 150, aqi: 180} → "pm25=150；aqi=180"
     */
    private String buildDetails(Map<String, Object> request) {
        List<String> parts = new ArrayList<>();

        Object details = request.get("details");
        if (details != null && !String.valueOf(details).isBlank()) {
            parts.add(String.valueOf(details));
        }
        for (Map.Entry<String, Object> entry : request.entrySet()) {
            String key = entry.getKey();
            if ("alertType".equals(key) || "city".equals(key) || "details".equals(key) || entry.getValue() == null) {
                continue;
            }
            String value = String.valueOf(entry.getValue());
            if (!value.isBlank()) {
                parts.add(key + "=" + value);
            }
        }
        return parts.isEmpty() ? "（无补充信息）" : String.join("；", parts);
    }

    /**
     * 保存决策记录（失败不影响主流程）
     */
    private Long saveRecord(String queryType, String input, String output) {
        try {
            DecisionRecord record = new DecisionRecord();
            record.setQueryType(queryType);
            record.setInput(input);
            record.setOutput(output);
            decisionRecordRepository.save(record);
            return record.getId();
        } catch (Exception e) {
            log.warn("决策记录保存失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 报告摘要（列表用，不含正文）
     */
    private Map<String, Object> toReportSummary(Report report) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", report.getId());
        item.put("title", report.getTitle());
        item.put("type", report.getType());
        item.put("typeCode", typeCode(report.getType()));
        item.put("city", report.getCity());
        item.put("period", report.getPeriod());
        item.put("author", report.getAuthor());
        item.put("status", report.getStatus());
        item.put("createdAt", report.getCreatedAt());
        return item;
    }

    /**
     * 报告详情（摘要字段 + 完整正文）
     */
    private Map<String, Object> toReportDetail(Report report) {
        Map<String, Object> detail = toReportSummary(report);
        detail.put("content", report.getContent());
        return detail;
    }

    /**
     * 报告类型中文名 → 英文代码（无法匹配时回落原值）
     */
    private String typeCode(String label) {
        ReportType type = ReportType.from(label);
        return type != null ? type.getCode() : label;
    }

    /**
     * 返回首个非空白字符串（兼容字段名与默认值）
     */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * 对象转字符串（null 转空字符串）
     *
     * @param value
     * @return
     */
    private String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

}
