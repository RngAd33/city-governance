package com.cityplatform.govai.ai;

import com.cityplatform.govai.ai.model.Explanation;
import com.cityplatform.govai.knowledge.KnowledgeBase;
import com.cityplatform.govai.knowledge.KnowledgeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 告警解释器
 * 结合规则引擎和LLM模板生成，对各类城市告警进行智能解释
 * 输出结构化的解释结果，包括根因、影响因素、建议和置信度
 */
@Component
public class AlertExplainer {

    private static final Logger log = LoggerFactory.getLogger(AlertExplainer.class);

    private final LocalLLMService llmService;
    private final KnowledgeBase knowledgeBase;

    public AlertExplainer(LocalLLMService llmService, KnowledgeBase knowledgeBase) {
        this.llmService = llmService;
        this.knowledgeBase = knowledgeBase;
    }

    /**
     * 解释空气质量告警
     */
    public Explanation explainAirQualityAlert(String city, double pm25, int aqi) {
        log.info("解释空气质量告警 - city: {}, PM2.5: {}, AQI: {}", city, pm25, aqi);

        String alertType = "空气质量";
        String rootCause;
        List<String> contributingFactors = new ArrayList<>();
        List<String> recommendedActions = new ArrayList<>();
        double confidence;

        // 基于AQI等级的规则判断
        if (aqi > 300) {
            rootCause = "严重污染：PM2.5浓度极高，可能由区域传输、极端不利气象条件叠加本地排放导致";
            contributingFactors.add("区域污染物传输");
            contributingFactors.add("极端静稳天气，扩散条件极差");
            contributingFactors.add("本地工业及机动车排放累积");
            recommendedActions.add("立即启动红色预警，实施机动车单双号限行");
            recommendedActions.add("暂停重污染企业生产和户外施工");
            recommendedActions.add("建议市民减少外出，佩戴防护口罩");
            confidence = 0.92;
        } else if (aqi > 200) {
            rootCause = "重度污染：PM2.5浓度显著偏高，不利气象条件下污染物持续累积";
            contributingFactors.add("静稳天气导致污染物扩散受限");
            contributingFactors.add("机动车尾气排放增加");
            contributingFactors.add("部分工业企业超标排放");
            recommendedActions.add("启动橙色预警，加强污染源管控");
            recommendedActions.add("实施机动车限行措施");
            recommendedActions.add("增加道路保洁频次，抑制扬尘");
            confidence = 0.88;
        } else if (aqi > 150) {
            rootCause = "中度污染：PM2.5浓度超过标准，气象条件不利于污染物扩散";
            contributingFactors.add("风速较低，污染物水平扩散能力弱");
            contributingFactors.add("逆温层抑制垂直扩散");
            contributingFactors.add("城市扬尘和机动车排放叠加");
            recommendedActions.add("发布橙色预警提醒");
            recommendedActions.add("加强重点污染源监管");
            recommendedActions.add("建议敏感人群减少户外活动");
            confidence = 0.85;
        } else if (aqi > 100) {
            rootCause = "轻度污染：PM2.5浓度略超标准，需关注污染趋势";
            contributingFactors.add("局部排放源影响");
            contributingFactors.add("气象条件一般，扩散能力有限");
            recommendedActions.add("加强监测，关注污染变化趋势");
            recommendedActions.add("提醒市民注意防护");
            confidence = 0.78;
        } else {
            rootCause = "空气质量正常，当前污染水平在可接受范围内";
            contributingFactors.add("正常气象波动");
            recommendedActions.add("继续保持常规监测");
            confidence = 0.95;
        }

        // 结合PM2.5细化分析
        if (pm25 > 250) {
            contributingFactors.add("PM2.5严重超标，可能存在突发排放事件");
        } else if (pm25 > 150) {
            contributingFactors.add("PM2.5浓度较高，需排查周边污染源");
        }

        // 从知识库获取补充建议
        List<KnowledgeItem> relatedRules = knowledgeBase.search("空气 污染 预警 " + city);
        for (KnowledgeItem item : relatedRules) {
            if (item.getContent().contains("橙色") && aqi > 150 && aqi <= 200) {
                recommendedActions.add("依据《城市空气质量污染防治条例》启动橙色预警措施");
            }
            if (item.getContent().contains("红色") && aqi > 200) {
                recommendedActions.add("依据《城市空气质量污染防治条例》启动红色预警措施");
            }
        }

        String summary = String.format("%s当前AQI为%d，PM2.5浓度为%.1fμg/m³，%s。",
                city, aqi, pm25, aqi > 150 ? "空气质量超标，需采取应对措施" : "空气质量正常");

        return new Explanation(alertType, city, rootCause, contributingFactors,
                recommendedActions, confidence, summary);
    }

