// src/main/java/com/syncstudy/PL/FileManager/SharedFileDAOPostgres.java
package com.syncstudy.PL.FileManager;

import com.syncstudy.BL.FileManager.SharedFile;
import com.syncstudy.BL.FileManager.SharedFileDAO;
import com.syncstudy.PL.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SharedFileDAOPostgres extends SharedFileDAO {

    private DatabaseConnection dbConnection;

    public SharedFileDAOPostgres() {
        this.dbConnection = DatabaseConnection.getInstance();
        initializeDatabase();
    }

    private void initializeDatabase() {
        try (Connection conn = dbConnection.getConnection()) {
            createTableSharedFiles(conn);
        } catch (SQLException e) {
            System.err.println("Error initializing shared_files table: " + e.getMessage());
        }
    }

    private void createTableSharedFiles(Connection conn) {
        String sql = "CREATE TABLE IF NOT EXISTS shared_files (" +
                "id SERIAL PRIMARY KEY, " +
                "group_id BIGINT NOT NULL, " +
                "uploader_id BIGINT NOT NULL REFERENCES users(id), " +
                "file_name VARCHAR(255) NOT NULL, " +
                "original_file_name VARCHAR(255) NOT NULL, " +
                "file_path TEXT NOT NULL, " +
                "file_size BIGINT NOT NULL, " +
                "file_type VARCHAR(50) NOT NULL, " +
                "description TEXT, " +
                "upload_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Table 'shared_files' created or already exists.");
        } catch (SQLException e) {
            System.err.println("Error creating shared_files table: " + e.getMessage());
        }
    }

    @Override
    public SharedFile insert(SharedFile file) {
        String sql = "INSERT INTO shared_files (group_id, uploader_id, file_name, original_file_name, " +
                     "file_path, file_size, file_type, description, upload_timestamp) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, file.getGroupId());
            stmt.setLong(2, file.getUploaderId());
            stmt.setString(3, file.getFileName());
            stmt.setString(4, file.getOriginalFileName());
            stmt.setString(5, file.getFilePath());
            stmt.setLong(6, file.getFileSize());
            stmt.setString(7, file.getFileType());
            stmt.setString(8, file.getDescription());
            stmt.setTimestamp(9, Timestamp.valueOf(file.getUploadTimestamp()));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    file.setId(rs.getLong("id"));
                    return file;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error inserting file: " + e.getMessage());
        }
        return null;
    }

    @Override
    public SharedFile findById(Long id) {
        String sql = "SELECT sf.*, u.full_name, u.username FROM shared_files sf " +
                     "JOIN users u ON sf.uploader_id = u.id WHERE sf.id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractFileFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding file: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<SharedFile> findByGroupId(Long groupId) {
        String sql = "SELECT sf.*, u.full_name, u.username FROM shared_files sf " +
                     "JOIN users u ON sf.uploader_id = u.id " +
                     "WHERE sf.group_id = ? ORDER BY sf.upload_timestamp DESC";

        List<SharedFile> files = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, groupId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    files.add(extractFileFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding files for group: " + e.getMessage());
        }
        return files;
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM shared_files WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting file: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean canDeleteFile(Long fileId, Long userId, boolean isAdmin) {
        if (isAdmin) return true;

        String sql = "SELECT uploader_id FROM shared_files WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, fileId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("uploader_id") == userId;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking delete permission: " + e.getMessage());
        }
        return false;
    }

    private SharedFile extractFileFromResultSet(ResultSet rs) throws SQLException {
        SharedFile file = new SharedFile();
        file.setId(rs.getLong("id"));
        file.setGroupId(rs.getLong("group_id"));
        file.setUploaderId(rs.getLong("uploader_id"));
        file.setFileName(rs.getString("file_name"));
        file.setOriginalFileName(rs.getString("original_file_name"));
        file.setFilePath(rs.getString("file_path"));
        file.setFileSize(rs.getLong("file_size"));
        file.setFileType(rs.getString("file_type"));
        file.setDescription(rs.getString("description"));
        file.setUploadTimestamp(rs.getTimestamp("upload_timestamp").toLocalDateTime());
        file.setUploaderFullName(rs.getString("full_name"));
        file.setUploaderUsername(rs.getString("username"));
        return file;
    }
}