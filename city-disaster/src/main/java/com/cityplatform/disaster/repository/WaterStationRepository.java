package com.cityplatform.disaster.repository;

import com.cityplatform.disaster.model.WaterStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 水文监测站点数据访问层（只读使用，写入由 city-water 负责）
 */
@Repository
public interface WaterStationRepository extends JpaRepository<WaterStation, Long> {

    /**
     * 根据站点编号查找站点
     */
    Optional<WaterStation> findByStationId(String stationId);
}
