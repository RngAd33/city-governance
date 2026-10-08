package com.cityplatform.govai.service;

import com.cityplatform.govai.model.entity.Report;
import com.lowagie.text.Font;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.cityplatform.govai.constant.ReportConstants.*;

/**
 * 报告 PDF 生成服务
 * 将报告（Markdown 正文）渲染为 PDF：标题 + 元信息 + 正文；
 * 支持 Markdown 简化排版（标题/列表/表格/分隔线），中文字体按级探测并内嵌
 */
@Service
@Slf4j
public class ReportPdfService {
    
    /** 分隔线正则（--- / *** / ___） */
    private static final Pattern SEPARATOR = Pattern.compile("^[-*_]{3,}$");

    private static final Color META_COLOR = new Color(100, 116, 139);
    private static final Color LINE_COLOR = new Color(226, 232, 240);
    private static final Color TABLE_BORDER_COLOR = new Color(203, 213, 225);
    private static final Color TABLE_HEADER_COLOR = new Color(241, 245, 249);

    /** 缓存的中文字体（首次渲染时加载，之后复用） */
    private volatile BaseFont cachedFont;

    /**
     * 渲染报告为 PDF 字节数组
     */
    public byte[] render(Report report) {
        BaseFont font = resolveChineseFont();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 54, 54, 60, 54);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            addHeader(document, report, font);
            renderMarkdown(document, report.getContent(), font);
            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("报告PDF渲染失败: " + e.getMessage(), e);
        }
    }

    /**
     * 文档标题与元信息
     */
    private void addHeader(Document document, Report report, BaseFont font) throws DocumentException {
        Paragraph title = new Paragraph(sanitize(report.getTitle()), new Font(font, 18));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(6);
        document.add(title);

        List<String> meta = new ArrayList<>();
        addIfPresent(meta, report.getType());
        addIfPresent(meta, report.getCity());
        addIfPresent(meta, report.getPeriod());
        addIfPresent(meta, report.getAuthor());
        if (report.getCreatedAt() != null) {
            meta.add(report.getCreatedAt().format(TIME_FORMAT));
        }
        if (!meta.isEmpty()) {
            Paragraph metaLine = new Paragraph(String.join(" · ", meta), new Font(font, 9, Font.NORMAL, META_COLOR));
            metaLine.setAlignment(Element.ALIGN_CENTER);
            metaLine.setSpacingAfter(10);
            document.add(metaLine);
        }
        addSeparator(document);
    }

    /**
     * Markdown 简化渲染：标题/列表/表格/分隔线/段落
     */
    private void renderMarkdown(Document document, String content, BaseFont font) throws DocumentException {
        if (content == null || content.isBlank()) {
            document.add(new Paragraph("（报告正文为空）", new Font(font, 11)));
            return;
        }
        List<String> tableLines = new ArrayList<>();
        for (String rawLine : content.split("\n")) {
            String line = rawLine.strip();
            if (line.startsWith("|") || line.endsWith("|")) {
                tableLines.add(line);
                continue;
            }
            flushTable(document, tableLines, font);
            if (line.isEmpty()) {
                continue;
            }
            if (SEPARATOR.matcher(line).matches()) {
                addSeparator(document);
            } else if (line.startsWith("### ")) {
                addHeading(document, line.substring(4), font, 12, 6, 4);
            } else if (line.startsWith("## ")) {
                addHeading(document, line.substring(3), font, 14, 8, 5);
            } else if (line.startsWith("# ")) {
                addHeading(document, line.substring(2), font, 16, 10, 6);
            } else if (isBullet(line)) {
                Paragraph item = new Paragraph("• " + sanitize(line.substring(2)), new Font(font, 11));
                item.setIndentationLeft(12);
                item.setSpacingAfter(4);
                document.add(item);
            } else {
                Paragraph paragraph = new Paragraph(sanitize(line), new Font(font, 11));
                paragraph.setSpacingAfter(6);
                document.add(paragraph);
            }
        }
        flushTable(document, tableLines, font);
    }

    /**
     * 输出累积的 Markdown 表格行（首行为表头，|---|---| 分隔行忽略，列数按最大补齐）
     */
    private void flushTable(Document document, List<String> tableLines, BaseFont font) throws DocumentException {
        if (tableLines.isEmpty()) {
            return;
        }
        List<String[]> rows = new ArrayList<>();
        for (String line : tableLines) {
            if (line.replaceAll("[\\s|:\\-]", "").isEmpty()) {
                continue;
            }
            String trimmed = line;
            if (trimmed.startsWith("|")) {
                trimmed = trimmed.substring(1);
            }
            if (trimmed.endsWith("|")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            String[] cells = trimmed.split("\\|");
            for (int i = 0; i < cells.length; i++) {
                cells[i] = sanitize(cells[i].trim());
            }
            rows.add(cells);
        }
        tableLines.clear();
        if (rows.isEmpty()) {
            return;
        }
        int columns = rows.stream().mapToInt(r -> r.length).max().orElse(1);
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        table.setSpacingAfter(8);
        for (int r = 0; r < rows.size(); r++) {
            String[] cells = rows.get(r);
            boolean header = r == 0;
            for (int c = 0; c < columns; c++) {
                String text = c < cells.length ? cells[c] : "";
                PdfPCell cell = new PdfPCell(new Phrase(text, new Font(font, 10)));
                cell.setPadding(5);
                cell.setBorderColor(TABLE_BORDER_COLOR);
                if (header) {
                    cell.setBackgroundColor(TABLE_HEADER_COLOR);
                }
                table.addCell(cell);
            }
        }
        document.add(table);
    }

    private void addHeading(Document document, String text, BaseFont font,
                            float size, float spacingBefore, float spacingAfter) throws DocumentException {
        Paragraph heading = new Paragraph(sanitize(text), new Font(font, size));
        heading.setSpacingBefore(spacingBefore);
        heading.setSpacingAfter(spacingAfter);
        document.add(heading);
    }

    private void addSeparator(Document document) throws DocumentException {
        Paragraph separator = new Paragraph();
        separator.add(new Chunk(new LineSeparator(0.7f, 100, LINE_COLOR, Element.ALIGN_CENTER, -2)));
        separator.setSpacingAfter(8);
        document.add(separator);
    }

    private void addIfPresent(List<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value.trim());
        }
    }

    private boolean isBullet(String line) {
        return line.startsWith("- ") || line.startsWith("* ") || line.startsWith("• ");
    }

    /**
     * 去除 Markdown 行内标记（粗体/斜体/行内代码），保留文本
     */
    private String sanitize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("**", "").replace("__", "").replace("`", "");
    }

    /**
     * 按级探测可用中文字体（仅首次加载，之后复用）
     */
    private BaseFont resolveChineseFont() {
        if (cachedFont != null) {
            return cachedFont;
        }
        synchronized (this) {
            if (cachedFont != null) {
                return cachedFont;
            }
            for (String resource : CLASSPATH_FONT_CANDIDATES) {
                if (getClass().getResource(resource) == null) {
                    continue;
                }
                try {
                    cachedFont = BaseFont.createFont(resource, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                    log.info("报告PDF中文字体已加载(classpath): {}", resource);
                    return cachedFont;
                } catch (Exception e) {
                    log.warn("classpath 中文字体加载失败({}): {}", resource, e.getMessage());
                }
            }
            for (String path : FILE_FONT_CANDIDATES) {
                if (!Files.isRegularFile(Path.of(path))) {
                    continue;
                }
                try {
                    cachedFont = BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                    log.info("报告PDF中文字体已加载: {}", path);
                    return cachedFont;
                } catch (Exception e) {
                    log.warn("中文字体加载失败({}): {}", path, e.getMessage());
                }
            }
            throw new FontUnavailableException(
                    "未找到可用的中文字体：请将中文 TTF 字体放入 classpath:/fonts/report-cn.ttf 或系统字体目录后重试");
        }
    }

    /**
     * 无可用中文字体异常（由接口层转换为明确错误响应）
     */
    public static class FontUnavailableException extends RuntimeException {
        public FontUnavailableException(String message) {
            super(message);
        }
    }
}
