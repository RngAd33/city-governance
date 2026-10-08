// ============================================================
// StationMap - Leaflet map with environment stations
// ============================================================
import React, { useEffect, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { createBaseTileLayer, wgs84ToGcj02 } from '@/utils/mapTiles';
import type { EnvironmentRecord } from '@/types';

const LEVEL_COLORS: Record<string, string> = {
  normal: '#52c41a',
  warning: '#faad14',
  danger: '#ff4d4f',
};

interface StationMapProps {
  stations: EnvironmentRecord[];
  center?: [number, number];
  zoom?: number;
}

const StationMap: React.FC<StationMapProps> = ({
  stations = [],
  center = [32.06, 118.79],
  zoom = 12,
}) => {
  const mapRef = useRef<L.Map | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const layerGroupRef = useRef<L.LayerGroup>(L.layerGroup());

  useEffect(() => {
    if (!containerRef.current || mapRef.current) return;

    const map = L.map(containerRef.current, {
      center: wgs84ToGcj02(center[0], center[1]),
      zoom,
      zoomControl: true,
      attributionControl: true,
    });

    createBaseTileLayer().addTo(map);

    layerGroupRef.current.addTo(map);
    mapRef.current = map;

    return () => {
      map.remove();
      mapRef.current = null;
    };
  }, []);

  useEffect(() => {
    if (!mapRef.current) return;

    layerGroupRef.current.clearLayers();

    stations.forEach((station) => {
      const level = (station.level || '').toLowerCase();
      const color = LEVEL_COLORS[level] || '#1890ff';
      const icon = L.divIcon({
        className: 'custom-marker',
        html: `<div style="
          width: 32px; height: 32px;
          border-radius: 50%;
          background: ${color};
          border: 3px solid #fff;
          box-shadow: 0 2px 8px rgba(0,0,0,0.3);
          display: flex; align-items: center; justify-content: center;
          font-size: 11px; color: #fff; font-weight: bold;
        ">${station.value}</div>`,
        iconSize: [32, 32],
        iconAnchor: [16, 16],
      });

      const marker = L.marker(wgs84ToGcj02(station.lat, station.lng), { icon });
      marker.bindPopup(`
        <div style="min-width: 180px;">
          <h3 style="margin: 0 0 8px; font-size: 14px;">${station.stationName}</h3>
          <p style="margin: 4px 0; font-size: 12px;">
            <strong>状态:</strong>
            <span style="color: ${color}; font-weight: 600;">
              ${level === 'normal' ? '正常' : level === 'warning' ? '警告' : '危险'}
            </span>
          </p>
          <p style="margin: 4px 0; font-size: 12px;"><strong>指标:</strong> ${station.metric}</p>
          <p style="margin: 4px 0; font-size: 12px;"><strong>数值:</strong> ${station.value} ${station.unit}</p>
          <p style="margin: 4px 0; font-size: 12px; color: #666;">${station.ts}</p>
        </div>
      `);
      layerGroupRef.current.addLayer(marker);
    });
  }, [stations]);

  return (
    <div
      ref={containerRef}
      style={{
        width: '100%',
        height: '100%',
        minHeight: 350,
        borderRadius: 8,
        overflow: 'hidden',
        zIndex: 0,
      }}
    />
  );
};

export default StationMap;
