package com.syncstudy.BL.GroupManager;

import com.syncstudy.BL.AbstractFactory;
import com.syncstudy.PL.PostgresFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * CategoryManager - Singleton class for managing category operations
 * Contains all business logic for category management
 */
public class CategoryManager {

    // Singleton instance
    private static CategoryManager instance;

    // Attributes
    private CategoryDAO categoryDAO;

    // Validation patterns
    private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9àâäéèêëïîôùûüç\\s\\-_']+$");
    private static final int MAX_DESCRIPTION_LENGTH = 200;

    // Private constructor for singleton
    private CategoryManager() {
        AbstractFactory factory = new PostgresFactory();
        this.categoryDAO = factory.createCategoryDAO();
    }

    /**
     * Get singleton instance
     * @return CategoryManager instance
     */
    public static synchronized CategoryManager getInstance() {
        if (instance == null) {
            instance = new CategoryManager();
        }
        return instance;
    }

    /**
     * Get all categories
     * @return List of all categories
     */
    public List<Category> getAllCategories() {
        return categoryDAO.getAllCategories();
    }

    /**
     * Get a category by ID
     * @param categoryId the category ID
     * @return Category or null if not found
     */
    public Category getCategoryById(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryDAO.getCategoryById(categoryId);
    }

    /**
     * Create a new category with validation
     * @param name category name (required, unique)
     * @param description category description (optional, max 200 chars)
     * @param icon category icon
     * @param color category color
     * @param adminId category administrator ID (optional)
     * @return created Category or null if failed
     * @throws IllegalArgumentException if validation fails
     */
    public Category createCategory(String name, String description, String icon, String color, Long adminId)
            throws IllegalArgumentException {

        // Validate name
        validateCategoryName(name, null);

        // Validate description
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters.");
        }

        // Create category
        Category category = new Category(name.trim(), description, icon, color, adminId);
        Category created = categoryDAO.createCategory(category);

        if (created != null && adminId != null) {
            // Log the assignment
            System.out.println("LOG: Category '" + name + "' created with administrator ID: " + adminId);

            // Send notification to new admin
            NotificationService.getInstance().sendNotification(adminId,
                "You have been assigned as Category Administrator for " + name +
                ". You are now responsible for managing groups in this category.");
        }

