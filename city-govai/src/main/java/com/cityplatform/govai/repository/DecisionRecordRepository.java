package com.cityplatform.govai.repository;

import com.cityplatform.govai.model.entity.DecisionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 决策记录数据访问层
 */
@Repository
public interface DecisionRecordRepository extends JpaRepository<DecisionRecord, Long> {

    /**
     * 按查询类型查找记录
     */
    List<DecisionRecord> findByQueryTypeOrderByCreatedAtDesc(String queryType);

    /**
     * 按置信度阈值查找记录
     */
    List<DecisionRecord> findByConfidenceGreaterThanEqualOrderByCreatedAtDesc(Double confidence);

}