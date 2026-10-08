package com.cityplatform.disaster.repository;

import com.cityplatform.disaster.model.FloodForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 洪水预报数据访问层
 */
@Repository
public interface FloodForecastRepository extends JpaRepository<FloodForecast, Long> {

    /**
     * 按河段与预报日期查询（用于同天数据更新覆盖）
     */
    Optional<FloodForecast> findByStationNameAndForecastDate(String stationName, LocalDate forecastDate);

    /**
     * 查询指定日期起的预报序列（含未来预报），按日期升序
     */
    List<FloodForecast> findByForecastDateGreaterThanEqualOrderByForecastDateAsc(LocalDate from);

    /**
     * 查询最近一次采集时间
     */
    Optional<FloodForecast> findTopByOrderByFetchedAtDesc();

    /**
     * 查询指定日期及之前最近的一条预报（用于获取当前径流量）
     */
    Optional<FloodForecast> findTopByForecastDateLessThanEqualOrderByForecastDateDesc(LocalDate to);

    /**
     * 查询指定日期之前的过期预报（用于清理历史数据）
     */
    List<FloodForecast> findByForecastDateBefore(LocalDate date);
}
