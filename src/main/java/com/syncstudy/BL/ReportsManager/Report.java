package com.syncstudy.BL.ReportsManager;

import java.time.LocalDateTime;

/**
 * Report - Entity class representing a user report
 * UC - Manage Reports
 */
public class Report {
    private Long reportId;
    private LocalDateTime reportDate;
    private Long reportedUserId;
    private Long reporterUserId;
    private String problemType;
    private String description;
    private String reportedContent;
    private String status;
    private Long adminId;
    private LocalDateTime actionDate;
    
    // Additional fields for display
    private String reportedUsername;
    private String reporterUsername;
    
    // Default constructor
    public Report() {
        this.reportDate = LocalDateTime.now();
        this.status = "Pending";
    }
    
    // Constructor for creating new report
    public Report(Long reportedUserId, Long reporterUserId, String problemType, 
                  String description, String reportedContent) {
        this.reportedUserId = reportedUserId;
        this.reporterUserId = reporterUserId;
        this.problemType = problemType;
        this.description = description;
        this.reportedContent = reportedContent;
        this.reportDate = LocalDateTime.now();
        this.status = "Pending";
    }
    
    // Full constructor
    public Report(Long reportId, LocalDateTime reportDate, Long reportedUserId, 
                  Long reporterUserId, String problemType, String description,
                  String reportedContent, String status, Long adminId, LocalDateTime actionDate) {
        this.reportId = reportId;
        this.reportDate = reportDate;
        this.reportedUserId = reportedUserId;
        this.reporterUserId = reporterUserId;
        this.problemType = problemType;
        this.description = description;
        this.reportedContent = reportedContent;
        this.status = status;
        this.adminId = adminId;
        this.actionDate = actionDate;
    }
    
    // Getters and Setters
    public Long getReportId() {
        return reportId;
    }
    
    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }
    
    public LocalDateTime getReportDate() {
        return reportDate;
    }
    
    public void setReportDate(LocalDateTime reportDate) {
        this.reportDate = reportDate;
    }
    
    public Long getReportedUserId() {
        return reportedUserId;
    }
    
    public void setReportedUserId(Long reportedUserId) {
        this.reportedUserId = reportedUserId;
    }
    
    public Long getReporterUserId() {
        return reporterUserId;
    }
    
    public void setReporterUserId(Long reporterUserId) {
        this.reporterUserId = reporterUserId;
    }
    
    public String getProblemType() {
        return problemType;
    }
    
    public void setProblemType(String problemType) {
        this.problemType = problemType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getReportedContent() {
        return reportedContent;
    }
    
    public void setReportedContent(String reportedContent) {
        this.reportedContent = reportedContent;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Long getAdminId() {
        return adminId;
    }
    
    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }
    
    public LocalDateTime getActionDate() {
        return actionDate;
    }
    
    public void setActionDate(LocalDateTime actionDate) {
        this.actionDate = actionDate;
    }
    
    public String getReportedUsername() {
        return reportedUsername;
    }
    
    public void setReportedUsername(String reportedUsername) {
        this.reportedUsername = reportedUsername;
    }
    
    public String getReporterUsername() {
        return reporterUsername;
    }
    
    public void setReporterUsername(String reporterUsername) {
        this.reporterUsername = reporterUsername;
    }
    
    // Helper methods
    public boolean isPending() {
        return "Pending".equals(status);
    }
    
    public boolean isValidated() {
        return "Validated".equals(status);
    }
    
    public boolean isRejected() {
        return "Rejected".equals(status);
    }
}
