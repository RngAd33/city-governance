package com.cityplatform.infra.repository;

import com.cityplatform.infra.model.FacilityStatusRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 基础设施状态记录数据访问层
 */
@Repository
public interface FacilityStatusRecordRepository extends JpaRepository<FacilityStatusRecord, Long> {

    /**
     * 某设施某指标类型的最新一条记录
     */
    Optional<FacilityStatusRecord> findTop1ByFacilityAndMetricTypeOrderByTimestampDesc(
            String facility, String metricType);

    /**
     * 某设施某指标类型在指定时间之后的记录（按时间升序）
     */
    List<FacilityStatusRecord> findByFacilityAndMetricTypeAndTimestampAfterOrderByTimestampAsc(
            String facility, String metricType, OffsetDateTime after);
}