    /**
     * 解释交通告警
     */
    public Explanation explainTrafficAlert(String city, String location, double congestionLevel) {
        log.info("解释交通告警 - city: {}, location: {}, congestionLevel: {}", city, location, congestionLevel);

        String alertType = "交通拥堵";
        String rootCause;
        List<String> contributingFactors = new ArrayList<>();
        List<String> recommendedActions = new ArrayList<>();
        double confidence;

        if (congestionLevel > 8.0) {
            rootCause = String.format("%s区域发生严重拥堵，交通流量远超道路设计通行能力", location);
            contributingFactors.add("通勤高峰流量集中爆发");
            contributingFactors.add("道路施工或突发事件导致车道减少");
            contributingFactors.add("信号配时不合理，通行效率低下");
            contributingFactors.add("交通事故导致车辆积压");
            recommendedActions.add("立即启动交通管制，限制进入拥堵区域");
            recommendedActions.add("实施远端分流，引导车辆绕行");
            recommendedActions.add("派遣交警现场疏导，快速处置事故");
            recommendedActions.add("通过交通广播和导航平台发布拥堵信息");
            confidence = 0.90;
        } else if (congestionLevel > 6.0) {
            rootCause = String.format("%s区域发生中度拥堵，交通流量超过道路承载能力", location);
            contributingFactors.add("高峰时段车流量大");
            contributingFactors.add("部分路口信号周期不匹配");
            contributingFactors.add("路边停车影响通行");
            recommendedActions.add("优化信号配时方案，实施绿波协调");
            recommendedActions.add("加强交通疏导，清理违停车辆");
            recommendedActions.add("发布实时路况信息，引导分流");
            confidence = 0.85;
        } else if (congestionLevel > 4.0) {
            rootCause = String.format("%s区域出现轻度拥堵，交通流量接近饱和", location);
            contributingFactors.add("短时流量波动");
            contributingFactors.add("信号配时局部不优");
            recommendedActions.add("微调信号配时参数");
            recommendedActions.add("关注流量变化趋势");
            confidence = 0.80;
        } else {
            rootCause = String.format("%s区域交通运行正常", location);
            contributingFactors.add("交通流量在正常范围内");
            recommendedActions.add("维持当前管理措施");
            confidence = 0.92;
        }

        // 从知识库获取补充建议
        List<KnowledgeItem> relatedRules = knowledgeBase.search("交通 拥堵 信号 " + city);
        for (KnowledgeItem item : relatedRules) {
            if (item.getContent().contains("分流") && congestionLevel > 6.0) {
                recommendedActions.add("依据《城市交通管理条例》实施交通分流措施");
            }
            if (item.getContent().contains("管制") && congestionLevel > 8.0) {
                recommendedActions.add("依据《城市交通管理条例》启动交通管制");
            }
        }

        String summary = String.format("%s区域拥堵指数为%.1f，%s。",
                location, congestionLevel,
                congestionLevel > 6.0 ? "交通拥堵严重，需立即采取疏导措施" : "交通运行基本正常");

        return new Explanation(alertType, city, rootCause, contributingFactors,
                recommendedActions, confidence, summary);
    }

    /**
     * 解释能耗告警
     */
    public Explanation explainEnergyAlert(String city, String building, double consumption) {
        log.info("解释能耗告警 - city: {}, building: {}, consumption: {}", city, building, consumption);

        String alertType = "能耗异常";
        String rootCause;
        List<String> contributingFactors = new ArrayList<>();
        List<String> recommendedActions = new ArrayList<>();
        double confidence;

        if (consumption > 150) {
            rootCause = String.format("%s能耗严重超标，单位面积能耗达到%.1f kWh/㎡·年，远超限额标准",
                    building, consumption);
            contributingFactors.add("空调系统运行效率低下或设备老化");
            contributingFactors.add("能源管理存在漏洞，设备空转现象严重");
            contributingFactors.add("极端天气导致用能需求激增");
            contributingFactors.add("建筑围护结构保温性能下降");
            recommendedActions.add("立即开展能源审计，排查能耗异常原因");
            recommendedActions.add("检查空调系统运行状态，清洗或更换滤网");
            recommendedActions.add("优化设备运行策略，杜绝空转浪费");
            recommendedActions.add("制定节能改造方案，推进设备升级");
            confidence = 0.88;
        } else if (consumption > 100) {
            rootCause = String.format("%s能耗偏高，单位面积能耗为%.1f kWh/㎡·年，超过限额标准",
                    building, consumption);
            contributingFactors.add("设备老化导致能效下降");
            contributingFactors.add("运行管理不够精细");
            contributingFactors.add("部分区域过度照明或空调");
            recommendedActions.add("加强能耗监测，分析用能结构");
            recommendedActions.add("优化设备运行时间表");
            recommendedActions.add("开展节能宣传，提高人员节能意识");
            confidence = 0.82;
        } else if (consumption > 80) {
            rootCause = String.format("%s能耗略高，需关注用能趋势", building);
            contributingFactors.add("用能设备逐渐老化");
            contributingFactors.add("使用需求增加");
            recommendedActions.add("定期维护设备，保持运行效率");
            recommendedActions.add("关注能耗变化趋势");
            confidence = 0.75;
        } else {
            rootCause = String.format("%s能耗处于正常水平", building);
            contributingFactors.add("设备运行正常");
            recommendedActions.add("继续保持当前管理措施");
            confidence = 0.90;
        }

        // 从知识库获取补充建议
        List<KnowledgeItem> relatedRules = knowledgeBase.search("建筑 能耗 节能 " + city);
        for (KnowledgeItem item : relatedRules) {
            if (item.getContent().contains("审计") && consumption > 100) {
                recommendedActions.add("依据《公共建筑节能管理办法》开展能源审计");
            }
            if (item.getContent().contains("改造") && consumption > 120) {
                recommendedActions.add("依据《公共建筑节能管理办法》实施节能改造");
            }
        }

        String summary = String.format("%s单位面积能耗为%.1f kWh/㎡·年，%s。",
                building, consumption,
                consumption > 100 ? "超过限额标准，需采取节能措施" : "处于合理水平");

        return new Explanation(alertType, city, rootCause, contributingFactors,
                recommendedActions, confidence, summary);
    }

