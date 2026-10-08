// ============================================================
// QuickActions - Quick action buttons for common tasks
// ============================================================
import React from 'react';
import { Card, Button, Row, Col, Tooltip } from 'antd';
import {
  FileTextOutlined,
  WarningOutlined,
  SearchOutlined,
  BulbOutlined,
  CarOutlined,
  BarChartOutlined,
  SafetyOutlined,
  EnvironmentOutlined,
} from '@ant-design/icons';
import type { QuickAction } from '@/types';

interface QuickActionsProps {
  onAction: (actionId: string) => void;
}

const ACTIONS: QuickAction[] = [
  { id: 'dailyReport', label: '生成日报', icon: <FileTextOutlined />, description: '生成今日城市运行日报', action: 'dailyReport' },
  { id: 'weeklyReport', label: '生成周报', icon: <BarChartOutlined />, description: '生成本周城市运行周报', action: 'weeklyReport' },
  { id: 'viewAlarms', label: '查看告警', icon: <WarningOutlined />, description: '查看当前活跃告警', action: 'viewAlarms' },
  { id: 'searchPlan', label: '检索预案', icon: <SearchOutlined />, description: '检索应急预案文档', action: 'searchPlan' },
  { id: 'energySaving', label: '节能建议', icon: <BulbOutlined />, description: '建筑节能优化建议', action: 'energySaving' },
  { id: 'trafficOpt', label: '交通优化', icon: <CarOutlined />, description: '交通流量优化建议', action: 'trafficOpt' },
  { id: 'safetyCheck', label: '安全检查', icon: <SafetyOutlined />, description: '城市安全隐患排查', action: 'safetyCheck' },
  { id: 'envAnalysis', label: '环境分析', icon: <EnvironmentOutlined />, description: '环境质量分析报告', action: 'envAnalysis' },
];

const QuickActions: React.FC<QuickActionsProps> = ({ onAction }) => {
  return (
    <Card title="快捷操作" size="small" style={{ height: '100%' }}>
      <Row gutter={[8, 8]}>
        {ACTIONS.map((action) => (
          <Col span={12} key={action.id}>
            <Tooltip title={action.description} placement="top">
              <Button
                block
                onClick={() => onAction(action.id)}
                style={{
                  height: 60,
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: 4,
                }}
              >
                <span style={{ fontSize: 18, color: '#1890ff' }}>{action.icon}</span>
                <span style={{ fontSize: 12 }}>{action.label}</span>
              </Button>
            </Tooltip>
          </Col>
        ))}
      </Row>
    </Card>
  );
};

export default QuickActions;
