import React from 'react';
import { Select, Space } from 'antd';
import { EnvironmentOutlined } from '@ant-design/icons';
import { CITY_LIST } from '@/constants';

interface CitySelectorProps {
  value?: string;
  onChange?: (city: string) => void;
  style?: React.CSSProperties;
}

const CitySelector: React.FC<CitySelectorProps> = ({ value = '南京', onChange, style }) => {
  return (
    <Space>
      <EnvironmentOutlined style={{ color: '#1890ff' }} />
      <Select
        value={value}
        onChange={onChange}
        style={{ width: 120, ...style }}
        options={CITY_LIST.map((city) => ({ label: city, value: city }))}
        showSearch
        optionFilterProp="label"
      />
    </Space>
  );
};

export default CitySelector;
