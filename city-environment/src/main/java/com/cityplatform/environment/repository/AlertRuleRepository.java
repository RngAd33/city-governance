package com.cityplatform.environment.repository;

import com.cityplatform.environment.model.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 告警规则数据访问接口
 */
@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    /**
     * 按规则ID查询
     */
    AlertRule findByRuleId(String ruleId);

    /**
     * 按指标查询
     */
    List<AlertRule> findByMetric(String metric);

    /**
     * 按启用状态查询
     */
    List<AlertRule> findByEnabledTrue();

    /**
     * 按指标和启用状态查询
     */
    List<AlertRule> findByMetricAndEnabledTrue(String metric);
}
