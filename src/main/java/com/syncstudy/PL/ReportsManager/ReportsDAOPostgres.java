package com.syncstudy.PL.ReportsManager;

import com.syncstudy.BL.ReportsManager.Report;
import com.syncstudy.BL.ReportsManager.ReportsDAO;
import com.syncstudy.PL.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ReportsDAOPostgres - PostgreSQL implementation for report operations
 * UC - Manage Reports
 */
public class ReportsDAOPostgres extends ReportsDAO {
    private DatabaseConnection dbConnection;
    
    public ReportsDAOPostgres() {
        this.dbConnection = DatabaseConnection.getInstance();
        initializeDatabase();
    }
    
    private void initializeDatabase() {
        try (Connection conn = dbConnection.getConnection()) {
            createTableReports(conn);
            System.out.println("Reports database table initialized successfully.");
        } catch (SQLException e) {
            System.err.println("Error initializing Reports database: " + e.getMessage());
        }
    }
    
    private void createTableReports(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS reports (
                report_id SERIAL PRIMARY KEY,
                report_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                reported_user_id BIGINT NOT NULL,
                reporter_user_id BIGINT NOT NULL,
                problem_type VARCHAR(50) NOT NULL,
                description TEXT,
                reported_content TEXT,
                status VARCHAR(20) DEFAULT 'Pending' CHECK (status IN ('Pending', 'Validated', 'Rejected')),
                admin_id BIGINT,
                action_date TIMESTAMP,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
        
        // Create indexes
        String indexSql = """
            CREATE INDEX IF NOT EXISTS idx_reports_status ON reports(status);
            CREATE INDEX IF NOT EXISTS idx_reports_reported_user ON reports(reported_user_id);
            CREATE INDEX IF NOT EXISTS idx_reports_reporter ON reports(reporter_user_id);
        """;
        
        try (PreparedStatement stmt = conn.prepareStatement(indexSql)) {
            stmt.executeUpdate();
        }
    }
    
    @Override
    public Report createReport(Report report) {
        String sql = """
            INSERT INTO reports (reported_user_id, reporter_user_id, problem_type, description, reported_content, status, report_date)
            VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING report_id
        """;
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, report.getReportedUserId());
            stmt.setLong(2, report.getReporterUserId());
            stmt.setString(3, report.getProblemType());
            stmt.setString(4, report.getDescription());
            stmt.setString(5, report.getReportedContent());
            stmt.setString(6, report.getStatus());
            stmt.setTimestamp(7, Timestamp.valueOf(report.getReportDate()));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    report.setReportId(rs.getLong("report_id"));
                }
            }
            
            return report;
            
        } catch (SQLException e) {
            throw new RuntimeException("Error creating report: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<Report> getAllReports() {
        String sql = """
            SELECT r.*, 
                   u1.username as reported_username,
                   u2.username as reporter_username
            FROM reports r
            LEFT JOIN users u1 ON r.reported_user_id = u1.id
            LEFT JOIN users u2 ON r.reporter_user_id = u2.id
            ORDER BY r.report_date DESC
        """;
        
        return executeReportQuery(sql, null);
    }
    
    @Override
    public List<Report> getPendingReports() {
        String sql = """
            SELECT r.*, 
                   u1.username as reported_username,
                   u2.username as reporter_username
            FROM reports r
            LEFT JOIN users u1 ON r.reported_user_id = u1.id
            LEFT JOIN users u2 ON r.reporter_user_id = u2.id
            WHERE r.status = 'Pending'
            ORDER BY r.report_date ASC
        """;
        
        return executeReportQuery(sql, null);
    }
    
    @Override
    public Report getReportById(Long reportId) {
        String sql = """
            SELECT r.*, 
                   u1.username as reported_username,
                   u2.username as reporter_username
            FROM reports r
            LEFT JOIN users u1 ON r.reported_user_id = u1.id
            LEFT JOIN users u2 ON r.reporter_user_id = u2.id
            WHERE r.report_id = ?
        """;
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, reportId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToReport(rs);
                }
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error getting report by ID: " + e.getMessage(), e);
        }
        
        return null;
    }
    
    @Override
    public void updateReportStatus(Long reportId, String status, Long adminId) {
        String sql = """
            UPDATE reports 
            SET status = ?, admin_id = ?, action_date = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
            WHERE report_id = ?
        """;
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            stmt.setLong(2, adminId);
            stmt.setLong(3, reportId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("Report not found with ID: " + reportId);
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error updating report status: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<Report> getReportsByReportedUser(Long userId) {
        String sql = """
            SELECT r.*, 
                   u1.username as reported_username,
                   u2.username as reporter_username
            FROM reports r
            LEFT JOIN users u1 ON r.reported_user_id = u1.id
            LEFT JOIN users u2 ON r.reporter_user_id = u2.id
            WHERE r.reported_user_id = ?
            ORDER BY r.report_date DESC
        """;
        
        List<Report> reports = new ArrayList<>();
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reports.add(mapResultSetToReport(rs));
                }
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error getting reports by reported user: " + e.getMessage(), e);
        }
        
        return reports;
    }
    
    @Override
    public List<Report> getReportsByReporter(Long userId) {
        String sql = """
            SELECT r.*, 
                   u1.username as reported_username,
                   u2.username as reporter_username
            FROM reports r
            LEFT JOIN users u1 ON r.reported_user_id = u1.id
            LEFT JOIN users u2 ON r.reporter_user_id = u2.id
            WHERE r.reporter_user_id = ?
            ORDER BY r.report_date DESC
        """;
        
        List<Report> reports = new ArrayList<>();
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reports.add(mapResultSetToReport(rs));
                }
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error getting reports by reporter: " + e.getMessage(), e);
        }
        
        return reports;
    }
    
    @Override
    public void deleteReport(Long reportId) {
        String sql = "DELETE FROM reports WHERE report_id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, reportId);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting report: " + e.getMessage(), e);
        }
    }
    
    // Helper methods
    private List<Report> executeReportQuery(String sql, Long param) {
        List<Report> reports = new ArrayList<>();
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            if (param != null) {
                stmt.setLong(1, param);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reports.add(mapResultSetToReport(rs));
                }
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error executing report query: " + e.getMessage(), e);
        }
        
        return reports;
    }
    
    private Report mapResultSetToReport(ResultSet rs) throws SQLException {
        Report report = new Report();
        
        report.setReportId(rs.getLong("report_id"));
        
        Timestamp reportDate = rs.getTimestamp("report_date");
        if (reportDate != null) {
            report.setReportDate(reportDate.toLocalDateTime());
        }
        
        report.setReportedUserId(rs.getLong("reported_user_id"));
        report.setReporterUserId(rs.getLong("reporter_user_id"));
        report.setProblemType(rs.getString("problem_type"));
        report.setDescription(rs.getString("description"));
        report.setReportedContent(rs.getString("reported_content"));
        report.setStatus(rs.getString("status"));
        
        Long adminId = rs.getLong("admin_id");
        if (!rs.wasNull()) {
            report.setAdminId(adminId);
        }
        
        Timestamp actionDate = rs.getTimestamp("action_date");
        if (actionDate != null) {
            report.setActionDate(actionDate.toLocalDateTime());
        }
        
        // Get usernames if available
        try {
            report.setReportedUsername(rs.getString("reported_username"));
            report.setReporterUsername(rs.getString("reporter_username"));
        } catch (SQLException e) {
            // Columns might not exist in all queries
        }
        
        return report;
    }
}
