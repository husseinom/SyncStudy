package com.syncstudy.BL.ReportsManager;

import java.util.List;

/**
 * ReportsFacade - Singleton facade for report operations
 * UC - Manage Reports
 */
public class ReportsFacade {
    private static ReportsFacade instance;
    private ReportsManager reportsManager;
    
    // Private constructor for singleton
    private ReportsFacade() {
        this.reportsManager = ReportsManager.getInstance();
    }
    
    // Singleton getInstance method
    public static ReportsFacade getInstance() {
        if (instance == null) {
            instance = new ReportsFacade();
        }
        return instance;
    }
    
    /**
     * Get all reports
     * @return List of all reports
     */
    public List<Report> getAllReports() {
        return reportsManager.getAllReports();
    }
    
    /**
     * Get pending reports
     * @return List of pending reports
     */
    public List<Report> getPendingReports() {
        return reportsManager.getPendingReports();
    }
    
    /**
     * Get report details
     * @param reportId Report ID
     * @return Report details
     */
    public Report getReportDetails(Long reportId) {
        return reportsManager.getReportDetails(reportId);
    }
    
    /**
     * Validate a report
     * @param reportId Report ID
     * @param adminId Admin who validates
     */
    public void validateReport(Long reportId, Long adminId) {
        reportsManager.validateReport(reportId, adminId);
    }
    
    /**
     * Reject a report
     * @param reportId Report ID
     * @param adminId Admin who rejects
     */
    public void rejectReport(Long reportId, Long adminId) {
        reportsManager.rejectReport(reportId, adminId);
    }
    
    /**
     * Create a new report
     * @param reportedUserId User being reported
     * @param reporterUserId User making the report
     * @param problemType Type of problem
     * @param description Description of the problem
     * @param reportedContent The content being reported
     * @return Created report
     */
    public Report createReport(Long reportedUserId, Long reporterUserId, 
                               String problemType, String description, String reportedContent) {
        Report report = new Report(reportedUserId, reporterUserId, problemType, description, reportedContent);
        return reportsManager.createReport(report);
    }
    
    /**
     * Get reports against a specific user
     * @param userId User ID
     * @return List of reports
     */
    public List<Report> getReportsByReportedUser(Long userId) {
        return reportsManager.getReportsByReportedUser(userId);
    }
    
    /**
     * Delete a report
     * @param reportId Report ID
     */
    public void deleteReport(Long reportId) {
        reportsManager.deleteReport(reportId);
    }
}
