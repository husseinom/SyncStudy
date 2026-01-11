// src/main/java/com/syncstudy/BL/FileManager/SharedFileDAO.java
package com.syncstudy.BL.FileManager;

import java.util.List;

public abstract class SharedFileDAO {
    public abstract SharedFile insert(SharedFile file);
    public abstract SharedFile findById(Long id);
    public abstract List<SharedFile> findByGroupId(Long groupId);
    public abstract boolean delete(Long id);
    public abstract boolean canDeleteFile(Long fileId, Long userId, boolean isAdmin);
}