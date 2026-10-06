package com.cityplatform.collector.repository;

import com.cityplatform.collector.entity.EventRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 事件记录数据访问层
 */
@Repository
public interface EventRecordRepository extends JpaRepository<EventRecord, Long> {

    /**
     * 按时间倒序查询最近的事件记录
     */
    List<EventRecord> findTop100ByOrderByTsDesc();

    /**
     * 按领域查询最近的事件记录
     */
    List<EventRecord> findTop50ByDomainOrderByTsDesc(String domain);

    /**
     * 按城市查询最近的事件记录
     */
    List<EventRecord> findTop50ByCityOrderByTsDesc(String city);

    /**
     * 按指标查询最近的一条事件记录（水位采集读取上次值保持连续性）
     */
    Optional<EventRecord> findTopByMetricOrderByTsDesc(String metric);
}
