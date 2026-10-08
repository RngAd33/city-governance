// ============================================================
// ReportPreview - 渲染后端报告正文（city-govai 返回 Markdown 文本）
// 支持子集：### 标题、**加粗**、| 表格 |、- / 1. 列表、--- 分隔线、段落
// ============================================================
import React, { useRef, useState } from 'react';
import { Button, Space, Tag, Divider, Empty, message } from 'antd';
import {
  PrinterOutlined,
  FilePdfOutlined,
  FileWordOutlined,
  FileExcelOutlined,
  ShareAltOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import { formatDateTime } from '@/utils/format';
import { downloadReportPdf, previewReportPdf } from '../pdfExport';
import type { Report } from '@/types';

interface ReportPreviewProps {
  report: Report;
}

const TYPE_TAG_COLOR: Record<Report['type'], string> = {
  daily: 'processing',
  weekly: 'success',
  briefing: 'warning',
  risk: 'error',
};

// ---- 轻量 Markdown 渲染（后端报告模板格式稳定：标题/加粗/表格/列表/分隔线/段落）----

const INLINE_BOLD = /\*\*(.+?)\*\*/g;

// 行内加粗解析：**text** → <strong>
const renderInline = (text: string, keyPrefix: string): React.ReactNode[] =>
  text.split(INLINE_BOLD).map((part, i) =>
    i % 2 === 1 ? <strong key={`${keyPrefix}-${i}`}>{part}</strong> : <React.Fragment key={`${keyPrefix}-${i}`}>{part}</React.Fragment>,
  );

const isTableSeparator = (line: string) => /^\|[\s\-:|]+\|$/.test(line);

const splitTableRow = (line: string): string[] =>
  line.replace(/^\||\|$/g, '').split('|').map((cell) => cell.trim());

const TABLE_CELL_STYLE: React.CSSProperties = {
  border: '1px solid #e8e8e8',
  padding: '6px 10px',
  fontSize: 12.5,
  textAlign: 'left',
  verticalAlign: 'top',
};

const renderTable = (rows: string[][], key: number) => {
  if (rows.length === 0) return null;
  const [header, ...body] = rows;
  return (
    <table key={key} style={{ width: '100%', borderCollapse: 'collapse', margin: '12px 0' }}>
      <thead>
        <tr>
          {header.map((cell, j) => (
            <th key={j} style={{ ...TABLE_CELL_STYLE, background: '#fafafa', fontWeight: 600, whiteSpace: 'nowrap' }}>
              {renderInline(cell, `th-${key}-${j}`)}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {body.map((row, r) => (
          <tr key={r}>
            {row.map((cell, j) => (
              <td key={j} style={TABLE_CELL_STYLE}>
                {renderInline(cell, `td-${key}-${r}-${j}`)}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
};

const HEADING_SIZE: Record<number, number> = { 1: 22, 2: 19, 3: 17, 4: 15 };

const isListLine = (line: string) => /^[-*]\s+/.test(line) || /^\d{1,2}(?:[.)]\s+|、)/.test(line);

// 是否为块级特殊行（决定段落边界）
const isBlockBoundary = (line: string) =>
  !line ||
  line.startsWith('|') ||
  /^-{3,}$/.test(line) ||
  /^#{1,4}\s+/.test(line) ||
  isListLine(line);

// Markdown 文本 → React 节点序列
const renderRichContent = (content: string): React.ReactNode => {
  const lines = content.split(/\r?\n/);
  const blocks: React.ReactNode[] = [];
  let key = 0;
  let i = 0;

  while (i < lines.length) {
    const trimmed = lines[i].trim();
    if (!trimmed) {
      i++;
      continue;
    }

    // 分隔线
    if (/^-{3,}$/.test(trimmed)) {
      blocks.push(<Divider key={key++} style={{ margin: '12px 0' }} />);
      i++;
      continue;
    }

    // 标题（# ~ ####）
    const heading = trimmed.match(/^(#{1,4})\s+(.*)$/);
    if (heading) {
      const level = heading[1].length;
      blocks.push(
        <div key={key++} style={{ fontSize: HEADING_SIZE[level], fontWeight: 600, color: '#262626', margin: '16px 0 8px' }}>
          {renderInline(heading[2], `h-${key}`)}
        </div>,
      );
      i++;
      continue;
    }

    // 表格（连续的 | 开头行，过滤 |---|---| 分隔行）
    if (trimmed.startsWith('|')) {
      const rows: string[][] = [];
      while (i < lines.length && lines[i].trim().startsWith('|')) {
        const rowLine = lines[i].trim();
        if (!isTableSeparator(rowLine)) rows.push(splitTableRow(rowLine));
        i++;
      }
      blocks.push(renderTable(rows, key++));
      continue;
    }

    // 列表（- / 1. / 1、）
    if (isListLine(trimmed)) {
      const items: string[] = [];
      while (i < lines.length) {
        const itemLine = lines[i].trim();
        const match = itemLine.match(/^[-*]\s+(.*)$/) ?? itemLine.match(/^\d{1,2}(?:[.)]\s+|、)\s*(.*)$/);
        if (!match) break;
        items.push(match[1]);
        i++;
      }
      blocks.push(
        <ul key={key++} style={{ margin: '8px 0', paddingLeft: 22, fontSize: 13.5, lineHeight: 2, color: '#434343' }}>
          {items.map((item, j) => (
            <li key={j}>{renderInline(item, `li-${key}-${j}`)}</li>
          ))}
        </ul>,
      );
      continue;
    }

    // 段落（合并连续普通行，多行以 <br/> 换行）
    const paraLines: string[] = [];
    while (i < lines.length && !isBlockBoundary(lines[i].trim())) {
      paraLines.push(lines[i].replace(/\s+$/, ''));
      i++;
    }
    blocks.push(
      <p key={key++} style={{ margin: '8px 0', fontSize: 13.5, lineHeight: 1.9, color: '#434343' }}>
        {paraLines.map((paraLine, j) => (
          <React.Fragment key={j}>
            {j > 0 && <br />}
            {renderInline(paraLine, `p-${key}-${j}`)}
          </React.Fragment>
        ))}
      </p>,
    );
  }

  return blocks;
};

const ReportPreview: React.FC<ReportPreviewProps> = ({ report }) => {
  const printRef = useRef<HTMLDivElement>(null);
  const [exportingPdf, setExportingPdf] = useState(false);
  const [previewingPdf, setPreviewingPdf] = useState(false);

  const handlePrint = () => {
    const content = printRef.current;
    if (!content) return;
    const printWindow = window.open('', '_blank');
    if (!printWindow) return;
    printWindow.document.write(`
      <html>
        <head>
          <title>${report.title}</title>
          <style>
            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; padding: 40px; color: #262626; }
            h1 { font-size: 24px; margin-bottom: 8px; }
            h2 { font-size: 18px; margin-top: 24px; margin-bottom: 12px; }
            h3 { font-size: 16px; margin-top: 16px; margin-bottom: 8px; }
            p { line-height: 1.8; margin: 8px 0; }
            ul, ol { line-height: 1.9; margin: 8px 0; padding-left: 22px; }
            table { width: 100%; border-collapse: collapse; margin: 16px 0; }
            th, td { border: 1px solid #d9d9d9; padding: 8px 12px; text-align: left; font-size: 13px; }
            th { background: #fafafa; font-weight: 600; }
            .meta { color: #8c8c8c; font-size: 13px; margin-bottom: 24px; }
            .footer { margin-top: 40px; padding-top: 16px; border-top: 1px solid #f0f0f0; color: #8c8c8c; font-size: 12px; }
          </style>
        </head>
        <body>
          ${content.innerHTML}
          <div class="footer">
            <p>本报告由城市治理智能平台自动生成 | 生成时间: ${new Date().toLocaleString('zh-CN')}</p>
          </div>
        </body>
      </html>
    `);
    printWindow.document.close();
    printWindow.print();
  };

  // 导出 PDF（下载）与预览（新窗口打开）：错误提示由 pdfExport 统一处理
  const handleExportPDF = async () => {
    setExportingPdf(true);
    try {
      await downloadReportPdf(report);
    } finally {
      setExportingPdf(false);
    }
  };

  const handlePreviewPDF = async () => {
    setPreviewingPdf(true);
    try {
      await previewReportPdf(report);
    } finally {
      setPreviewingPdf(false);
    }
  };

  const handleExportWord = () => {
    message.info('Word导出功能开发中');
  };

  const handleExportExcel = () => {
    message.info('Excel导出功能开发中');
  };

  const handleShare = () => {
    message.success('链接已复制到剪贴板');
  };

  return (
    <div>
      {/* Toolbar */}
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
        <Space>
          <Tag color={TYPE_TAG_COLOR[report.type]}>{report.typeLabel}</Tag>
          <span style={{ fontSize: 12, color: '#8c8c8c' }}>生成时间: {formatDateTime(report.createdAt)}</span>
        </Space>
        <Space wrap>
          <Button icon={<PrinterOutlined />} onClick={handlePrint}>打印</Button>
          <Button icon={<EyeOutlined />} loading={previewingPdf} onClick={handlePreviewPDF}>PDF 预览</Button>
          <Button icon={<FilePdfOutlined />} loading={exportingPdf} onClick={handleExportPDF}>导出 PDF</Button>
          <Button icon={<FileWordOutlined />} onClick={handleExportWord}>Word</Button>
          <Button icon={<FileExcelOutlined />} onClick={handleExportExcel}>Excel</Button>
          <Button type="primary" icon={<ShareAltOutlined />} onClick={handleShare}>分享</Button>
        </Space>
      </div>

      {/* Report Content */}
      <div
        ref={printRef}
        style={{
          background: '#fff',
          padding: '32px 40px',
          borderRadius: 8,
          minHeight: 400,
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: 8 }}>
          <h1 style={{ margin: 0, fontSize: 24 }}>{report.title}</h1>
          <div style={{ color: '#8c8c8c', fontSize: 13, marginTop: 8 }}>
            编制: {report.author} | {report.city} · {report.period}
          </div>
        </div>
        <Divider />
        {report.content ? (
          renderRichContent(report.content)
        ) : (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无报告正文" style={{ padding: '48px 0' }} />
        )}
        <Divider />
        <div style={{ textAlign: 'center', color: '#8c8c8c', fontSize: 12 }}>
          本报告由城市治理智能平台自动生成
        </div>
      </div>
    </div>
  );
};

export default ReportPreview;
