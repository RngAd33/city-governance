package com.cityplatform.govai.ai;

import com.cityplatform.govai.knowledge.KnowledgeBase;
import com.cityplatform.govai.knowledge.KnowledgeItem;
import com.cityplatform.govai.model.entity.ChatMessage;
import com.cityplatform.govai.service.GovDataSummaryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 政务AI大模型服务
 * 所有生成能力统一走：系统提示词 + 用户输入 + 知识库检索上下文（RAG） → DashScope通义千问
 * 大模型不可用（未配置API Key或调用失败/超时）时返回明确的不可用提示，不生成编造内容
 */
@Service
@Slf4j
public class LocalLLMService {

    /** 政务AI助手系统提示词：角色设定与回答规范 */
    private static final String SYSTEM_PROMPT =
            "你是城市治理平台的政务AI决策助手，服务于政府管理人员。" +
            "你熟悉城市环境监测、交通管理、能源调度、水务防汛、应急管理等政务领域知识。" +
            "平台会提供【实时政务数据快照】与【知识库检索结果】作为参考，回答时优先依据这些数据与知识。" +
            "回答要求：专业严谨、简洁清晰、分点陈述；涉及应急处置时注明依据并给出建议的响应级别；" +
            "不得编造监测数据，数据缺失时明确说明；回答末尾不添加免责声明。";

    /** 大模型不可用时的提示前缀（供调用方判定生成失败） */
    public static final String UNAVAILABLE_PREFIX = "【AI服务暂不可用】";

    @Resource
    private KnowledgeBase knowledgeBase;

    @Resource
    private DashScopeChatService chatService;

    @Resource
    private GovDataSummaryService govDataSummaryService;

    /**
     * 当前AI提供方标识（供状态接口对外展示）
     */
    public String getProviderName() {
        return chatService.getProviderName();
    }

    /**
     * 通用生成接口（供事件消费者等调用）：用户提示词 + 上下文交由大模型生成
     */
    public String generateResponse(String prompt, String context) {
        String userPrompt = prompt;
        if (context != null && !context.isBlank()) {
            userPrompt = prompt + "\n\n【上下文信息】\n" + context;
        }
        return chat("决策建议", userPrompt);
    }

    /**
     * 自然语言问答：知识库检索结果（RAG）与实时政务数据快照作为上下文注入
     */
    public String naturalLanguageQuery(String query) {
        List<KnowledgeItem> searchResults = knowledgeBase.search(query);
        return chat("问答", buildQueryPrompt(query, searchResults, govDataSummaryService.buildSummary()));
    }

    /**
     * 多轮对话问答：历史对话轮次 + 知识库检索结果（RAG） + 实时政务数据快照交由大模型生成
     *
     * @param query   当前用户问题
     * @param hits    知识库检索结果
     * @param history 最近若干轮对话消息（按时间升序，可为空）
     */
    public String conversationalQuery(String query, List<KnowledgeItem> hits, List<ChatMessage> history) {
        List<DashScopeChatService.DialogueTurn> turns = new ArrayList<>();
        if (history != null) {
            for (ChatMessage message : history) {
                if (message.getContent() == null || message.getContent().isBlank()) {
                    continue;
                }
                turns.add(new DashScopeChatService.DialogueTurn(message.getRole(), message.getContent()));
            }
        }
        String userPrompt = buildConversationPrompt(query, hits, govDataSummaryService.buildSummary());
        return chatWithHistory("多轮问答", turns, userPrompt);
    }

    /**
     * 告警解释：告警信息（自然语言化）交由大模型分析
     */
    public String alertExplanation(String alertType, String city, String details) {
        List<KnowledgeItem> relatedKnowledge = knowledgeBase.search(alertType + " " + city);
        return chat("告警解释", buildAlertExplanationPrompt(alertType, city, details, relatedKnowledge,
                govDataSummaryService.buildSummary()));
    }

    /**
     * 政务报告生成
     */
    public String reportGeneration(String reportType, String city, String period) {
        List<KnowledgeItem> relatedKnowledge = knowledgeBase.search(reportType + " " + city);
        return chat("政务报告", buildReportPrompt(reportType, city, period, relatedKnowledge,
                govDataSummaryService.buildSummary()));
    }

    /**
     * 应急预案检索：知识库候选预案作为上下文
     */
    public String planRetrieval(String eventType, String severity) {
        List<KnowledgeItem> plans = searchPlans(eventType, severity);
        return chat("应急预案", buildPlanPrompt(eventType, severity, plans));
    }

    /**
     * 统一模型调用（单轮）：可用时调用DashScope生成，不可用时返回明确提示
     */
    private String chat(String action, String userPrompt) {
        return chatWithHistory(action, null, userPrompt);
    }

