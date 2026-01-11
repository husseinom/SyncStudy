package com.syncstudy.BL.GroupManager;

import java.util.List;

/**
 * CategoryFacade - Singleton facade for category operations
 * Provides simplified interface for UI layer
 */
public class CategoryFacade {

    // Singleton instance
    private static CategoryFacade instance;

    // Attributes
    private CategoryManager categoryManager;

    // Private constructor for singleton
    private CategoryFacade() {
        this.categoryManager = CategoryManager.getInstance();
    }

    /**
     * Get singleton instance
     * @return CategoryFacade instance
     */
    public static synchronized CategoryFacade getInstance() {
        if (instance == null) {
            instance = new CategoryFacade();
        }
        return instance;
    }

    /**
     * Get all categories
     * @return List of all categories
     */
    public List<Category> getAllCategories() {
        return categoryManager.getAllCategories();
    }

    /**
     * Get a category by ID
     * @param categoryId the category ID
     * @return Category or null
     */
    public Category getCategoryById(Long categoryId) {
        return categoryManager.getCategoryById(categoryId);
    }

    /**
     * Create a new category
     * @param name category name
     * @param description category description
     * @param icon category icon
     * @param color category color
     * @param adminId category administrator ID
     * @return created Category or null
     * @throws IllegalArgumentException if validation fails
     */
    public Category createCategory(String name, String description, String icon, String color, Long adminId)
            throws IllegalArgumentException {
        return categoryManager.createCategory(name, description, icon, color, adminId);
    }

    /**
     * Update an existing category
     * @param categoryId category ID
     * @param name new name
     * @param description new description
     * @param icon new icon
     * @param color new color
     * @param adminId new administrator ID
     * @return true if successful
     * @throws IllegalArgumentException if validation fails
     */
    public boolean updateCategory(Long categoryId, String name, String description,
            String icon, String color, Long adminId) throws IllegalArgumentException {
        return categoryManager.updateCategory(categoryId, name, description, icon, color, adminId);
    }

    /**
     * Delete a category
     * @param categoryId category ID
     * @return true if successful
     * @throws IllegalStateException if category has groups
     */
    public boolean deleteCategory(Long categoryId) throws IllegalStateException {
        return categoryManager.deleteCategory(categoryId);
    }

    /**
     * Check if a category can be deleted
     * @param categoryId category ID
     * @return true if category has no groups
     */
    public boolean canDeleteCategory(Long categoryId) {
        return categoryManager.canDeleteCategory(categoryId);
    }

    /**
     * Get groups in a category
     * @param categoryId category ID
     * @return List of groups
     */
    public List<Group> getCategoryGroups(Long categoryId) {
        return categoryManager.getCategoryGroups(categoryId);
    }

    /**
     * Get group count in a category
     * @param categoryId category ID
     * @return number of groups
     */
    public int getCategoryGroupCount(Long categoryId) {
        return categoryManager.getCategoryGroupCount(categoryId);
    }

    /**
     * Get all groups
     * @return List of all groups
     */
    public List<Group> getAllGroups() {
        return categoryManager.getAllGroups();
    }

    /**
     * Get unassigned groups
     * @return List of unassigned groups
     */
    public List<Group> getUnassignedGroups() {
        return categoryManager.getUnassignedGroups();
    }

    /**
     * Assign groups to a category
     * @param categoryId category ID
     * @param groupIds list of group IDs
     * @return true if successful
     */
    public boolean assignGroups(Long categoryId, List<Long> groupIds) {
        return categoryManager.assignGroups(categoryId, groupIds);
    }

    /**
     * Remove a group from its category
     * @param groupId group ID
     * @return true if successful
     */
    public boolean removeGroupFromCategory(Long groupId) {
        return categoryManager.removeGroupFromCategory(groupId);
    }

    /**
     * Search categories
     * @param searchTerm search term
     * @return List of matching categories
     */
    public List<Category> searchCategories(String searchTerm) {
        return categoryManager.searchCategories(searchTerm);
    }
}

