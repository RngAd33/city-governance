// ============================================================
// RiskMap - Leaflet map with water station markers, flood zones,
// and drainage network overlay
// ============================================================
import React, { useEffect, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { createBaseTileLayer, wgs84ToGcj02 } from '@/utils/mapTiles';
import type { WaterStation, FloodZone, DrainageNode, Level } from '@/types';

const LEVEL_COLORS: Record<Level, string> = {
  normal: '#52c41a',
  warning: '#faad14',
  danger: '#ff4d4f',
  critical: '#a8071a',
};

interface RiskMapProps {
  stations: WaterStation[];
  floodZones: FloodZone[];
  drainageNodes: DrainageNode[];
  selectedStation: string;
  onStationSelect: (stationId: string) => void;
  center?: [number, number];
  zoom?: number;
}

const RiskMap: React.FC<RiskMapProps> = ({
  stations,
  floodZones,
  drainageNodes,
  selectedStation,
  onStationSelect,
  center = [32.06, 118.79],
  zoom = 12,
}) => {
  const mapRef = useRef<L.Map | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const floodLayerRef = useRef<L.LayerGroup | null>(null);
  const stationLayerRef = useRef<L.LayerGroup | null>(null);
  const drainageLayerRef = useRef<L.LayerGroup | null>(null);

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
    stationLayerRef.current = L.layerGroup().addTo(map);
    drainageLayerRef.current = L.layerGroup().addTo(map);

    const overlays: Record<string, L.LayerGroup> = {
      '洪涝区域': floodLayerRef.current,
      '水位站': stationLayerRef.current,
      '排水管网': drainageLayerRef.current,
    };
    L.control.layers(undefined, overlays, { position: 'topright' }).addTo(map);

    mapRef.current = map;

    return () => {
      map.remove();
      mapRef.current = null;
    };
  }, []);

  // Update flood zones
  useEffect(() => {
    if (!floodLayerRef.current) return;
    floodLayerRef.current.clearLayers();

    floodZones.forEach((zone) => {
      const color = LEVEL_COLORS[zone.riskLevel];
      const polygon = L.polygon(
        zone.coordinates.map(([lat, lng]) => wgs84ToGcj02(lat, lng)),
        {
          color,
          fillColor: color,
          fillOpacity: 0.2,
          weight: 2,
          dashArray: zone.riskLevel === 'warning' ? '5, 5' : undefined,
        },
      );

      polygon.bindPopup(`
        <div style="min-width: 180px;">
          <h3 style="margin: 0 0 8px; font-size: 14px;">${zone.name}</h3>
          <p style="margin: 4px 0; font-size: 12px;">
            <strong>风险等级:</strong>
            <span style="color: ${color}; font-weight: 600;">
              ${zone.riskLevel === 'normal' ? '低风险' : zone.riskLevel === 'warning' ? '中风险' : zone.riskLevel === 'danger' ? '高风险' : '极高风险'}
            </span>
          </p>
          <p style="margin: 4px 0; font-size: 12px;"><strong>面积:</strong> ${zone.area} km²</p>
          <p style="margin: 4px 0; font-size: 12px;"><strong>影响人口:</strong> ${zone.population} 人</p>
        </div>
      `);

      floodLayerRef.current!.addLayer(polygon);
    });
  }, [floodZones]);

  // Update station markers
  useEffect(() => {
    if (!stationLayerRef.current) return;
    stationLayerRef.current.clearLayers();

    stations.forEach((station) => {
      const color = LEVEL_COLORS[station.status];
      const isSelected = station.id === selectedStation;
      const icon = L.divIcon({
        className: 'custom-marker',
        html: `<div style="
          width: ${isSelected ? 36 : 28}px; height: ${isSelected ? 36 : 28}px;
          border-radius: 50%;
          background: ${color};
          border: 3px solid ${isSelected ? '#1890ff' : '#fff'};
          box-shadow: 0 2px 6px rgba(0,0,0,0.3);
          display: flex; align-items: center; justify-content: center;
          font-size: ${isSelected ? 16 : 14}px;
          transition: all 0.2s;
        ">💧</div>`,
        iconSize: [isSelected ? 36 : 28, isSelected ? 36 : 28],
        iconAnchor: [isSelected ? 18 : 14, isSelected ? 18 : 14],
      });

      const marker = L.marker(
        wgs84ToGcj02(station.position[0], station.position[1]),
        { icon },
      );

      marker.bindPopup(`
        <div style="min-width: 200px;">
          <h3 style="margin: 0 0 8px; font-size: 14px;">${station.name}</h3>
          <p style="margin: 4px 0; font-size: 12px;">
            <strong>当前水位:</strong> <span style="color: ${color}; font-weight: 600;">${station.currentLevel} m</span>
          </p>
          <p style="margin: 4px 0; font-size: 12px;"><strong>警戒水位:</strong> ${station.warningLevel} m</p>
          <p style="margin: 4px 0; font-size: 12px;"><strong>危险水位:</strong> ${station.dangerLevel} m</p>
          <p style="margin: 4px 0; font-size: 12px;">
            <strong>趋势:</strong>
            ${station.trend === 'rising' ? '↑ 上升' : station.trend === 'falling' ? '↓ 下降' : '→ 平稳'}
          </p>
          <p style="margin: 4px 0; font-size: 12px;"><strong>更新时间:</strong> ${station.updatedAt}</p>
        </div>
      `);

      marker.on('click', () => onStationSelect(station.id));
      stationLayerRef.current!.addLayer(marker);
    });
  }, [stations, selectedStation, onStationSelect]);

  // Update drainage network
  useEffect(() => {
    if (!drainageLayerRef.current) return;
    drainageLayerRef.current.clearLayers();

    drainageNodes.forEach((node) => {
      const color = LEVEL_COLORS[node.status];
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
        ">🔧</div>`,
        iconSize: [20, 20],
        iconAnchor: [10, 10],
      });

      const marker = L.marker(
        wgs84ToGcj02(node.position[0], node.position[1]),
        { icon },
      );

      marker.bindPopup(`
        <div style="min-width: 180px;">
          <h3 style="margin: 0 0 8px; font-size: 14px;">${node.name}</h3>
          <p style="margin: 4px 0; font-size: 12px;"><strong>容量:</strong> ${node.capacity} m³/s</p>
          <p style="margin: 4px 0; font-size: 12px;"><strong>当前流量:</strong> ${node.currentFlow} m³/s</p>
          <p style="margin: 4px 0; font-size: 12px;">
            <strong>状态:</strong>
            <span style="color: ${color}; font-weight: 600;">
              ${node.status === 'normal' ? '正常' : node.status === 'warning' ? '警告' : node.status === 'danger' ? '危险' : '严重'}
            </span>
          </p>
        </div>
      `);

      drainageLayerRef.current!.addLayer(marker);
    });
  }, [drainageNodes]);

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

export default RiskMap;
