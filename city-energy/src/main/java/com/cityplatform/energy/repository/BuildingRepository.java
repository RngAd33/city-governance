package com.cityplatform.energy.repository;

import com.cityplatform.energy.model.Building;
import com.cityplatform.energy.model.BuildingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 建筑数据访问层
 */
@Repository
public interface BuildingRepository extends JpaRepository<Building, Long> {

    Optional<Building> findByBuildingId(String buildingId);

    List<Building> findByType(BuildingType type);

    List<Building> findByAvgDailyConsumptionGreaterThan(Double threshold);
}
