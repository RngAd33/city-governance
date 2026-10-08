-- ============================================================
-- city-govai 对话持久化与RAG知识库种子脚本（幂等，可重复执行）
-- 1) 创建对话会话表 chat_session / 对话消息表 chat_message
-- 2) 扩充政务知识库 knowledge_items（法规/应急预案/调度规则/技术标准 18 条）
-- 执行方式（UTF-8 文件，避免命令行直传中文损坏）：
--   mysql -uroot -p -D city_platform --default-character-set=utf8mb4 -e "SOURCE sql/seed_govai_knowledge_chat.sql"
-- ============================================================

-- 1. 对话会话表

CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE COMMENT '会话ID',
    title VARCHAR(128) NOT NULL COMMENT '会话标题',
    message_count INT NOT NULL DEFAULT 0 COMMENT '消息数',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_updated_at (updated_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话会话表';

-- 2. 对话消息表

CREATE TABLE IF NOT EXISTS chat_message (
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

-- 3. 扩充政务知识库（与运行时RAG检索内容一致，按 item_id 幂等更新）

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
    ('STD-004', '建筑能耗监测标准', 'standard', '能耗监测数据上传频率：每15分钟一次。分项计量：照明、空调、动力、特殊用电应分别计量。数据质量：采集准确率不低于95%。异常告警：能耗突增超过30%时触发告警。', '能耗监测,分项计量,告警,数据质量', '市住建局', 7, 1)
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    category = VALUES(category),
    content = VALUES(content),
    keywords = VALUES(keywords),
    source = VALUES(source),
    priority = VALUES(priority),
    enabled = VALUES(enabled);
