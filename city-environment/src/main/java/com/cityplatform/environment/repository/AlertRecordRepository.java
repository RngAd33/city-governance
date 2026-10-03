package com.cityplatform.environment.repository;

import com.cityplatform.environment.model.AlertRecord;
import com.cityplatform.environment.model.enums.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 告警记录数据访问接口
 */
@Repository
public interface AlertRecordRepository extends JpaRepository<AlertRecord, Long> {

    /**
     * 按告警ID查询
     */
    AlertRecord findByAlertId(String alertId);

    /**
     * 按规则ID查询
     */
    List<AlertRecord> findByRuleId(String ruleId);

    /**
     * 按城市查询
     */
    List<AlertRecord> findByCity(String city);

    /**
     * 按严重级别查询
     */
    List<AlertRecord> findBySeverity(Severity severity);

    /**
     * 按确认状态查询
     */
    List<AlertRecord> findByAcknowledgedFalse();

    /**
     * 按城市和确认状态查询
     */
    List<AlertRecord> findByAcknowledgedFalseAndCity(String city);

    /**
     * 按严重级别和确认状态查询
     */
    List<AlertRecord> findByAcknowledgedFalseAndSeverity(Severity severity);

    /**
     * 按城市、严重级别和确认状态查询
     */
    List<AlertRecord> findByAcknowledgedFalseAndCityAndSeverity(String city, Severity severity);
}
