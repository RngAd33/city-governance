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

DROP TABLE IF EXISTS event_records;

CREATE TABLE event_records (
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

DROP TABLE IF EXISTS optimization_records;

CREATE TABLE optimization_records (
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

DROP TABLE IF EXISTS environment_records;

CREATE TABLE environment_records (
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

DROP TABLE IF EXISTS alert_rules;

CREATE TABLE alert_rules (
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

DROP TABLE IF EXISTS alert_records;

CREATE TABLE alert_records (
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

DROP TABLE IF EXISTS water_stations;

CREATE TABLE water_stations (
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

DROP TABLE IF EXISTS water_level_records;

CREATE TABLE water_level_records (
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

DROP TABLE IF EXISTS flood_scenarios;

CREATE TABLE flood_scenarios (
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

DROP TABLE IF EXISTS decision_records;

CREATE TABLE decision_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_id VARCHAR(64) NOT NULL UNIQUE COMMENT '记录ID',
    query_type VARCHAR(32) NOT NULL COMMENT '查询类型: CHAT/ALERT_EXPLAIN/REPORT/PLAN',
    input_text TEXT NOT NULL COMMENT '输入内容',
    output_text TEXT NOT NULL COMMENT '输出内容',
    confidence DOUBLE DEFAULT 0.8 COMMENT '置信度',
    sources JSON COMMENT '依据来源',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_query_type (query_type),
    INDEX idx_record_id (record_id),
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

-- 9.2 初始化示例建筑

INSERT INTO buildings (building_id, name, type, area, floors, build_year, lat, lng, avg_daily_consumption, carbon_factor) VALUES
    ('B001', '市政服务中心', 'OFFICE', 50000, 12, 2010, 32.0600, 118.7900, 3500, 0.58),
    ('B002', '第一人民医院', 'HOSPITAL', 120000, 20, 2005, 32.0500, 118.8000, 8000, 0.58),
    ('B003', '中央商务区A座', 'COMMERCIAL', 80000, 30, 2015, 32.0700, 118.7800, 5500, 0.58),
    ('B004', '阳光花园小区', 'RESIDENTIAL', 60000, 18, 2008, 32.0400,118.8100, 2000, 0.58),
    ('B005', '科技园区办公楼', 'OFFICE', 45000, 10, 2018, 32.0800, 118.7700, 2800, 0.58),
    ('B006', '大学城图书馆', 'SCHOOL', 30000, 5, 2012, 32.0300, 118.8200, 1500, 0.58),
    ('B007', '国际会议中心', 'COMMERCIAL', 70000, 8, 2016, 32.0650, 118.7950, 4200, 0.58),
    ('B008', '老年活动中心', 'PUBLIC', 15000, 3, 2000, 32.0550, 118.8050, 800, 0.58);

-- 9.3 初始化示例水位站

INSERT INTO water_stations (station_id, name, type, lat, lng, warning_level, danger_level, current_level, max_capacity) VALUES
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

