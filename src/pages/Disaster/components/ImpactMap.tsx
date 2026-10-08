// ============================================================
// ImpactMap - Leaflet map for disaster impact visualization
// ============================================================
import React, { useEffect, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { createBaseTileLayer, wgs84ToGcj02 } from '@/utils/mapTiles';
import type { ImpactArea, Level } from '@/types';

const LEVEL_COLORS: Record<Level, string> = {
  normal: '#52c41a',
  warning: '#faad14',
  danger: '#ff4d4f',
  critical: '#a8071a',
};

interface ImpactMapProps {
  impactAreas: ImpactArea[];
  /** 洪水扩散帧（time 为分钟） */
  floodSpread: { time: number; coordinates: [number, number][]; intensity: number }[];
  evacuationZones: { id: string; name: string; coordinates: [number, number][]; capacity: number }[];
  /** 设施/站点标记（type=station 时按监测站点渲染） */
  facilityMarkers: { id: string; name: string; position: [number, number]; type: string; status: Level }[];
  /** 当前播放时间（分钟） */
  currentTime: number;
  center?: [number, number];
  zoom?: number;
}

const ImpactMap: React.FC<ImpactMapProps> = ({
  impactAreas,
  floodSpread,
  evacuationZones,
  facilityMarkers,
  currentTime,
  center = [31.2304, 121.4737],
  zoom = 12,
}) => {
  const mapRef = useRef<L.Map | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const floodLayerRef = useRef<L.LayerGroup | null>(null);
  const impactLayerRef = useRef<L.LayerGroup | null>(null);
  const evacuationLayerRef = useRef<L.LayerGroup | null>(null);
  const facilityLayerRef = useRef<L.LayerGroup | null>(null);

  // Initialize map
  useEffect(() => {
    if (!containerRef.current || mapRef.current) return;

    const map = L.map(containerRef.current, {
      center: wgs84ToGcj02(center[0], center[1]),
      zoom,
      zoomControl: true,
      attributionControl: true,
    });

    createBaseTileLayer().addTo(map);

    floodLayerRef.current = L.layerGroup().addTo(map);
    impactLayerRef.current = L.layerGroup().addTo(map);
    evacuationLayerRef.current = L.layerGroup().addTo(map);
    facilityLayerRef.current = L.layerGroup().addTo(map);

    const overlays: Record<string, L.LayerGroup> = {
      '洪水蔓延': floodLayerRef.current,
      '影响区域': impactLayerRef.current,
      '疏散区域': evacuationLayerRef.current,
      '设施状态': facilityLayerRef.current,
    };
    L.control.layers(undefined, overlays, { position: 'topright' }).addTo(map);

    mapRef.current = map;

    return () => {
      map.remove();
      mapRef.current = null;
    };
  }, []);

  // 数据异步到达后中心点数值会变化：重新定位地图
  // （依赖到坐标数值，避免父组件重渲染时数组新引用导致视角被反复重置）
  // 有推演结果时视角改由下方 fitBounds 聚焦淹没区，此处不再重置
  const [centerLat, centerLng] = center;
  useEffect(() => {
    if (!mapRef.current || floodSpread.length > 0) return;
    mapRef.current.setView(wgs84ToGcj02(centerLat, centerLng), zoom);
  }, [centerLat, centerLng, zoom, floodSpread.length]);

  // 推演结果到达后自动聚焦淹没区：淹没半径仅数百米，zoom 12 下只有几十像素不可见
  useEffect(() => {
    if (!mapRef.current || floodSpread.length === 0) return;
    const points: [number, number][] = [];
    floodSpread.forEach((f) =>
      f.coordinates.forEach(([lat, lng]) => points.push(wgs84ToGcj02(lat, lng))),
    );
    impactAreas.forEach((area) => {
      if (area.position) points.push(wgs84ToGcj02(area.position[0], area.position[1]));
    });
    if (points.length === 0) return;
    mapRef.current.fitBounds(L.latLngBounds(points), { padding: [48, 48], maxZoom: 16 });
  }, [floodSpread, impactAreas]);

  // 洪水蔓延渲染：沉积层（已完成扩散范围）+ 前沿层（相邻帧逐顶点插值）。
  // 后端帧间隔大（分钟级时间轴仅约 9 帧），直接跳帧播放几乎看不出变化；
  // 插值后播放时前沿边界持续外扩，形成连续扩散动画
  useEffect(() => {
    if (!floodLayerRef.current) return;
    floodLayerRef.current.clearLayers();
    if (floodSpread.length === 0 || currentTime < floodSpread[0].time) return;

    // 最后一个已到达帧
    let lastIdx = 0;
    for (let i = 0; i < floodSpread.length; i++) {
      if (floodSpread[i].time <= currentTime) lastIdx = i;
    }
    const base = floodSpread[lastIdx];

    const drawLayer = (coordinates: [number, number][], fillOpacity: number, outline: boolean) => {
      const polygon = L.polygon(
        coordinates.map(([lat, lng]) => wgs84ToGcj02(lat, lng)),
        {
          color: outline ? '#0050b3' : '#1890ff',
          fillColor: '#1890ff',
          fillOpacity,
          weight: outline ? 3 : 1,
        },
      );
      floodLayerRef.current!.addLayer(polygon);
    };

    // 沉积层：已完成扩散范围
    drawLayer(base.coordinates, 0.25, false);

    // 前沿层：与下一帧逐顶点线性插值（顶点数一致时），播放中持续外扩
    const next = floodSpread[lastIdx + 1];
    if (next && next.coordinates.length === base.coordinates.length) {
      const span = next.time - base.time || 1;
      const ratio = Math.min(1, Math.max(0, (currentTime - base.time) / span));
      const interpolated: [number, number][] = base.coordinates.map(([lat, lng], i) => [
        lat + (next.coordinates[i][0] - lat) * ratio,
        lng + (next.coordinates[i][1] - lng) * ratio,
      ]);
      drawLayer(interpolated, 0.45, true);
    } else {
      // 最后一帧 / 顶点数不一致：不做插值，强调已完成边界
      drawLayer(base.coordinates, 0.45, true);
    }
  }, [floodSpread, currentTime]);

  // Update impact areas
  useEffect(() => {
    if (!impactLayerRef.current) return;
    impactLayerRef.current.clearLayers();

    impactAreas.forEach((area) => {
      const color = LEVEL_COLORS[area.status];
      const icon = L.divIcon({
        className: 'custom-marker',
        html: `<div style="
          width: 24px; height: 24px;
          border-radius: 50%;
          background: ${color};
          border: 2px solid #fff;
          box-shadow: 0 2px 4px rgba(0,0,0,0.3);
          display: flex; align-items: center; justify-content: center;
          font-size: 12px;
        ">${area.type === 'road' ? '🛣' : area.type === 'facility' ? '🏢' : area.type === 'zone' ? '🌊' : '🏠'}</div>`,
        iconSize: [24, 24],
        iconAnchor: [12, 12],
      });

      // 使用后端返回的真实坐标；无坐标的区域不渲染，避免伪造位置
      if (!area.position) return;
      const marker = L.marker(wgs84ToGcj02(area.position[0], area.position[1]), { icon });

      marker.bindPopup(`
        <div style="min-width: 180px;">
          <h3 style="margin: 0 0 8px; font-size: 14px;">${area.name}</h3>
          <p style="margin: 4px 0; font-size: 12px;">
            <strong>类型:</strong> ${area.type === 'road' ? '道路' : area.type === 'facility' ? '设施' : area.type === 'zone' ? '淹没区' : '居民区'}
          </p>
          <p style="margin: 4px 0; font-size: 12px;">
            <strong>状态:</strong>
            <span style="color: ${color}; font-weight: 600;">
              ${area.status === 'normal' ? '正常' : area.status === 'warning' ? '受影响' : area.status === 'danger' ? '严重受损' : '完全损毁'}
            </span>
          </p>
          <p style="margin: 4px 0; font-size: 12px;"><strong>影响人口:</strong> ${area.affectedPopulation} 人</p>
          ${area.description ? `<p style="margin: 4px 0; font-size: 12px; color: #666;">${area.description}</p>` : ''}
        </div>
      `);

      impactLayerRef.current!.addLayer(marker);
    });
  }, [impactAreas]);

  // Update evacuation zones
  useEffect(() => {
    if (!evacuationLayerRef.current) return;
    evacuationLayerRef.current.clearLayers();

    evacuationZones.forEach((zone) => {
      const polygon = L.polygon(
        zone.coordinates.map(([lat, lng]) => wgs84ToGcj02(lat, lng)),
        {
          color: '#52c41a',
          fillColor: '#52c41a',
          fillOpacity: 0.15,
          weight: 2,
          dashArray: '5, 5',
        },
      );

      polygon.bindPopup(`
        <div style="min-width: 160px;">
          <h3 style="margin: 0 0 8px; font-size: 14px;">${zone.name}</h3>
          <p style="margin: 4px 0; font-size: 12px;"><strong>容量:</strong> ${zone.capacity} 人</p>
          <p style="margin: 4px 0; font-size: 12px; color: #52c41a;"><strong>开放中</strong></p>
        </div>
      `);

      evacuationLayerRef.current!.addLayer(polygon);
    });
  }, [evacuationZones]);

  // Update facility markers
  useEffect(() => {
    if (!facilityLayerRef.current) return;
    facilityLayerRef.current.clearLayers();

    facilityMarkers.forEach((facility) => {
      const color = LEVEL_COLORS[facility.status];
      const icon = L.divIcon({
        className: 'custom-marker',
        html: `<div style="
          width: 20px; height: 20px;
          border-radius: 4px;
          background: ${color};
          border: 2px solid #fff;
          box-shadow: 0 1px 4px rgba(0,0,0,0.3);
          display: flex; align-items: center; justify-content: center;
          font-size: 10px;
        ">${facility.type === 'hospital' ? '🏥' : facility.type === 'school' ? '🏫' : facility.type === 'shelter' ? '🏠' : facility.type === 'station' ? '💧' : '🏢'}</div>`,
        iconSize: [20, 20],
        iconAnchor: [10, 10],
      });

      const marker = L.marker(
        wgs84ToGcj02(facility.position[0], facility.position[1]),
        { icon },
      );

      marker.bindPopup(`
        <div style="min-width: 160px;">
          <h3 style="margin: 0 0 8px; font-size: 14px;">${facility.name}</h3>
          <p style="margin: 4px 0; font-size: 12px;">
            <strong>类型:</strong> ${facility.type === 'hospital' ? '医院' : facility.type === 'school' ? '学校' : facility.type === 'shelter' ? '避难所' : facility.type === 'station' ? '监测站点' : '设施'}
          </p>
          <p style="margin: 4px 0; font-size: 12px;">
            <strong>状态:</strong>
            <span style="color: ${color}; font-weight: 600;">
              ${facility.status === 'normal' ? '正常' : facility.status === 'warning' ? '警告' : facility.status === 'danger' ? '危险' : '严重'}
            </span>
          </p>
        </div>
      `);

      facilityLayerRef.current!.addLayer(marker);
    });
  }, [facilityMarkers]);

  return (
    <div
      ref={containerRef}
      style={{
        width: '100%',
        height: '100%',
        minHeight: 400,
        borderRadius: 8,
        overflow: 'hidden',
        zIndex: 0,
      }}
    />
  );
};

export default ImpactMap;
