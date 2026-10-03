package com.cityplatform.traffic.repository;

import com.cityplatform.traffic.entity.OptimizationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 优化记录数据访问层
 */
@Repository
public interface OptimizationRecordRepository extends JpaRepository<OptimizationRecord, Long> {

    /**
     * 根据路口ID查询优化记录
     */
    List<OptimizationRecord> findByIntersectionId(String intersectionId);

    /**
     * 根据路口ID查询优化记录，按创建时间倒序
     */
    List<OptimizationRecord> findByIntersectionIdOrderByCreatedAtDesc(String intersectionId);
}
