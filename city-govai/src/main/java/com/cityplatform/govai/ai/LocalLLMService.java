package com.cityplatform.govai.ai;

import com.cityplatform.govai.knowledge.KnowledgeBase;
import com.cityplatform.govai.knowledge.KnowledgeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

/**
 * 政务AI大模型服务
 * 主路径：调用阿里云百炼（DashScope）通义千问大模型，知识库检索结果作为上下文注入（RAG）
 * 降级路径：未配置API Key或云端调用失败时，使用本地模板生成兑底，保证服务可用
 */
@Service
public class LocalLLMService {

    private static final Logger log = LoggerFactory.getLogger(LocalLLMService.class);

    /** 政务AI助手系统提示词：角色设定与回答规范 */
    private static final String SYSTEM_PROMPT =
            "你是城市治理平台的政务AI决策助手，服务于政府管理人员。" +
            "你熟悉城市环境监测、交通管理、能源调度、水务防汛、应急管理等政务领域知识。" +
            "回答要求：专业严谨、简洁清晰、分点陈述；涉及应急处置时注明依据并给出建议的响应级别；" +
            "不得编造监测数据，数据缺失时明确说明；回答末尾不添加免责声明。";

    private final KnowledgeBase knowledgeBase;
    private final DashScopeChatService chatService;

    public LocalLLMService(KnowledgeBase knowledgeBase, DashScopeChatService chatService) {
        this.knowledgeBase = knowledgeBase;
        this.chatService = chatService;
    }

    /**
     * 当前AI提供方标识（供状态接口对外展示）
     */
    public String getProviderName() {
        return chatService.getProviderName();
    }

    /**
     * 通用生成接口
     * 主路径调用真实大模型，失败时降级为模板生成
     */
    public String generateResponse(String prompt, String context) {
        log.debug("生成响应 - prompt: {}, context长度: {}", prompt, context != null ? context.length() : 0);

        if (chatService.isAvailable()) {
            try {
                String userPrompt = prompt;
                if (context != null && !context.isBlank()) {
                    userPrompt = prompt + "\n\n【上下文信息】\n" + context;
                }
                return chatService.chat(SYSTEM_PROMPT, userPrompt);
            } catch (Exception e) {
                log.warn("DashScope调用失败，降级为模板生成: {}", e.getMessage());
            }
        }
        return generateTemplateResponse(prompt, context);
    }

    /**
     * 模板降级：按关键词路由到各领域固定模板
     */
    private String generateTemplateResponse(String prompt, String context) {
        String lowerPrompt = prompt.toLowerCase();

        if (lowerPrompt.contains("告警") || lowerPrompt.contains("alert") || lowerPrompt.contains("预警")) {
            return generateAlertResponse(prompt, context);
        } else if (lowerPrompt.contains("报告") || lowerPrompt.contains("report") || lowerPrompt.contains("日报")) {
            return generateReportResponse(prompt, context);
        } else if (lowerPrompt.contains("预案") || lowerPrompt.contains("plan") || lowerPrompt.contains("应急")) {
            return generatePlanResponse(prompt, context);
        } else if (lowerPrompt.contains("交通") || lowerPrompt.contains("traffic") || lowerPrompt.contains("拥堵")) {
            return generateTrafficResponse(prompt, context);
        } else if (lowerPrompt.contains("空气") || lowerPrompt.contains("aqi") || lowerPrompt.contains("污染")) {
            return generateAirQualityResponse(prompt, context);
        } else if (lowerPrompt.contains("能耗") || lowerPrompt.contains("energy") || lowerPrompt.contains("电力")) {
            return generateEnergyResponse(prompt, context);
        } else if (lowerPrompt.contains("水") || lowerPrompt.contains("water") || lowerPrompt.contains("洪涝")) {
            return generateWaterResponse(prompt, context);
        } else {
            return generateGeneralResponse(prompt, context);
        }
    }

    /**
     * 告警解释生成
     * 主路径调用真实大模型（知识库内容作为参考上下文），失败时降级为模板
     */
    public String alertExplanation(String alertType, String city, String details) {
        List<KnowledgeItem> relatedKnowledge = knowledgeBase.search(alertType + " " + city);

        if (chatService.isAvailable()) {
            try {
                return chatService.chat(SYSTEM_PROMPT,
                        buildAlertExplanationPrompt(alertType, city, details, relatedKnowledge));
            } catch (Exception e) {
                log.warn("DashScope调用失败，降级为模板生成告警解释: {}", e.getMessage());
            }
        }
        return templateAlertExplanation(alertType, city, details, relatedKnowledge);
    }

