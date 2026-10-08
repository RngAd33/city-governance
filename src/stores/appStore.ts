import { create } from 'zustand';
import type { Intersection, SimulationResult } from '../types';

export type TimeRange = '24h' | '7d' | '30d';
export type Theme = 'light' | 'dark';

export interface AlertThresholds {
  waterLevel: number;
  rainfall: number;
  windSpeed: number;
}

export interface TrafficState {
  selectedIntersection: Intersection | null;
  intersections: Intersection[];
  strategy: string;
  simulationProgress: number;
  simulationResult: SimulationResult | null;
}

export interface AppState {
  city: string;
  setCity: (city: string) => void;

  timeRange: TimeRange;
  setTimeRange: (range: TimeRange) => void;

  theme: Theme;
  setTheme: (theme: Theme) => void;

  refreshInterval: number;
  setRefreshInterval: (interval: number) => void;

  alertThresholds: AlertThresholds;
  setAlertThresholds: (thresholds: AlertThresholds) => void;

  // 交通模块结构化状态
  traffic: TrafficState;
  setSelectedIntersection: (intersection: Intersection | null) => void;
  setIntersections: (intersections: Intersection[]) => void;
  setStrategy: (strategy: string) => void;
  setSimulationProgress: (progress: number) => void;
  setSimulationResult: (result: SimulationResult | null) => void;
  resetSimulation: () => void;
}

const initialTraffic: TrafficState = {
  selectedIntersection: null,
  intersections: [],
  strategy: 'genetic',
  simulationProgress: 0,
  simulationResult: null,
};

const useAppStore = create<AppState>((set) => ({
  city: '南京市',
  setCity: (city) => set({ city }),

  timeRange: '24h',
  setTimeRange: (range) => set({ timeRange: range }),

  theme: 'light',
  setTheme: (theme) => set({ theme }),

  refreshInterval: 30,
  setRefreshInterval: (interval) => set({ refreshInterval: interval }),

  alertThresholds: {
    waterLevel: 3.0,
    rainfall: 50,
    windSpeed: 17,
  },
  setAlertThresholds: (thresholds) => set({ alertThresholds: thresholds }),

  // 交通模块
  traffic: initialTraffic,
  setSelectedIntersection: (intersection) =>
    set((state) => ({ traffic: { ...state.traffic, selectedIntersection: intersection } })),
  setIntersections: (intersections) =>
    set((state) => ({ traffic: { ...state.traffic, intersections } })),
  setStrategy: (strategy) =>
    set((state) => ({ traffic: { ...state.traffic, strategy } })),
  setSimulationProgress: (progress) =>
    set((state) => ({ traffic: { ...state.traffic, simulationProgress: progress } })),
  setSimulationResult: (result) =>
    set((state) => ({ traffic: { ...state.traffic, simulationResult: result } })),
  resetSimulation: () =>
    set((state) => ({
      traffic: {
        ...state.traffic,
        simulationProgress: 0,
        simulationResult: null,
      },
    })),
}));

export default useAppStore;
