// ============================================================
// FacilityList - Ant Design Table for facility management
// ============================================================
import React, { useEffect, useState } from 'react';
import { Table, Tag, Input, Select, Space, Button, Drawer, Descriptions, Badge } from 'antd';
import { SearchOutlined, EyeOutlined } from '@ant-design/icons';
import type { Facility, Level } from '@/types';
import type { ColumnsType } from 'antd/es/table';

const { Search } = Input;

const LEVEL_TAG_COLORS: Record<Level, string> = {
  normal: 'success',
  warning: 'warning',
  danger: 'error',
  critical: 'error',
};

const LEVEL_LABELS: Record<Level, string> = {
  normal: '正常',
  warning: '警告',
  danger: '危险',
  critical: '严重',
};

interface FacilityListProps {
  facilities: Facility[];
  onFacilityClick?: (facility: Facility) => void;
}

const FacilityList: React.FC<FacilityListProps> = ({ facilities, onFacilityClick }) => {
  const [searchText, setSearchText] = useState('');
  const [statusFilter, setStatusFilter] = useState<Level | 'all'>('all');
  const [typeFilter, setTypeFilter] = useState<string>('all');
  const [selectedFacility, setSelectedFacility] = useState<Facility | null>(null);
  const [drawerVisible, setDrawerVisible] = useState(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const handleRowClick = (facility: Facility) => {
    setSelectedFacility(facility);
    setDrawerVisible(true);
    onFacilityClick?.(facility);
  };

  const filteredFacilities = facilities.filter((f) => {
    const matchSearch = f.name.toLowerCase().includes(searchText.toLowerCase()) ||
      f.location.toLowerCase().includes(searchText.toLowerCase());
    const matchStatus = statusFilter === 'all' || f.status === statusFilter;
    const matchType = typeFilter === 'all' || f.type === typeFilter;
    return matchSearch && matchStatus && matchType;
  });

  // 过滤条件或数据变化时，把页码收敛到有效范围
  useEffect(() => {
    const maxPage = Math.max(1, Math.ceil(filteredFacilities.length / pageSize));
    setPage((p) => Math.min(p, maxPage));
  }, [filteredFacilities.length, pageSize]);

  const columns: ColumnsType<Facility> = [
    {
      title: '设施名称',
      dataIndex: 'name',
      key: 'name',
      sorter: (a, b) => a.name.localeCompare(b.name),
      render: (text: string) => <span style={{ fontWeight: 500 }}>{text}</span>,
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      filters: [
        { text: '泵站', value: 'pump' },
        { text: '变电站', value: 'substation' },
        { text: '水厂', value: 'water' },
        { text: '燃气站', value: 'gas' },
        { text: '通信塔', value: 'tower' },
      ],
      onFilter: (value, record) => record.type === value,
      render: (type: string) => {
        const typeMap: Record<string, string> = {
          pump: '泵站',
          substation: '变电站',
          water: '水厂',
          gas: '燃气站',
          tower: '通信塔',
        };
        return <Tag>{typeMap[type] || type}</Tag>;
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      sorter: (a, b) => {
        const order = { critical: 0, danger: 1, warning: 2, normal: 3 };
        return order[a.status] - order[b.status];
      },
      render: (status: Level) => (
        <Tag color={LEVEL_TAG_COLORS[status]}>{LEVEL_LABELS[status]}</Tag>
      ),
    },
    {
      title: '位置',
      dataIndex: 'location',
      key: 'location',
      ellipsis: true,
    },
    {
      title: '上次维护',
      dataIndex: 'lastMaintenance',
      key: 'lastMaintenance',
      sorter: (a, b) => new Date(a.lastMaintenance).getTime() - new Date(b.lastMaintenance).getTime(),
    },
    {
      title: '下次维护',
      dataIndex: 'nextMaintenance',
      key: 'nextMaintenance',
      sorter: (a, b) => new Date(a.nextMaintenance).getTime() - new Date(b.nextMaintenance).getTime(),
      render: (text: string) => {
        const days = Math.ceil((new Date(text).getTime() - Date.now()) / (1000 * 60 * 60 * 24));
        const color = days < 0 ? '#ff4d4f' : days < 7 ? '#faad14' : '#52c41a';
        return <span style={{ color }}>{text}</span>;
      },
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: Facility) => (
        <Button
          type="link"
          size="small"
          icon={<EyeOutlined />}
          onClick={(e) => {
            e.stopPropagation();
            handleRowClick(record);
          }}
        >
          查看
        </Button>
      ),
    },
  ];

  return (
    <div>
      {/* Filters */}
      <Space style={{ marginBottom: 12 }} wrap>
        <Search
          placeholder="搜索设施名称或位置"
          allowClear
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          style={{ width: 240 }}
          prefix={<SearchOutlined />}
        />
        <Select
          value={statusFilter}
          onChange={setStatusFilter}
          style={{ width: 120 }}
          options={[
            { label: '全部状态', value: 'all' },
            { label: '正常', value: 'normal' },
            { label: '警告', value: 'warning' },
            { label: '危险', value: 'danger' },
            { label: '严重', value: 'critical' },
          ]}
        />
        <Select
          value={typeFilter}
          onChange={setTypeFilter}
          style={{ width: 120 }}
          options={[
            { label: '全部类型', value: 'all' },
            { label: '泵站', value: 'pump' },
            { label: '变电站', value: 'substation' },
            { label: '水厂', value: 'water' },
            { label: '燃气站', value: 'gas' },
            { label: '通信塔', value: 'tower' },
          ]}
        />
      </Space>

      {/* Table */}
      <Table
        dataSource={filteredFacilities}
        columns={columns}
        rowKey="id"
        size="small"
        pagination={{
          current: page,
          pageSize,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条`,
          onChange: (p, size) => {
            setPage(p);
            setPageSize(size);
          },
        }}
        onRow={(record) => ({
          onClick: () => handleRowClick(record),
          style: { cursor: 'pointer' },
        })}
        scroll={{ y: 350 }}
      />

      {/* Detail Drawer */}
      <Drawer
        title="设施详情"
        placement="right"
        width={400}
        open={drawerVisible}
        onClose={() => setDrawerVisible(false)}
      >
        {selectedFacility && (
          <div>
            <div style={{ textAlign: 'center', marginBottom: 24 }}>
              <Badge
                status={selectedFacility.status === 'normal' ? 'success' : selectedFacility.status === 'warning' ? 'warning' : 'error'}
                text={
                  <span style={{ fontSize: 16, fontWeight: 600 }}>
                    {selectedFacility.name}
                  </span>
                }
              />
            </div>
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="设施类型">
                <Tag>{selectedFacility.type === 'pump' ? '泵站' : selectedFacility.type === 'substation' ? '变电站' : selectedFacility.type === 'water' ? '水厂' : selectedFacility.type === 'gas' ? '燃气站' : '通信塔'}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="运行状态">
                <Tag color={LEVEL_TAG_COLORS[selectedFacility.status]}>
                  {LEVEL_LABELS[selectedFacility.status]}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="位置">{selectedFacility.location}</Descriptions.Item>
              <Descriptions.Item label="上次维护">{selectedFacility.lastMaintenance}</Descriptions.Item>
              <Descriptions.Item label="下次维护">{selectedFacility.nextMaintenance}</Descriptions.Item>
              <Descriptions.Item label="描述">{selectedFacility.description}</Descriptions.Item>
            </Descriptions>
          </div>
        )}
      </Drawer>
    </div>
  );
};

export default FacilityList;
