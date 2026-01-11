package com.syncstudy.BL.ReportsManager;

import com.syncstudy.BL.AbstractFactory;
import com.syncstudy.PL.PostgresFactory;
import java.util.List;

/**
 * ReportsManager - Singleton manager for report operations
 * UC - Manage Reports
 */
public class ReportsManager {
    private static ReportsManager instance;
    private ReportsDAO reportsDAO;
    
    // Private constructor for singleton
    private ReportsManager() {
        AbstractFactory factory = new PostgresFactory();
        this.reportsDAO = factory.createReportsDAO();
    }
    
    // Singleton getInstance method
    public static ReportsManager getInstance() {
        if (instance == null) {
            instance = new ReportsManager();
        }
        return instance;
    }
    
    /**
     * Get all reports
     * @return List of all reports
     */
    public List<Report> getAllReports() {
        return reportsDAO.getAllReports();
    }
    
    /**
     * Get pending reports
     * @return List of pending reports
     */
    public List<Report> getPendingReports() {
        return reportsDAO.getPendingReports();
    }
    
    /**
     * Get report details
     * @param reportId Report ID
     * @return Report details
     */
    public Report getReportDetails(Long reportId) {
        return reportsDAO.getReportById(reportId);
    }
    
    /**
     * Validate a report
     * @param reportId Report ID
     * @param adminId Admin who validates
     */
    public void validateReport(Long reportId, Long adminId) {
        reportsDAO.updateReportStatus(reportId, "Validated", adminId);
    }
    
    /**
     * Reject a report
     * @param reportId Report ID
     * @param adminId Admin who rejects
     */
    public void rejectReport(Long reportId, Long adminId) {
        reportsDAO.updateReportStatus(reportId, "Rejected", adminId);
    }
    
    /**
     * Create a new report
     * @param report Report to create
     * @return Created report
     */
    public Report createReport(Report report) {
        return reportsDAO.createReport(report);
    }
    
    /**
     * Get reports against a specific user
     * @param userId User ID
     * @return List of reports
     */
    public List<Report> getReportsByReportedUser(Long userId) {
        return reportsDAO.getReportsByReportedUser(userId);
    }
    
    /**
     * Delete a report
     * @param reportId Report ID
     */
    public void deleteReport(Long reportId) {
        reportsDAO.deleteReport(reportId);
    }
}
