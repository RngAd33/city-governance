// ============================================================
// pdfExport - 报告 PDF 导出/预览（city-govai GET /api/govai/report/{id}/pdf）
// 200 返回二进制流（文件名由后端设置在 Content-Disposition）；404 空体；500 JSON {error}
// ============================================================
import { message } from 'antd';
import { fetchReportPdf } from '@/api/dashboard';

// 从 Content-Disposition 解析文件名：优先 RFC5987 的 filename*=UTF-8''，回退 filename=""
const parseFilename = (disposition: unknown, fallback: string): string => {
  if (typeof disposition !== 'string' || !disposition) return fallback;
  const encoded = disposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (encoded) {
    try {
      return decodeURIComponent(encoded[1]);
    } catch {
      /* 编码异常时退回普通 filename */
    }
  }
  const plain = disposition.match(/filename="?([^";]+)"?/i);
  return plain ? plain[1] : fallback;
};

// 错误响应体（Blob）转可读提示：500 为 JSON {error}，404 为空体
const extractError = async (err: unknown): Promise<string> => {
  const response = (err as { response?: { status?: number; data?: unknown } })?.response;
  const data = response?.data;
  if (data instanceof Blob) {
    try {
      const text = await data.text();
      if (text) {
        try {
          const parsed = JSON.parse(text) as { error?: unknown };
          if (typeof parsed.error === 'string' && parsed.error) return parsed.error;
        } catch {
          return text.slice(0, 200);
        }
      }
    } catch {
      /* 读取失败时走状态码兜底 */
    }
  }
  if (response?.status === 404) return '报告不存在或已被删除';
  return 'PDF 导出失败，请稍后重试';
};

// 触发浏览器下载（blob + 临时 a 标签）
const triggerDownload = (blob: Blob, filename: string) => {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  // 延迟回收：立即 revoke 可能中断尚未开始的下载
  window.setTimeout(() => URL.revokeObjectURL(url), 10000);
};

/** 导出报告 PDF：成功后触发下载，失败时展示后端错误信息（内部消化异常，调用方无需 try/catch） */
export const downloadReportPdf = async (report: { id: number; title: string }): Promise<boolean> => {
  try {
    const resp = await fetchReportPdf(report.id);
    const filename = parseFilename(resp.headers['content-disposition'], `${report.title}.pdf`);
    triggerDownload(resp.data, filename);
    message.success('PDF 已开始下载');
    return true;
  } catch (err) {
    message.error(await extractError(err));
    return false;
  }
};

/** 预览报告 PDF：新窗口打开。同步开窗（点击手势内）避免被弹窗拦截器拦截，加载完成后导航到 blob 地址 */
export const previewReportPdf = async (report: { id: number; title: string }): Promise<boolean> => {
  const previewWindow = window.open('', '_blank');
  try {
    const resp = await fetchReportPdf(report.id);
    if (previewWindow && !previewWindow.closed) {
      const url = URL.createObjectURL(resp.data);
      previewWindow.location.href = url;
      // blob URL 由新窗口持有，延迟回收
      window.setTimeout(() => URL.revokeObjectURL(url), 60000);
      return true;
    }
    // 新窗口被拦截或已关闭：退化为下载
    const filename = parseFilename(resp.headers['content-disposition'], `${report.title}.pdf`);
    triggerDownload(resp.data, filename);
    message.success('新窗口被拦截，PDF 已开始下载');
    return true;
  } catch (err) {
    try {
      previewWindow?.close();
    } catch {
      /* 窗口可能已不可达 */
    }
    message.error(await extractError(err));
    return false;
  }
};
