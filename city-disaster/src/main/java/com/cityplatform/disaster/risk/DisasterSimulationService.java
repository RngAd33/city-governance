package com.cityplatform.disaster.risk;

import com.cityplatform.common.util.JsonUtil;
import com.cityplatform.disaster.model.FloodForecast;
import com.cityplatform.disaster.model.FloodScenario;
import com.cityplatform.disaster.model.WaterLevelRecord;
import com.cityplatform.disaster.model.WaterStation;
import com.cityplatform.disaster.repository.FloodForecastRepository;
import com.cityplatform.disaster.repository.WaterLevelRecordRepository;
import com.cityplatform.disaster.repository.WaterStationRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 灾害模拟与态势服务
 * 面向灾害模拟页面提供两类数据（全部基于真实采集数据，非随机模拟）：
 * - 当前态势：南京各站点真实水位（采集服务每10分钟发布）、
 *   长江真实径流量预报（Open-Meteo GloFAS）、真实降水量
 * - 灾害模拟：以站点真实当前水位为起点，按用户输入的降雨情景
 *   经水量平衡模型推演，输出时间轴、影响区域、洪水扩散等可视化数据
 */
@Service
@Slf4j
public class DisasterSimulationService {

    /** 纬度方向每度对应公里数 */
    private static final double EARTH_KM_PER_DEG_LAT = 111.0;

    /** 时间轴采样点数（含起点） */
    private static final int TIMELINE_POINTS = 12;

    /** 洪水扩散动画帧数 */
    private static final int SPREAD_FRAMES = 9;

    /** 影响区域多边形顶点数 */
    private static final int CIRCLE_POINTS = 12;

    /** 默认模拟降雨量（毫米，暴雨情景） */
    private static final double DEFAULT_RAINFALL_MM = 50.0;

    /** 默认模拟时长（小时） */
    private static final int DEFAULT_DURATION_HOURS = 24;

    @Resource
    private WaterStationRepository waterStationRepository;

    @Resource
    private WaterLevelRecordRepository waterLevelRecordRepository;

    @Resource
    private FloodForecastRepository floodForecastRepository;

    @Resource
    private FloodRiskAssessor floodRiskAssessor;

    /**
     * 当前灾害态势：站点水位/风险/趋势、长江径流量预报、最新降水、数据来源
     */
    public CurrentDisasterStatus getCurrentStatus() {
        List<WaterStation> stations = waterStationRepository.findAll();
        List<StationStatus> stationStatuses = stations.stream()
                .map(this::toStationStatus)
                .toList();

        WaterLevelRecord latestRecord = waterLevelRecordRepository
                .findTopByOrderByTimestampDesc()
                .orElse(null);

        String overallRisk = stationStatuses.stream()
                .map(StationStatus::riskLevel)
                .max(Comparator.comparingInt(DisasterSimulationService::riskRank))
                .orElse(WaterLevelRecord.RiskLevel.LOW.name());

        return new CurrentDisasterStatus(
                "南京",
                LocalDateTime.now().toString(),
                buildSources(stations, latestRecord),
                new WeatherInfo(
                        latestRecord != null && latestRecord.getRainfall() != null
                                ? latestRecord.getRainfall() : 0.0,
                        "mm", "open-meteo",
                        latestRecord != null && latestRecord.getTimestamp() != null
                                ? latestRecord.getTimestamp().toString() : null),
                buildRiverDischargeInfo(),
                stationStatuses,
                overallRisk,
                buildRecommended(stations));
    }

