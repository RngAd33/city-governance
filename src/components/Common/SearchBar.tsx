import React from 'react';
import { Input, Button, Space, Select } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';

interface SearchBarProps {
  onSearch: (keyword: string) => void;
  onReset?: () => void;
  placeholder?: string;
  filters?: Array<{
    key: string;
    label: string;
    options: Array<{ label: string; value: string }>;
  }>;
  onFilterChange?: (key: string, value: string) => void;
}

const SearchBar: React.FC<SearchBarProps> = ({
  onSearch,
  onReset,
  placeholder = '请输入关键词搜索',
  filters = [],
  onFilterChange,
}) => {
  const [keyword, setKeyword] = React.useState('');

  const handleSearch = () => {
    onSearch(keyword);
  };

  const handleReset = () => {
    setKeyword('');
    onReset?.();
  };

  return (
    <Space wrap style={{ marginBottom: 16 }}>
      <Input
        placeholder={placeholder}
        value={keyword}
        onChange={(e) => setKeyword(e.target.value)}
        onPressEnter={handleSearch}
        style={{ width: 240 }}
        prefix={<SearchOutlined />}
      />
      <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
        搜索
      </Button>
      <Button icon={<ReloadOutlined />} onClick={handleReset}>
        重置
      </Button>
      {filters.map((filter) => (
        <Select
          key={filter.key}
          placeholder={filter.label}
          style={{ width: 140 }}
          options={filter.options}
          onChange={(value) => onFilterChange?.(filter.key, value)}
          allowClear
        />
      ))}
    </Space>
  );
};

export default SearchBar;
