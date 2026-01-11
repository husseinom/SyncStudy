package com.syncstudy.BL.GroupManager;

import java.util.List;

/**
 * CategoryDAO - Abstract class for Category data access operations
 * Defines all database operations related to categories and category assignments
 */
public abstract class CategoryDAO {

    /**
     * Get all categories from the database
     * @return List of all categories with their groups count
     */
    public abstract List<Category> getAllCategories();

    /**
     * Get a category by its ID
     * @param categoryId the category ID
     * @return Category object or null if not found
     */
    public abstract Category getCategoryById(Long categoryId);

    /**
     * Create a new category
     * @param category the category to create
     * @return the created category with generated ID
     */
    public abstract Category createCategory(Category category);

    /**
     * Update an existing category
     * @param category the category with updated values
     * @return true if update was successful
     */
    public abstract boolean updateCategory(Category category);

    /**
     * Delete a category by its ID
     * @param categoryId the category ID to delete
     * @return true if deletion was successful
     */
    public abstract boolean deleteCategory(Long categoryId);

    /**
     * Check if a category name is unique
     * @param name the name to check
     * @param excludeCategoryId optional category ID to exclude (for updates)
     * @return true if the name is unique
     */
    public abstract boolean isCategoryNameUnique(String name, Long excludeCategoryId);

    /**
     * Check if a category has groups assigned
     * @param categoryId the category ID to check
     * @return true if the category has groups
     */
    public abstract boolean hasCategoryGroups(Long categoryId);

    /**
     * Get the count of groups in a category
     * @param categoryId the category ID
     * @return number of groups in the category
     */
    public abstract int getCategoryGroupCount(Long categoryId);

    /**
     * Get all groups in a specific category
     * @param categoryId the category ID
     * @return List of groups in the category
     */
    public abstract List<Group> getGroupsInCategory(Long categoryId);

    /**
     * Assign a category administrator
     * @param categoryId the category ID
     * @param userId the user ID to assign as administrator
     * @return true if assignment was successful
     */
    public abstract boolean assignCategoryAdministrator(Long categoryId, Long userId);

    /**
     * Remove the category administrator
     * @param categoryId the category ID
     * @param previousAdminId the previous admin user ID
     * @return true if removal was successful
     */
    public abstract boolean removeCategoryAdministrator(Long categoryId, Long previousAdminId);

    /**
     * Get the current category assignment
     * @param categoryId the category ID
     * @return the active CategoryAssignment or null
     */
    public abstract CategoryAssignment getCategoryAssignment(Long categoryId);

    /**
     * Get all groups (for assignment purposes)
     * @return List of all groups
     */
    public abstract List<Group> getAllGroups();

    /**
     * Get groups that are not assigned to any category
     * @return List of unassigned groups
     */
    public abstract List<Group> getUnassignedGroups();

    /**
     * Assign a group to a category
     * @param groupId the group ID
     * @param categoryId the category ID
     * @return true if assignment was successful
     */
    public abstract boolean assignGroupToCategory(Long groupId, Long categoryId);

    /**
     * Remove a group from its category
     * @param groupId the group ID
     * @return true if removal was successful
     */
    public abstract boolean removeGroupFromCategory(Long groupId);

    /**
     * Search categories by name
     * @param searchTerm the search term
     * @return List of matching categories
     */
    public abstract List<Category> searchCategories(String searchTerm);
}