    /**
     * 灾害模拟：以站点真实当前水位为起点，按降雨情景推演灾害影响
     *
     * @param stationId     站点ID，为空时自动选择水位最接近阈值的站点
     * @param rainfallMm    模拟降雨量（毫米），为空取默认暴雨情景
     * @param durationHours 模拟时长（小时），为空取默认24小时
     */
    public SimulationResult simulate(String stationId, Double rainfallMm, Integer durationHours) {
        WaterStation station;
        if (stationId != null && !stationId.isBlank()) {
            station = waterStationRepository.findByStationId(stationId).orElse(null);
            if (station == null) {
                throw new IllegalArgumentException("未找到监测站点: " + stationId);
            }
        } else {
            station = waterStationRepository.findAll().stream()
                    .max(Comparator.comparingDouble(WaterStation::getCapacityPercentage))
                    .orElseThrow(() -> new IllegalArgumentException("暂无监测站点数据"));
        }

        double rainfall = rainfallMm != null ? clamp(rainfallMm, 0.0, 500.0) : DEFAULT_RAINFALL_MM;
        int duration = durationHours != null ? clamp(durationHours, 1, 168) : DEFAULT_DURATION_HOURS;

        // 长江真实径流量预报（GloFAS），驱动水量平衡模型中的径流调节项
        RiverDischargeInfo dischargeInfo = buildRiverDischargeInfo();
        Double riverDischarge = dischargeInfo != null ? dischargeInfo.current() : null;

        // 复用洪涝评估服务：场景生成（入库）+ 风险评估 + 水量平衡推演
        FloodScenario scenario = floodRiskAssessor.simulateFloodScenario(
                station, rainfall, duration, riverDischarge);
        FloodRiskAssessor.RiskAssessment assessment = floodRiskAssessor.assessRisk(
                station, station.getCurrentLevel(), rainfall, duration, riverDischarge);
        double projectedLevel = floodRiskAssessor.projectLevel(station, rainfall, duration, riverDischarge);

        log.info("灾害模拟完成 [{}]: 站点={}, 降雨={}mm, 时长={}h, 等级={}, 影响面积={}km²",
                scenario.getScenarioId(), station.getStationId(), rainfall, duration,
                assessment.getRiskLevel(), String.format("%.2f", assessment.getAffectedArea()));

        return new SimulationResult(
                scenario.getScenarioId(),
                scenario.getName(),
                LocalDateTime.now().toString(),
                new StationInfo(
                        station.getStationId(), station.getName(), station.getType().name(),
                        station.getLat(), station.getLng(),
                        station.getCurrentLevel(), station.getWarningLevel(),
                        station.getDangerLevel(), station.getMaxCapacity()),
                round(rainfall, 1),
                duration,
                round(projectedLevel, 2),
                assessment.getRiskLevel().name(),
                round(assessment.getRiskScore(), 1),
                round(assessment.getAffectedArea(), 2),
                scenario.getAffectedPopulation(),
                round(scenario.getEstimatedDamage(), 1),
                parseRoadClosures(scenario.getRoadClosures()),
                assessment.getEvacuationZones(),
                assessment.getRecommendedActions(),
                buildTimeline(station, projectedLevel, duration),
                buildImpactAreas(station, assessment, scenario.getAffectedPopulation()),
                buildFloodSpread(station, assessment.getAffectedArea(), duration),
                dischargeInfo);
    }

    /**
     * 站点态势（真实水位、阈值、容量占比、风险等级、趋势）
     */
    private StationStatus toStationStatus(WaterStation station) {
        List<WaterLevelRecord> recent = waterLevelRecordRepository
                .findTop5ByStationIdOrderByTimestampDesc(station.getStationId());
        WaterLevelRecord latest = recent.isEmpty() ? null : recent.get(0);

        String trend = "stable";
        if (recent.size() >= 2
                && recent.get(0).getWaterLevel() != null && recent.get(1).getWaterLevel() != null) {
            double delta = recent.get(0).getWaterLevel() - recent.get(1).getWaterLevel();
            if (delta > 0.01) {
                trend = "rising";
            } else if (delta < -0.01) {
                trend = "falling";
            }
        }

        double level = station.getCurrentLevel() != null ? station.getCurrentLevel() : 0.0;
        return new StationStatus(
                station.getStationId(), station.getName(), station.getType().name(),
                station.getLat(), station.getLng(),
                level, station.getWarningLevel(), station.getDangerLevel(), station.getMaxCapacity(),
                round(station.getCapacityPercentage(), 1),
                assessRisk(level, station).name(), trend,
                latest != null && latest.getTimestamp() != null
                        ? latest.getTimestamp().toString() : null);
    }

    /**
     * 站点风险等级：委托洪涝评估服务（危险/预警水位阈值与库容占比综合判定）
     */
    private WaterLevelRecord.RiskLevel assessRisk(double level, WaterStation station) {
        return floodRiskAssessor.determineRiskLevel(station, level);
    }

    /**
     * 长江径流量信息（当前值 + 预报序列）
     */
    private RiverDischargeInfo buildRiverDischargeInfo() {
        FloodForecast currentRow = floodForecastRepository
                .findTopByForecastDateLessThanEqualOrderByForecastDateDesc(LocalDate.now())
                .orElseGet(() -> floodForecastRepository.findTopByOrderByFetchedAtDesc().orElse(null));
        if (currentRow == null) {
            return null;
        }

        List<ForecastDay> forecast = floodForecastRepository
                .findByForecastDateGreaterThanEqualOrderByForecastDateAsc(LocalDate.now()).stream()
                .map(row -> new ForecastDay(row.getForecastDate().toString(),
                        row.getDischarge(), row.getDischargeMax(), row.getDischargeMean()))
                .toList();

        Double forecastMax = forecast.stream()
                .map(ForecastDay::discharge)
                .filter(Objects::nonNull)
                .max(Double::compare)
                .orElse(null);

        return new RiverDischargeInfo(
                currentRow.getStationName(),
                currentRow.getDischarge(),
                forecastMax,
                "m³/s",
                currentRow.getFetchedAt() != null ? currentRow.getFetchedAt().toString() : null,
                forecast);
    }

