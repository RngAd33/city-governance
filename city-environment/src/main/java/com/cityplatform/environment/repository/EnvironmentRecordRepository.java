package com.cityplatform.environment.repository;

import com.cityplatform.environment.model.EnvironmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 环境记录数据访问接口
 */
@Repository
public interface EnvironmentRecordRepository extends JpaRepository<EnvironmentRecord, Long> {

    /**
     * 按城市查询
     */
    List<EnvironmentRecord> findByCity(String city);

    /**
     * 按指标查询
     */
    List<EnvironmentRecord> findByMetric(String metric);

    /**
     * 按城市和指标查询
     */
    List<EnvironmentRecord> findByCityAndMetric(String city, String metric);

    /**
     * 按城市和时间范围查询
     */
    List<EnvironmentRecord> findByCityAndTsBetween(String city, OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * 按指标和时间范围查询
     */
    List<EnvironmentRecord> findByMetricAndTsBetween(String metric, OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * 按城市、指标和时间范围查询
     */
    List<EnvironmentRecord> findByCityAndMetricAndTsBetween(String city, String metric,
                                                             OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * 按时间范围查询
     */
    List<EnvironmentRecord> findByTsBetween(OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * 按城市和指标查询，按时间倒序
     */
    List<EnvironmentRecord> findByCityAndMetricOrderByTsDesc(String city, String metric);

    /**
     * 按记录ID查询
     */
    EnvironmentRecord findByRecordId(String recordId);
}