    /**
     * 统一模型调用（可携带历史轮次）：可用时调用DashScope生成，不可用时返回明确提示
     */
    private String chatWithHistory(String action, List<DashScopeChatService.DialogueTurn> history, String userPrompt) {
        if (!chatService.isAvailable()) {
            log.warn("DashScope不可用，无法生成{}内容（未配置API Key）", action);
            return unavailable(action, "未配置DashScope API Key");
        }
        try {
            return chatService.chat(SYSTEM_PROMPT, history, userPrompt);
        } catch (Exception e) {
            log.warn("DashScope调用失败，无法生成{}内容: {}", action, e.getMessage());
            return unavailable(action, "云端调用异常（" + e.getMessage() + "）");
        }
    }

    /**
     * 大模型不可用时的明确提示（不编造内容）
     */
    private String unavailable(String action, String reason) {
        return UNAVAILABLE_PREFIX + "无法生成" + action + "内容：" + reason + "。请检查智能服务配置后重试。";
    }

    /**
     * 从知识库检索候选预案（事件类型+预案，兜底按事件类型+应急检索）
     */
    private List<KnowledgeItem> searchPlans(String eventType, String severity) {
        List<KnowledgeItem> plans = knowledgeBase.search(eventType + " 预案 " + severity);
        if (plans.isEmpty()) {
            plans = knowledgeBase.search(eventType + " 应急");
        }
        return plans;
    }

    /**
     * 将知识库检索结果格式化为大模型上下文
     */
    private String buildKnowledgeContext(List<KnowledgeItem> items) {
        if (items == null || items.isEmpty()) {
            return "（知识库中无相关内容）";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            KnowledgeItem item = items.get(i);
            sb.append(i + 1).append(". [").append(item.getCategory()).append("] ")
              .append(item.getTitle()).append("（来源：").append(item.getSource()).append("）\n")
              .append(item.getContent()).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 构建自然语言查询Prompt（RAG）
     */
    private String buildQueryPrompt(String query, List<KnowledgeItem> searchResults, String govData) {
        return "【实时政务数据快照】\n" + govData
                + "【知识库检索结果】\n" + buildKnowledgeContext(searchResults)
                + "【用户问题】\n" + query
                + "\n\n请基于以上实时数据与知识库内容回答用户问题，涉及具体数值时以实时数据快照为准；" +
                "知识库无相关内容时，结合政务领域常识谨慎回答，并注明信息可能不完整。";
    }

    /**
     * 构建多轮对话Prompt（历史轮次随消息数组传递，此处补充实时数据与知识库上下文）
     */
    private String buildConversationPrompt(String query, List<KnowledgeItem> hits, String govData) {
        return "【实时政务数据快照】\n" + govData
                + "【知识库检索结果】\n" + buildKnowledgeContext(hits)
                + "【用户问题】\n" + query
                + "\n\n请结合历史对话、实时数据与知识库内容回答用户问题，涉及具体数值时以实时数据快照为准。";
    }

    /**
     * 构建告警解释Prompt
     */
    private String buildAlertExplanationPrompt(String alertType, String city, String details,
                                                List<KnowledgeItem> relatedKnowledge, String govData) {
        return "请对以下城市告警进行分析，输出：根因判断、影响因素、处置建议。\n"
                + "【告警类型】" + alertType + "\n"
                + "【城市】" + city + "\n"
                + "【告警详情】" + (details != null && !details.isBlank() ? details : "未提供具体参数") + "\n\n"
                + "【实时政务数据快照】\n" + govData
                + "【参考知识】\n" + buildKnowledgeContext(relatedKnowledge);
    }

    /**
     * 构建报告生成Prompt
     */
    private String buildReportPrompt(String reportType, String city, String period,
                                     List<KnowledgeItem> relatedKnowledge, String govData) {
        return "请生成一份政务报告，包含摘要、关键指标、分析、建议四个部分。"
                + "基于给定信息合理撰写，不得编造具体监测数值，未提供的指标请注明暂无数据。\n"
                + "【报告类型】" + reportType + "\n"
                + "【城市】" + city + "\n"
                + "【周期】" + period + "\n\n"
                + "【实时政务数据快照】\n" + govData
                + "【参考知识】\n" + buildKnowledgeContext(relatedKnowledge);
    }

    /**
     * 构建预案检索Prompt
     */
    private String buildPlanPrompt(String eventType, String severity, List<KnowledgeItem> plans) {
        return "请根据事件类型和严重等级整理应急预案，输出：匹配预案、建议响应级别、行动要点、责任部门建议。\n"
                + "【事件类型】" + eventType + "\n"
                + "【严重等级】" + severity + "\n\n"
                + "【知识库候选预案】\n" + buildKnowledgeContext(plans);
    }
}
