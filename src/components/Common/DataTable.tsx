import { Table, TableProps, Button } from 'antd';

interface DataTableProps<T> extends TableProps<T> {
  onRowClick?: (record: T) => void;
  exportable?: boolean;
  onExport?: () => void;
}

function DataTable<T extends Record<string, any>>({
  onRowClick,
  exportable = false,
  onExport,
  ...tableProps
}: DataTableProps<T>) {
  return (
    <div>
      {exportable && (
        <div style={{ marginBottom: 12, textAlign: 'right' }}>
          <Button onClick={onExport}>导出数据</Button>
        </div>
      )}
      <Table<T>
        {...tableProps}
        onRow={(record) => ({
          onClick: () => onRowClick?.(record),
          style: { cursor: onRowClick ? 'pointer' : 'default' },
        })}
        pagination={{
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 条`,
          ...tableProps.pagination,
        }}
        scroll={{ x: 'max-content' }}
        size="middle"
      />
    </div>
  );
}

export default DataTable;
