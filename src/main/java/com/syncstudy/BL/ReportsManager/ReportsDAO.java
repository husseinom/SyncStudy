package com.syncstudy.BL.ReportsManager;

import java.util.List;

/**
 * ReportsDAO - Abstract DAO for managing report operations
 * UC - Manage Reports
 */
public abstract class ReportsDAO {
    
    /**
     * Create a new report
     * @param report Report to create
     * @return Created Report with ID
     */
    public abstract Report createReport(Report report);
    
    /**
     * Get all reports
     * @return List of all reports
     */
    public abstract List<Report> getAllReports();
    
    /**
     * Get all pending reports
     * @return List of pending reports
     */
    public abstract List<Report> getPendingReports();
    
    /**
     * Get report by ID
     * @param reportId Report ID
     * @return Report if exists, null otherwise
     */
    public abstract Report getReportById(Long reportId);
    
    /**
     * Update report status
     * @param reportId Report ID
     * @param status New status
     * @param adminId Admin who took action
     */
    public abstract void updateReportStatus(Long reportId, String status, Long adminId);
    
    /**
     * Get reports by reported user
     * @param userId User ID who was reported
     * @return List of reports against this user
     */
    public abstract List<Report> getReportsByReportedUser(Long userId);
    
    /**
     * Get reports by reporter
     * @param userId User ID who made the reports
     * @return List of reports made by this user
     */
    public abstract List<Report> getReportsByReporter(Long userId);
    
    /**
     * Delete a report
     * @param reportId Report ID to delete
     */
    public abstract void deleteReport(Long reportId);
}
