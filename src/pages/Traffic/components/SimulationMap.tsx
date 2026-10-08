import React, { useEffect, useRef, useState, useCallback } from 'react';
import { Card, Tag, Empty, Spin } from 'antd';
import { EnvironmentOutlined } from '@ant-design/icons';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { createBaseTileLayer, wgs84ToGcj02 } from '@/utils/mapTiles';
import type { Intersection } from '@/types';

interface SimulationMapProps {
  intersections: Intersection[];
  selectedIntersection?: Intersection | null;
  onIntersectionClick?: (intersection: Intersection) => void;
  loading?: boolean;
  /** 卡片固定高度（px）。地图容器依赖确定高度渲染，默认 420 */
  height?: number;
}

const congestionColors: Record<string, string> = {
  smooth: '#52c41a',
  moderate: '#fadb14',
  congested: '#fa8c16',
  severe: '#ff4d4f',
};

const congestionLabels: Record<string, string> = {
  smooth: '畅通',
  moderate: '缓行',
  congested: '拥堵',
  severe: '严重拥堵',
};

const SimulationMap: React.FC<SimulationMapProps> = ({
  intersections,
  selectedIntersection,
  onIntersectionClick,
  loading = false,
  height = 420,
}) => {
  const mapRef = useRef<L.Map | null>(null);
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const markersRef = useRef<L.LayerGroup | null>(null);
  const particlesRef = useRef<L.LayerGroup | null>(null);
  const [mapReady, setMapReady] = useState(false);

  // Default center (Nanjing city center，初始优先显示南京地区)
  const defaultCenter: [number, number] = [32.06, 118.79];
  const defaultZoom = 12;

  // Initialize map
  useEffect(() => {
    if (!mapContainerRef.current || mapRef.current) return;

    const map = L.map(mapContainerRef.current, {
      center: wgs84ToGcj02(defaultCenter[0], defaultCenter[1]),
      zoom: defaultZoom,
      zoomControl: true,
      attributionControl: true,
    });

    createBaseTileLayer().addTo(map);

    markersRef.current = L.layerGroup().addTo(map);
    particlesRef.current = L.layerGroup().addTo(map);

    mapRef.current = map;
    setMapReady(true);

    return () => {
      map.remove();
      mapRef.current = null;
      setMapReady(false);
    };
  }, []);

  // Create custom icon
  const createCustomIcon = useCallback((level: string, isSelected: boolean) => {
    const color = congestionColors[level];
    const size = isSelected ? 36 : 28;
    const borderWidth = isSelected ? 3 : 2;

    return L.divIcon({
      className: 'custom-intersection-marker',
      html: `
        <div style="
          width: ${size}px;
          height: ${size}px;
          border-radius: 50%;
          background: ${color};
          border: ${borderWidth}px solid ${isSelected ? '#1890ff' : '#fff'};
          box-shadow: 0 2px 8px rgba(0,0,0,0.3);
          display: flex;
          align-items: center;
          justify-content: center;
          color: #fff;
          font-size: 10px;
          font-weight: bold;
          cursor: pointer;
          transition: all 0.3s ease;
        ">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
          </svg>
        </div>
      `,
      iconSize: [size, size],
      iconAnchor: [size / 2, size / 2],
    });
  }, []);

  // Update markers when intersections change
  useEffect(() => {
    if (!mapReady || !markersRef.current) return;

    markersRef.current.clearLayers();

    intersections.forEach((intersection) => {
      const isSelected = selectedIntersection?.id === intersection.id;
      const icon = createCustomIcon(intersection.congestionLevel ?? 'smooth', isSelected);

      const marker = L.marker(
        wgs84ToGcj02(
          intersection.lat ?? 32.06,
          intersection.lng ?? 118.79,
        ),
        { icon },
      ).addTo(markersRef.current!);

      marker.bindTooltip(
        `<div style="padding: 4px 8px;">
          <div style="font-weight: 500;">${intersection.name}</div>
          <div style="font-size: 11px; color: #666;">${congestionLabels[intersection.congestionLevel ?? 'smooth']}</div>
        </div>`,
        { direction: 'top', offset: [0, -20] }
      );

      marker.on('click', () => {
        onIntersectionClick?.(intersection);
      });
    });
  }, [intersections, selectedIntersection, mapReady, createCustomIcon, onIntersectionClick]);

  // Animate flow particles along roads
  useEffect(() => {
    if (!mapReady || !particlesRef.current || intersections.length === 0) return;

    particlesRef.current.clearLayers();

    // Create animated particles between intersections
    const particleCount = 3;
    const particles: L.CircleMarker[] = [];
    const particlePaths: [number, number][][] = [];

    for (let i = 0; i < intersections.length - 1; i++) {
      const start = intersections[i];
      const end = intersections[i + 1];
      const path: [number, number][] = [];

      // Create a simple curved path
      const steps = 20;
      for (let j = 0; j <= steps; j++) {
        const t = j / steps;
        const lat = (start.lat ?? 32.06) + ((end.lat ?? 32.06) - (start.lat ?? 32.06)) * t;
        const lng = (start.lng ?? 118.79) + ((end.lng ?? 118.79) - (start.lng ?? 118.79)) * t;
        path.push(wgs84ToGcj02(lat, lng));
      }
      particlePaths.push(path);

      // Create particles for this path
      for (let k = 0; k < particleCount; k++) {
        const particle = L.circleMarker(path[0], {
          radius: 3,
          color: '#1890ff',
          fillColor: '#1890ff',
          fillOpacity: 0.8,
          weight: 1,
        }).addTo(particlesRef.current!);
        particles.push(particle);
      }
    }

    // Animate particles
    let animationId: number;
    let progress = 0;

    const animate = () => {
      progress += 0.005;
      if (progress > 1) progress = 0;

      particles.forEach((particle, index) => {
        const pathIndex = Math.floor(index / particleCount);
        const path = particlePaths[pathIndex];
        if (path && path.length > 0) {
          const pointIndex = Math.floor(progress * (path.length - 1));
          const point = path[pointIndex];
          if (point) {
            particle.setLatLng(point);
          }
        }
      });

      animationId = requestAnimationFrame(animate);
    };

    animate();

    return () => {
      cancelAnimationFrame(animationId);
    };
  }, [mapReady, intersections]);

  // Fly to selected intersection
  useEffect(() => {
    if (!mapReady || !mapRef.current || !selectedIntersection) return;

    mapRef.current.flyTo(
      wgs84ToGcj02(
        selectedIntersection.lat ?? 32.06,
        selectedIntersection.lng ?? 118.79,
      ),
      15,
      { duration: 1 }
    );
  }, [selectedIntersection, mapReady]);

  return (
    <Card
      title={
        <span>
          <EnvironmentOutlined style={{ marginRight: 8, color: '#1890ff' }} />
          交通仿真地图
        </span>
      }
      size="small"
      extra={
        <div style={{ display: 'flex', gap: 8 }}>
          {Object.entries(congestionLabels).map(([level, label]) => (
            <Tag
              key={level}
              color={congestionColors[level as string]}
              style={{ marginInlineEnd: 0 }}
            >
              {label}
            </Tag>
          ))}
        </div>
      }
      style={{ width: '100%', height }}
      bodyStyle={{ padding: 0, height: 'calc(100% - 40px)' }}
    >
      <div style={{ position: 'relative', height: '100%' }}>
        <div
          ref={mapContainerRef}
          style={{ width: '100%', height: '100%', zIndex: 1 }}
        />
        {loading && (
          <div
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              background: 'rgba(255,255,255,0.8)',
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              zIndex: 1000,
            }}
          >
            <Spin size="large" tip="加载地图数据..." />
          </div>
        )}
        {intersections.length === 0 && !loading && (
          <div
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              zIndex: 1000,
              background: '#fafafa',
            }}
          >
            <Empty description="暂无路口数据" />
          </div>
        )}
        {selectedIntersection && (
          <div
            style={{
              position: 'absolute',
              bottom: 16,
              left: 16,
              zIndex: 1000,
              background: '#fff',
              padding: 12,
              borderRadius: 8,
              boxShadow: '0 2px 12px rgba(0,0,0,0.15)',
              maxWidth: 280,
            }}
          >
            <div style={{ fontWeight: 500, marginBottom: 4 }}>
              {selectedIntersection.name}
            </div>
            <div style={{ fontSize: 12, color: '#8c8c8c', marginBottom: 8 }}>
              {selectedIntersection.area}
            </div>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <Tag color={congestionColors[selectedIntersection.congestionLevel ?? 'smooth']}>
                {congestionLabels[selectedIntersection.congestionLevel ?? 'smooth']}
              </Tag>
              <span style={{ fontSize: 12, color: '#8c8c8c' }}>
                流量: {selectedIntersection.currentFlow} 辆/h
              </span>
            </div>
          </div>
        )}
      </div>
    </Card>
  );
};

export default SimulationMap;
