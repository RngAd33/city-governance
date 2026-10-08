package com.cityplatform.govai.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 政务报告实体
 * 持久化 AI 生成的政务报告（报告中心数据源），正文为 Markdown
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "govai_report")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 报告标题 */
    @Column(nullable = false, length = 256)
    private String title;

    /** 报告类型（中文标准名：日报/周报/简报/风险提示） */
    @Column(nullable = false, length = 32)
    private String type;

    /** 城市 */
    @Column(length = 64)
    private String city;

    /** 报告周期（如 今日 / 2026-10-06） */
    @Column(length = 64)
    private String period;

    /** 作者 */
    @Column(length = 64)
    private String author;

    /** 状态：COMPLETED 生成成功 / FAILED 生成失败（大模型不可用） */
    @Column(length = 16)
    private String status;

    /** 报告正文（Markdown） */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 创建时间 */
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 业务便利构造器：不含自增主键 id，创建时间自动初始化
     */
    public Report(String title, String type, String city, String period,
                  String author, String status, String content) {
        this.title = title;
        this.type = type;
        this.city = city;
        this.period = period;
        this.author = author;
        this.status = status;
        this.content = content;
    }

    /**
     * 精简输出（避免长正文污染日志）
     */
    @Override
    public String toString() {
        return "Report{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
