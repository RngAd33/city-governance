// ============================================================
// DisasterSimulation - 灾害模拟推演页
// 态势与推演均来自 city-disaster 服务（Vite 代理 /api/disaster → 8088）：
//   GET  /api/disaster/current  ：真实采集的站点水位 / 降水 / 长江径流量
//   POST /api/disaster/simulate ：后端水量平衡模型推演（以真实站点水位为起点，结果入库）
// ============================================================
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Col,
  Divider,
  Empty,
  InputNumber,
  List,
  Row,
  Select,
  Space,
  Statistic,
  Tag,
} from 'antd';
import {
  AppstoreOutlined,
  CheckCircleOutlined,
  EnvironmentOutlined,
  ExportOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  TeamOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import ScenarioPlayer from './components/ScenarioPlayer';
import ImpactMap from './components/ImpactMap';
import { fetchDisasterCurrent, runDisasterSimulation } from '@/api/dashboard';
import { formatDateTime } from '@/utils/format';
import type {
  DisasterCurrentStatus,
  DisasterImpactArea,
  DisasterRiskLevel,
  DisasterSimulationResult,
  DisasterStationStatus,
  DisasterTimelinePoint,
  ImpactArea,
  Level,
} from '@/types';

// 后端风险等级 → 前端视觉等级
const RISK_TO_LEVEL: Record<DisasterRiskLevel, Level> = {
  LOW: 'normal',
  MEDIUM: 'warning',
  HIGH: 'danger',
  CRITICAL: 'critical',
};

// 视觉等级 → 文案与 Tag 色（站点、影响区域、建议面板共用）
const LEVEL_LABEL: Record<Level, string> = {
  normal: '低风险',
  warning: '中风险',
  danger: '高风险',
  critical: '严重',
};

const LEVEL_TAG_COLOR: Record<Level, string> = {
  normal: 'success',
  warning: 'warning',
  danger: 'error',
  critical: 'magenta',
};

// 淹没区域类型与站点类型文案
const ZONE_TYPE_LABEL: Record<DisasterImpactArea['type'], string> = {
  core: '核心淹没区',
  direct: '直接影响区',
  outer: '外围波及区',
};

const STATION_TYPE_LABEL: Record<DisasterStationStatus['type'], string> = {
  RIVER: '河流',
  LAKE: '湖泊',
  RESERVOIR: '水库',
  DRAINAGE: '排水管网',
};

const TREND_LABEL: Record<DisasterStationStatus['trend'], string> = {
  rising: '上涨',
  falling: '回落',
  stable: '平稳',
};

const TREND_COLOR: Record<DisasterStationStatus['trend'], string> = {
  rising: '#ff4d4f',
  falling: '#52c41a',
  stable: '#8c8c8c',
};

// 后端影响区域 → 地图 / 列表视图模型（显式映射，无坐标的区域不渲染）
const toViewImpactAreas = (areas: DisasterImpactArea[]): ImpactArea[] =>
  areas.map((area, i) => ({
    id: `${area.type}-${i}`,
    name: area.name,
    type: 'zone',
    status: RISK_TO_LEVEL[area.riskLevel],
    affectedPopulation: area.population,
    description: `波及半径 ${area.radiusKm} km，淹没面积约 ${area.areaKm2} km²（${ZONE_TYPE_LABEL[area.type]}）`,
    position: area.position,
  }));

// 后端时间轴 → 播放器事件标记（每 2 小时抽一个节点，避免标记过密）
const toTimelineEvents = (
  timeline: DisasterTimelinePoint[],
): { time: number; label: string; type: 'info' | 'warning' | 'danger' }[] =>
  timeline
    .filter((_, i) => i % 2 === 0)
    .map((point) => ({
      time: point.minute,
      label: `${point.waterLevel.toFixed(1)}m`,
      type:
        point.riskLevel === 'CRITICAL' || point.riskLevel === 'HIGH'
          ? 'danger'
          : point.riskLevel === 'MEDIUM'
            ? 'warning'
            : 'info',
    }));

// 建议类文本列表（有内容渲染列表，无内容显示说明文案）
const renderTextList = (items: string[], emptyText: string) =>
  items.length > 0 ? (
    <ul style={{ margin: 0, paddingLeft: 20, fontSize: 13, color: '#595959', lineHeight: 2 }}>
      {items.map((text, i) => (
        <li key={i}>{text}</li>
      ))}
    </ul>
  ) : (
    <span style={{ fontSize: 13, color: '#8c8c8c' }}>{emptyText}</span>
  );

const DisasterSimulation: React.FC = () => {
  // 当前态势（真实采集数据）
  const [current, setCurrent] = useState<DisasterCurrentStatus | null>(null);
  const [currentLoading, setCurrentLoading] = useState(false);
  const [currentError, setCurrentError] = useState<string | null>(null);
  const initializedRef = useRef(false);

  // 模拟参数与推演结果
  const [selectedStationId, setSelectedStationId] = useState<string>('');
  const [rainfall, setRainfall] = useState<number>(50);
  const [duration, setDuration] = useState<number>(24);
  const [simResult, setSimResult] = useState<DisasterSimulationResult | null>(null);
  const [simLoading, setSimLoading] = useState(false);
  const [simError, setSimError] = useState<string | null>(null);

  // 播放器
  const [currentTime, setCurrentTime] = useState<number>(0);
  const [isPlaying, setIsPlaying] = useState<boolean>(false);
  const [speed, setSpeed] = useState<number>(30);

  const loadCurrent = useCallback(async () => {
    setCurrentLoading(true);
    setCurrentError(null);
    try {
      const resp = await fetchDisasterCurrent();
      const data = resp.data;
      setCurrent(data);
      // 首次加载用后端推荐值初始化站点与参数；手动刷新时保留用户已选
      if (!initializedRef.current) {
        initializedRef.current = true;
        const recommended = data.recommended ?? null;
        setSelectedStationId(recommended?.stationId ?? data.stations[0]?.stationId ?? '');
        if (recommended) {
          setRainfall(recommended.rainfallMm);
          setDuration(recommended.durationHours);
        }
      }
    } catch (err) {
      setCurrentError(err instanceof Error ? err.message : '当前态势加载失败');
    } finally {
      setCurrentLoading(false);
    }
  }, []);

  useEffect(() => {
    loadCurrent();
  }, [loadCurrent]);

  const handleSimulate = useCallback(async () => {
    setSimLoading(true);
    setSimError(null);
    try {
      const resp = await runDisasterSimulation({
        stationId: selectedStationId || undefined,
        rainfallMm: rainfall,
        durationHours: duration,
      });
      setSimResult(resp.data);
      setCurrentTime(0);
      // 结果就绪立即自动播放：直接呈现洪水蔓延动画效果
      setIsPlaying(true);
    } catch (err) {
      setSimError(err instanceof Error ? err.message : '模拟请求失败');
    } finally {
      setSimLoading(false);
    }
  }, [selectedStationId, rainfall, duration]);

  const selectedStation = useMemo(
    () => current?.stations.find((s) => s.stationId === selectedStationId) ?? null,
    [current, selectedStationId],
  );

  const viewImpactAreas = useMemo(
    () => toViewImpactAreas(simResult?.impactAreas ?? []),
    [simResult],
  );

  const timelineEvents = useMemo(
    () => toTimelineEvents(simResult?.timeline ?? []),
    [simResult],
  );

  // 播放时刻对应的推演状态（timeline 按分钟升序，取最后一个已到达节点）
  const activeTimeline = useMemo(() => {
    const tl = simResult?.timeline ?? [];
    if (tl.length === 0) return null;
    let currentPoint = tl[0];
    for (const point of tl) {
      if (point.minute <= currentTime) currentPoint = point;
      else break;
    }
    return currentPoint;
  }, [simResult, currentTime]);

  // 洪水扩散帧：intensity 归一化为相对强度（0-1，最大帧为 1）
  const floodSpread = useMemo(() => {
    const frames = simResult?.floodSpread ?? [];
    const maxRadius = Math.max(...frames.map((f) => f.radiusKm), 0.001);
    return frames.map((frame) => ({
      time: frame.time,
      coordinates: frame.polygon,
      intensity: frame.radiusKm / maxRadius,
    }));
  }, [simResult]);

  // 监测站点地图标记（真实坐标）
  const stationMarkers = useMemo(
    () =>
      (current?.stations ?? []).map((s) => ({
        id: s.stationId,
        name: s.name,
        position: [s.lat, s.lng] as [number, number],
        type: 'station',
        status: RISK_TO_LEVEL[s.riskLevel],
      })),
    [current],
  );

  // 地图中心：优先模拟站点，其次首个监测站点（仅数据变化时更新视角）
  const mapCenter = useMemo<[number, number]>(() => {
    if (simResult) return [simResult.station.lat, simResult.station.lng];
    const firstStation = current?.stations[0];
    if (firstStation) return [firstStation.lat, firstStation.lng];
    return [32.06, 118.79];
  }, [simResult, current]);

  const totalDuration = (simResult?.durationHours ?? duration) * 60;

  return (
    <div style={{ padding: 24, background: '#f0f2f5', minHeight: '100%' }}>
      {/* 页头 */}
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h2 style={{ margin: 0, fontSize: 20, fontWeight: 600 }}>灾害模拟推演</h2>
          <span style={{ color: '#8c8c8c', fontSize: 13 }}>基于真实水文站点数据的洪涝情景推演与影响评估</span>
        </div>
        <Space>
          <Select
            value={current?.city ?? undefined}
            placeholder="城市"
            style={{ width: 120 }}
            options={current ? [{ label: current.city, value: current.city }] : []}
          />
          <Button icon={<ExportOutlined />}>导出报告</Button>
        </Space>
      </div>

      {/* 当前态势 */}
      {currentError ? (
        <Alert
          message="当前态势加载失败"
          description={`${currentError}（请确认 city-disaster 服务（8088）已启动）`}
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
        />
      ) : current ? (
        <Alert
          message={
            <span>
              当前态势：{current.city} · 综合风险{' '}
              <Tag color={LEVEL_TAG_COLOR[RISK_TO_LEVEL[current.overallRisk]]} style={{ marginInlineEnd: 0 }}>
                {LEVEL_LABEL[RISK_TO_LEVEL[current.overallRisk]]}
              </Tag>
              ，实时降水 {current.weather.rainfallMm} {current.weather.unit}
              {current.riverDischarge
                ? `，长江径流量 ${current.riverDischarge.current ?? '--'} ${current.riverDischarge.unit}`
                : ''}
            </span>
          }
          description={`数据来源：${current.sources.join('；')}（${formatDateTime(current.generatedAt)}）`}
          type={
            current.overallRisk === 'CRITICAL' || current.overallRisk === 'HIGH'
              ? 'error'
              : current.overallRisk === 'MEDIUM'
                ? 'warning'
                : 'info'
          }
          showIcon
          style={{ marginBottom: 16 }}
        />
      ) : null}

      {/* 监测站点 + 模拟参数 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} lg={12}>
          <Card
            title="监测站点"
            size="small"
            style={{ height: '100%' }}
            loading={currentLoading && !current}
            extra={
              <Space>
                {current && (
                  <span style={{ fontSize: 12, color: '#8c8c8c' }}>
                    更新于 {formatDateTime(current.generatedAt)}
                  </span>
                )}
                <Button size="small" icon={<ReloadOutlined />} onClick={loadCurrent} loading={currentLoading}>
                  刷新
                </Button>
              </Space>
            }
          >
            {(current?.stations.length ?? 0) === 0 ? (
              <Empty description="暂无站点数据" image={Empty.PRESENTED_IMAGE_SIMPLE} />
            ) : (
              <>
                <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                  {current!.stations.map((s) => (
                    <Button
                      key={s.stationId}
                      type={s.stationId === selectedStationId ? 'primary' : 'default'}
                      onClick={() => setSelectedStationId(s.stationId)}
                      style={{ height: 'auto', padding: '6px 12px' }}
                    >
                      <div>
                        <div style={{ fontSize: 13, fontWeight: 500 }}>{s.name}</div>
                        <div style={{ fontSize: 11, opacity: 0.85 }}>
                          水位 {s.currentLevel}m · 库容 {s.capacityPercentage}%
                        </div>
                      </div>
                    </Button>
                  ))}
                </div>
                {selectedStation && (
                  <div
                    style={{
                      marginTop: 12,
                      padding: 12,
                      background: '#fafafa',
                      borderRadius: 6,
                      fontSize: 13,
                      color: '#595959',
                    }}
                  >
                    <Space split={<Divider type="vertical" />} wrap size="middle">
                      <span>类型：{STATION_TYPE_LABEL[selectedStation.type]}</span>
                      <span>预警水位：{selectedStation.warningLevel}m</span>
                      <span>危险水位：{selectedStation.dangerLevel}m</span>
                      <span>
                        趋势：
                        <span style={{ color: TREND_COLOR[selectedStation.trend] }}>
                          {TREND_LABEL[selectedStation.trend]}
                        </span>
                      </span>
                      <Tag color={LEVEL_TAG_COLOR[RISK_TO_LEVEL[selectedStation.riskLevel]]}>
                        {LEVEL_LABEL[RISK_TO_LEVEL[selectedStation.riskLevel]]}
                      </Tag>
                    </Space>
                  </div>
                )}
              </>
            )}
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="模拟参数" size="small" style={{ height: '100%' }}>
            <Row gutter={16}>
              <Col span={8}>
                <div style={{ marginBottom: 4, fontSize: 12, color: '#8c8c8c' }}>降雨量 (mm)</div>
                <InputNumber
                  min={0}
                  max={500}
                  value={rainfall}
                  onChange={(v) => setRainfall(v ?? 0)}
                  style={{ width: '100%' }}
                />
              </Col>
              <Col span={8}>
                <div style={{ marginBottom: 4, fontSize: 12, color: '#8c8c8c' }}>持续时间 (h)</div>
                <InputNumber
                  min={1}
                  max={168}
                  value={duration}
                  onChange={(v) => setDuration(v ?? 1)}
                  style={{ width: '100%' }}
                />
              </Col>
              <Col span={8}>
                <div style={{ marginBottom: 4, fontSize: 12, color: '#8c8c8c' }}>模拟站点</div>
                <div
                  style={{
                    fontSize: 13,
                    lineHeight: '32px',
                    color: '#595959',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                  }}
                >
                  {selectedStation?.name ?? '--'}
                </div>
              </Col>
            </Row>
            <Button
              type="primary"
              icon={<PlayCircleOutlined />}
              loading={simLoading}
              onClick={handleSimulate}
              disabled={!current}
              block
              style={{ marginTop: 16 }}
            >
              开始模拟
            </Button>
            {simError && <Alert type="error" showIcon style={{ marginTop: 12 }} message={simError} />}
            {simResult && (
              <div style={{ marginTop: 12, fontSize: 12, color: '#8c8c8c' }}>
                最近推演：{simResult.scenarioName}（编号 {simResult.scenarioId}，预计峰值水位{' '}
                {simResult.projectedLevel}m，风险评分 {simResult.riskScore}）
              </div>
            )}
          </Card>
        </Col>
      </Row>

      {/* 播放器 + 地图 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} lg={10}>
          <Card
            title="模拟播放"
            size="small"
            style={{ height: '100%' }}
            extra={simResult && <Tag color="blue">{simResult.scenarioName}</Tag>}
          >
            <ScenarioPlayer
              totalDuration={totalDuration}
              currentTime={currentTime}
              isPlaying={isPlaying}
              speed={speed}
              events={timelineEvents}
              statusText={
                activeTimeline
                  ? `水位 ${activeTimeline.waterLevel.toFixed(2)}m · 库容 ${activeTimeline.capacityPercentage}% · ${LEVEL_LABEL[RISK_TO_LEVEL[activeTimeline.riskLevel]]}`
                  : undefined
              }
              disabled={!simResult}
              onTimeChange={setCurrentTime}
              onPlayPause={() => {
                if (!simResult) return;
                setIsPlaying((prev) => !prev);
              }}
              onSpeedChange={setSpeed}
              onSeek={setCurrentTime}
            />
            {!simResult && (
              <div style={{ textAlign: 'center', fontSize: 12, color: '#8c8c8c' }}>
                尚未执行模拟：时间轴按所选参数时长（{duration} 小时）预览，点击“开始模拟”生成推演结果
              </div>
            )}
          </Card>
        </Col>
        <Col xs={24} lg={14}>
          <Card title="影响范围可视化" size="small" style={{ height: '100%' }}>
            <div style={{ height: 400 }}>
              <ImpactMap
                impactAreas={viewImpactAreas}
                floodSpread={floodSpread}
                evacuationZones={[]}
                facilityMarkers={stationMarkers}
                currentTime={currentTime}
                center={mapCenter}
              />
            </div>
          </Card>
        </Col>
      </Row>

      {/* 影响统计 + 受影响区域 */}
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={24} lg={8}>
          <Card title="影响统计" size="small" style={{ height: '100%' }}>
            <Row gutter={[16, 16]}>
              <Col span={12}>
                <Statistic
                  title="影响区域"
                  value={viewImpactAreas.length}
                  suffix="处"
                  prefix={<AppstoreOutlined />}
                  valueStyle={{ color: '#1890ff' }}
                />
              </Col>
              <Col span={12}>
                <Statistic
                  title="影响人口"
                  value={simResult?.affectedPopulation ?? 0}
                  suffix="人"
                  prefix={<TeamOutlined />}
                  valueStyle={{ color: '#ff4d4f' }}
                />
              </Col>
              <Col span={12}>
                <Statistic
                  title="淹没面积"
                  value={simResult?.affectedArea ?? 0}
                  precision={2}
                  suffix="km²"
                  prefix={<EnvironmentOutlined />}
                  valueStyle={{ color: '#faad14' }}
                />
              </Col>
              <Col span={12}>
                <Statistic
                  title="预估损失"
                  value={simResult?.estimatedDamage ?? 0}
                  suffix="万元"
                  prefix={<WarningOutlined />}
                  valueStyle={{ color: '#ff4d4f' }}
                />
              </Col>
            </Row>
          </Card>
        </Col>
        <Col xs={24} lg={16}>
          <Card title="受影响区域列表" size="small" style={{ height: '100%' }}>
            {viewImpactAreas.length === 0 ? (
              <Empty
                description={simResult ? '本次推演未产生受影响区域' : '执行模拟后展示受影响区域'}
                image={Empty.PRESENTED_IMAGE_SIMPLE}
              />
            ) : (
              <List
                size="small"
                dataSource={viewImpactAreas}
                renderItem={(item) => (
                  <List.Item>
                    <div
                      style={{ width: '100%', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
                    >
                      <div>
                        <Tag color="cyan">淹没区</Tag>
                        <span style={{ marginLeft: 8, fontWeight: 500 }}>{item.name}</span>
                        <div style={{ fontSize: 12, color: '#8c8c8c', marginTop: 4 }}>{item.description}</div>
                      </div>
                      <div style={{ textAlign: 'right' }}>
                        <div style={{ fontSize: 12, color: '#595959', marginBottom: 4 }}>
                          影响人口 {item.affectedPopulation} 人
                        </div>
                        <Tag color={LEVEL_TAG_COLOR[item.status]}>{LEVEL_LABEL[item.status]}</Tag>
                      </div>
                    </div>
                  </List.Item>
                )}
                style={{ maxHeight: 280, overflowY: 'auto' }}
              />
            )}
          </Card>
        </Col>
      </Row>

      {/* 应急处置建议 */}
      {simResult && (
        <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
          <Col xs={24}>
            <Card
              size="small"
              title={
                <span>
                  <CheckCircleOutlined style={{ color: '#52c41a', marginRight: 8 }} />
                  应急处置建议
                </span>
              }
            >
              <Row gutter={[16, 16]}>
                <Col xs={24} md={8}>
                  <div style={{ fontWeight: 500, marginBottom: 8 }}>推荐措施</div>
                  {renderTextList(simResult.recommendedActions, '暂无推荐措施')}
                </Col>
                <Col xs={24} md={8}>
                  <div style={{ fontWeight: 500, marginBottom: 8 }}>道路封闭建议</div>
                  {renderTextList(simResult.roadClosures, '暂无道路封闭建议')}
                </Col>
                <Col xs={24} md={8}>
                  <div style={{ fontWeight: 500, marginBottom: 8 }}>疏散安置建议</div>
                  {renderTextList(simResult.evacuationZones, '暂无需疏散区域')}
                </Col>
              </Row>
            </Card>
          </Col>
        </Row>
      )}

      {/* 数据说明 */}
      <Row gutter={[16, 16]}>
        <Col xs={24}>
          <Card size="small">
            <div style={{ fontSize: 12, color: '#8c8c8c', lineHeight: 1.8 }}>
              数据说明：站点水位、降水与长江径流量来自 city-disaster 服务实时采集
              {current?.sources?.length ? `（${current.sources.join('；')}）` : ''}；情景推演由后端水量平衡模型完成，
              以真实站点水位为起点逐时演算并自动入库（FloodScenario），页面不使用任何模拟数据。
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default DisasterSimulation;
