// ============================================================
// CityMap - Leaflet map with status-coded markers & layer control
// ============================================================
import React, { useEffect, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { createBaseTileLayer, wgs84ToGcj02 } from '@/utils/mapTiles';
import type { Building, EnvironmentRecord, WaterStation } from '@/types';

type Level = 'normal' | 'warning' | 'danger' | 'critical';

const LEVEL_COLORS: Record<Level, string> = {
  critical: '#ff4d4f',
  normal: '#52c41a',
  warning: '#faad14',
  danger: '#ff4d4f',
};

interface CityMapProps {
  buildings?: Building[];
  environmentRecords?: EnvironmentRecord[];
  waterStations?: WaterStation[];
  center?: [number, number];
  zoom?: number;
}

const CityMap: React.FC<CityMapProps> = ({
  buildings = [],
  environmentRecords = [],
  waterStations = [],
  center = [32.06, 118.79],
  zoom = 12,
}) => {
  const mapRef = useRef<L.Map | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const buildingGroupRef = useRef<L.LayerGroup>(L.layerGroup());
  const envGroupRef = useRef<L.LayerGroup>(L.layerGroup());
  const waterGroupRef = useRef<L.LayerGroup>(L.layerGroup());

  useEffect(() => {
    if (!containerRef.current || mapRef.current) return;

    const map = L.map(containerRef.current, {
      center: wgs84ToGcj02(center[0], center[1]),
      zoom,
      zoomControl: true,
      attributionControl: true,
    });

    createBaseTileLayer().addTo(map);

    buildingGroupRef.current.addTo(map);
    envGroupRef.current.addTo(map);
    waterGroupRef.current.addTo(map);

    const overlays: Record<string, L.LayerGroup> = {
      建筑: buildingGroupRef.current,
      环境监测: envGroupRef.current,
      水位站: waterGroupRef.current,
    };
    L.control.layers(undefined, overlays, { position: 'topright' }).addTo(map);

    mapRef.current = map;

    return () => {
      map.remove();
      mapRef.current = null;
    };
  }, []);

  useEffect(() => {
    if (!mapRef.current) return;

    buildingGroupRef.current.clearLayers();
    envGroupRef.current.clearLayers();
    waterGroupRef.current.clearLayers();

    // Building markers
    buildings.forEach((b) => {
      const icon = L.divIcon({
        className: 'custom-marker',
        html: `<div style="
          width: 24px; height: 24px;
          border-radius: 4px;
          background: #1890ff;
          border: 2px solid #fff;
          box-shadow: 0 2px 6px rgba(0,0,0,0.3);
          display: flex; align-items: center; justify-content: center;
          font-size: 12px; color: #fff; font-weight: bold;
        ">🏢</div>`,
        iconSize: [24, 24],
        iconAnchor: [12, 12],
      });
      const marker = L.marker(wgs84ToGcj02(b.lat, b.lng), { icon });
      marker.bindPopup(`
        <div style="min-width: 160px;">
          <h3 style="margin: 0 0 8px; font-size: 14px;">${b.name}</h3>
          <p style="margin: 4px 0; font-size: 12px;"><strong>类型:</strong> ${b.type}</p>
          <p style="margin: 4px 0; font-size: 12px;"><strong>面积:</strong> ${b.area} m²</p>
          <p style="margin: 4px 0; font-size: 12px;"><strong>楼层:</strong> ${b.floors}</p>
          <p style="margin: 4px 0; font-size: 12px;"><strong>日均能耗:</strong> ${b.avgDailyConsumption} kWh</p>
        </div>
      `);
      buildingGroupRef.current.addLayer(marker);
    });

    // Environment station markers
    environmentRecords.forEach((r) => {
      const level = (r.level || '').toLowerCase() as Level;
      const color = LEVEL_COLORS[level] || '#1890ff';
      const icon = L.divIcon({
        className: 'custom-marker',
        html: `<div style="
          width: 28px; height: 28px;
          border-radius: 50%;
          background: ${color};
          border: 3px solid #fff;
          box-shadow: 0 2px 6px rgba(0,0,0,0.3);
          display: flex; align-items: center; justify-content: center;
          font-size: 14px;
        ">📡</div>`,
        iconSize: [28, 28],
        iconAnchor: [14, 14],
      });
      const marker = L.marker(wgs84ToGcj02(r.lat, r.lng), { icon });
      marker.bindPopup(`
        <div style="min-width: 160px;">
          <h3 style="margin: 0 0 8px; font-size: 14px;">${r.stationName}</h3>
          <p style="margin: 4px 0; font-size: 12px;">
            <strong>状态:</strong>
            <span style="color: ${color}; font-weight: 600;">
              ${level === 'normal' ? '正常' : level === 'warning' ? '警告' : '危险'}
            </span>
          </p>
          <p style="margin: 4px 0; font-size: 12px;"><strong>指标:</strong> ${r.metric}</p>
          <p style="margin: 4px 0; font-size: 12px;"><strong>数值:</strong> ${r.value} ${r.unit}</p>
          <p style="margin: 4px 0; font-size: 12px; color: #666;">${r.ts}</p>
        </div>
      `);
      envGroupRef.current.addLayer(marker);
    });

    // Water station markers
    waterStations.forEach((w) => {
      // 坐标缺失时跳过该标记，避免访问 undefined 导致整页渲染崩溃
      const [lat, lng] = w.position ?? [];
      if (lat == null || lng == null) return;
      const level: Level =
        w.currentLevel >= w.dangerLevel
          ? 'danger'
          : w.currentLevel >= w.warningLevel
            ? 'warning'
            : 'normal';
      const color = LEVEL_COLORS[level];
      const icon = L.divIcon({
        className: 'custom-marker',
        html: `<div style="
          width: 28px; height: 28px;
          border-radius: 50%;
          background: ${color};
          border: 3px solid #fff;
          box-shadow: 0 2px 6px rgba(0,0,0,0.3);
          display: flex; align-items: center; justify-content: center;
          font-size: 14px;
        ">💧</div>`,
        iconSize: [28, 28],
        iconAnchor: [14, 14],
      });
      const marker = L.marker(wgs84ToGcj02(lat, lng), { icon });
      marker.bindPopup(`
        <div style="min-width: 160px;">
          <h3 style="margin: 0 0 8px; font-size: 14px;">${w.name}</h3>
          <p style="margin: 4px 0; font-size: 12px;"><strong>当前水位:</strong> ${w.currentLevel} m</p>
          <p style="margin: 4px 0; font-size: 12px;"><strong>警戒水位:</strong> ${w.warningLevel} m</p>
          <p style="margin: 4px 0; font-size: 12px;"><strong>危险水位:</strong> ${w.dangerLevel} m</p>
        </div>
      `);
      waterGroupRef.current.addLayer(marker);
    });
  }, [buildings, environmentRecords, waterStations]);

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

export default CityMap;
