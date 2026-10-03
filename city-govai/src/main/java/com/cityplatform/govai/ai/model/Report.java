package com.cityplatform.govai.ai.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * 政务报告
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Report {

    private String reportType;
    private String city;
    private String date;
    private String summary;
    private Map<String, String> keyMetrics;
    private String analysis;
    private List<String> recommendations;

    public Report() {}

    public Report(String reportType, String city, String date, String summary,
                 Map<String, String> keyMetrics, String analysis, List<String> recommendations) {
        this.reportType = reportType;
        this.city = city;
        this.date = date;
        this.summary = summary;
        this.keyMetrics = keyMetrics;
        this.analysis = analysis;
        this.recommendations = recommendations;
    }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public Map<String, String> getKeyMetrics() { return keyMetrics; }
    public void setKeyMetrics(Map<String, String> keyMetrics) { this.keyMetrics = keyMetrics; }

    public String getAnalysis() { return analysis; }
    public void setAnalysis(String analysis) { this.analysis = analysis; }

    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }

    @Override
    public String toString() {
        return "Report{" +
                "reportType='" + reportType + '\'' +
                ", city='" + city + '\'' +
                ", date='" + date + '\'' +
                '}';
    }
}
