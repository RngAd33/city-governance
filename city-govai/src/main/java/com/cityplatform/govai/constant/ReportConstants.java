package com.cityplatform.govai.constant;

import java.time.format.DateTimeFormatter;
import java.util.Map;

public interface ReportConstants {

    /** 指标名 → 中文展示名（未收录的指标保留原名） */
    Map<String, String> METRIC_NAMES = Map.ofEntries(
            Map.entry("temperature", "温度"), Map.entry("humidity", "湿度"),
            Map.entry("wind_speed", "风速"), Map.entry("precipitation", "降雨量"),
            Map.entry("pm25", "PM2.5"), Map.entry("pm10", "PM10"),
            Map.entry("no2", "NO2"), Map.entry("o3", "O3"),
            Map.entry("so2", "SO2"), Map.entry("co", "CO"),
            Map.entry("flow", "车流量"), Map.entry("speed", "平均车速"),
            Map.entry("queue_length", "排队长度"),
            Map.entry("water_level", "水位"), Map.entry("flow_rate", "流量"),
            Map.entry("electricity_consumption_office", "办公楼用电"),
            Map.entry("electricity_consumption_hospital", "医院用电"),
            Map.entry("electricity_consumption_commercial", "商业用电"),
            Map.entry("electricity_consumption_residential", "居民用电"),
            Map.entry("electricity_consumption_school", "学校用电"));

    /** 数据域 → 中文展示名 */
    Map<String, String> DOMAIN_NAMES = Map.of(
            "ENVIRONMENT", "环境", "TRAFFIC", "交通", "ENERGY", "能源",
            "WATER", "水务", "INFRA", "基础设施");

    /** 元信息日期格式 */
    DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** classpath 中文字体资源（容器部署可打包或挂载放入） */
    String[] CLASSPATH_FONT_CANDIDATES = {"/fonts/report-cn.ttf"};

    /** 文件系统中文字体候选（Windows 开发机 → 容器挂载 → 类 Unix 系统字体） */
    String[] FILE_FONT_CANDIDATES = {
            "C:/Windows/Fonts/simhei.ttf",
            "C:/Windows/Fonts/deng.ttf",
            "C:/Windows/Fonts/simkai.ttf",
            "C:/Windows/Fonts/simfang.ttf",
            "/app/fonts/report-cn.ttf",
            "/usr/share/fonts/truetype/noto/NotoSansSC-Regular.ttf",
            "/usr/share/fonts/truetype/wqy/wqy-microhei.ttf"
    };

    /** 报告作者标识 */
    String AUTHOR = "城市治理AI助手";

    /** 生成成功状态 */
    String STATUS_COMPLETED = "COMPLETED";

    /** 生成失败状态（大模型不可用） */
    String STATUS_FAILED = "FAILED";

    /** 会话标题最大长度 */
    int SESSION_TITLE_MAX_LENGTH = 50;

    /** 标题最大长度（与实体列宽一致） */
    int TITLE_MAX_LENGTH = 256;

    /** 多轮对话注入大模型的最大历史消息条数 */
    int HISTORY_LIMIT = 21;

    /** 对话消息中记录的知识命中标题最大长度 */
    int HITS_SUMMARY_MAX = 1000;

}