    /**
     * 数据来源说明（基于实际存在的数据）
     */
    private List<String> buildSources(List<WaterStation> stations, WaterLevelRecord latestRecord) {
        List<String> sources = new ArrayList<>();
        if (floodForecastRepository.findTopByOrderByFetchedAtDesc().isPresent()) {
            sources.add("Open-Meteo GloFAS 洪水预报（长江南京段径流量）");
        }
        if (latestRecord != null) {
            sources.add("Open-Meteo 气象API（南京实时降水）");
            sources.add("南京水文站点采集（" + stations.size() + "个站点，10分钟粒度）");
        }
        if (sources.isEmpty()) {
            sources.add("暂无真实数据，等待采集服务首次运行");
        }
        return sources;
    }

    /**
     * 推荐模拟站点：当前容量占比最高的站点
     */
    private RecommendedScenario buildRecommended(List<WaterStation> stations) {
        WaterStation target = stations.stream()
                .max(Comparator.comparingDouble(WaterStation::getCapacityPercentage))
                .orElse(null);
        if (target == null) {
            return null;
        }
        return new RecommendedScenario(target.getStationId(), target.getName(),
                DEFAULT_RAINFALL_MM, DEFAULT_DURATION_HOURS);
    }

    /**
     * 时间轴：从真实当前水位平滑推演至预测水位，时间单位为分钟
     */
    private List<TimelinePoint> buildTimeline(WaterStation station, double projectedLevel,
                                              int durationHours) {
        double current = station.getCurrentLevel() != null ? station.getCurrentLevel() : 0.0;
        double rise = projectedLevel - current;

        List<TimelinePoint> timeline = new ArrayList<>();
        for (int i = 0; i <= TIMELINE_POINTS; i++) {
            double t = (double) i / TIMELINE_POINTS;
            // 平滑上升曲线：前期快速上涨后趋于平缓
            double curve = 1 - Math.pow(1 - t, 2);
            double level = current + rise * curve;
            double capacity = station.getMaxCapacity() != null && station.getMaxCapacity() > 0
                    ? level / station.getMaxCapacity() * 100.0 : 0.0;
            timeline.add(new TimelinePoint(
                    (int) Math.round(durationHours * 60.0 * t),
                    round(level, 2),
                    round(capacity, 1),
                    assessRisk(level, station).name()));
        }
        return timeline;
    }

    /**
     * 影响区域：核心淹没区/直接影响区/外围波及区（以站点为中心，向长江下游方向错位排布）
     */
    private List<ImpactArea> buildImpactAreas(WaterStation station,
                                              FloodRiskAssessor.RiskAssessment assessment,
                                              int affectedPopulation) {
        double area = assessment.getAffectedArea();
        double maxRadius = radiusKm(area);
        double lat = station.getLat();
        double lng = station.getLng();
        String riskLevel = assessment.getRiskLevel().name();

        int corePopulation = (int) Math.round(affectedPopulation * 0.6);
        int directPopulation = (int) Math.round(affectedPopulation * 0.3);

        List<ImpactArea> zones = new ArrayList<>();
        zones.add(new ImpactArea("核心淹没区", "core",
                new double[]{round(lat, 5), round(lng, 5)},
                round(maxRadius * 0.45, 2), round(area * 0.25, 2),
                corePopulation, riskLevel));
        zones.add(new ImpactArea("直接影响区", "direct",
                new double[]{round(lat + 0.008, 5), round(lng + 0.01, 5)},
                round(maxRadius * 0.75, 2), round(area * 0.4, 2),
                directPopulation, riskLevel));
        zones.add(new ImpactArea("外围波及区", "outer",
                new double[]{round(lat + 0.015, 5), round(lng + 0.02, 5)},
                round(maxRadius, 2), round(area * 0.35, 2),
                Math.max(0, affectedPopulation - corePopulation - directPopulation), riskLevel));
        return zones;
    }

    /**
     * 洪水扩散动画数据：随时间的扩散半径与多边形（time 为分钟）
     */
    private List<SpreadFrame> buildFloodSpread(WaterStation station, double affectedArea,
                                               int durationHours) {
        double maxRadius = radiusKm(affectedArea);
        List<SpreadFrame> frames = new ArrayList<>();
        for (int i = 0; i < SPREAD_FRAMES; i++) {
            double t = (double) i / (SPREAD_FRAMES - 1);
            double curve = 1 - Math.pow(1 - t, 2);
            double radius = Math.max(0.1, maxRadius * curve);
            frames.add(new SpreadFrame(
                    (int) Math.round(durationHours * 60.0 * t),
                    round(radius, 2),
                    circlePolygon(station.getLat(), station.getLng(), radius)));
        }
        return frames;
    }

