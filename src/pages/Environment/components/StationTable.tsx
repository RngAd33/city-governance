// ============================================================
// StationTable - Ant Design Table with anomaly highlighting
// ============================================================
import React, { useEffect, useMemo, useState } from 'react';
import { Table, Button, Tag } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { EnvironmentRecord } from '@/types';

interface StationTableProps {
  data: EnvironmentRecord[];
  loading?: boolean;
}

const StationTable: React.FC<StationTableProps> = ({ data, loading }) => {
  const columns: ColumnsType<EnvironmentRecord> = useMemo(
    () => [
      {
        title: '站点名称',
        dataIndex: 'stationName',
        key: 'stationName',
        sorter: (a, b) => a.stationName.localeCompare(b.stationName),
        width: 140,
      },
      {
        title: '城市',
        dataIndex: 'city',
        key: 'city',
        sorter: (a, b) => a.city.localeCompare(b.city),
        width: 100,
      },
      {
        title: '指标',
        dataIndex: 'metric',
        key: 'metric',
        width: 80,
      },
      {
        title: '数值',
        dataIndex: 'value',
        key: 'value',
        sorter: (a, b) => a.value - b.value,
        width: 100,
        render: (val: number, record) => (
          <span>
            {val} <span style={{ color: '#8c8c8c', fontSize: 12 }}>{record.unit}</span>
          </span>
        ),
      },
      {
        title: '状态',
        dataIndex: 'level',
        key: 'level',
        width: 80,
        filters: [
          { text: '正常', value: 'normal' },
          { text: '警告', value: 'warning' },
          { text: '危险', value: 'danger' },
        ],
        onFilter: (value, record) => (record.level || '').toLowerCase() === value,
        render: (level: string) => {
          const lv = (level || '').toLowerCase();
          const colorMap: Record<string, string> = {
            normal: 'success',
            warning: 'warning',
            danger: 'error',
          };
          const labelMap: Record<string, string> = {
            normal: '正常',
            warning: '警告',
            danger: '危险',
          };
          return <Tag color={colorMap[lv]}>{labelMap[lv] ?? level}</Tag>;
        },
      },
      {
        title: '时间',
        dataIndex: 'ts',
        key: 'ts',
        sorter: (a, b) => new Date(a.ts).getTime() - new Date(b.ts).getTime(),
        width: 160,
      },
    ],
    [],
  );

  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // 数据量变化时（切换指标/时间范围），把当前页收敛到有效范围
  useEffect(() => {
    const maxPage = Math.max(1, Math.ceil(data.length / pageSize));
    setPage((p) => Math.min(p, maxPage));
  }, [data.length, pageSize]);

  const handleExport = () => {
    const headers = ['站点名称', '城市', '指标', '数值', '单位', '状态', '时间'];
    const rows = data.map((d) => [
      d.stationName,
      d.city,
      d.metric,
      d.value,
      d.unit,
      (d.level || '').toLowerCase() === 'normal'
        ? '正常'
        : (d.level || '').toLowerCase() === 'warning'
          ? '警告'
          : '危险',
      d.ts,
    ]);
    const csv = [headers, ...rows].map((r) => r.join(',')).join('\n');
    const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `environment_stations_${Date.now()}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div>
      <div style={{ marginBottom: 12, textAlign: 'right' }}>
        <Button icon={<DownloadOutlined />} onClick={handleExport}>
          导出 CSV
        </Button>
      </div>
      <Table<EnvironmentRecord>
        columns={columns}
        dataSource={data}
        rowKey="id"
        loading={loading}
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
        rowClassName={(record) =>
          (record.level || '').toLowerCase() === 'danger' ? 'ant-table-row-danger' : ''
        }
        size="small"
      />
      <style>{`
        .ant-table-row-danger > td {
          background-color: #fff1f0 !important;
        }
        .ant-table-row-danger:hover > td {
          background-color: #ffe7e6 !important;
        }
      `}</style>
    </div>
  );
};

export default StationTable;
