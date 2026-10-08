import React, { useEffect, useRef } from 'react';
import { Card } from 'antd';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { createBaseTileLayer, wgs84ToGcj02 } from '@/utils/mapTiles';

interface TrafficFlowMapProps {
  title?: string;
  height?: number;
  center?: [number, number];
  zoom?: number;
  segments?: Array<{
    id: string;
    path: Array<[number, number]>;
    congestionLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
    flow: number;
    speed: number;
  }>;
  intersections?: Array<{
    id: string;
    position: [number, number];
    name: string;
    phases: Array<{ direction: string; greenTime: number; yellowTime: number; redTime: number }>;
  }>;
}

const congestionColors: Record<string, string> = {
  LOW: '#52c41a',
  MEDIUM: '#faad14',
  HIGH: '#ff7a45',
  CRITICAL: '#ff4d4f',
};

const TrafficFlowMap: React.FC<TrafficFlowMapProps> = ({
  title,
  height = 400,
  center = [32.06, 118.79],
  zoom = 13,
  segments = [],
  intersections = [],
}) => {
  const mapRef = useRef<HTMLDivElement>(null);
  const mapInstance = useRef<L.Map | null>(null);
  const layersRef = useRef<L.LayerGroup | null>(null);

  useEffect(() => {
    if (!mapRef.current) return;

    mapInstance.current = L.map(mapRef.current).setView(
      wgs84ToGcj02(center[0], center[1]),
      zoom,
    );

    createBaseTileLayer().addTo(mapInstance.current);

    layersRef.current = L.layerGroup().addTo(mapInstance.current);

    return () => {
      mapInstance.current?.remove();
    };
  }, []);

  useEffect(() => {
    if (!layersRef.current) return;
    layersRef.current.clearLayers();

    // Draw road segments
    segments.forEach((segment) => {
      const color = congestionColors[segment.congestionLevel] || '#1890ff';
      const weight = segment.congestionLevel === 'CRITICAL' ? 6 : segment.congestionLevel === 'HIGH' ? 5 : 4;

      L.polyline(
        segment.path.map(([lat, lng]) => wgs84ToGcj02(lat, lng)),
        {
          color,
          weight,
          opacity: 0.8,
        },
      )
        .bindPopup(`
          <div>
            <strong>路段: ${segment.id}</strong><br/>
            流量: ${segment.flow} 辆/小时<br/>
            车速: ${segment.speed} km/h<br/>
            拥堵: ${segment.congestionLevel}
          </div>
        `)
        .addTo(layersRef.current!);
    });

    // Draw intersections
    intersections.forEach((intersection) => {
      const icon = L.divIcon({
        className: 'intersection-marker',
        html: `<div style="width:16px;height:16px;border-radius:50%;background:#1890ff;border:3px solid #fff;box-shadow:0 0 6px rgba(0,0,0,0.4)"></div>`,
        iconSize: [16, 16],
        iconAnchor: [8, 8],
      });

      L.marker(wgs84ToGcj02(intersection.position[0], intersection.position[1]), {
        icon,
      })
        .bindPopup(`
          <div>
            <strong>${intersection.name}</strong><br/>
            相位: ${intersection.phases.map((p) => `${p.direction}(${p.greenTime}s)`).join(', ')}
          </div>
        `)
        .addTo(layersRef.current!);
    });
  }, [segments, intersections]);

  return (
    <Card title={title} bodyStyle={{ padding: 0 }} style={{ borderRadius: 8, overflow: 'hidden' }}>
      <div ref={mapRef} style={{ width: '100%', height }} />
    </Card>
  );
};

export default TrafficFlowMap;