    /**
     * 以中心点生成圆形多边形（[纬度, 经度] 顶点列表）
     */
    private List<double[]> circlePolygon(double lat, double lng, double radiusKm) {
        List<double[]> points = new ArrayList<>(CIRCLE_POINTS);
        for (int i = 0; i < CIRCLE_POINTS; i++) {
            double angle = 2 * Math.PI * i / CIRCLE_POINTS;
            double dLat = radiusKm / EARTH_KM_PER_DEG_LAT * Math.cos(angle);
            double dLng = radiusKm / (EARTH_KM_PER_DEG_LAT * Math.cos(Math.toRadians(lat))) * Math.sin(angle);
            points.add(new double[]{round(lat + dLat, 5), round(lng + dLng, 5)});
        }
        return points;
    }

    /**
     * 解析道路封闭建议（场景中存储为JSON数组字符串）
     */
    private List<String> parseRoadClosures(String roadClosures) {
        if (roadClosures == null || roadClosures.isBlank()) {
            return List.of();
        }
        try {
            List<String> list = JsonUtil.fromJson(roadClosures, List.class);
            return list != null ? list : List.of();
        } catch (Exception e) {
            return List.of(roadClosures);
        }
    }

    /**
     * 圆形面积对应的等效半径（公里）
     */
    private double radiusKm(double areaKm2) {
        return Math.sqrt(Math.max(areaKm2, 0.01) / Math.PI);
    }

    private static int riskRank(String riskLevel) {
        return switch (riskLevel) {
            case "CRITICAL" -> 3;
            case "HIGH" -> 2;
            case "MEDIUM" -> 1;
            default -> 0;
        };
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round(double value, int decimalPlaces) {
        double factor = Math.pow(10, decimalPlaces);
        return Math.round(value * factor) / factor;
    }

    /**
     * 当前灾害态势响应
     */
    public record CurrentDisasterStatus(
            String city,
            String generatedAt,
            List<String> sources,
            WeatherInfo weather,
            RiverDischargeInfo riverDischarge,
            List<StationStatus> stations,
            String overallRisk,
            RecommendedScenario recommended
    ) {}

    /**
     * 实时天气信息
     */
    public record WeatherInfo(double rainfallMm, String unit, String source, String updatedAt) {}

    /**
     * 长江径流量信息
     */
    public record RiverDischargeInfo(
            String stationName,
            Double current,
            Double forecastMax,
            String unit,
            String updatedAt,
            List<ForecastDay> forecast
    ) {}

    /**
     * 单日径流量预报
     */
    public record ForecastDay(String date, Double discharge, Double dischargeMax, Double dischargeMean) {}

    /**
     * 站点态势
     */
    public record StationStatus(
            String stationId,
            String name,
            String type,
            double lat,
            double lng,
            double currentLevel,
            double warningLevel,
            double dangerLevel,
            double maxCapacity,
            double capacityPercentage,
            String riskLevel,
            String trend,
            String updatedAt
    ) {}

    /**
     * 推荐模拟参数（容量占比最高站点的暴雨情景）
     */
    public record RecommendedScenario(String stationId, String stationName,
                                      double rainfallMm, int durationHours) {}

    /**
     * 灾害模拟结果
     */
    public record SimulationResult(
            String scenarioId,
            String scenarioName,
            String generatedAt,
            StationInfo station,
            double rainfallMm,
            int durationHours,
            double projectedLevel,
            String riskLevel,
            double riskScore,
            double affectedArea,
            int affectedPopulation,
            double estimatedDamage,
            List<String> roadClosures,
            List<String> evacuationZones,
            List<String> recommendedActions,
            List<TimelinePoint> timeline,
            List<ImpactArea> impactAreas,
            List<SpreadFrame> floodSpread,
            RiverDischargeInfo riverDischarge
    ) {}

    /**
     * 模拟站点信息
     */
    public record StationInfo(
            String stationId,
            String name,
            String type,
            double lat,
            double lng,
            double currentLevel,
            double warningLevel,
            double dangerLevel,
            double maxCapacity
    ) {}

    /**
     * 时间轴节点（minute 为分钟）
     */
    public record TimelinePoint(int minute, double waterLevel,
                                double capacityPercentage, String riskLevel) {}

    /**
     * 影响区域（position 为 [纬度, 经度]）
     */
    public record ImpactArea(String name, String type, double[] position,
                             double radiusKm, double areaKm2, int population, String riskLevel) {}

    /**
     * 洪水扩散帧（time 为分钟，polygon 为 [纬度, 经度] 顶点列表）
     */
    public record SpreadFrame(int time, double radiusKm, List<double[]> polygon) {}
}
