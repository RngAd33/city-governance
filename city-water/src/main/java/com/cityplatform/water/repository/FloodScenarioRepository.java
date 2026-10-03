package com.cityplatform.water.repository;

import com.cityplatform.water.model.FloodScenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 洪涝灾害模拟场景数据访问层
 */
@Repository
public interface FloodScenarioRepository extends JpaRepository<FloodScenario, Long> {
}
