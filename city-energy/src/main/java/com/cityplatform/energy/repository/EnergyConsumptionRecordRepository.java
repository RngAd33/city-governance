package com.cityplatform.energy.repository;

import com.cityplatform.energy.model.EnergyConsumptionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 能耗记录数据访问层
 */
@Repository
public interface EnergyConsumptionRecordRepository extends JpaRepository<EnergyConsumptionRecord, Long> {

    List<EnergyConsumptionRecord> findByBuildingId(String buildingId);

    List<EnergyConsumptionRecord> findByBuildingIdAndTimestampBetween(
            String buildingId, LocalDateTime start, LocalDateTime end);

    List<EnergyConsumptionRecord> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
