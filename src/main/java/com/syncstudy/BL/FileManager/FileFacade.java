package com.syncstudy.BL.FileManager;

import java.io.File;
import java.util.List;

/**
 * Singleton Facade providing simplified interface for file management
 */
public class FileFacade {
    private static FileFacade instance;
    private SharedFileManager fileManager;

    private FileFacade() {
        this.fileManager = new SharedFileManager();
    }

    /**
     * Get the singleton instance of FileFacade
     * @return FileFacade instance
     */
    public static FileFacade getInstance() {
        if (instance == null) {
            synchronized (FileFacade.class) {
                if (instance == null) {
                    instance = new FileFacade();
                }
            }
        }
        return instance;
    }

    /**
     * Upload a file to a group
     * @param groupId the group ID
     * @param uploaderId the uploader's user ID
     * @param file the file to upload
     * @param description optional file description
     * @return the created SharedFile, or null if failed
     */
    public SharedFile uploadFile(Long groupId, Long uploaderId, File file, String description) {
        try {
            return fileManager.uploadFile(groupId, uploaderId, file, description);
        } catch (IllegalArgumentException e) {
            System.err.println("Validation error: " + e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            System.err.println("Upload error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Error uploading file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get all files for a group
     * @param groupId the group ID
     * @return list of shared files
     */
    public List<SharedFile> getFilesByGroupId(Long groupId) {
        try {
            return fileManager.getFilesByGroupId(groupId);
        } catch (Exception e) {
            System.err.println("Error fetching files: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Get a specific file by ID
     * @param fileId the file ID
     * @return the SharedFile, or null if not found
     */
    public SharedFile getFileById(Long fileId) {
        try {
            return fileManager.getFileById(fileId);
        } catch (IllegalStateException e) {
            System.err.println("File error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Error fetching file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Download a file
     * @param fileId the file ID
     * @param userId the user ID requesting download
     * @return the File object for download
     */
    public File downloadFile(Long fileId, Long userId) {
        try {
            return fileManager.downloadFile(fileId, userId);
        } catch (RuntimeException e) {
            System.err.println("Download error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Error downloading file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Delete a file
     * @param fileId the file ID
     * @param userId the user ID attempting deletion
     * @param isAdmin whether the user is an admin
     * @return true if deletion successful, false otherwise
     */
    public boolean deleteFile(Long fileId, Long userId, boolean isAdmin) {
        try {
            return fileManager.deleteFile(fileId, userId, isAdmin);
        } catch (SecurityException | IllegalStateException e) {
            System.err.println("Delete file error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Error deleting file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if a user can delete a specific file
     * @param fileId the file ID
     * @param userId the user ID
     * @param isAdmin whether the user is an admin
     * @return true if user can delete, false otherwise
     */
    public boolean canDeleteFile(Long fileId, Long userId, boolean isAdmin) {
        if (isAdmin) {
            return true;
        }
        try {
            SharedFile file = fileManager.getFileById(fileId);
            return file != null && file.getUploaderId().equals(userId);
        } catch (Exception e) {
            System.err.println("Error checking delete permission: " + e.getMessage());
            return false;
        }
    }
}