    /**
     * 模板降级：告警解释
     */
    private String templateAlertExplanation(String alertType, String city, String details,
                                            List<KnowledgeItem> relatedKnowledge) {
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(alertType).append("告警分析】\n");
        sb.append("城市：").append(city).append("\n");
        sb.append("详情：").append(details).append("\n\n");

        if (!relatedKnowledge.isEmpty()) {
            sb.append("关联知识：\n");
            for (KnowledgeItem item : relatedKnowledge) {
                sb.append("  - ").append(item.getTitle()).append("：")
                  .append(truncate(item.getContent(), 100)).append("\n");
            }
            sb.append("\n");
        }

        sb.append("初步判断：");
        if (details.contains("PM2.5") || details.contains("AQI")) {
            sb.append("当前空气质量指数偏高，主要原因为污染物排放累积及不利气象条件。");
        } else if (details.contains("拥堵") || details.contains("congestion")) {
            sb.append("当前交通流量超过道路承载能力，建议启动分流措施。");
        } else if (details.contains("能耗") || details.contains("consumption")) {
            sb.append("建筑能耗异常升高，可能存在设备故障或管理漏洞。");
        } else if (details.contains("水位") || details.contains("water")) {
            sb.append("水位超过安全阈值，需立即启动防汛响应。");
        } else {
            sb.append("检测到异常指标，建议相关部门核实并采取相应措施。");
        }

        return sb.toString();
    }

    /**
     * 报告生成
     * 主路径调用真实大模型（知识库内容作为参考上下文），失败时降级为模板
     */
    public String reportGeneration(String reportType, String city, String period) {
        List<KnowledgeItem> relatedKnowledge = knowledgeBase.search(reportType + " " + city);

        if (chatService.isAvailable()) {
            try {
                return chatService.chat(SYSTEM_PROMPT,
                        buildReportPrompt(reportType, city, period, relatedKnowledge));
            } catch (Exception e) {
                log.warn("DashScope调用失败，降级为模板生成报告: {}", e.getMessage());
            }
        }
        return templateReportGeneration(reportType, city, period);
    }

    /**
     * 模板降级：报告生成
     */
    private String templateReportGeneration(String reportType, String city, String period) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════\n");
        sb.append("       ").append(reportType).append("\n");
        sb.append("═══════════════════════════════════════\n");
        sb.append("城市：").append(city).append("\n");
        sb.append("周期：").append(period).append("\n");
        sb.append("生成时间：").append(java.time.LocalDateTime.now()).append("\n");
        sb.append("───────────────────────────────────────\n\n");

        sb.append("【摘要】\n");
        sb.append(generateSummary(reportType, city, period)).append("\n\n");

        sb.append("【关键指标】\n");
        sb.append(generateKeyMetrics(reportType)).append("\n\n");

        sb.append("【分析】\n");
        sb.append(generateAnalysis(reportType, city)).append("\n\n");

        sb.append("【建议】\n");
        sb.append(generateRecommendations(reportType)).append("\n");

        sb.append("───────────────────────────────────────\n");
        sb.append("报告生成完毕 | 政务AI决策支持系统\n");

