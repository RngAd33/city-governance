package com.cityplatform.govai.knowledge;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 政务知识库
 * 预加载政府法规、应急预案、调度规则等知识条目
 * 支持基于关键词的检索
 */
@Component
public class KnowledgeBase {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBase.class);

    private final List<KnowledgeItem> items = new ArrayList<>();

    @PostConstruct
    public void init() {
        loadRegulations();
        loadEmergencyPlans();
        loadDispatchRules();
        loadSignalTimingStandards();
        loadDrainageRules();
        loadEnergyStandards();
        log.info("政务知识库初始化完成，共加载 {} 条知识条目", items.size());
    }

    /**
     * 搜索知识库
     */
    public List<KnowledgeItem> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String lowerQuery = query.toLowerCase();
        String[] keywords = lowerQuery.split("[\\s,，。；;：:]+");

        return items.stream()
                .map(item -> {
                    double score = calculateRelevance(item, keywords);
                    KnowledgeItem copy = new KnowledgeItem(
                            item.getId(), item.getCategory(), item.getTitle(),
                            item.getContent(), item.getSource());
                    copy.setRelevanceScore(score);
                    return copy;
                })
                .filter(item -> item.getRelevanceScore() > 0)
                .sorted(Comparator.comparingDouble(KnowledgeItem::getRelevanceScore).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * 按类别获取知识条目
     */
    public List<KnowledgeItem> getByCategory(String category) {
        return items.stream()
                .filter(item -> item.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有知识条目
     */
    public List<KnowledgeItem> getAll() {
        return new ArrayList<>(items);
    }

    private double calculateRelevance(KnowledgeItem item, String[] keywords) {
        double score = 0;
        String titleLower = item.getTitle().toLowerCase();
        String contentLower = item.getContent().toLowerCase();
        String categoryLower = item.getCategory().toLowerCase();

        for (String keyword : keywords) {
            if (keyword.isEmpty()) continue;
            if (titleLower.contains(keyword)) score += 3;
            if (contentLower.contains(keyword)) score += 1;
            if (categoryLower.contains(keyword)) score += 2;
        }
        return score;
    }

    private void loadRegulations() {
        items.add(new KnowledgeItem("REG-001", "regulation",
                "《城市空气质量污染防治条例》",
                "当AQI超过150时，启动橙色预警，建议减少户外施工和机动车出行；" +
                "当AQI超过200时，启动红色预警，实施机动车单双号限行，暂停重污染企业生产。",
                "市生态环境局"));

        items.add(new KnowledgeItem("REG-002", "regulation",
                "《城市供水应急预案管理办法》",
                "当水位超过警戒水位1.5米时，启动三级防汛响应；" +
                "当水位超过警戒水位2.5米时，启动二级防汛响应，组织低洼地区居民转移。",
                "市水务局"));

        items.add(new KnowledgeItem("REG-003", "regulation",
                "《城市交通管理条例》",
                "高峰时段主干道平均车速低于20km/h时，实施交通分流措施；" +
                "拥堵指数超过8.0时，启动交通管制，限制货车进入核心区。",
                "市交通管理局"));

        items.add(new KnowledgeItem("REG-004", "regulation",
                "《公共建筑节能管理办法》",
                "办公建筑单位面积能耗超过80kWh/㎡·年时，需进行能源审计；" +
                "超过120kWh/㎡·年时，强制实施节能改造。",
                "市住建局"));

        items.add(new KnowledgeItem("REG-005", "regulation",
                "《城市突发事件应对条例》",
                "突发事件按照影响范围分为四级：一般（IV级）、较大（III级）、重大（II级）、特别重大（I级）。" +
                "各级别对应不同的响应机制和资源调配方案。",
                "市应急管理局"));
    }

    private void loadEmergencyPlans() {
        items.add(new KnowledgeItem("PLAN-001", "emergency_plan",
                "城市防洪应急预案",
                "适用于城市范围内因暴雨、洪水引发的洪涝灾害。" +
                "响应流程：监测预警→会商研判→启动响应→抢险救援→灾后恢复。" +
                "关键措施：开启排水泵站、疏散低洼地区群众、加固堤防、预置抢险物资。",
                "市防汛抗旱指挥部"));

        items.add(new KnowledgeItem("PLAN-002", "emergency_plan",
                "交通事故应急处置预案",
                "适用于城市主干道、高速公路发生的重大交通事故。" +
                "响应流程：接警→现场勘查→交通疏导→事故处理→恢复通行。" +
                "关键措施：启动无人机勘查、实施临时交通管制、开辟应急救援通道。",
                "市交通管理局"));

        items.add(new KnowledgeItem("PLAN-003", "emergency_plan",
                "大气污染应急响应预案",
                "适用于AQI持续超过150的重污染天气。" +
                "响应流程：监测预报→预警发布→应急响应→措施落实→效果评估。" +
                "关键措施：工业企业限产、机动车限行、停止露天作业、加强道路保洁。",
                "市生态环境局"));

        items.add(new KnowledgeItem("PLAN-004", "emergency_plan",
                "大面积停电应急预案",
                "适用于因设备故障、自然灾害导致的大面积停电事故。" +
                "响应流程：故障定位→负荷转移→抢修恢复→善后处理。" +
                "关键措施：优先保障医院、交通信号、供水等关键设施供电。",
                "市供电公司"));
    }

    private void loadDispatchRules() {
        items.add(new KnowledgeItem("RULE-001", "dispatch_rule",
                "交通信号配时调度规则",
                "信号周期时长应在60-180秒之间；" +
                "主干道绿波协调带宽应达到周期时长的40%-60%；" +
                "行人过街最短绿灯时间不小于15秒；" +
                "公交优先信号延迟不超过10秒。",
                "市交通研究中心"));

        items.add(new KnowledgeItem("RULE-002", "dispatch_rule",
                "排水泵站调度规则",
                "当降雨量达到10mm/h时，预降管网水位；" +
                "当降雨量达到20mm/h时，开启全部排水泵站；" +
                "当管网水位达到警戒线时，启动应急排水方案；" +
                "泵站运行优先级：下穿隧道>低洼路段>一般路段。",
                "市排水管理处"));

        items.add(new KnowledgeItem("RULE-003", "dispatch_rule",
                "重污染天气减排调度规则",
                "橙色预警期间：涉气企业减排30%，停止土石方作业；" +
                "红色预警期间：涉气企业减排50%，机动车单双号限行，停止所有露天施工。",
                "市生态环境局"));
    }

    private void loadSignalTimingStandards() {
        items.add(new KnowledgeItem("STD-001", "standard",
                "城市道路信号配时设计标准",
                "交叉口信号周期：主干道60-150秒，次干道40-100秒，支路30-60秒。" +
                "相位差协调：相邻交叉口相位差应满足绿波带设计要求。" +
                "黄灯时间：3-4秒；全红时间：1-3秒。" +
                "左转保护相位：当左转流量超过200pcu/h时设置。",
                "市交通设计研究院"));

        items.add(new KnowledgeItem("STD-002", "standard",
                "公交信号优先技术标准",
                "公交优先策略：绿灯延长、红灯早断、相位插入。" +
                "公交检测器布设距离：距停止线80-150米。" +
                "优先响应时间：检测到公交车辆后5秒内调整信号。" +
                "社会车辆影响：优先相位对社会车辆延误增加不超过15%。",
                "市交通研究中心"));
    }

    private void loadDrainageRules() {
        items.add(new KnowledgeItem("RULE-004", "dispatch_rule",
                "城市排水防涝调度规则",
                "排水管网设计标准：一般地区1年一遇，重要地区3年一遇，地下通道5年一遇。" +
                "泵站排水能力：应满足设计重现期降雨量的排放需求。" +
                "应急响应：积水深度超过30cm时封闭交通，超过50cm时强制疏散。" +
                "调度原则：高水高排、低水低排、分区排水、系统调度。",
                "市排水管理处"));

        items.add(new KnowledgeItem("RULE-005", "dispatch_rule",
                "河道水位调度规则",
                "正常水位：低于警戒水位1.0米以上。" +
                "警戒水位：启动巡查，预置抢险物资。" +
                "保证水位：全力抢险，组织转移。" +
                "调度原则：上下游统筹、蓄泄兼顾、确保重点。",
                "市水务局"));
    }

    private void loadEnergyStandards() {
        items.add(new KnowledgeItem("STD-003", "standard",
                "公共建筑能耗限额标准",
                "办公建筑：60-100 kWh/㎡·年。" +
                "商业建筑：80-150 kWh/㎡·年。" +
                "医疗建筑：120-200 kWh/㎡·年。" +
                "教育建筑：40-80 kWh/㎡·年。" +
                "超过限额20%时需进行能源审计和节能改造。",
                "市住建局"));

        items.add(new KnowledgeItem("STD-004", "standard",
                "建筑能耗监测标准",
                "能耗监测数据上传频率：每15分钟一次。" +
                "分项计量：照明、空调、动力、特殊用电应分别计量。" +
                "数据质量：采集准确率不低于95%。" +
                "异常告警：能耗突增超过30%时触发告警。",
                "市住建局"));
    }
}
