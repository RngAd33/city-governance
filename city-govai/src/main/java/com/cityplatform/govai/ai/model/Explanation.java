package com.cityplatform.govai.ai.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 告警解释结果
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Explanation {

    private String alertType;
    private String city;
    private String rootCause;
    private List<String> contributingFactors;
    private List<String> recommendedActions;
    private double confidence;
    private String summary;

    public Explanation() {}

    public Explanation(String alertType, String city, String rootCause,
                       List<String> contributingFactors, List<String> recommendedActions,
                       double confidence, String summary) {
        this.alertType = alertType;
        this.city = city;
        this.rootCause = rootCause;
        this.contributingFactors = contributingFactors;
        this.recommendedActions = recommendedActions;
        this.confidence = confidence;
        this.summary = summary;
    }

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }

    public List<String> getContributingFactors() { return contributingFactors; }
    public void setContributingFactors(List<String> contributingFactors) { this.contributingFactors = contributingFactors; }

    public List<String> getRecommendedActions() { return recommendedActions; }
    public void setRecommendedActions(List<String> recommendedActions) { this.recommendedActions = recommendedActions; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    @Override
    public String toString() {
        return "Explanation{" +
                "alertType='" + alertType + '\'' +
                ", city='" + city + '\'' +
                ", rootCause='" + rootCause + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}
