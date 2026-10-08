package com.cityplatform.energy.repository;

import com.cityplatform.energy.model.GridEnergyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 电网指标记录数据访问层
 */
@Repository
public interface GridEnergyRecordRepository extends JpaRepository<GridEnergyRecord, Long> {

    /** 某分区某指标的最新一条记录 */
    Optional<GridEnergyRecord> findTopByZoneAndMetricOrderByTsDesc(String zone, String metric);

    /** 某分区某指标在时间窗口内的记录（按时间升序） */
    List<GridEnergyRecord> findByZoneAndMetricAndTsBetweenOrderByTsAsc(
            String zone, String metric, OffsetDateTime start, OffsetDateTime end);

    /** 某分区在时间窗口内的全部记录（按时间升序） */
    List<GridEnergyRecord> findByZoneAndTsBetweenOrderByTsAsc(
            String zone, OffsetDateTime start, OffsetDateTime end);
}
