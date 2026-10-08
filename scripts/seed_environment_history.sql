-- ============================================================
-- Seed ~30 days of hourly air-quality history for 南京
-- Purpose: environment page 24h/7d/30d range selector needs
--          historical rows; collector only produces real-time data.
-- Target : city_platform.environment_record (run with mysql client, utf8mb4)
-- Notes  : ASCII-only file; Chinese/unit strings are hex UTF-8 literals
--          X'E58D97E4BAAC' = 南京, X'CEBC672F6DC2B3' = μg/m³, X'6D672F6DC2B3' = mg/m³
--          Idempotent: inserts only when no older-than-2-day history exists.
-- Usage  : mysql -h127.0.0.1 -P3306 -uroot -p******** --default-character-set=utf8mb4 city_platform < seed_environment_history.sql
-- ============================================================
INSERT INTO environment_record (record_id, city, station_name, lat, lng, metric, value, unit, level, ts)
WITH RECURSIVE h(n) AS (
  SELECT 0
  UNION ALL
  SELECT n + 1 FROM h WHERE n < 719
),
p(metric, base, amp, phase, noise, vmin, dp, unit) AS (
  SELECT 'pm25', 55, 40, 0.0, 30, 8, 1, X'CEBC672F6DC2B3'
  UNION ALL SELECT 'pm10', 85, 55, 0.8, 40, 15, 1, X'CEBC672F6DC2B3'
  UNION ALL SELECT 'no2', 45, 25, 1.6, 15, 10, 1, X'CEBC672F6DC2B3'
  UNION ALL SELECT 'o3', 80, 55, -1.2, 25, 15, 1, X'CEBC672F6DC2B3'
  UNION ALL SELECT 'so2', 18, 8, 2.2, 6, 5, 1, X'CEBC672F6DC2B3'
  UNION ALL SELECT 'co', 1.0, 0.6, 1.0, 0.4, 0.3, 1, X'6D672F6DC2B3'
)
SELECT UUID(), X'E58D97E4BAAC', 'open-meteo', 32.06, 118.79,
       x.metric,
       x.val,
       x.unit,
       CASE WHEN x.metric IN ('pm25', 'pm10')
            THEN CASE WHEN x.val <= 75 THEN 'NORMAL' WHEN x.val <= 150 THEN 'WARNING' ELSE 'DANGER' END
            ELSE 'NORMAL' END,
       x.tsv
FROM (
  SELECT t.metric, t.unit, t.tsv,
         ROUND(GREATEST(t.vmin,
           t.base
           + t.amp * SIN(2 * PI() * HOUR(t.tsv) / 24 + t.phase)
           + t.amp * 0.35 * SIN(2 * PI() * TO_DAYS(t.tsv) / 6.5)
           + (RAND() - 0.5) * t.noise), t.dp) AS val
  FROM (
    SELECT p.metric, p.unit, p.base, p.amp, p.phase, p.noise, p.vmin, p.dp,
           DATE_SUB(CAST(DATE_FORMAT(UTC_TIMESTAMP(), '%Y-%m-%d %H:00:00') AS DATETIME), INTERVAL h.n HOUR) AS tsv
    FROM h CROSS JOIN p
  ) t
) x
WHERE (SELECT COUNT(*) FROM environment_record er
       WHERE er.station_name = 'open-meteo'
         AND er.metric = 'pm25'
         AND er.ts < UTC_TIMESTAMP() - INTERVAL 2 DAY) = 0;
