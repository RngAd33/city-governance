import React from 'react';
import { Alert, Button } from 'antd';

interface ErrorAlertProps {
  message: string;
  description?: string;
  onRetry?: () => void;
}

const ErrorAlert: React.FC<ErrorAlertProps> = ({ message, description, onRetry }) => {
  return (
    <Alert
      message={message}
      description={description}
      type="error"
      showIcon
      action={
        onRetry && (
          <Button size="small" danger onClick={onRetry}>
            重试
          </Button>
        )
      }
    />
  );
};

export default ErrorAlert;
