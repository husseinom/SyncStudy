package com.syncstudy.BL.GroupManager;

import java.time.LocalDateTime;

/**
 * Category - Entity class representing a group category
 */
public class Category {
    // Attributes
    private Long categoryId;                    // PK
    private String name;                        // Unique, non null
    private String description;                 // max 200 chars
    private String icon;
    private String color;
    private Long categoryAdministratorId;       // FK to User, nullable
    private String categoryAdministratorName;   // Display name (not persisted directly)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int groupsCount;                    // Calculated dynamically

    // Constructors
    public Category() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Category(String name, String description, String icon, String color) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.color = color;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Category(String name, String description, String icon, String color, Long categoryAdministratorId) {
        this(name, description, icon, color);
        this.categoryAdministratorId = categoryAdministratorId;
    }
    
    // Getters and Setters
    public Long getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }

    public Long getCategoryAdministratorId() {
        return categoryAdministratorId;
    }

    public void setCategoryAdministratorId(Long categoryAdministratorId) {
        this.categoryAdministratorId = categoryAdministratorId;
    }

    public String getCategoryAdministratorName() {
        return categoryAdministratorName;
    }

    public void setCategoryAdministratorName(String categoryAdministratorName) {
        this.categoryAdministratorName = categoryAdministratorName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getGroupsCount() {
        return groupsCount;
    }

    public void setGroupsCount(int groupsCount) {
        this.groupsCount = groupsCount;
    }

    /**
     * Get display name for the icon/color combination
     * @return formatted display string
     */
    public String getIconColorDisplay() {
        return (icon != null ? icon : "") + " " + (color != null ? color : "");
    }

    @Override
    public String toString() {
        return name != null ? name : "Category";
    }
}
