// src/main/java/com/syncstudy/BL/FileManager/SharedFileManager.java
package com.syncstudy.BL.FileManager;

import com.syncstudy.PL.FileManager.SharedFileDAOPostgres;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

public class SharedFileManager {

    private SharedFileDAO sharedFileDAO;
    private static final String UPLOAD_DIRECTORY = "uploads/group_files/";
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50 MB
    private static final String[] SUPPORTED_TYPES = {"pdf", "docx", "pptx", "txt", "jpg", "png", "jpeg"};

    public SharedFileManager() {
        this.sharedFileDAO = new SharedFileDAOPostgres();
        initializeUploadDirectory();
    }

    private void initializeUploadDirectory() {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIRECTORY);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            System.err.println("Error creating upload directory: " + e.getMessage());
        }
    }

    public SharedFile uploadFile(Long groupId, Long uploaderId, File file, String description) {
        // Validate file
        validateFile(file);

        String originalFileName = file.getName();
        String fileExtension = getFileExtension(originalFileName);
        String uniqueFileName = generateUniqueFileName(fileExtension);
        String filePath = UPLOAD_DIRECTORY + uniqueFileName;

        try {
            // Copy file to upload directory
            Path targetPath = Paths.get(filePath);
            Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Create SharedFile object
            SharedFile sharedFile = new SharedFile(
                    groupId,
                    uploaderId,
                    uniqueFileName,
                    originalFileName,
                    filePath,
                    file.length(),
                    fileExtension,
                    description
            );

            // Save to database
            return sharedFileDAO.insert(sharedFile);

        } catch (IOException e) {
            throw new RuntimeException("Upload failed. Please check your connection and try again.", e);
        }
    }

    public List<SharedFile> getFilesByGroupId(Long groupId) {
        return sharedFileDAO.findByGroupId(groupId);
    }

    public SharedFile getFileById(Long fileId) {
        SharedFile file = sharedFileDAO.findById(fileId);
        if (file == null) {
            throw new IllegalStateException("File not found.");
        }
        return file;
    }

    public File downloadFile(Long fileId, Long userId) {
        SharedFile sharedFile = sharedFileDAO.findById(fileId);
        if (sharedFile == null) {
            throw new IllegalStateException("File not found.");
        }

        File file = new File(sharedFile.getFilePath());
        if (!file.exists()) {
            throw new RuntimeException("Download failed. Please try again.");
        }

        return file;
    }

    public boolean deleteFile(Long fileId, Long userId, boolean isAdmin) {
        if (!sharedFileDAO.canDeleteFile(fileId, userId, isAdmin)) {
            throw new SecurityException("You do not have permission to delete this file. Only the uploader or group admins can delete files.");
        }

        SharedFile file = sharedFileDAO.findById(fileId);
        if (file == null) {
            throw new IllegalStateException("File not found.");
        }

        // Delete physical file
        try {
            Path filePath = Paths.get(file.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("Error deleting physical file: " + e.getMessage());
        }

        // Delete from database
        return sharedFileDAO.delete(fileId);
    }

    private void validateFile(File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File not found.");
        }

        // Check file size
        if (file.length() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds the maximum limit of 50 MB. Please select a smaller file.");
        }

        // Check file type
        String extension = getFileExtension(file.getName()).toLowerCase();
        boolean isSupported = false;
        for (String supportedType : SUPPORTED_TYPES) {
            if (supportedType.equals(extension)) {
                isSupported = true;
                break;
            }
        }

        if (!isSupported) {
            throw new IllegalArgumentException("File type not supported. Please upload PDF, DOCX, PPTX, TXT, JPG, or PNG files.");
        }
    }

    private String getFileExtension(String fileName) {
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return fileName.substring(lastIndexOf + 1);
    }

    private String generateUniqueFileName(String extension) {
        return UUID.randomUUID().toString() + "." + extension;
    }
}