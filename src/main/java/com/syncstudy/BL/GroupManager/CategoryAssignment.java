package com.syncstudy.BL.GroupManager;

import java.time.LocalDateTime;

/**
 * CategoryAssignment - Entity class representing a category administrator assignment
 * Tracks the history of category administrator assignments
 */
public class CategoryAssignment {
    // Attributes
    private Long id;                    // PK
    private Long categoryId;            // FK to Category
    private Long userId;                // FK to User
    private LocalDateTime assignedAt;
    private LocalDateTime removedAt;
    private boolean isActive;

    // Constructors
    public CategoryAssignment() {
        this.assignedAt = LocalDateTime.now();
        this.isActive = true;
    }

    public CategoryAssignment(Long categoryId, Long userId) {
        this();
        this.categoryId = categoryId;
        this.userId = userId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getRemovedAt() {
        return removedAt;
    }

    public void setRemovedAt(LocalDateTime removedAt) {
        this.removedAt = removedAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}

