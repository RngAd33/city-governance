package com.cityplatform.govai.ai;

import com.cityplatform.govai.ai.model.Report;
import com.cityplatform.govai.knowledge.KnowledgeBase;
import com.cityplatform.govai.knowledge.KnowledgeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 政务报告生成器
 * 基于模板和知识库数据，自动生成各类政务报告
 * 包含摘要、关键指标、分析和建议四个核心部分
 */
@Component
public class ReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerator.class);

    private final LocalLLMService llmService;
    private final KnowledgeBase knowledgeBase;

    public ReportGenerator(LocalLLMService llmService, KnowledgeBase knowledgeBase) {
        this.llmService = llmService;
        this.knowledgeBase = knowledgeBase;
    }

    /**
     * 生成城市运行日报
     */
    public Report generateDailyReport(String city, String date) {
        log.info("生成城市运行日报 - city: {}, date: {}", city, date);

        String summary = String.format(
                "%s在%s期间，城市运行总体平稳有序。空气质量良好，" +
                "交通运行正常，水务系统稳定，建筑能耗处于合理区间。" +
                "未发生重大突发事件，各保障部门运行正常。",
                city, date);

        Map<String, String> keyMetrics = new LinkedHashMap<>();
        keyMetrics.put("空气质量指数(AQI)", "65（良）");
        keyMetrics.put("PM2.5浓度", "45μg/m³");
        keyMetrics.put("主干道平均车速", "32km/h");
        keyMetrics.put("高峰拥堵指数", "5.8（轻度拥堵）");
        keyMetrics.put("供水管网压力", "0.28MPa（正常）");
        keyMetrics.put("排水管网运行率", "100%");
        keyMetrics.put("建筑综合能耗", "72kWh/㎡·年");
        keyMetrics.put("突发事件数量", "0起");

        String analysis = "本日城市运行分析：\n" +
                "1. 空气质量方面，PM2.5浓度处于良好水平，臭氧浓度略有上升但仍在标准范围内。" +
                "气象条件总体有利于污染物扩散。\n" +
                "2. 交通方面，早晚高峰流量正常，主干道平均车速32km/h，信号优化效果持续显现。" +
                "商业区周边出现短时拥堵，经疏导后恢复正常。\n" +
                "3. 水务方面，供水管网压力稳定，排水系统运行正常，未出现积水点。\n" +
                "4. 能源方面，建筑综合能耗72kWh/㎡·年，处于合理区间。" +
                "部分公共建筑能耗偏高，已纳入重点监测。";

        List<String> recommendations = new ArrayList<>();
        recommendations.add("持续关注臭氧污染趋势，加强VOCs排放管控");
        recommendations.add("优化商业区周边信号配时，缓解高峰拥堵");
        recommendations.add("加强供水管网巡查，确保供水安全");
        recommendations.add("推进公共建筑节能改造，落实能耗双控目标");
        recommendations.add("完善应急值守机制，确保信息畅通");

        return new Report("daily", city, date, summary, keyMetrics, analysis, recommendations);
    }

    /**
     * 生成交通优化简报
     */
    public Report generateTrafficBrief(String city, String date) {
        log.info("生成交通优化简报 - city: {}, date: {}", city, date);

        String summary = String.format(
                "%s在%s期间，交通运行整体可控。高峰时段部分路段出现拥堵，" +
                "经信号优化和交通分流后恢复正常。公交服务水平持续提升，" +
                "交通事故发生率同比下降。",
                city, date);

        Map<String, String> keyMetrics = new LinkedHashMap<>();
        keyMetrics.put("高峰拥堵指数", "6.2（中度拥堵）");
        keyMetrics.put("平均通勤时间", "38分钟");
        keyMetrics.put("公交准点率", "87%");
        keyMetrics.put("交通事故数", "12起（同比下降15%）");
        keyMetrics.put("信号优化路口数", "25个");
        keyMetrics.put("绿波协调路段", "8条");
        keyMetrics.put("违法停车查处", "320起");

        String analysis = "本周期交通运行分析：\n" +
                "1. 拥堵特征：拥堵主要集中在商业区周边道路和主要通勤走廊，" +
                "早高峰拥堵指数达到7.2，晚高峰为6.8。\n" +
                "2. 成因分析：商业活动集中导致停车需求激增，部分道路施工占道影响通行能力，" +
                "信号配时不匹配加剧了拥堵。\n" +
                "3. 优化效果：完成25个路口信号优化，8条路段实现绿波协调，" +
                "优化后平均延误下降12%。\n" +
                "4. 公交服务：公交准点率87%，新增2条公交专用道，公交吸引力有所提升。";

        List<String> recommendations = new ArrayList<>();
        recommendations.add("加强商业区停车管理，提高停车位利用率");
        recommendations.add("推进智慧交通建设，实现信号自适应控制");
        recommendations.add("优化公交线网，提升公交服务水平和吸引力");
        recommendations.add("加快道路施工进度，减少占道影响");
        recommendations.add("加强交通安全宣传，减少交通事故发生");

        return new Report("traffic", city, date, summary, keyMetrics, analysis, recommendations);
    }

    /**
     * 生成洪涝风险预警报告
     */
    public Report generateFloodRiskAlert(String city, String date) {
        log.info("生成洪涝风险预警报告 - city: {}, date: {}", city, date);

        String summary = String.format(
                "%s在%s期间，受强降雨影响，部分地区出现积水。" +
                "经全力排水调度，未造成重大损失。当前防汛形势依然严峻，" +
                "需继续保持高度警惕。",
                city, date);

        Map<String, String> keyMetrics = new LinkedHashMap<>();
        keyMetrics.put("最大降雨量", "85mm/24h");
        keyMetrics.put("最高水位", "警戒水位+0.8m");
        keyMetrics.put("积水点数量", "3处");
        keyMetrics.put("排水泵站运行率", "100%");
        keyMetrics.put("转移群众数", "120人");
        keyMetrics.put("直接经济损失", "约50万元");
        keyMetrics.put("应急响应级别", "III级");

        String analysis = "本次降雨过程分析：\n" +
                "1. 降雨特征：本次降雨强度大、历时短，属于典型城市暴雨过程。" +
                "最大小时雨强达到45mm/h，超过排水管网设计标准。\n" +
                "2. 积水情况：3处积水点分别位于下穿隧道和低洼路段，" +
                "最大积水深度40cm，经紧急排水后2小时内排除。\n" +
                "3. 响应情况：启动III级应急响应，开启全部排水泵站，" +
                "组织120名群众转移，调配抢险物资200余件。\n" +
                "4. 经验教训：排水系统经受住了考验，但部分低洼地区仍存在短板，" +
                "需加快排水管网改造。";

        List<String> recommendations = new ArrayList<>();
        recommendations.add("加快排水管网改造，提升排水能力");
        recommendations.add("加强低洼地区隐患排查，完善应急措施");
        recommendations.add("推进海绵城市建设，增强城市韧性");
        recommendations.add("加强气象预警，完善联动响应机制");
        recommendations.add("定期开展防汛演练，提高应急处置能力");

        return new Report("flood", city, date, summary, keyMetrics, analysis, recommendations);
    }

    /**
     * 生成建筑能耗周报
     */
    public Report generateEnergyWeeklyReport(String city, String week) {
        log.info("生成建筑能耗周报 - city: {}, week: {}", city, week);

        String summary = String.format(
                "%s在%s期间，建筑能耗总体处于合理水平。" +
                "受气温升高影响，空调用能增加，整体能耗有所上升。" +
                "部分公共建筑能耗偏高，已纳入重点监测名单。",
                city, week);

        Map<String, String> keyMetrics = new LinkedHashMap<>();
        keyMetrics.put("公共建筑平均能耗", "78kWh/㎡·年");
        keyMetrics.put("能耗异常建筑", "5栋");
        keyMetrics.put("节能改造完成率", "65%");
        keyMetrics.put("可再生能源占比", "12%");
        keyMetrics.put("能源审计完成", "3栋");
        keyMetrics.put("能耗监测覆盖率", "92%");
        keyMetrics.put("单位GDP能耗下降", "3.2%");

        String analysis = "本周能耗分析：\n" +
                "1. 总体情况：公共建筑平均能耗78kWh/㎡·年，略高于上周。" +
                "主要受气温升高影响，空调用能增加约15%。\n" +
                "2. 异常建筑：5栋建筑能耗超过限额标准，主要原因为设备老化、" +
                "管理粗放。已下发整改通知，要求限期完成能源审计。\n" +
                "3. 节能进展：节能改造完成率65%，本周完成2栋建筑节能改造，" +
                "预计年节约标煤500吨。\n" +
                "4. 监测情况：能耗监测覆盖率92%，数据上传准确率98%，" +
                "为能耗管理提供了有力支撑。";

        List<String> recommendations = new ArrayList<>();
        recommendations.add("推进能源审计，排查能耗异常建筑");
        recommendations.add("加快节能改造，推广高效节能设备");
        recommendations.add("建设智慧能源管理平台，实现精细化管控");
        recommendations.add("推广可再生能源应用，优化能源结构");
        recommendations.add("加强节能宣传，提高公众节能意识");

        return new Report("energy", city, week, summary, keyMetrics, analysis, recommendations);
    }

    /**
     * 生成格式化报告文本
     */
    public String formatReportAsText(Report report) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════\n");
        sb.append("       ").append(getReportTitle(report.getReportType())).append("\n");
        sb.append("═══════════════════════════════════════\n");
        sb.append("城市：").append(report.getCity()).append("\n");
        sb.append("周期：").append(report.getDate()).append("\n");
        sb.append("生成时间：").append(java.time.LocalDateTime.now()).append("\n");
        sb.append("───────────────────────────────────────\n\n");

        sb.append("【摘要】\n").append(report.getSummary()).append("\n\n");

        sb.append("【关键指标】\n");
        report.getKeyMetrics().forEach((key, value) ->
                sb.append("  - ").append(key).append("：").append(value).append("\n"));
        sb.append("\n");

        sb.append("【分析】\n").append(report.getAnalysis()).append("\n\n");

        sb.append("【建议】\n");
        for (int i = 0; i < report.getRecommendations().size(); i++) {
            sb.append("  ").append(i + 1).append(". ")
              .append(report.getRecommendations().get(i)).append("\n");
        }

        sb.append("\n───────────────────────────────────────\n");
        sb.append("报告生成完毕 | 政务AI决策支持系统\n");

        return sb.toString();
    }

    private String getReportTitle(String reportType) {
        switch (reportType) {
            case "daily": return "城市运行日报";
            case "traffic": return "交通优化简报";
            case "flood": return "洪涝风险预警报告";
            case "energy": return "建筑能耗周报";
            default: return "政务报告";
        }
    }
}
