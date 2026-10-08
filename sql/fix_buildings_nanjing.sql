-- 建筑数据订正：B001-B008 对齐南京真实地标（名称/坐标/规模/能耗基线）
-- 用法（Windows PowerShell，路径用正斜杠，须用 SOURCE 方式避免中文损坏）：
--   mysql -uroot -p*** --default-character-set=utf8mb4 -D city_platform -e 'SOURCE sql/fix_buildings_nanjing.sql'
-- 说明：按 building_id 更新，幂等可重复执行。
--   规模参数（面积/楼层/年份）取自公开资料；日均能耗基线 = 真实建筑面积 × 行业能耗强度
--   （办公 80/100、医疗 150、商业 120、教育 55、住宅 35、会展 80、文化 90 kWh/㎡·年，参考知识库 STD-003），
--   与采集端 DataSimulatorService.NANJING_BUILDINGS 保持一致。
UPDATE buildings SET name = '南京市政务服务中心', lat = 32.0435, lng = 118.7295, area = 28000, floors = 2, build_year = 2010, avg_daily_consumption = 6100 WHERE building_id = 'B001';
UPDATE buildings SET name = '江苏省人民医院', lat = 32.0536, lng = 118.7700, area = 480000, floors = 20, build_year = 1936, avg_daily_consumption = 197300 WHERE building_id = 'B002';
UPDATE buildings SET name = '德基广场', lat = 32.0422, lng = 118.7843, area = 313000, floors = 48, build_year = 2012, avg_daily_consumption = 102900 WHERE building_id = 'B003';
UPDATE buildings SET name = '仁恒江湾城', lat = 32.0085, lng = 118.7340, area = 740000, floors = 33, build_year = 2016, avg_daily_consumption = 70900 WHERE building_id = 'B004';
UPDATE buildings SET name = '紫峰大厦', lat = 32.0603, lng = 118.7770, area = 261075, floors = 89, build_year = 2010, avg_daily_consumption = 71500 WHERE building_id = 'B005';
UPDATE buildings SET name = '南京外国语学校', lat = 32.0578, lng = 118.7968, area = 136506, floors = 7, build_year = 1963, avg_daily_consumption = 20600 WHERE building_id = 'B006';
UPDATE buildings SET name = '南京国际博览中心', lat = 32.0348, lng = 118.7245, area = 360000, floors = 1, build_year = 2008, avg_daily_consumption = 78900 WHERE building_id = 'B007';
UPDATE buildings SET name = '南京人民大会堂', lat = 32.0449, lng = 118.7936, area = 12000, floors = 5, build_year = 1936, avg_daily_consumption = 3000 WHERE building_id = 'B008';
