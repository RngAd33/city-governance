package com.cityplatform.govai.service;

import com.cityplatform.govai.ai.LocalLLMService;
import com.cityplatform.govai.model.entity.Report;
import com.cityplatform.govai.model.enums.ReportType;
import com.cityplatform.govai.repository.ReportRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.cityplatform.govai.constant.ReportConstants.*;

/**
 * 政务报告服务
 * 生成：调用大模型撰写报告（Markdown）并持久化到 govai_report 表，供报告中心列表/详情查询
 */
@Service
@Slf4j
public class ReportService {

    @Resource
    private LocalLLMService llmService;

    @Resource
    private ReportRepository reportRepository;

    /**
     * 生成政务报告：大模型撰写正文后入库
     */
    public Report generate(ReportType type, String city, String period) {
        String content = llmService.reportGeneration(type.getLabel(), city, period);
        boolean available = isAvailable(content);
        Report report = new Report(
                extractTitle(content, city + type.getLabel()),
                type.getLabel(),
                city,
                period,
                AUTHOR,
                available ? STATUS_COMPLETED : STATUS_FAILED,
                content);
        reportRepository.save(report);
        log.info("政务报告已入库: id={}, type={}, city={}, status={}",
                report.getId(), report.getType(), report.getCity(), report.getStatus());
        return report;
    }

    /**
     * 报告列表（可按类型筛选，创建时间倒序）
     */
    public List<Report> listByType(ReportType type) {
        return type == null
                ? reportRepository.findAllByOrderByCreatedAtDesc()
                : reportRepository.findByTypeOrderByCreatedAtDesc(type.getLabel());
    }

    /**
     * 报告详情
     */
    public Optional<Report> findById(Long id) {
        return reportRepository.findById(id);
    }

    /**
     * 大模型不可用时（内容为不可用提示）判定为生成失败
     */
    private boolean isAvailable(String content) {
        return content != null && !content.startsWith(LocalLLMService.UNAVAILABLE_PREFIX);
    }

    /**
     * 提取报告标题：正文首个非空行若为 Markdown 一级标题(# )则取其文本，否则使用兜底标题
     */
    private String extractTitle(String content, String fallback) {
        if (content != null) {
            for (String line : content.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.startsWith("# ")) {
                    String title = trimmed.substring(2).trim();
                    if (!title.isEmpty()) {
                        return title.length() > TITLE_MAX_LENGTH ? title.substring(0, TITLE_MAX_LENGTH) : title;
                    }
                }
                break;
            }
        }
        return fallback;
    }
}