        return created;
    }

    /**
     * Update an existing category
     * @param categoryId category ID to update
     * @param name new name
     * @param description new description
     * @param icon new icon
     * @param color new color
     * @param newAdminId new administrator ID (can be null to remove)
     * @return true if update was successful
     * @throws IllegalArgumentException if validation fails
     */
    public boolean updateCategory(Long categoryId, String name, String description,
            String icon, String color, Long newAdminId) throws IllegalArgumentException {

        if (categoryId == null) {
            throw new IllegalArgumentException("Category ID is required.");
        }

        // Get current category
        Category currentCategory = categoryDAO.getCategoryById(categoryId);
        if (currentCategory == null) {
            throw new IllegalArgumentException("Category not found.");
        }

        // Validate name (exclude current category for uniqueness check)
        validateCategoryName(name, categoryId);

        // Validate description
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters.");
        }

        Long previousAdminId = currentCategory.getCategoryAdministratorId();

        // Update category
        Category updatedCategory = new Category(name.trim(), description, icon, color, newAdminId);
        updatedCategory.setCategoryId(categoryId);

        boolean success = categoryDAO.updateCategory(updatedCategory);

        if (success) {
            System.out.println("LOG: Category '" + name + "' updated.");

            // Handle admin change notifications
            handleAdminChangeNotifications(currentCategory.getName(), previousAdminId, newAdminId);
        }

        return success;
    }

    /**
     * Delete a category (only if it has no groups)
     * @param categoryId category ID to delete
     * @return true if deletion was successful
     * @throws IllegalStateException if category has groups
     */
    public boolean deleteCategory(Long categoryId) throws IllegalStateException {
        if (categoryId == null) {
            throw new IllegalArgumentException("Category ID is required.");
        }

        // Check if category can be deleted
        if (!canDeleteCategory(categoryId)) {
            int groupCount = categoryDAO.getCategoryGroupCount(categoryId);
            throw new IllegalStateException(
                "This category contains " + groupCount + " groups. " +
                "Please reassign or remove all associated groups before deleting this category.");
        }

        Category category = categoryDAO.getCategoryById(categoryId);
        if (category == null) {
            throw new IllegalArgumentException("Category not found.");
        }

        // Notify admin if exists
        if (category.getCategoryAdministratorId() != null) {
            NotificationService.getInstance().sendNotification(
                category.getCategoryAdministratorId(),
                "The category " + category.getName() + " has been deleted.");
        }

        boolean success = categoryDAO.deleteCategory(categoryId);

        if (success) {
            System.out.println("LOG: Category '" + category.getName() + "' deleted.");
        }

        return success;
    }

    /**
     * Check if a category can be deleted (has no groups)
     * @param categoryId category ID
     * @return true if category can be deleted
     */
    public boolean canDeleteCategory(Long categoryId) {
        return !categoryDAO.hasCategoryGroups(categoryId);
    }

    /**
     * Get groups in a category
     * @param categoryId category ID
     * @return List of groups in the category
     */
    public List<Group> getCategoryGroups(Long categoryId) {
        if (categoryId == null) {
            return new ArrayList<>();
        }
        return categoryDAO.getGroupsInCategory(categoryId);
    }

    /**
     * Get the count of groups in a category
     * @param categoryId category ID
     * @return number of groups
     */
    public int getCategoryGroupCount(Long categoryId) {
        if (categoryId == null) {
            return 0;
        }
        return categoryDAO.getCategoryGroupCount(categoryId);
    }

    /**
     * Get all groups (for assignment purposes)
     * @return List of all groups
     */
    public List<Group> getAllGroups() {
        return categoryDAO.getAllGroups();
    }

    /**
     * Get unassigned groups
     * @return List of groups not assigned to any category
     */
    public List<Group> getUnassignedGroups() {
        return categoryDAO.getUnassignedGroups();
    }

    /**
     * Assign multiple groups to a category
     * @param categoryId target category ID
     * @param groupIds list of group IDs to assign
     * @return true if all assignments were successful
     */
    public boolean assignGroups(Long categoryId, List<Long> groupIds) {
        if (categoryId == null || groupIds == null || groupIds.isEmpty()) {
            return false;
        }

        Category category = categoryDAO.getCategoryById(categoryId);
        if (category == null) {
            return false;
        }

        int successCount = 0;
        List<String> assignedGroupNames = new ArrayList<>();

        for (Long groupId : groupIds) {
            if (categoryDAO.assignGroupToCategory(groupId, categoryId)) {
                successCount++;
                // Could get group name here for notification
            }
        }

        // Notify category administrator
        if (category.getCategoryAdministratorId() != null && successCount > 0) {
            NotificationService.getInstance().sendNotification(
                category.getCategoryAdministratorId(),
                successCount + " new groups have been assigned to " + category.getName() + ".");
        }

        System.out.println("LOG: " + successCount + " groups assigned to category '" + category.getName() + "'.");

        return successCount == groupIds.size();
    }

    /**
     * Remove a group from its category
     * @param groupId group ID
     * @return true if removal was successful
     */
    public boolean removeGroupFromCategory(Long groupId) {
        return categoryDAO.removeGroupFromCategory(groupId);
    }

    /**
     * Search categories by name or description
     * @param searchTerm search term
     * @return List of matching categories
     */
    public List<Category> searchCategories(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllCategories();
        }
        return categoryDAO.searchCategories(searchTerm.trim());
    }

    // Private helper methods

    /**
     * Validate category name
     * @param name the name to validate
     * @param excludeCategoryId category ID to exclude for uniqueness check (for updates)
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCategoryName(String name, Long excludeCategoryId) throws IllegalArgumentException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name is required.");
        }

        String trimmedName = name.trim();

        if (!VALID_NAME_PATTERN.matcher(trimmedName).matches()) {
            throw new IllegalArgumentException("Category name contains invalid characters. Only letters, numbers, spaces, and basic punctuation are allowed.");
        }

        if (!categoryDAO.isCategoryNameUnique(trimmedName, excludeCategoryId)) {
            throw new IllegalArgumentException("A category with this name already exists. Please choose a different name.");
        }
    }

    /**
     * Handle notifications when admin changes
     */
    private void handleAdminChangeNotifications(String categoryName, Long previousAdminId, Long newAdminId) {
        // If admin changed
        if (previousAdminId != null && !previousAdminId.equals(newAdminId)) {
            // Notify previous admin
            NotificationService.getInstance().sendNotification(previousAdminId,
                "You have been removed as Category Administrator for " + categoryName + ".");
        }

        if (newAdminId != null && !newAdminId.equals(previousAdminId)) {
            // Notify new admin
            NotificationService.getInstance().sendNotification(newAdminId,
                "You have been assigned as Category Administrator for " + categoryName +
                ". You are now responsible for managing groups in this category.");
        }
    }
}

