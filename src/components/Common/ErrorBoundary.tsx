// ============================================================
// ErrorBoundary - 全局错误边界
// 单个页面渲染/effect 异常不再卸载整棵应用树（整页白屏），
// 仅替换出错页面的内容区，侧边栏/顶栏保持可用，可切换或重试
// ============================================================
import React from 'react';
import { Button, Result } from 'antd';

interface ErrorBoundaryProps {
  children: React.ReactNode;
}

interface ErrorBoundaryState {
  error: Error | null;
}

class ErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { error: null };

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('[ErrorBoundary] 页面渲染异常:', error, errorInfo);
  }

  handleRetry = () => {
    this.setState({ error: null });
  };

  render() {
    const { error } = this.state;
    if (error) {
      return (
        <Result
          status="error"
          title="页面渲染出错"
          subTitle={error.message || '页面加载过程中发生异常，请重试或切换其他页面'}
          extra={
            <Button type="primary" onClick={this.handleRetry}>
              重试
            </Button>
          }
        />
      );
    }
    return this.props.children;
  }
}

export default ErrorBoundary;
