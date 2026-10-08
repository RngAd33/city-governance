package com.cityplatform.disaster.repository;

import com.cityplatform.disaster.model.WaterLevelRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 水位监测记录数据访问层（只读使用，写入由 city-water 负责）
 */
@Repository
public interface WaterLevelRecordRepository extends JpaRepository<WaterLevelRecord, Long> {

    /**
     * 根据站点ID查找最新5条水位记录
     */
    List<WaterLevelRecord> findTop5ByStationIdOrderByTimestampDesc(String stationId);

    /**
     * 查找全局最新一条水位记录（获取最新降雨量等上下文）
     */
    Optional<WaterLevelRecord> findTopByOrderByTimestampDesc();
}
