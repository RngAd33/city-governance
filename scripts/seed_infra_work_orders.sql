-- ============================================================
-- 基础设施工单种子数据（江苏南京地区）
-- 配合 city-infra 服务 GET /api/infra/work-orders 接口使用
-- 表：work_orders（含 progress 处理进度列）
-- 说明：脚本幂等（order_id 唯一键 upsert）；progress 列若已存在，
--       首行 ALTER 会报 Duplicate column 错误，可忽略继续执行。
-- ============================================================

ALTER TABLE work_orders ADD COLUMN progress TINYINT NOT NULL DEFAULT 0 COMMENT '处理进度百分比 0-100';

INSERT INTO work_orders
  (order_id, facility_id, title, description, priority, status, assigned_dept, assigned_person, progress, due_date, completed_at, created_at)
VALUES
  ('WO-NJ-2026-001', 'bridge', '南京长江大桥主缆除湿系统检修',
   '监测发现主缆除湿系统3号机组风量不足，需停机检查送风管路并更换干燥剂组件。',
   'URGENT', 'IN_PROGRESS', '南京市交通运输局', '张建国', 65, '2026-10-08', NULL, '2026-09-30 09:15:00'),
  ('WO-NJ-2026-002', 'tunnel', '定淮门长江隧道射流风机季度维护',
   '按季度维保计划对隧道左右线射流风机进行振动检测、绝缘测试与轴承润滑。',
   'HIGH', 'PENDING', '南京市城市管理局', '李海峰', 0, '2026-10-12', NULL, '2026-10-02 14:20:00'),
  ('WO-NJ-2026-003', 'road', '玄武大道沥青路面裂缝修复',
   '玄武大道东段快车道出现纵向裂缝12处，采用乳化沥青灌缝工艺集中修复。',
   'HIGH', 'IN_PROGRESS', '南京市市政工程管理处', '王卫东', 40, '2026-10-10', NULL, '2026-10-01 08:40:00'),
  ('WO-NJ-2026-004', 'pipeline', '江北新区综合管廊消防系统年检',
   '对管廊内部火灾报警、气体灭火及消防泵组系统开展年度检测并出具检测报告。',
   'MEDIUM', 'COMPLETED', '江北新区管委会', '赵春华', 100, '2026-10-01', '2026-09-29 16:30:00', '2026-09-22 10:05:00'),
  ('WO-NJ-2026-005', 'power_grid', '新街口片区电网箱变红外测温巡检',
   '迎峰度夏后对片区12台箱式变压器开展红外测温专项巡检，排查接点过热隐患。',
   'MEDIUM', 'IN_PROGRESS', '国网南京供电公司', '陈立群', 80, '2026-10-07', NULL, '2026-10-03 09:50:00'),
  ('WO-NJ-2026-006', 'bridge', '长江大桥桥面伸缩缝止水带更换',
   '北引桥2号、5号伸缩缝止水带老化脱落，雨季前需完成更换并做淋水试验。',
   'HIGH', 'PENDING', '南京市交通运输局', '刘志强', 0, '2026-10-15', NULL, '2026-10-04 11:10:00'),
  ('WO-NJ-2026-007', 'road', '玄武大道智慧路灯通信模块升级',
   '更换沿线86杆智慧路灯的NB-IoT通信模块，升级后统一接入市级路灯管理平台。',
   'LOW', 'COMPLETED', '南京市城管局路灯管理处', '周敏', 100, '2026-09-28', '2026-09-27 17:40:00', '2026-09-20 15:25:00'),
  ('WO-NJ-2026-008', 'tunnel', '定淮门隧道排水泵站清淤',
   '泵站集水井淤积影响排水能力，计划清淤并检修备用泵组。（因交通导改审批未通过取消）',
   'LOW', 'CANCELLED', '南京市城市管理局', '吴国庆', 0, '2026-10-20', NULL, '2026-09-28 13:35:00'),
  ('WO-NJ-2026-009', 'pipeline', '江北新区管廊结构监测点校准',
   '对管廊沉降、应变监测点传感器开展年度校准，同比对基准数据进行复核。',
   'HIGH', 'IN_PROGRESS', '江北新区管委会', '孙明远', 30, '2026-10-03', NULL, '2026-09-29 16:05:00')
ON DUPLICATE KEY UPDATE
  facility_id = VALUES(facility_id),
  title = VALUES(title),
  description = VALUES(description),
  priority = VALUES(priority),
  status = VALUES(status),
  assigned_dept = VALUES(assigned_dept),
  assigned_person = VALUES(assigned_person),
  progress = VALUES(progress),
  due_date = VALUES(due_date),
  completed_at = VALUES(completed_at),
  created_at = VALUES(created_at);

SELECT COUNT(*) AS work_order_count FROM work_orders;
SELECT order_id, facility_id, status, priority, progress, due_date FROM work_orders ORDER BY created_at DESC;
