package com.cityplatform.govai.ai.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 应急预案
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmergencyPlan {

    private String planName;
    private String eventType;
    private String severity;
    private String applicableScope;
    private String responseLevel;
    private List<String> actionItems;
    private List<String> responsibleDepartments;

    public EmergencyPlan() {}

    public EmergencyPlan(String planName, String eventType, String severity,
                         String applicableScope, String responseLevel,
                         List<String> actionItems, List<String> responsibleDepartments) {
        this.planName = planName;
        this.eventType = eventType;
        this.severity = severity;
        this.applicableScope = applicableScope;
        this.responseLevel = responseLevel;
        this.actionItems = actionItems;
        this.responsibleDepartments = responsibleDepartments;
    }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getApplicableScope() { return applicableScope; }
    public void setApplicableScope(String applicableScope) { this.applicableScope = applicableScope; }

    public String getResponseLevel() { return responseLevel; }
    public void setResponseLevel(String responseLevel) { this.responseLevel = responseLevel; }

    public List<String> getActionItems() { return actionItems; }
    public void setActionItems(List<String> actionItems) { this.actionItems = actionItems; }

    public List<String> getResponsibleDepartments() { return responsibleDepartments; }
    public void setResponsibleDepartments(List<String> responsibleDepartments) { this.responsibleDepartments = responsibleDepartments; }

    @Override
    public String toString() {
        return "EmergencyPlan{" +
                "planName='" + planName + '\'' +
                ", eventType='" + eventType + '\'' +
                ", severity='" + severity + '\'' +
                ", responseLevel='" + responseLevel + '\'' +
                '}';
    }
}
