package com.cityplatform.water.repository;

import com.cityplatform.water.model.WaterLevelRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 水位监测记录数据访问层
 */
@Repository
public interface WaterLevelRecordRepository extends JpaRepository<WaterLevelRecord, Long> {

    /**
     * 根据站点ID查找最新5条水位记录
     */
    List<WaterLevelRecord> findTop5ByStationIdOrderByTimestampDesc(String stationId);
}
