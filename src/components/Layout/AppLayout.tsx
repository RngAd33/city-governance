import React, { Suspense, useState } from 'react';
import { Layout, Menu, Row, Col, Skeleton, theme } from 'antd';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import ErrorBoundary from '@/components/Common/ErrorBoundary';
import {
  DashboardOutlined,
  EnvironmentOutlined,
  ThunderboltOutlined,
  CarOutlined,
  AlertOutlined,
  RobotOutlined,
  BarChartOutlined,
  SettingOutlined,
  CloudOutlined,
} from '@ant-design/icons';

const { Header, Sider, Content } = Layout;

const menuItems = [
  { key: '/', icon: <DashboardOutlined />, label: '城市运行总览' },
  { key: '/monitor/environment', icon: <EnvironmentOutlined />, label: '环境监测' },
  { key: '/monitor/energy', icon: <ThunderboltOutlined />, label: '能源与建筑节能' },
  { key: '/traffic/simulation', icon: <CarOutlined />, label: '交通信号优化' },
  { key: '/water/risk', icon: <CloudOutlined />, label: '水务与内涝风险' },
  { key: '/disaster/simulation', icon: <AlertOutlined />, label: '灾害模拟' },
  { key: '/infra/ops', icon: <SettingOutlined />, label: '基础设施运维' },
  { key: '/ai-assistant', icon: <RobotOutlined />, label: '政务AI助手' },
  { key: '/reports', icon: <BarChartOutlined />, label: '报告中心' },
];

// 内容区路由懒加载骨架：先给出页面形状（标题 + 卡片网格），chunk 就绪后自然替换
const PageContentSkeleton: React.FC = () => (
  <div>
    <Skeleton.Input active style={{ width: 200, height: 28, marginBottom: 24, display: 'block' }} />
    <Row gutter={[16, 16]}>
      {[0, 1, 2, 3].map((i) => (
        <Col xs={24} sm={12} lg={6} key={i}>
          <Skeleton.Node active style={{ width: '100%', height: 120 }} />
        </Col>
      ))}
    </Row>
    <div style={{ marginTop: 16 }}>
      <Skeleton.Node active style={{ width: '100%', height: 320 }} />
    </div>
  </div>
);

const AppLayout: React.FC = () => {
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  
  

  const {
    token: { colorBgContainer, borderRadiusLG },
  } = theme.useToken();

  return (
    // 整体固定为整屏高度，滚动收敛到内容区，侧边栏/顶栏不随页面滚动
    <Layout style={{ height: '100vh' }}>
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        width={220}
        style={{ background: '#001529', overflow: 'auto' }}
      >
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#fff',
            fontSize: collapsed ? 14 : 16,
            fontWeight: 'bold',
            padding: '0 16px',
            whiteSpace: 'nowrap',
            overflow: 'hidden',
          }}
        >
          {collapsed ? 'CGP' : '城市治理平台'}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            padding: '0 24px',
            background: colorBgContainer,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            boxShadow: '0 2px 8px rgba(0,21,41,0.08)',
            zIndex: 1,
          }}
        >
          <div style={{ fontSize: 18, fontWeight: 600 }}>
            {menuItems.find((item) => item.key === location.pathname)?.label || '城市治理平台'}
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <span style={{ color: '#666' }}>南京</span>
          </div>
        </Header>
        <Content
          style={{
            margin: 24,
            padding: 24,
            background: colorBgContainer,
            borderRadius: borderRadiusLG,
            minHeight: 360,
            overflow: 'auto',
          }}
        >
          {/* 路由懒加载的 Suspense 边界：侧边栏保持可见，内容区以骨架占位 */}
          <Suspense fallback={<PageContentSkeleton />}>
            {/* 错误边界：页面级异常只影响内容区，切换菜单即自动重置 */}
            <ErrorBoundary key={location.pathname}>
              <Outlet />
            </ErrorBoundary>
          </Suspense>
        </Content>
      </Layout>
    </Layout>
  );
};

export default AppLayout;
