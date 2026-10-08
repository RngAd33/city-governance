// ============================================================
// SystemSettings - System configuration page
// ============================================================
import React, { useState } from 'react';
import { Card, Form, Select, InputNumber, Switch, Button, Row, Col, Divider, message, Space, Tag, Alert } from 'antd';
import {
  SaveOutlined,
  ReloadOutlined,
  BulbOutlined,
  ClockCircleOutlined,
  WarningOutlined,
  EnvironmentOutlined,
} from '@ant-design/icons';
import useAppStore from '@/stores/appStore';



const SystemSettings: React.FC = () => {
  const { city, timeRange, theme, refreshInterval, alertThresholds, setCity, setTimeRange, setTheme, setRefreshInterval, setAlertThresholds } = useAppStore();
  const [form] = Form.useForm();
  const [localTheme, setLocalTheme] = useState(theme);

  const handleSave = () => {
    message.success('设置已保存');
  };

  const handleReset = () => {
    form.resetFields();
    message.info('已重置为默认值');
  };

  const handleThemeChange = (checked: boolean) => {
    const newTheme = checked ? 'dark' : 'light';
    setLocalTheme(newTheme);
    setTheme(newTheme);
  };

  return (
    <div style={{ padding: 24, background: '#f0f2f5', minHeight: '100%' }}>
      {/* Header */}
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h2 style={{ margin: 0, fontSize: 20, fontWeight: 600 }}>系统设置</h2>
          <span style={{ color: '#8c8c8c', fontSize: 13 }}>配置系统参数和个性化选项</span>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={handleReset}>重置</Button>
          <Button type="primary" icon={<SaveOutlined />} onClick={handleSave}>保存设置</Button>
        </Space>
      </div>

      <Alert
        message="设置更改后需要点击&quot;保存设置&quot;按钮才会生效"
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
      />

      <Row gutter={[16, 16]}>
        {/* City Selector */}
        <Col xs={24} lg={12}>
          <Card title={<span><EnvironmentOutlined style={{ marginRight: 8 }} />城市选择</span>} size="small">
            <Form form={form} layout="vertical" initialValues={{ city }}>
              <Form.Item
                name="city"
                label="当前城市"
                rules={[{ required: true, message: '请选择城市' }]}
              >
                <Select
                  value={city}
                  onChange={setCity}
                  options={[
                    { label: '上海市', value: '上海市' },
                    { label: '北京市', value: '北京市' },
                    { label: '广州市', value: '广州市' },
                    { label: '深圳市', value: '深圳市' },
                    { label: '杭州市', value: '杭州市' },
                    { label: '南京市', value: '南京市' },
                    { label: '成都市', value: '成都市' },
                    { label: '武汉市', value: '武汉市' },
                  ]}
                />
              </Form.Item>
              <div style={{ fontSize: 12, color: '#8c8c8c', marginTop: 8 }}>
                选择城市后，系统将加载该城市的数据和配置
              </div>
            </Form>
          </Card>
        </Col>

        {/* Time Range Default */}
        <Col xs={24} lg={12}>
          <Card title={<span><ClockCircleOutlined style={{ marginRight: 8 }} />时间范围</span>} size="small">
            <Form form={form} layout="vertical" initialValues={{ timeRange }}>
              <Form.Item
                name="timeRange"
                label="默认时间范围"
                rules={[{ required: true, message: '请选择默认时间范围' }]}
              >
                <Select
                  value={timeRange}
                  onChange={(v) => setTimeRange(v as '24h' | '7d' | '30d')}
                  options={[
                    { label: '最近24小时', value: '24h' },
                    { label: '最近7天', value: '7d' },
                    { label: '最近30天', value: '30d' },
                  ]}
                />
              </Form.Item>
              <div style={{ fontSize: 12, color: '#8c8c8c', marginTop: 8 }}>
                设置图表和数据的默认时间范围
              </div>
            </Form>
          </Card>
        </Col>

        {/* Alert Thresholds */}
        <Col xs={24} lg={12}>
          <Card title={<span><WarningOutlined style={{ marginRight: 8 }} />告警阈值配置</span>} size="small">
            <Form form={form} layout="vertical" initialValues={alertThresholds}>
              <Form.Item
                name="waterLevel"
                label="水位告警阈值 (m)"
                rules={[{ required: true, message: '请输入水位告警阈值' }]}
              >
                <InputNumber
                  min={0}
                  max={10}
                  step={0.1}
                  value={alertThresholds.waterLevel}
                  onChange={(v) => setAlertThresholds({ ...alertThresholds, waterLevel: v || 0 })}
                  style={{ width: '100%' }}
                />
              </Form.Item>
              <Form.Item
                name="rainfall"
                label="降雨量告警阈值 (mm/h)"
                rules={[{ required: true, message: '请输入降雨量告警阈值' }]}
              >
                <InputNumber
                  min={0}
                  max={200}
                  step={5}
                  value={alertThresholds.rainfall}
                  onChange={(v) => setAlertThresholds({ ...alertThresholds, rainfall: v || 0 })}
                  style={{ width: '100%' }}
                />
              </Form.Item>
              <Form.Item
                name="windSpeed"
                label="风速告警阈值 (m/s)"
                rules={[{ required: true, message: '请输入风速告警阈值' }]}
              >
                <InputNumber
                  min={0}
                  max={50}
                  step={1}
                  value={alertThresholds.windSpeed}
                  onChange={(v) => setAlertThresholds({ ...alertThresholds, windSpeed: v || 0 })}
                  style={{ width: '100%' }}
                />
              </Form.Item>
            </Form>
          </Card>
        </Col>

        {/* Data Refresh Interval */}
        <Col xs={24} lg={12}>
          <Card title={<span><ClockCircleOutlined style={{ marginRight: 8 }} />数据刷新间隔</span>} size="small">
            <Form form={form} layout="vertical" initialValues={{ refreshInterval }}>
              <Form.Item
                name="refreshInterval"
                label="自动刷新间隔 (秒)"
                rules={[{ required: true, message: '请输入刷新间隔' }]}
              >
                <InputNumber
                  min={5}
                  max={3600}
                  step={5}
                  value={refreshInterval}
                  onChange={(v) => setRefreshInterval(v || 30)}
                  style={{ width: '100%' }}
                />
              </Form.Item>
              <div style={{ fontSize: 12, color: '#8c8c8c', marginTop: 8 }}>
                设置数据自动刷新的时间间隔，最小5秒，最大3600秒
              </div>
            </Form>
          </Card>
        </Col>

        {/* Theme Toggle */}
        <Col xs={24} lg={12}>
          <Card title={<span><BulbOutlined style={{ marginRight: 8 }} />主题设置</span>} size="small">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
              <div>
                <div style={{ fontWeight: 500, marginBottom: 4 }}>深色模式</div>
                <div style={{ fontSize: 12, color: '#8c8c8c' }}>切换系统界面为深色主题</div>
              </div>
              <Switch
                checked={localTheme === 'dark'}
                onChange={handleThemeChange}
                checkedChildren="深色"
                unCheckedChildren="浅色"
              />
            </div>
            <Divider style={{ margin: '12px 0' }} />
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <div style={{ fontWeight: 500, marginBottom: 4 }}>当前主题</div>
                <div style={{ fontSize: 12, color: '#8c8c8c' }}>系统当前使用的界面主题</div>
              </div>
              <Tag color={localTheme === 'dark' ? 'default' : 'processing'}>
                {localTheme === 'dark' ? '深色模式' : '浅色模式'}
              </Tag>
            </div>
          </Card>
        </Col>

        {/* System Info */}
        <Col xs={24} lg={12}>
          <Card title="系统信息" size="small">
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: '#8c8c8c' }}>系统版本</span>
                <span>v1.0.0</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: '#8c8c8c' }}>前端框架</span>
                <span>React 18 + TypeScript</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: '#8c8c8c' }}>UI组件库</span>
                <span>Ant Design 5</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: '#8c8c8c' }}>状态管理</span>
                <span>Zustand</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: '#8c8c8c' }}>图表库</span>
                <span>ECharts 5</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: '#8c8c8c' }}>地图引擎</span>
                <span>Leaflet</span>
              </div>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default SystemSettings;
