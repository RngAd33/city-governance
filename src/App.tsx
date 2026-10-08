import { lazy } from 'react';
import { Routes, Route } from 'react-router-dom';
import AppLayout from './components/Layout/AppLayout';

// 路由级代码分割：页面按需加载，首屏不再打包全部页面及其重型依赖
const Dashboard = lazy(() => import('./pages/Dashboard'));
const Environment = lazy(() => import('./pages/Environment'));
const Energy = lazy(() => import('./pages/Energy'));
const Traffic = lazy(() => import('./pages/Traffic'));
const WaterRisk = lazy(() => import('./pages/Water'));
const DisasterSimulation = lazy(() => import('./pages/Disaster'));
const GovAIAssistant = lazy(() => import('./pages/AIAssistant'));
const InfrastructureOps = lazy(() => import('./pages/Infrastructure'));
const ReportCenter = lazy(() => import('./pages/Reports'));
const SystemSettings = lazy(() => import('./pages/Settings'));

function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route path="/" element={<Dashboard />} />
        <Route path="/monitor/environment" element={<Environment />} />
        <Route path="/monitor/energy" element={<Energy />} />
        <Route path="/traffic/simulation" element={<Traffic />} />
        <Route path="/water/risk" element={<WaterRisk />} />
        <Route path="/disaster/simulation" element={<DisasterSimulation />} />
        <Route path="/infra/ops" element={<InfrastructureOps />} />
        <Route path="/ai-assistant" element={<GovAIAssistant />} />
        <Route path="/reports" element={<ReportCenter />} />
        <Route path="/settings" element={<SystemSettings />} />
      </Route>
    </Routes>
  );
}

export default App;
