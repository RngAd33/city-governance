package com.cityplatform.govai.service;

import com.cityplatform.govai.model.entity.Report;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 报告PDF生成单元测试（不依赖Spring上下文，直接实例化服务）
 */
class ReportPdfServiceTest {

    @Test
    void renderShouldGenerateValidPdfWithChineseContent() throws Exception {
        ReportPdfService service = new ReportPdfService();
        Report report = new Report(
                "南京市城市运行日报",
                "日报",
                "南京市",
                "今日",
                "城市治理AI助手",
                "COMPLETED",
                "# 南京市城市运行日报\n\n## 摘要\n今日城市运行总体平稳，各项指标处于正常区间。\n\n"
                        + "## 关键指标\n\n| 指标 | 数值 | 状态 |\n|---|---|---|\n| AQI | 65 | 良好 |\n| 平均车速 | 38km/h | 畅通 |\n\n"
                        + "## 分析\n- **空气质量**保持良好\n- 交通运行平稳\n\n---\n\n## 建议\n1. 持续关注晚高峰车流\n2. 加强重点区域巡查\n");

        byte[] pdf = service.render(report);

        assertNotNull(pdf);
        assertTrue(pdf.length > 500, "PDF 内容应非空");
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));

        Path output = Path.of("target", "report-pdf-test.pdf");
        Files.write(output, pdf);
        System.out.println("PDF 测试输出: " + output.toAbsolutePath() + " (" + pdf.length + " bytes)");
    }
}
