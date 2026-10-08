-- ============================================================
-- Seed water monitoring stations and recent level records
-- Purpose: city_platform.water_station is empty, so city-water
--          /api/water/* returns no data for the frontend page.
--          Backfills 6 Nanjing stations and 12 hourly records
--          per station so the water page can render statistics.
-- Target : city_platform.water_station / water_level_record
-- Notes  : ASCII-only file; Chinese station names use UTF-8 hex
--          literals. Idempotent: INSERT IGNORE by unique
--          station_id; records only inserted when a station has
--          none yet.
-- Usage  : mysql -h127.0.0.1 -P3306 -uroot -p******** --default-character-set=utf8mb4 city_platform < seed_water_stations.sql
-- ============================================================

INSERT IGNORE INTO water_station (station_id, name, type, lat, lng, warning_level, danger_level, current_level, max_capacity) VALUES
    ('WS001', X'E995BFE6B19FE5A4A7E6A1A5E6B0B4E69687E7AB99', 'RIVER', 32.0600, 118.7900, 3.5, 5.0, 2.8, 8.0),
    ('WS002', X'E78E84E6ADA6E6B996E79B91E6B58BE7AB99', 'LAKE', 32.0700, 118.7800, 2.5, 3.5, 1.9, 5.0),
    ('WS003', X'E7A7A6E6B7AEE6B2B3E585A5E6B19FE58FA3', 'RIVER', 32.0500, 118.8000, 3.0, 4.5, 2.2, 7.0),
    ('WS004', X'E88EABE68481E6B996E79B91E6B58BE7AB99', 'LAKE', 32.0400, 118.8100, 2.0, 3.0, 1.5, 4.0),
    ('WS005', X'E99BA8E88AB1E58FB0E68E92E6B0B4E7AB99', 'DRAINAGE', 32.0300, 118.8200, 1.5, 2.5, 0.8, 3.5),
    ('WS006', X'E7B4ABE98791E5B1B1E6B0B4E5BA93', 'RESERVOIR', 32.0800, 118.7700, 4.0, 6.0, 3.2, 10.0);

-- Hourly level records for the last 12 hours, derived from current_level
INSERT INTO water_level_record (station_id, record_time, water_level, flow_rate, rainfall, risk_level)
SELECT s.station_id,
       NOW() - INTERVAL h.n HOUR,
       ROUND(s.current_level + SIN(h.n / 2.5) * 0.12 - h.n * 0.005, 2),
       ROUND(20 + (s.current_level / s.max_capacity) * 60 + h.n * 0.8, 1),
       CASE h.n % 4 WHEN 0 THEN 2.4 WHEN 1 THEN 1.1 WHEN 2 THEN 0.0 ELSE 3.2 END,
       CASE WHEN s.current_level / s.max_capacity > 0.95 THEN 'CRITICAL'
            WHEN s.current_level / s.max_capacity > 0.8 THEN 'HIGH'
            WHEN s.current_level / s.max_capacity > 0.6 THEN 'MEDIUM'
            ELSE 'LOW' END
FROM water_station s
CROSS JOIN (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
            UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
            UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11) h
WHERE NOT EXISTS (SELECT 1 FROM water_level_record r WHERE r.station_id = s.station_id);
