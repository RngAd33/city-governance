-- ============================================================
-- 城市环境能源与交通协同优化平台 - 数据库初始化脚本
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS city_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE city_platform;

-- ============================================================
-- 1. 公共事件表 (city-collector)
-- ============================================================

DROP TABLE IF EXISTS event_record;

CREATE TABLE event_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL UNIQUE COMMENT '事件唯一ID',
    domain VARCHAR(32) NOT NULL COMMENT '领域: traffic/environment/energy/water/disaster/infra',
    city VARCHAR(64) NOT NULL DEFAULT '南京' COMMENT '城市',
    lat DECIMAL(10, 6) COMMENT '纬度',
    lng DECIMAL(10, 6) COMMENT '经度',
    metric VARCHAR(64) NOT NULL COMMENT '指标名称',
    value DOUBLE NOT NULL COMMENT '指标值',
    unit VARCHAR(32) COMMENT '单位',
    source VARCHAR(32) NOT NULL COMMENT '数据来源: openaq/open-meteo/dataset/api/simulator',
    raw_data JSON COMMENT '原始数据',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_domain (domain),
    INDEX idx_city (city),
    INDEX idx_metric (metric),
    INDEX idx_created_at (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='城市统一事件记录表';

-- ============================================================
-- 2. 交通优化模块 (city-traffic)
-- ============================================================

-- 2.1 路口信息表

DROP TABLE IF EXISTS intersections;

CREATE TABLE intersections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    intersection_id VARCHAR(64) NOT NULL UNIQUE COMMENT '路口唯一ID',
    name VARCHAR(128) NOT NULL COMMENT '路口名称',
    lat DECIMAL(10, 6) NOT NULL COMMENT '纬度',
    lng DECIMAL(10, 6) NOT NULL COMMENT '经度',
    area VARCHAR(64) COMMENT '所属区域',
    road_name VARCHAR(128) COMMENT '道路名称',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_area (area),
    INDEX idx_road (road_name)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='路口信息表';

-- 2.2 信号相位表

DROP TABLE IF EXISTS signal_phases;

CREATE TABLE signal_phases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    intersection_id VARCHAR(64) NOT NULL COMMENT '路口ID',
    phase_id VARCHAR(64) NOT NULL COMMENT '相位ID',
    direction VARCHAR(16) NOT NULL COMMENT '方向: NS/EW',
    green_time INT NOT NULL DEFAULT 30 COMMENT '绿灯时间(秒)',
    yellow_time INT NOT NULL DEFAULT 3 COMMENT '黄灯时间(秒)',
    red_time INT NOT NULL DEFAULT 30 COMMENT '红灯时间(秒)',
    min_green INT NOT NULL DEFAULT 10 COMMENT '最小绿灯时间(秒)',
    max_cycle INT NOT NULL DEFAULT 180 COMMENT '最大周期(秒)',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '相位顺序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_intersection_phase (intersection_id, phase_id),
    INDEX idx_intersection (intersection_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信号相位配置表';

-- 2.3 交通流数据表

DROP TABLE IF EXISTS traffic_flow_data;

CREATE TABLE traffic_flow_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    segment_id VARCHAR(64) NOT NULL COMMENT '路段ID',
    intersection_id VARCHAR(64) COMMENT '关联路口ID',
    direction VARCHAR(16) NOT NULL COMMENT '方向',
    flow DOUBLE NOT NULL COMMENT '流量(辆/小时)',
    avg_speed DOUBLE NOT NULL COMMENT '平均车速(km/h)',
    queue_length DOUBLE DEFAULT 0 COMMENT '排队长度(m)',
    congestion_level VARCHAR(16) DEFAULT 'SMOOTH' COMMENT '拥堵等级: SMOOTH/MODERATE/CONGESTED/SEVERE',
    weather VARCHAR(32) COMMENT '天气状况',
    rainfall DOUBLE DEFAULT 0 COMMENT '降雨量(mm)',
    aqi INT COMMENT '空气质量指数',
    record_time DATETIME NOT NULL COMMENT '记录时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_segment (segment_id),
    INDEX idx_intersection (intersection_id),
    INDEX idx_record_time (record_time),
    INDEX idx_congestion (congestion_level)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交通流数据表';

-- 2.4 优化记录表

DROP TABLE IF EXISTS optimization_record;

CREATE TABLE optimization_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    intersection_id VARCHAR(64) NOT NULL COMMENT '路口ID',
    strategy VARCHAR(32) NOT NULL COMMENT '优化策略: genetic/reinforcement/rule',
    before_config JSON NOT NULL COMMENT '优化前配置',
    after_config JSON NOT NULL COMMENT '优化后配置',
    before_wait_time DOUBLE COMMENT '优化前平均等待时间(s)',
    after_wait_time DOUBLE COMMENT '优化后平均等待时间(s)',
    before_queue_length DOUBLE COMMENT '优化前平均排队长度(m)',
    after_queue_length DOUBLE COMMENT '优化后平均排队长度(m)',
    before_throughput DOUBLE COMMENT '优化前通行量(辆/h)',
    after_throughput DOUBLE COMMENT '优化后通行量(辆/h)',
    improvement_percent DOUBLE COMMENT '改善百分比',
    ai_conclusion TEXT COMMENT 'AI优化结论',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_intersection (intersection_id),
    INDEX idx_strategy (strategy),
    INDEX idx_created_at (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信号优化记录表';

-- 2.5 仿真运行记录表

DROP TABLE IF EXISTS simulation_runs;

CREATE TABLE simulation_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL UNIQUE COMMENT '运行ID',
    intersection_id VARCHAR(64) NOT NULL COMMENT '路口ID',
    scenario_name VARCHAR(128) COMMENT '场景名称',
    duration_minutes INT NOT NULL DEFAULT 60 COMMENT '仿真时长(分钟)',
    total_wait_time DOUBLE COMMENT '总等待时间(s)',
    avg_queue_length DOUBLE COMMENT '平均排队长度(m)',
    throughput DOUBLE COMMENT '通行量(辆/h)',
    avg_speed DOUBLE COMMENT '平均车速(km/h)',
    stop_count DOUBLE COMMENT '启停次数',
    energy_consumption DOUBLE COMMENT '估算能耗(kWh)',
    carbon_emission DOUBLE COMMENT '估算碳排放(kg)',
    status VARCHAR(16) NOT NULL DEFAULT 'COMPLETED' COMMENT '状态: RUNNING/COMPLETED/FAILED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_intersection (intersection_id),
    INDEX idx_status (status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交通仿真运行记录表';

-- ============================================================
-- 3. 能源分析模块 (city-energy)
-- ============================================================

-- 3.1 建筑信息表

DROP TABLE IF EXISTS buildings;

CREATE TABLE buildings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_id VARCHAR(64) NOT NULL UNIQUE COMMENT '建筑唯一ID',
    name VARCHAR(128) NOT NULL COMMENT '建筑名称',
    type VARCHAR(32) NOT NULL COMMENT '类型: OFFICE/SCHOOL/HOSPITAL/RESIDENTIAL/COMMERCIAL/PUBLIC',
    area DOUBLE NOT NULL COMMENT '建筑面积(m²)',
    floors INT NOT NULL DEFAULT 1 COMMENT '楼层数',
    build_year INT COMMENT '建造年份',
    lat DECIMAL(10, 6) COMMENT '纬度',
    lng DECIMAL(10, 6) COMMENT '经度',
    address VARCHAR(256) COMMENT '地址',
    avg_daily_consumption DOUBLE COMMENT '日均能耗(kWh)',
    carbon_factor DOUBLE DEFAULT 0.58 COMMENT '碳排放因子(kgCO2/kWh)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (type),
    INDEX idx_building_id (building_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='建筑信息表';

-- 3.2 能耗记录表

DROP TABLE IF EXISTS energy_consumption_records;

CREATE TABLE energy_consumption_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_id VARCHAR(64) NOT NULL COMMENT '建筑ID',
    record_time DATETIME NOT NULL COMMENT '记录时间',
    electricity_kwh DOUBLE NOT NULL DEFAULT 0 COMMENT '电耗(kWh)',
    heating_kwh DOUBLE NOT NULL DEFAULT 0 COMMENT '热耗(kWh)',
    cooling_kwh DOUBLE NOT NULL DEFAULT 0 COMMENT '冷耗(kWh)',
    total_kwh DOUBLE NOT NULL COMMENT '总能耗(kWh)',
    carbon_emission DOUBLE COMMENT '碳排放(kg)',
    cost DOUBLE COMMENT '费用(元)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_building_id (building_id),
    INDEX idx_record_time (record_time)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='能耗记录表';

-- 3.3 节能建议表

DROP TABLE IF EXISTS energy_saving_recommendations;

CREATE TABLE energy_saving_recommendations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_id VARCHAR(64) NOT NULL COMMENT '建筑ID',
    title VARCHAR(256) NOT NULL COMMENT '建议标题',
    description TEXT COMMENT '建议描述',
    priority VARCHAR(16) DEFAULT 'MEDIUM' COMMENT '优先级: HIGH/MEDIUM/LOW',
    estimated_saving_kwh DOUBLE COMMENT '预计节能量(kWh/年)',
    estimated_saving_cost DOUBLE COMMENT '预计节省费用(元/年)',
    estimated_carbon_reduction DOUBLE COMMENT '预计碳减排(kg/年)',
    status VARCHAR(16) DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED/IMPLEMENTED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_building_id (building_id),
    INDEX idx_priority (priority),
    INDEX idx_status (status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节能建议表';

-- ============================================================
-- 4. 环境监测模块 (city-environment)
-- ============================================================

-- 4.1 环境记录表

DROP TABLE IF EXISTS environment_record;

CREATE TABLE environment_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_id VARCHAR(64) NOT NULL UNIQUE COMMENT '记录ID',
    city VARCHAR(64) NOT NULL DEFAULT '南京' COMMENT '城市',
    station_name VARCHAR(128) COMMENT '站点名称',
    lat DECIMAL(10, 6) COMMENT '纬度',
    lng DECIMAL(10, 6) COMMENT '经度',
    metric VARCHAR(32) NOT NULL COMMENT '指标: pm25/pm10/no2/o3/so2/co/temperature/humidity/wind_speed',
    value DOUBLE NOT NULL COMMENT '数值',
    unit VARCHAR(32) COMMENT '单位',
    `level` VARCHAR(16) COMMENT '等级: NORMAL/WARNING/DANGER',
    ts DATETIME NOT NULL COMMENT '时间戳',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_city (city),
    INDEX idx_metric (metric),
    INDEX idx_ts (ts),
    INDEX idx_level (`level`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='环境监测记录表';

-- 4.2 告警规则表

DROP TABLE IF EXISTS alert_rule;

CREATE TABLE alert_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id VARCHAR(64) NOT NULL UNIQUE COMMENT '规则ID',
    name VARCHAR(128) NOT NULL COMMENT '规则名称',
    metric VARCHAR(32) NOT NULL COMMENT '监控指标',
    operator VARCHAR(8) NOT NULL COMMENT '操作符: GT/LT/GTE/LTE',
    threshold DOUBLE NOT NULL COMMENT '阈值',
    severity VARCHAR(16) NOT NULL COMMENT '严重级别: INFO/WARNING/CRITICAL',
    message_template VARCHAR(512) COMMENT '消息模板',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_metric (metric),
    INDEX idx_enabled (enabled)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警规则表';

-- 4.3 告警记录表

DROP TABLE IF EXISTS alert_record;

CREATE TABLE alert_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_id VARCHAR(64) NOT NULL UNIQUE COMMENT '告警ID',
    rule_id VARCHAR(64) COMMENT '规则ID',
    metric VARCHAR(32) NOT NULL COMMENT '指标',
    value DOUBLE NOT NULL COMMENT '数值',
    threshold DOUBLE NOT NULL COMMENT '阈值',
    severity VARCHAR(16) NOT NULL COMMENT '严重级别',
    `message` TEXT COMMENT '告警消息',
    city VARCHAR(64) DEFAULT '南京' COMMENT '城市',
    location VARCHAR(256) COMMENT '位置',
    ts DATETIME NOT NULL COMMENT '时间戳',
    acknowledged TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否确认',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rule_id (rule_id),
    INDEX idx_severity (severity),
    INDEX idx_ts (ts),
    INDEX idx_acknowledged (acknowledged)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表';

-- ============================================================
-- 5. 水务风险模块 (city-water)
-- ============================================================

-- 5.1 水位监测站表

DROP TABLE IF EXISTS water_station;

CREATE TABLE water_station (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    station_id VARCHAR(64) NOT NULL UNIQUE COMMENT '站点ID',
    name VARCHAR(128) NOT NULL COMMENT '站点名称',
    type VARCHAR(32) NOT NULL COMMENT '类型: RIVER/LAKE/RESERVOIR/DRAINAGE',
    lat DECIMAL(10, 6) NOT NULL COMMENT '纬度',
    lng DECIMAL(10, 6) NOT NULL COMMENT '经度',
    warning_level DOUBLE COMMENT '警戒水位(m)',
    danger_level DOUBLE COMMENT '危险水位(m)',
    current_level DOUBLE DEFAULT 0 COMMENT '当前水位(m)',
    max_capacity DOUBLE COMMENT '最大容量(m)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (type),
    INDEX idx_station_id (station_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='水位监测站表';

-- 5.2 水位记录表

DROP TABLE IF EXISTS water_level_record;

CREATE TABLE water_level_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    station_id VARCHAR(64) NOT NULL COMMENT '站点ID',
    record_time DATETIME NOT NULL COMMENT '记录时间',
    water_level DOUBLE NOT NULL COMMENT '水位(m)',
    flow_rate DOUBLE COMMENT '流量(m³/s)',
    rainfall DOUBLE DEFAULT 0 COMMENT '降雨量(mm)',
    risk_level VARCHAR(16) DEFAULT 'LOW' COMMENT '风险等级: LOW/MEDIUM/HIGH/CRITICAL',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_station_id (station_id),
    INDEX idx_record_time (record_time),
    INDEX idx_risk_level (risk_level)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='水位记录表';

-- 5.3 洪涝场景表

DROP TABLE IF EXISTS flood_scenario;

CREATE TABLE flood_scenario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scenario_id VARCHAR(64) NOT NULL UNIQUE COMMENT '场景ID',
    station_id VARCHAR(64) NOT NULL COMMENT '站点ID',
    name VARCHAR(128) NOT NULL COMMENT '场景名称',
    rainfall_mm DOUBLE NOT NULL COMMENT '降雨量(mm)',
    duration_hours INT NOT NULL COMMENT '持续时长(小时)',
    affected_area DOUBLE COMMENT '影响面积(km²)',
    estimated_damage DOUBLE COMMENT '预估损失(万元)',
    affected_population INT DEFAULT 0 COMMENT '影响人口',
    road_closures INT DEFAULT 0 COMMENT '道路封闭数',
    risk_level VARCHAR(16) NOT NULL COMMENT '风险等级',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_station_id (station_id),
    INDEX idx_scenario_id (scenario_id),
    INDEX idx_risk_level (risk_level)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='洪涝场景表';

-- ============================================================
-- 6. 政务AI模块 (city-govai)
-- ============================================================

-- 6.1 决策记录表

DROP TABLE IF EXISTS decision_record;

CREATE TABLE decision_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    query_type VARCHAR(50) NOT NULL COMMENT '查询类型: natural_language_query/alert_explanation/report_generation/plan_retrieval/decision_suggestion',
    input TEXT NOT NULL COMMENT '输入内容',
    output TEXT COMMENT '输出内容',
    confidence DOUBLE COMMENT '置信度',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间',
    INDEX idx_query_type (query_type),
    INDEX idx_created_at (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI决策记录表';

-- 6.2 知识库表

DROP TABLE IF EXISTS knowledge_items;

CREATE TABLE knowledge_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id VARCHAR(64) NOT NULL UNIQUE COMMENT '条目ID',
    title VARCHAR(256) NOT NULL COMMENT '标题',
    category VARCHAR(64) NOT NULL COMMENT '分类: EMERGENCY/ENERGY/TRAFFIC/WATER/INFRA/ENVIRONMENT/REPORT/DISASTER',
    content TEXT NOT NULL COMMENT '内容',
    keywords VARCHAR(512) COMMENT '关键词(逗号分隔)',
    source VARCHAR(256) COMMENT '来源',
    priority INT DEFAULT 0 COMMENT '优先级',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_item_id (item_id),
    INDEX idx_enabled (enabled)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='政务知识库表';

-- 6.3 应急预案表

DROP TABLE IF EXISTS emergency_plans;

CREATE TABLE emergency_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id VARCHAR(64) NOT NULL UNIQUE COMMENT '预案ID',
    plan_name VARCHAR(256) NOT NULL COMMENT '预案名称',
    event_type VARCHAR(64) NOT NULL COMMENT '事件类型: FLOOD/TYPHOON/TRAFFIC/ENERGY/POLLUTION',
    severity VARCHAR(16) NOT NULL COMMENT '严重级别: LOW/MEDIUM/HIGH/CRITICAL',
    applicable_scope TEXT COMMENT '适用范围',
    response_level VARCHAR(16) COMMENT '响应级别',
    action_items JSON NOT NULL COMMENT '行动项',
    responsible_departments JSON NOT NULL COMMENT '责任部门',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_event_type (event_type),
    INDEX idx_severity (severity),
    INDEX idx_plan_id (plan_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应急预案表';

-- 6.4 对话会话表

DROP TABLE IF EXISTS chat_session;

CREATE TABLE chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE COMMENT '会话ID',
    title VARCHAR(128) NOT NULL COMMENT '会话标题',
    message_count INT NOT NULL DEFAULT 0 COMMENT '消息数',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_updated_at (updated_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话会话表';

-- 6.5 对话消息表

DROP TABLE IF EXISTS chat_message;

CREATE TABLE chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    role VARCHAR(16) NOT NULL COMMENT '角色: user/assistant/system',
    content TEXT NOT NULL COMMENT '消息内容',
    knowledge_hits VARCHAR(1024) COMMENT 'RAG命中的知识条目标题(分号分隔)',
    provider VARCHAR(64) COMMENT '模型提供方',
    seq INT NOT NULL COMMENT '会话内消息序号',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_session_seq (session_id, seq),
    INDEX idx_created_at (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话消息表';

-- 6.6 政务报告表

DROP TABLE IF EXISTS govai_report;

CREATE TABLE govai_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(256) NOT NULL COMMENT '报告标题',
    type VARCHAR(32) NOT NULL COMMENT '报告类型: 日报/周报/简报/风险提示',
    city VARCHAR(64) COMMENT '城市',
    period VARCHAR(64) COMMENT '报告周期',
    author VARCHAR(64) COMMENT '作者',
    status VARCHAR(16) DEFAULT 'COMPLETED' COMMENT '状态: COMPLETED/FAILED',
    content TEXT COMMENT '报告正文(Markdown)',
    created_at DATETIME(6) NOT NULL COMMENT '创建时间',
    INDEX idx_type (type),
    INDEX idx_created_at (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='政务报告表';

-- ============================================================
-- 7. 基础设施运维模块 (city-infra)
-- ============================================================

-- 7.1 设施信息表

DROP TABLE IF EXISTS facilities;

CREATE TABLE facilities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    facility_id VARCHAR(64) NOT NULL UNIQUE COMMENT '设施ID',
    name VARCHAR(128) NOT NULL COMMENT '设施名称',
    type VARCHAR(64) NOT NULL COMMENT '类型: ROAD/BRIDGE/TUNNEL/PIPE/STATION',
    lat DECIMAL(10, 6) NOT NULL COMMENT '纬度',
    lng DECIMAL(10, 6) NOT NULL COMMENT '经度',
    address VARCHAR(256) COMMENT '地址',
    status VARCHAR(16) DEFAULT 'NORMAL' COMMENT '状态: NORMAL/WARNING/DANGER',
    last_inspection DATE COMMENT '上次巡检日期',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_facility_id (facility_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基础设施信息表';

-- 7.2 工单表

DROP TABLE IF EXISTS work_orders;

CREATE TABLE work_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL UNIQUE COMMENT '工单ID',
    facility_id VARCHAR(64) NOT NULL COMMENT '设施ID',
    title VARCHAR(256) NOT NULL COMMENT '标题',
    description TEXT COMMENT '描述',
    priority VARCHAR(16) DEFAULT 'MEDIUM' COMMENT '优先级: HIGH/MEDIUM/LOW',
    status VARCHAR(16) DEFAULT 'PENDING' COMMENT '状态: PENDING/IN_PROGRESS/COMPLETED/CANCELLED',
    assigned_dept VARCHAR(128) COMMENT '分配部门',
    assigned_person VARCHAR(64) COMMENT '分配人员',
    progress TINYINT NOT NULL DEFAULT 0 COMMENT '处理进度百分比 0-100',
    due_date DATE COMMENT '截止日期',
    completed_at DATETIME COMMENT '完成时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_facility_id (facility_id),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_order_id (order_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运维工单表';

-- ============================================================
-- 8. 系统配置表
-- ============================================================

-- 8.1 告警阈值配置表

DROP TABLE IF EXISTS alert_thresholds;

CREATE TABLE alert_thresholds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric VARCHAR(64) NOT NULL COMMENT '指标名称',
    warning_value DOUBLE COMMENT '预警值',
    danger_value DOUBLE COMMENT '危险值',
    unit VARCHAR(32) COMMENT '单位',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_metric (metric),
    INDEX idx_enabled (enabled)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警阈值配置表';

-- 8.2 系统日志表

DROP TABLE IF EXISTS system_logs;

CREATE TABLE system_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    `level` VARCHAR(16) NOT NULL COMMENT '日志级别: INFO/WARNING/ERROR/CRITICAL',
    module VARCHAR(64) NOT NULL COMMENT '模块',
    `message` TEXT NOT NULL COMMENT '日志内容',
    details JSON COMMENT '详细信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_level (`level`),
    INDEX idx_module (module),
    INDEX idx_created_at (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统日志表';

-- ============================================================
-- 9. 初始化数据
-- ============================================================

-- 9.1 初始化告警阈值

INSERT INTO alert_thresholds (metric, warning_value, danger_value, unit) VALUES
    ('pm25', 75, 150, 'μg/m³'),
    ('pm10', 150, 250, 'μg/m³'),
    ('no2', 80, 180, 'μg/m³'),
    ('o3', 160, 250, 'μg/m³'),
    ('temperature', 35, 40, '°C'),
    ('humidity', 80, 95, '%'),
    ('wind_speed', 10, 17, 'm/s'),
    ('water_level', 3.0, 4.5, 'm'),
    ('flow', 1500, 2000, 'vehicles/h'),
    ('queue_length', 200, 500, 'm'),
    ('electricity', 5000, 8000, 'kWh'),
    ('congestion_index', 6, 8, 'level');

-- 9.2 初始化示例建筑（南京真实地标：规模参数取自公开资料，日均能耗 = 建筑面积 × 行业能耗强度，参考知识库 STD-003）

INSERT INTO buildings (building_id, name, type, area, floors, build_year, lat, lng, avg_daily_consumption, carbon_factor) VALUES
    ('B001', '南京市政务服务中心', 'OFFICE', 28000, 2, 2010, 32.0435, 118.7295, 6100, 0.58),
    ('B002', '江苏省人民医院', 'HOSPITAL', 480000, 20, 1936, 32.0536, 118.7700, 197300, 0.58),
    ('B003', '德基广场', 'COMMERCIAL', 313000, 48, 2012, 32.0422, 118.7843, 102900, 0.58),
    ('B004', '仁恒江湾城', 'RESIDENTIAL', 740000, 33, 2016, 32.0085, 118.7340, 70900, 0.58),
    ('B005', '紫峰大厦', 'OFFICE', 261075, 89, 2010, 32.0603, 118.7770, 71500, 0.58),
    ('B006', '南京外国语学校', 'SCHOOL', 136506, 7, 1963, 32.0578, 118.7968, 20600, 0.58),
    ('B007', '南京国际博览中心', 'COMMERCIAL', 360000, 1, 2008, 32.0348, 118.7245, 78900, 0.58),
    ('B008', '南京人民大会堂', 'OFFICE', 12000, 5, 1936, 32.0449, 118.7936, 3000, 0.58);

-- 9.3 初始化示例水位站

INSERT INTO water_station (station_id, name, type, lat, lng, warning_level, danger_level, current_level, max_capacity) VALUES
    ('WS001', '长江大桥水文站', 'RIVER', 32.0600, 118.7900, 3.5, 5.0, 2.8, 8.0),
    ('WS002', '玄武湖监测站', 'LAKE', 32.0700, 118.7800, 2.5, 3.5, 1.9, 5.0),
    ('WS003', '秦淮河入江口', 'RIVER', 32.0500, 118.8000, 3.0, 4.5, 2.2, 7.0),
    ('WS004', '莫愁湖监测站', 'LAKE', 32.0400, 118.8100, 2.0, 3.0, 1.5, 4.0),
    ('WS005', '雨花台排水站', 'DRAINAGE', 32.0300, 118.8200, 1.5, 2.5, 0.8, 3.5),
    ('WS006', '紫金山水库', 'RESERVOIR', 32.0800, 118.7700, 4.0, 6.0, 3.2, 10.0);

-- 9.4 初始化示例路口

INSERT INTO intersections (intersection_id, name, lat, lng) VALUES
    ('INT-001', '中山北路-中央路交叉口', 32.0600, 118.7900),
    ('INT-002', '北京东路-太平北路交叉口', 32.0700, 118.7800),
    ('INT-003', '汉中路-上海路交叉口', 32.0500, 118.8000),
    ('INT-004', '江东中路-奥体大街交叉口', 32.0400, 118.8100),
    ('INT-005', '软件大道-花神大道交叉口', 32.0300, 118.8200),
    ('INT-006', '玄武大道-板仓街交叉口', 32.0800, 118.7700);

-- 9.5 初始化知识库

INSERT INTO knowledge_items (item_id, title, category, content, keywords, source, priority, enabled) VALUES
    ('KB001', '城市内涝应急预案', 'EMERGENCY', '当城市发生内涝时，应立即启动应急响应机制：1. 通知相关部门；2. 开启排水泵站；3. 疏散低洼地区居民；4. 封闭积水路段；5. 调配救援物资。', '内涝,应急,排水,疏散', '市应急管理局', 10, 1),
    ('KB002', '交通信号控制规范', 'TRAFFIC', '交通信号灯配时应遵循以下原则：1. 主干道优先；2. 行人过街安全时间不少于15秒；3. 周期长度不超过180秒；4. 绿信比根据流量动态调整。', '信号灯,配时,绿信比,周期', '市交通管理局', 9, 1),
    ('KB003', '建筑节能标准', 'ENERGY', '公共建筑能耗限额：办公建筑≤80kWh/m²/年，商业建筑≤100kWh/m²/年，医院建筑≤150kWh/m²/年。超过限额需进行节能改造。', '建筑,能耗,节能,限额', '市住建局', 8, 1),
    ('KB004', '空气质量预警响应', 'ENVIRONMENT', 'AQI超过150时启动黄色预警：1. 建议减少户外活动；2. 加强道路洒水；3. 限制高排放车辆通行。AQI超过200时启动橙色预警。', 'AQI,预警,空气质量,响应', '市生态环境局', 9, 1),
    ('KB005', '排水调度规则', 'WATER', '排水调度应遵循：1. 先下游后上游；2. 先主干后支管；3. 泵站满负荷运行；4. 必要时启用调蓄设施。', '排水,调度,泵站,调蓄', '市水务局', 8, 1),
    ('KB006', '极端天气交通管制', 'TRAFFIC', '暴雨、大雪等极端天气下：1. 启动交通管制预案；2. 限制货车通行；3. 开放应急车道；4. 加强巡逻疏导。', '极端天气,交通管制,暴雨,大雪', '市交通管理局', 9, 1),
    ('KB007', '建筑能耗异常处理', 'ENERGY', '当建筑能耗异常升高时：1. 检查设备运行状态；2. 分析用能结构；3. 排查泄漏点；4. 调整运行策略。', '能耗,异常,设备,泄漏', '市住建局', 7, 1),
    ('KB008', '城市运行日报模板', 'REPORT', '城市运行日报应包含：1. 环境空气质量；2. 交通运行状况；3. 能源消耗情况；4. 水务运行状态；5. 基础设施运维情况；6. 重大事件记录。', '日报,城市运行,报告', '市城市管理局', 6, 1),
    ('KB009', '洪涝灾害风险评估', 'DISASTER', '洪涝风险评估要素：1. 降雨量及持续时间；2. 地形高程；3. 排水能力；4. 历史积水记录；5. 人口密度。', '洪涝,风险,评估,降雨', '市应急管理局', 8, 1),
    ('KB010', '多部门协同处置流程', 'EMERGENCY', '多部门协同处置：1. 统一指挥调度；2. 信息共享互通；3. 资源统筹调配；4. 行动协调一致；5. 效果评估反馈。', '协同,处置,指挥,调度', '市应急管理局', 9, 1);

-- 9.6 扩充政务知识库（法规/应急预案/调度规则/技术标准，与运行时RAG检索一致）

INSERT INTO knowledge_items (item_id, title, category, content, keywords, source, priority, enabled) VALUES
    ('REG-001', '《城市空气质量污染防治条例》', 'regulation', '当AQI超过150时，启动橙色预警，建议减少户外施工和机动车出行；当AQI超过200时，启动红色预警，实施机动车单双号限行，暂停重污染企业生产。', 'AQI,空气质量,污染,预警,限行,排放', '市生态环境局', 10, 1),
    ('REG-002', '《城市供水应急预案管理办法》', 'regulation', '当水位超过警戒水位1.5米时，启动三级防汛响应；当水位超过警戒水位2.5米时，启动二级防汛响应，组织低洼地区居民转移。', '水位,警戒水位,防汛,供水,应急,转移', '市水务局', 10, 1),
    ('REG-003', '《城市交通管理条例》', 'regulation', '高峰时段主干道平均车速低于20km/h时，实施交通分流措施；拥堵指数超过8.0时，启动交通管制，限制货车进入核心区。', '交通,拥堵,分流,管制,货车,高峰', '市交通管理局', 9, 1),
    ('REG-004', '《公共建筑节能管理办法》', 'regulation', '办公建筑单位面积能耗超过80kWh/㎡·年时，需进行能源审计；超过120kWh/㎡·年时，强制实施节能改造。', '建筑,节能,能耗,能源审计,限额', '市住建局', 8, 1),
    ('REG-005', '《城市突发事件应对条例》', 'regulation', '突发事件按照影响范围分为四级：一般（IV级）、较大（III级）、重大（II级）、特别重大（I级）。各级别对应不同的响应机制和资源调配方案。', '突发事件,分级,响应,应急管理', '市应急管理局', 9, 1),
    ('PLAN-001', '城市防洪应急预案', 'emergency_plan', '适用于城市范围内因暴雨、洪水引发的洪涝灾害。响应流程：监测预警→会商研判→启动响应→抢险救援→灾后恢复。关键措施：开启排水泵站、疏散低洼地区群众、加固堤防、预置抢险物资。', '防洪,暴雨,洪水,内涝,排水,抢险', '市防汛抗旱指挥部', 10, 1),
    ('PLAN-002', '交通事故应急处置预案', 'emergency_plan', '适用于城市主干道、高速公路发生的重大交通事故。响应流程：接警→现场勘查→交通疏导→事故处理→恢复通行。关键措施：启动无人机勘查、实施临时交通管制、开辟应急救援通道。', '交通事故,处置,疏导,管制,救援', '市交通管理局', 9, 1),
    ('PLAN-003', '大气污染应急响应预案', 'emergency_plan', '适用于AQI持续超过150的重污染天气。响应流程：监测预报→预警发布→应急响应→措施落实→效果评估。关键措施：工业企业限产、机动车限行、停止露天作业、加强道路保洁。', '大气污染,重污染,AQI,应急响应,限产', '市生态环境局', 9, 1),
    ('PLAN-004', '大面积停电应急预案', 'emergency_plan', '适用于因设备故障、自然灾害导致的大面积停电事故。响应流程：故障定位→负荷转移→抢修恢复→善后处理。关键措施：优先保障医院、交通信号、供水等关键设施供电。', '停电,供电,故障,抢修,应急预案', '市供电公司', 8, 1),
    ('RULE-001', '交通信号配时调度规则', 'dispatch_rule', '信号周期时长应在60-180秒之间；主干道绿波协调带宽应达到周期时长的40%-60%；行人过街最短绿灯时间不小于15秒；公交优先信号延迟不超过10秒。', '信号,配时,绿波,公交优先,周期', '市交通研究中心', 9, 1),
    ('RULE-002', '排水泵站调度规则', 'dispatch_rule', '当降雨量达到10mm/h时，预降管网水位；当降雨量达到20mm/h时，开启全部排水泵站；当管网水位达到警戒线时，启动应急排水方案；泵站运行优先级：下穿隧道>低洼路段>一般路段。', '排水,泵站,调度,降雨,管网', '市排水管理处', 8, 1),
    ('RULE-003', '重污染天气减排调度规则', 'dispatch_rule', '橙色预警期间：涉气企业减排30%，停止土石方作业；红色预警期间：涉气企业减排50%，机动车单双号限行，停止所有露天施工。', '重污染,减排,橙色预警,红色预警,限行', '市生态环境局', 8, 1),
    ('RULE-004', '城市排水防涝调度规则', 'dispatch_rule', '排水管网设计标准：一般地区1年一遇，重要地区3年一遇，地下通道5年一遇。泵站排水能力：应满足设计重现期降雨量的排放需求。应急响应：积水深度超过30cm时封闭交通，超过50cm时强制疏散。调度原则：高水高排、低水低排、分区排水、系统调度。', '排水防涝,积水,封闭,疏解,设计标准', '市排水管理处', 8, 1),
    ('RULE-005', '河道水位调度规则', 'dispatch_rule', '正常水位：低于警戒水位1.0米以上。警戒水位：启动巡查，预置抢险物资。保证水位：全力抢险，组织转移。调度原则：上下游统筹、蓄泄兼顾、确保重点。', '河道,水位,警戒,抢险,调度', '市水务局', 8, 1),
    ('STD-001', '城市道路信号配时设计标准', 'standard', '交叉口信号周期：主干道60-150秒，次干道40-100秒，支路30-60秒。相位差协调：相邻交叉口相位差应满足绿波带设计要求。黄灯时间：3-4秒；全红时间：1-3秒。左转保护相位：当左转流量超过200pcu/h时设置。', '信号周期,相位,黄灯,左转,设计标准', '市交通设计研究院', 8, 1),
    ('STD-002', '公交信号优先技术标准', 'standard', '公交优先策略：绿灯延长、红灯早断、相位插入。公交检测器布设距离：距停止线80-150米。优先响应时间：检测到公交车辆后5秒内调整信号。社会车辆影响：优先相位对社会车辆延误增加不超过15%。', '公交优先,绿灯延长,检测器,优先响应', '市交通研究中心', 7, 1),
    ('STD-003', '公共建筑能耗限额标准', 'standard', '办公建筑：60-100 kWh/㎡·年。商业建筑：80-150 kWh/㎡·年。医疗建筑：120-200 kWh/㎡·年。教育建筑：40-80 kWh/㎡·年。超过限额20%时需进行能源审计和节能改造。', '能耗限额,办公建筑,商业建筑,节能改造', '市住建局', 8, 1),
    ('STD-004', '建筑能耗监测标准', 'standard', '能耗监测数据上传频率：每15分钟一次。分项计量：照明、空调、动力、特殊用电应分别计量。数据质量：采集准确率不低于95%。异常告警：能耗突增超过30%时触发告警。', '能耗监测,分项计量,告警,数据质量', '市住建局', 7, 1);