    /**
     * 解释水位告警
     */
    public Explanation explainWaterAlert(String city, String station, double waterLevel) {
        log.info("解释水位告警 - city: {}, station: {}, waterLevel: {}", city, station, waterLevel);

        String alertType = "水位异常";
        String rootCause;
        List<String> contributingFactors = new ArrayList<>();
        List<String> recommendedActions = new ArrayList<>();
        double confidence;

        if (waterLevel > 3.0) {
            rootCause = String.format("%s水位达到%.2f米，超过保证水位，存在严重洪涝风险",
                    station, waterLevel);
            contributingFactors.add("上游来水量大，河道水位持续上涨");
            contributingFactors.add("强降雨导致地表径流急剧增加");
            contributingFactors.add("排水管网能力不足，雨水无法及时排出");
            contributingFactors.add("河道行洪能力受限");
            recommendedActions.add("立即启动一级防汛响应");
            recommendedActions.add("开启全部排水泵站，全力排水");
            recommendedActions.add("组织低洼地区群众紧急转移");
            recommendedActions.add("加固堤防，预置抢险物资和人员");
            recommendedActions.add("封闭积水严重路段，确保交通安全");
            confidence = 0.93;
        } else if (waterLevel > 2.0) {
            rootCause = String.format("%s水位达到%.2f米，超过警戒水位，存在洪涝风险",
                    station, waterLevel);
            contributingFactors.add("持续降雨导致水位上涨");
            contributingFactors.add("排水管网部分堵塞");
            contributingFactors.add("河道水位顶托，排水不畅");
            recommendedActions.add("启动二级防汛响应");
            recommendedActions.add("开启主要排水泵站");
            recommendedActions.add("加强堤防巡查，排查隐患");
            recommendedActions.add("预置抢险物资，做好人员转移准备");
            confidence = 0.87;
        } else if (waterLevel > 1.0) {
            rootCause = String.format("%s水位达到%.2f米，接近警戒水位，需密切关注",
                    station, waterLevel);
            contributingFactors.add("降雨导致水位上升");
            contributingFactors.add("排水管网运行正常但接近满负荷");
            recommendedActions.add("启动三级防汛响应");
            recommendedActions.add("加强水位监测，关注变化趋势");
            recommendedActions.add("清理排水管网进水口杂物");
            recommendedActions.add("通知低洼地区做好防范准备");
            confidence = 0.80;
        } else {
            rootCause = String.format("%s水位正常，当前水位%.2f米在安全范围内",
                    station, waterLevel);
            contributingFactors.add("排水系统运行正常");
            recommendedActions.add("维持常规监测");
            confidence = 0.92;
        }

        // 从知识库获取补充建议
        List<KnowledgeItem> relatedRules = knowledgeBase.search("水位 防汛 排水 " + city);
        for (KnowledgeItem item : relatedRules) {
            if (item.getContent().contains("三级") && waterLevel > 1.0 && waterLevel <= 2.0) {
                recommendedActions.add("依据《城市供水应急预案管理办法》启动三级防汛响应");
            }
            if (item.getContent().contains("二级") && waterLevel > 2.0 && waterLevel <= 3.0) {
                recommendedActions.add("依据《城市供水应急预案管理办法》启动二级防汛响应");
            }
        }

        String summary = String.format("%s当前水位%.2f米，%s。",
                station, waterLevel,
                waterLevel > 2.0 ? "超过警戒水位，需立即启动防汛应急响应" :
                        waterLevel > 1.0 ? "接近警戒水位，需加强监测" : "水位正常");

        return new Explanation(alertType, city, rootCause, contributingFactors,
                recommendedActions, confidence, summary);
    }
}
