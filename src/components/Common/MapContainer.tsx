import React, { useEffect, useRef } from 'react';
import { Card } from 'antd';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { createBaseTileLayer, wgs84ToGcj02 } from '@/utils/mapTiles';

interface MapContainerProps {
  title?: string;
  height?: number;
  center?: [number, number];
  zoom?: number;
  markers?: Array<{
    position: [number, number];
    color?: string;
    label?: string;
    popup?: string;
  }>;
  onMapReady?: (map: L.Map) => void;
}

const MapContainer: React.FC<MapContainerProps> = ({
  title,
  height = 400,
  center = [32.06, 118.79],
  zoom = 12,
  markers = [],
  onMapReady,
}) => {
  const mapRef = useRef<HTMLDivElement>(null);
  const mapInstance = useRef<L.Map | null>(null);
  const markersLayer = useRef<L.LayerGroup | null>(null);

  useEffect(() => {
    if (!mapRef.current) return;

    mapInstance.current = L.map(mapRef.current).setView(
      wgs84ToGcj02(center[0], center[1]),
      zoom,
    );

    createBaseTileLayer().addTo(mapInstance.current);

    markersLayer.current = L.layerGroup().addTo(mapInstance.current);

    if (onMapReady) {
      onMapReady(mapInstance.current);
    }

    return () => {
      mapInstance.current?.remove();
    };
  }, []);

  useEffect(() => {
    if (!markersLayer.current || !mapInstance.current) return;

    markersLayer.current.clearLayers();

    markers.forEach((marker) => {
      const color = marker.color || '#1890ff';
      const icon = L.divIcon({
        className: 'custom-marker',
        html: `<div style="width:12px;height:12px;border-radius:50%;background:${color};border:2px solid #fff;box-shadow:0 0 4px rgba(0,0,0,0.3)"></div>`,
        iconSize: [12, 12],
        iconAnchor: [6, 6],
      });

      const leafletMarker = L.marker(
        wgs84ToGcj02(marker.position[0], marker.position[1]),
        { icon },
      ).addTo(markersLayer.current!);

      if (marker.popup) {
        leafletMarker.bindPopup(marker.popup);
      }

      if (marker.label) {
        leafletMarker.bindTooltip(marker.label, { permanent: false });
      }
    });
  }, [markers]);

  return (
    <Card title={title} bodyStyle={{ padding: 0 }} style={{ borderRadius: 8, overflow: 'hidden' }}>
      <div ref={mapRef} style={{ width: '100%', height }} />
    </Card>
  );
};

export default MapContainer;
