import React from 'react';
import { Radio, Card, Tag, Tooltip } from 'antd';
import {
  ExperimentOutlined,
  RobotOutlined,
  SettingOutlined,
  StarOutlined,
} from '@ant-design/icons';

import useAppStore from '@/stores/appStore';

type StrategyType = 'genetic' | 'reinforcement' | 'rule';

interface StrategyOption {
  value: StrategyType;
  label: string;
  description: string;
  icon: React.ReactNode;
  recommended?: boolean;
  pros: string[];
  cons: string[];
}

const strategyOptions: StrategyOption[] = [
  {
    value: 'genetic',
    label: '遗传算法',
    description: '基于生物进化原理的全局优化算法，通过选择、交叉、变异等操作迭代搜索最优信号配时方案。',
    icon: <ExperimentOutlined style={{ fontSize: 24, color: '#1890ff' }} />,
    recommended: true,
    pros: ['全局搜索能力强', '适应复杂路口', '收敛速度快'],
    cons: ['参数敏感', '计算资源消耗较大'],
  },
  {
    value: 'reinforcement',
    label: '强化学习',
    description: '通过与环境交互学习最优控制策略，能够实时适应交通流变化，实现自适应信号控制。',
    icon: <RobotOutlined style={{ fontSize: 24, color: '#52c41a' }} />,
    pros: ['实时自适应', '无需精确模型', '持续学习优化'],
    cons: ['训练周期长', '需要大量数据'],
  },
  {
    value: 'rule',
    label: '规则优化',
    description: '基于交通工程专家经验和经典配时方法（如Webster法），通过规则引擎进行信号配时优化。',
    icon: <SettingOutlined style={{ fontSize: 24, color: '#fa8c16' }} />,
    pros: ['实现简单', '可解释性强', '部署快速'],
    cons: ['灵活性有限', '难以应对复杂场景'],
  },
];

interface StrategySelectorProps {
  disabled?: boolean;
}

const StrategySelector: React.FC<StrategySelectorProps> = ({ disabled = false }) => {
  const { traffic, setStrategy } = useAppStore();

  return (
    <div style={{ padding: 16 }}>
      <Radio.Group
        value={traffic.strategy}
        onChange={(e) => setStrategy(e.target.value as StrategyType)}
        disabled={disabled}
        style={{ width: '100%' }}
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {strategyOptions.map((option) => (
            <Radio
              key={option.value}
              value={option.value}
              style={{ width: '100%' }}
            >
              <Card
                size="small"
                hoverable
                style={{
                  width: '100%',
                  borderColor:
                    traffic.strategy === option.value ? '#1890ff' : undefined,
                  borderWidth: traffic.strategy === option.value ? 2 : 1,
                }}
                bodyStyle={{ padding: 12 }}
              >
                <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
                  <div style={{ marginTop: 2 }}>{option.icon}</div>
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                      <span style={{ fontWeight: 500, fontSize: 14 }}>{option.label}</span>
                      {option.recommended && (
                        <Tooltip title="系统推荐使用遗传算法进行信号优化">
                          <Tag
                            icon={<StarOutlined />}
                            color="gold"
                            style={{ marginInlineEnd: 0 }}
                          >
                            推荐
                          </Tag>
                        </Tooltip>
                      )}
                    </div>
                    <div style={{ fontSize: 12, color: '#8c8c8c', marginBottom: 8 }}>
                      {option.description}
                    </div>
                    <div style={{ display: 'flex', gap: 16, fontSize: 11 }}>
                      <div>
                        <span style={{ color: '#52c41a' }}>优点: </span>
                        {option.pros.join('、')}
                      </div>
                      <div>
                        <span style={{ color: '#ff4d4f' }}>局限: </span>
                        {option.cons.join('、')}
                      </div>
                    </div>
                  </div>
                </div>
              </Card>
            </Radio>
          ))}
        </div>
      </Radio.Group>
    </div>
  );
};

export default StrategySelector;
