package com.cityplatform.govai.ai;

import com.cityplatform.govai.ai.model.EmergencyPlan;
import com.cityplatform.govai.knowledge.KnowledgeBase;
import com.cityplatform.govai.knowledge.KnowledgeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 应急预案检索器
 * 根据事件类型和严重等级，从知识库中匹配最合适的应急预案
 * 每个预案包含名称、适用范围、响应级别、行动项和责任部门
 */
@Component
public class PlanRetriever {

    private static final Logger log = LoggerFactory.getLogger(PlanRetriever.class);

    private final KnowledgeBase knowledgeBase;

    public PlanRetriever(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    /**
     * 根据事件类型和严重等级检索应急预案
     */
    public EmergencyPlan retrievePlan(String eventType, String severity) {
        log.info("检索应急预案 - eventType: {}, severity: {}", eventType, severity);

        String lowerEventType = eventType.toLowerCase();

        // 根据事件类型匹配预案
        if (lowerEventType.contains("flood") || lowerEventType.contains("洪涝") || lowerEventType.contains("水")) {
            return buildFloodPlan(severity);
        } else if (lowerEventType.contains("traffic") || lowerEventType.contains("交通") || lowerEventType.contains("事故")) {
            return buildTrafficPlan(severity);
        } else if (lowerEventType.contains("air") || lowerEventType.contains("空气") || lowerEventType.contains("污染")) {
            return buildAirPollutionPlan(severity);
        } else if (lowerEventType.contains("power") || lowerEventType.contains("电力") || lowerEventType.contains("停电")) {
            return buildPowerOutagePlan(severity);
        } else {
            return buildGeneralPlan(eventType, severity);
        }
    }

    /**
     * 构建防洪应急预案
     */
    private EmergencyPlan buildFloodPlan(String severity) {
        String planName = "城市防洪应急预案";
        String applicableScope = "城市范围内因暴雨、洪水引发的洪涝灾害";
        String responseLevel = mapSeverityToResponseLevel(severity);

        List<String> actionItems = new ArrayList<>();
        actionItems.add("启动防汛应急响应，成立现场指挥部");
        actionItems.add("开启全部排水泵站，全力排水");
        actionItems.add("组织低洼地区群众转移安置");
        actionItems.add("加强堤防巡查，排查安全隐患");
        actionItems.add("封闭积水严重路段，确保交通安全");
        actionItems.add("预置抢险物资和救援队伍");
        actionItems.add("持续监测水位变化，及时发布预警信息");

        List<String> departments = new ArrayList<>();
        departments.add("市防汛抗旱指挥部");
        departments.add("市水务局");
        departments.add("市应急管理局");
        departments.add("市交通管理局");
        departments.add("市住建局");
        departments.add("市气象局");

        return new EmergencyPlan(planName, "flood", severity, applicableScope,
                responseLevel, actionItems, departments);
    }

    /**
     * 构建交通事故应急预案
     */
    private EmergencyPlan buildTrafficPlan(String severity) {
        String planName = "交通事故应急处置预案";
        String applicableScope = "城市主干道、高速公路发生的重大交通事故";
        String responseLevel = mapSeverityToResponseLevel(severity);

        List<String> actionItems = new ArrayList<>();
        actionItems.add("接警后迅速出警，赶赴现场");
        actionItems.add("启动无人机进行现场勘查");
        actionItems.add("实施临时交通管制，疏导车辆");
        actionItems.add("开辟应急救援通道");
        actionItems.add("组织伤员救治和转运");
        actionItems.add("快速清理现场，恢复交通");
        actionItems.add("开展事故调查，认定责任");

        List<String> departments = new ArrayList<>();
        departments.add("市交通管理局");
        departments.add("市公安局交警支队");
        departments.add("市卫健委");
        departments.add("市应急管理局");
        departments.add("市消防救援支队");

        return new EmergencyPlan(planName, "traffic", severity, applicableScope,
                responseLevel, actionItems, departments);
    }

    /**
     * 构建大气污染应急预案
     */
    private EmergencyPlan buildAirPollutionPlan(String severity) {
        String planName = "大气污染应急响应预案";
        String applicableScope = "AQI持续超过150的重污染天气";
        String responseLevel = mapSeverityToResponseLevel(severity);

        List<String> actionItems = new ArrayList<>();
        actionItems.add("发布重污染天气预警信息");
        actionItems.add("涉气企业限产减排");
        actionItems.add("实施机动车限行措施");
        actionItems.add("停止土石方作业和露天施工");
        actionItems.add("增加道路保洁频次");
        actionItems.add("建议市民减少户外活动");
        actionItems.add("加强空气质量监测和会商研判");

        List<String> departments = new ArrayList<>();
        departments.add("市生态环境局");
        departments.add("市交通管理局");
        departments.add("市住建局");
        departments.add("市工信局");
        departments.add("市气象局");
        departments.add("市卫健委");

        return new EmergencyPlan(planName, "air_pollution", severity, applicableScope,
                responseLevel, actionItems, departments);
    }

    /**
     * 构建停电应急预案
     */
    private EmergencyPlan buildPowerOutagePlan(String severity) {
        String planName = "大面积停电应急预案";
        String applicableScope = "因设备故障、自然灾害导致的大面积停电事故";
        String responseLevel = mapSeverityToResponseLevel(severity);

        List<String> actionItems = new ArrayList<>();
        actionItems.add("迅速定位故障点，隔离故障区域");
        actionItems.add("启动负荷转移，恢复非故障区域供电");
        actionItems.add("优先保障医院、交通信号、供水等关键设施");
        actionItems.add("组织抢修队伍，全力修复故障");
        actionItems.add("启动应急发电车，保障重要场所供电");
        actionItems.add("及时发布停电信息和抢修进展");
        actionItems.add("开展故障分析，制定防范措施");

        List<String> departments = new ArrayList<>();
        departments.add("市供电公司");
        departments.add("市应急管理局");
        departments.add("市交通管理局");
        departments.add("市卫健委");
        departments.add("市通信管理局");

        return new EmergencyPlan(planName, "power_outage", severity, applicableScope,
                responseLevel, actionItems, departments);
    }

    /**
     * 构建通用应急预案
     */
    private EmergencyPlan buildGeneralPlan(String eventType, String severity) {
        String planName = "突发事件通用应急预案";
        String applicableScope = "各类突发事件的通用应急处置";
        String responseLevel = mapSeverityToResponseLevel(severity);

        List<String> actionItems = new ArrayList<>();
        actionItems.add("启动应急响应机制，成立指挥机构");
        actionItems.add("迅速收集信息，研判事态发展");
        actionItems.add("调配应急资源，组织处置力量");
        actionItems.add("及时发布信息，回应社会关切");
        actionItems.add("做好善后处理，恢复正常秩序");
        actionItems.add("总结经验教训，完善预案体系");

        List<String> departments = new ArrayList<>();
        departments.add("市应急管理局");
        departments.add("市政府办公室");
        departments.add("市公安局");
        departments.add("市卫健委");

        return new EmergencyPlan(planName, eventType, severity, applicableScope,
                responseLevel, actionItems, departments);
    }

    /**
     * 将严重等级映射为响应级别
     */
    private String mapSeverityToResponseLevel(String severity) {
        if (severity == null) return "III级";
        switch (severity.toLowerCase()) {
            case "1":
            case "i":
            case "特别重大":
                return "I级（特别重大）";
            case "2":
            case "ii":
            case "重大":
                return "II级（重大）";
            case "3":
            case "iii":
            case "较大":
                return "III级（较大）";
            case "4":
            case "iv":
            case "一般":
                return "IV级（一般）";
            default:
                return "III级（较大）";
        }
    }

    /**
     * 获取所有可用预案类型
     */
    public List<String> getAvailablePlanTypes() {
        return Arrays.asList("flood", "traffic", "air_pollution", "power_outage", "general");
    }
}
