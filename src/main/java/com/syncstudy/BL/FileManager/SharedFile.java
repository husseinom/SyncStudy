package com.syncstudy.BL.FileManager;

import java.time.LocalDateTime;

public class SharedFile {
    private Long id;
    private Long groupId;
    private Long uploaderId;
    private String fileName;
    private String originalFileName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private String description;
    private LocalDateTime uploadTimestamp;

    private String uploaderFullName;
    private String uploaderUsername;

    public SharedFile() {}

    public SharedFile(Long groupId, Long uploaderId, String fileName, String originalFileName,
                      String filePath, Long fileSize, String fileType, String description) {
        this.groupId = groupId;
        this.uploaderId = uploaderId;
        this.fileName = fileName;
        this.originalFileName = originalFileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.description = description;
        this.uploadTimestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public Long getUploaderId() { return uploaderId; }
    public void setUploaderId(Long uploaderId) { this.uploaderId = uploaderId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getUploadTimestamp() { return uploadTimestamp; }
    public void setUploadTimestamp(LocalDateTime uploadTimestamp) { this.uploadTimestamp = uploadTimestamp; }

    public String getUploaderFullName() { return uploaderFullName; }
    public void setUploaderFullName(String uploaderFullName) { this.uploaderFullName = uploaderFullName; }

    public String getUploaderUsername() { return uploaderUsername; }
    public void setUploaderUsername(String uploaderUsername) { this.uploaderUsername = uploaderUsername; }

    public String getFormattedFileSize() {
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.2f KB", fileSize / 1024.0);
        return String.format("%.2f MB", fileSize / (1024.0 * 1024.0));
    }
}