        return sb.toString();
    }

    /**
     * 预案检索
     * 主路径调用真实大模型（知识库候选预案作为参考上下文），失败时降级为模板
     */
    public String planRetrieval(String eventType, String severity) {
        List<KnowledgeItem> plans = searchPlans(eventType, severity);

        if (chatService.isAvailable()) {
            try {
                return chatService.chat(SYSTEM_PROMPT,
                        buildPlanPrompt(eventType, severity, plans));
            } catch (Exception e) {
                log.warn("DashScope调用失败，降级为模板生成预案检索结果: {}", e.getMessage());
            }
        }
        return templatePlanRetrieval(eventType, severity, plans);
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
     * 模板降级：预案检索
     */
    private String templatePlanRetrieval(String eventType, String severity, List<KnowledgeItem> plans) {
        StringBuilder sb = new StringBuilder();
        sb.append("【应急预案检索结果】\n");
        sb.append("事件类型：").append(eventType).append("\n");
        sb.append("严重等级：").append(severity).append("\n\n");

        if (!plans.isEmpty()) {
            sb.append("匹配预案：\n");
            for (int i = 0; i < plans.size(); i++) {
                KnowledgeItem plan = plans.get(i);
                sb.append(String.format("  %d. %s\n", i + 1, plan.getTitle()));
                sb.append("     来源：").append(plan.getSource()).append("\n");
                sb.append("     内容：").append(plan.getContent()).append("\n\n");
            }
        } else {
            sb.append("未找到精确匹配的预案，建议参考通用应急处置流程。\n");
        }

        sb.append("响应建议：\n");
        sb.append(generateResponseAdvice(eventType, severity));

        return sb.toString();
    }

    /**
     * 自然语言查询
     * 主路径：RAG——知识库检索结果注入上下文，由真实大模型生成回答；失败时降级为模板
     */
    public String naturalLanguageQuery(String query) {
        log.info("处理自然语言查询: {}", query);

        List<KnowledgeItem> searchResults = knowledgeBase.search(query);

        if (chatService.isAvailable()) {
            try {
                return chatService.chat(SYSTEM_PROMPT, buildQueryPrompt(query, searchResults));
            } catch (Exception e) {
                log.warn("DashScope调用失败，降级为模板回答: {}", e.getMessage());
            }
        }
        return templateNaturalLanguageQuery(query, searchResults);
    }

    /**
     * 模板降级：自然语言查询
     */
    private String templateNaturalLanguageQuery(String query, List<KnowledgeItem> searchResults) {
        StringBuilder sb = new StringBuilder();
        sb.append("【政务AI智能问答】\n");
        sb.append("查询：").append(query).append("\n\n");

        if (!searchResults.isEmpty()) {
            sb.append("检索到 ").append(searchResults.size()).append(" 条相关知识：\n\n");
            for (int i = 0; i < Math.min(3, searchResults.size()); i++) {
                KnowledgeItem item = searchResults.get(i);
                sb.append(String.format("%d. [%s] %s\n", i + 1, item.getCategory(), item.getTitle()));
                sb.append("   ").append(item.getContent()).append("\n\n");
            }
        }

        sb.append("综合回答：\n");
        sb.append(generateQueryAnswer(query, searchResults));

        return sb.toString();
    }

    // ==================== 真实大模型Prompt构建 ====================

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
    private String buildQueryPrompt(String query, List<KnowledgeItem> searchResults) {
        return "【知识库检索结果】\n" + buildKnowledgeContext(searchResults)
                + "【用户问题】\n" + query
                + "\n\n请基于以上知识库内容回答用户问题。知识库无相关内容时，结合政务领域常识谨慎回答，并注明信息可能不完整。";
    }

    /**
     * 构建告警解释Prompt
     */
    private String buildAlertExplanationPrompt(String alertType, String city, String details,
                                                List<KnowledgeItem> relatedKnowledge) {
        return "请对以下城市告警进行分析，输出：根因判断、影响因素、处置建议。\n"
                + "【告警类型】" + alertType + "\n"
                + "【城市】" + city + "\n"
                + "【告警详情】" + details + "\n\n"
                + "【参考知识】\n" + buildKnowledgeContext(relatedKnowledge);
    }

    /**
     * 构建报告生成Prompt
     */
    private String buildReportPrompt(String reportType, String city, String period,
                                     List<KnowledgeItem> relatedKnowledge) {
        return "请生成一份政务报告，包含摘要、关键指标、分析、建议四个部分。\n"
                + "【报告类型】" + reportType + "\n"
                + "【城市】" + city + "\n"
                + "【周期】" + period + "\n\n"
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

    // ==================== 内部模板生成方法（降级路径） ====================

    private String generateAlertResponse(String prompt, String context) {
        return "基于当前监测数据分析，该告警事件的主要成因包括：气象条件不利、污染物排放累积、" +
                "城市热岛效应加剧等因素的叠加效应。建议启动相应级别的应急响应，" +
                "加强重点区域巡查，及时发布公众防护提示。";
    }

    private String generateReportResponse(String prompt, String context) {
        return "本报告周期内，城市运行总体平稳。各项核心指标处于正常波动区间，" +
                "未发生重大突发事件。建议持续关注交通流量变化趋势，优化信号配时方案，" +
                "同时加强建筑能耗监测，推进节能减排目标落实。";
    }

    private String generatePlanResponse(String prompt, String context) {
        return "根据事件类型和严重等级，建议启动相应级别的应急响应机制。" +
                "各责任部门应按照预案要求，迅速到位、协同处置，" +
                "确保人员安全和城市运行秩序。";
    }

    private String generateTrafficResponse(String prompt, String context) {
        return "当前交通运行状态分析：主干道平均车速下降，部分路段出现拥堵。" +
                "主要原因包括：通勤高峰流量集中、信号配时不合理、交通事故影响等。" +
                "建议采取以下措施：1)优化信号配时方案；2)实施交通分流；" +
                "3)加强事故快速处置；4)引导公共交通出行。";
    }

    private String generateAirQualityResponse(String prompt, String context) {
        return "空气质量分析：当前AQI指数偏高，首要污染物为PM2.5。" +
                "主要成因：1)不利气象条件导致污染物扩散受限；" +
                "2)机动车尾气排放；3)工业污染源排放；4)扬尘污染。" +
                "建议：加强污染源管控，实施机动车限行，增加道路保洁频次。";
    }

    private String generateEnergyResponse(String prompt, String context) {
        return "建筑能耗分析：当前能耗指标异常升高。" +
                "可能原因：1)空调系统运行效率下降；2)照明设备老化；" +
                "3)能源管理存在漏洞；4)极端天气导致用能增加。" +
                "建议：开展能源审计，优化设备运行策略，推进节能改造。";
    }

    private String generateWaterResponse(String prompt, String context) {
        return "水务监测分析：当前水位超过安全阈值。" +
                "主要风险：1)城市内涝风险加剧；2)地下设施进水风险；" +
                "3)河道堤防安全受到威胁。" +
                "建议：立即启动防汛应急响应，开启全部排水泵站，" +
                "组织低洼地区群众转移，加强堤防巡查。";
    }

    private String generateGeneralResponse(String prompt, String context) {
        return "根据已有数据和知识库信息，针对您提出的问题，" +
                "建议从以下几个方面进行分析和处理：1)核实相关监测数据的准确性；" +
                "2)结合历史同期数据进行对比分析；3)参考相关法规和标准；" +
                "4)必要时启动现场核查。如需更详细的分析，请提供具体的参数信息。";
    }

    private String generateSummary(String reportType, String city, String period) {
        switch (reportType) {
            case "daily":
                return String.format("%s在%s期间，城市运行总体平稳，各项核心指标正常。" +
                        "交通运行有序，空气质量良好，水务系统正常运转，建筑能耗处于合理区间。", city, period);
            case "traffic":
                return String.format("%s在%s期间，交通运行整体可控。" +
                        "高峰时段部分路段出现短时拥堵，经信号优化和交通分流后恢复正常。", city, period);
            case "flood":
                return String.format("%s在%s期间，受强降雨影响，部分地区出现积水。" +
                        "经全力排水调度，未造成重大损失。", city, period);
            case "energy":
                return String.format("%s在%s期间，建筑能耗总体处于合理水平。" +
                        "部分公共建筑能耗偏高，已纳入重点监测名单。", city, period);
            default:
                return String.format("%s在%s期间运行正常。", city, period);
        }
    }

    private String generateKeyMetrics(String reportType) {
        switch (reportType) {
            case "daily":
                return "  - 空气质量指数(AQI)：65（良）\n" +
                       "  - 主干道平均车速：32km/h\n" +
                       "  - 供水管网压力：0.28MPa\n" +
                       "  - 建筑综合能耗：72kWh/㎡·年";
            case "traffic":
                return "  - 高峰拥堵指数：6.2（中度拥堵）\n" +
                       "  - 平均通勤时间：38分钟\n" +
                       "  - 公交准点率：87%\n" +
                       "  - 交通事故数：12起";
            case "flood":
                return "  - 最大降雨量：85mm/24h\n" +
                       "  - 最高水位：警戒水位+0.8m\n" +
                       "  - 积水点数量：3处\n" +
                       "  - 排水泵站运行率：100%";
            case "energy":
                return "  - 公共建筑平均能耗：78kWh/㎡·年\n" +
                       "  - 能耗异常建筑：5栋\n" +
                       "  - 节能改造完成率：65%\n" +
                       "  - 可再生能源占比：12%";
            default:
                return "  - 各项指标正常";
        }
    }

    private String generateAnalysis(String reportType, String city) {
        switch (reportType) {
            case "daily":
                return "本日城市运行整体平稳。空气质量方面，PM2.5浓度处于良好水平，" +
                        "但臭氧浓度略有上升，需持续关注。交通方面，早晚高峰流量正常，" +
                        "信号优化效果显著。水务方面，供水稳定，排水系统运行正常。";
            case "traffic":
                return "本周期交通运行分析：拥堵主要集中在商业区周边道路，" +
                        "原因为商业活动集中、停车资源不足。建议加强停车管理，" +
                        "优化公交线网，引导绿色出行。";
            case "flood":
                return "本次降雨过程分析：降雨强度大、历时短，属于典型城市暴雨过程。" +
                        "排水系统经受住了考验，但部分低洼地区仍出现短时积水。" +
                        "建议加强排水管网改造，提升城市韧性。";
            case "energy":
                return "本周能耗分析：受气温升高影响，空调用能增加，" +
                        "导致整体能耗上升。部分建筑存在设备老化、管理粗放问题。" +
                        "建议推进智慧能源管理，实施精细化管控。";
            default:
                        return "各项指标分析正常。";
        }
    }

    private String generateRecommendations(String reportType) {
        switch (reportType) {
            case "daily":
                return "  1. 持续关注臭氧污染趋势，加强VOCs排放管控\n" +
                       "  2. 优化重点路口信号配时，提升通行效率\n" +
                       "  3. 加强供水管网巡查，确保供水安全\n" +
                       "  4. 推进公共建筑节能改造，落实能耗双控目标";
            case "traffic":
                return "  1. 加强商业区停车管理，提高停车位利用率\n" +
                       "  2. 优化公交线网，提升公交服务水平\n" +
                       "  3. 推进智慧交通建设，实现信号自适应控制\n" +
                       "  4. 加强交通安全宣传，减少交通事故发生";
            case "flood":
                return "  1. 加快排水管网改造，提升排水能力\n" +
                       "  2. 加强低洼地区隐患排查，完善应急措施\n" +
                       "  3. 推进海绵城市建设，增强城市韧性\n" +
                       "  4. 加强气象预警，完善联动响应机制";
            case "energy":
                return "  1. 推进能源审计，排查能耗异常建筑\n" +
                       "  2. 加快节能改造，推广高效节能设备\n" +
                       "  3. 建设智慧能源管理平台，实现精细化管控\n" +
                       "  4. 推广可再生能源应用，优化能源结构";
            default:
                return "  继续保持当前管理措施，持续监测关键指标。";
        }
    }

    private String generateResponseAdvice(String eventType, String severity) {
        switch (severity.toLowerCase()) {
            case "1":
            case "i":
            case "特别重大":
                return "  建议立即启动I级响应，成立现场指挥部，调动全市应急资源进行处置。";
            case "2":
            case "ii":
            case "重大":
                return "  建议启动II级响应，相关部门负责人到场指挥，调动主要应急资源。";
            case "3":
            case "iii":
            case "较大":
                return "  建议启动III级响应，责任部门牵头处置，做好资源调配准备。";
            case "4":
            case "iv":
            case "一般":
                return "  建议启动IV级响应，按常规流程处置，持续监测事态发展。";
            default:
                return "  建议根据事态发展及时调整响应级别，确保处置有效。";
        }
    }

    private String generateQueryAnswer(String query, List<KnowledgeItem> searchResults) {
        if (searchResults.isEmpty()) {
            return "  根据现有知识库，暂未找到与查询直接相关的信息。建议提供更具体的关键词或联系相关业务部门获取详细资料。";
        }

        StringBuilder answer = new StringBuilder();
        answer.append("  根据知识库检索结果，为您整理以下信息：\n\n");

        for (int i = 0; i < Math.min(3, searchResults.size()); i++) {
            KnowledgeItem item = searchResults.get(i);
            answer.append(String.format("  %d. %s\n", i + 1, item.getTitle()));
            answer.append("     ").append(item.getContent()).append("\n\n");
        }

        answer.append("  如需了解更多详情，请参考上述资料来源或联系相关业务部门。");
        return answer.toString();
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }
}
