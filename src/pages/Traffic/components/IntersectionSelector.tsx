import React, { useState, useMemo } from 'react';
import { Select, Tag, Input, Empty, Spin } from 'antd';
import {
  EnvironmentOutlined,
  SearchOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  CloseCircleOutlined,
  StopOutlined,
} from '@ant-design/icons';
import type { Intersection } from '@/types';

const congestionConfig: Record<
  string,
  { label: string; color: string; icon: React.ReactNode }
> = {
  smooth: {
    label: '畅通',
    color: 'success',
    icon: <CheckCircleOutlined />,
  },
  moderate: {
    label: '缓行',
    color: 'processing',
    icon: <WarningOutlined />,
  },
  congested: {
    label: '拥堵',
    color: 'warning',
    icon: <CloseCircleOutlined />,
  },
  severe: {
    label: '严重拥堵',
    color: 'error',
    icon: <StopOutlined />,
  },
};

interface IntersectionSelectorProps {
  intersections: Intersection[];
  loading?: boolean;
  onSelect?: (intersection: Intersection) => void;
}

const IntersectionSelector: React.FC<IntersectionSelectorProps> = ({
  intersections,
  loading = false,
  onSelect,
}) => {
  const [searchText, setSearchText] = useState('');
  const [selectedArea, setSelectedArea] = useState<string | undefined>(undefined);
  const [selectedId, setSelectedId] = useState<string | undefined>(undefined);

  const areas = useMemo(() => {
    const areaSet = new Set(intersections.map((i) => i.area || '未分类'));
    return Array.from(areaSet).sort();
  }, [intersections]);

  const filteredIntersections = useMemo(() => {
    return intersections.filter((intersection) => {
      const matchesArea = !selectedArea || (intersection.area || '未分类') === selectedArea;
      const matchesSearch =
        !searchText ||
        intersection.name.toLowerCase().includes(searchText.toLowerCase()) ||
        intersection.id.toLowerCase().includes(searchText.toLowerCase());
      return matchesArea && matchesSearch;
    });
  }, [intersections, searchText, selectedArea]);

  const groupedIntersections = useMemo(() => {
    const groups: Record<string, Intersection[]> = {};
    filteredIntersections.forEach((intersection) => {
      const area = intersection.area || '未分类';
      if (!groups[area]) {
        groups[area] = [];
      }
      groups[area].push(intersection);
    });
    return groups;
  }, [filteredIntersections]);

  const handleChange = (value: string) => {
    setSelectedId(value);
    const intersection = intersections.find((i) => i.id === value) || null;
    if (intersection && onSelect) {
      onSelect(intersection);
    }
  };

  const selectedIntersection = intersections.find((i) => i.id === selectedId) || null;

  return (
    <div style={{ padding: 16 }}>
      <div style={{ marginBottom: 12 }}>
        <Input
          prefix={<SearchOutlined style={{ color: '#bfbfbf' }} />}
          placeholder="搜索路口名称或ID"
          allowClear
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          style={{ width: '100%' }}
        />
      </div>
      <div style={{ marginBottom: 12 }}>
        <Select
          placeholder="按区域筛选"
          allowClear
          style={{ width: '100%' }}
          value={selectedArea}
          onChange={(value) => setSelectedArea(value)}
        >
          {areas.map((area) => (
            <Select.Option key={area} value={area}>
              {area}
            </Select.Option>
          ))}
        </Select>
      </div>
      <Select
        placeholder="选择路口"
        style={{ width: '100%' }}
        value={selectedId}
        onChange={handleChange}
        showSearch
        filterOption={(input, option) =>
          (option?.children as unknown as string)
            ?.toLowerCase()
            .includes(input.toLowerCase())
        }
        notFoundContent={
          loading ? (
            <div style={{ textAlign: 'center', padding: 16 }}>
              <Spin size="small" />
            </div>
          ) : (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description="暂无路口数据"
            />
          )
        }
      >
        {Object.entries(groupedIntersections).map(([area, items]) => (
          <Select.OptGroup key={area} label={area}>
            {items.map((intersection) => {
              const config = congestionConfig[intersection.congestionLevel ?? 'smooth'] || congestionConfig.smooth;
              return (
                <Select.Option key={intersection.id} value={intersection.id}>
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      width: '100%',
                    }}
                  >
                    <span>
                      <EnvironmentOutlined style={{ marginRight: 8, color: '#1890ff' }} />
                      {intersection.name}
                    </span>
                    <Tag
                      color={config.color}
                      icon={config.icon}
                      style={{ marginInlineEnd: 0 }}
                    >
                      {config.label}
                    </Tag>
                  </div>
                </Select.Option>
              );
            })}
          </Select.OptGroup>
        ))}
      </Select>
      {selectedIntersection && (
        <div
          style={{
            marginTop: 12,
            padding: 12,
            background: '#f6ffed',
            border: '1px solid #b7eb8f',
            borderRadius: 6,
          }}
        >
          <div style={{ fontWeight: 500, marginBottom: 4 }}>
            {selectedIntersection.name}
          </div>
          <div style={{ fontSize: 12, color: '#8c8c8c' }}>
            当前流量: {selectedIntersection.currentFlow || '--'} 辆/小时
          </div>
          <div style={{ marginTop: 8 }}>
            <Tag
              color={congestionConfig[selectedIntersection.congestionLevel ?? 'smooth']?.color || 'default'}
              icon={congestionConfig[selectedIntersection.congestionLevel ?? 'smooth']?.icon}
            >
              {congestionConfig[selectedIntersection.congestionLevel ?? 'smooth']?.label || '未知'}
            </Tag>
          </div>
        </div>
      )}
    </div>
  );
};

export default IntersectionSelector;
