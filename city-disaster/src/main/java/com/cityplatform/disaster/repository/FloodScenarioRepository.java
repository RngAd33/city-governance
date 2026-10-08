package com.cityplatform.disaster.repository;

import com.cityplatform.disaster.model.FloodScenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 洪涝灾害模拟场景数据访问层
 */
@Repository
public interface FloodScenarioRepository extends JpaRepository<FloodScenario, Long> {

    /**
     * 查询站点最近一次生成的灾害场景（用于去重，避免持续超标期间重复生成）
     */
    Optional<FloodScenario> findTopByStationIdOrderByCreatedAtDesc(String stationId);
}
