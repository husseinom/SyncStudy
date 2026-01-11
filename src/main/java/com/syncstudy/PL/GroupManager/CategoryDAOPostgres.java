package com.syncstudy.PL.GroupManager;

import com.syncstudy.BL.GroupManager.Category;
import com.syncstudy.BL.GroupManager.CategoryAssignment;
import com.syncstudy.BL.GroupManager.CategoryDAO;
import com.syncstudy.BL.GroupManager.Group;
import com.syncstudy.PL.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CategoryDAOPostgres - PostgreSQL implementation of CategoryDAO
 * Handles all database operations for categories and category assignments
 */
public class CategoryDAOPostgres extends CategoryDAO {

    private DatabaseConnection dbConnection;

    public CategoryDAOPostgres() {
        this.dbConnection = DatabaseConnection.getInstance();
        initializeDatabase();
    }

    /**
     * Initialize database tables for categories
     */
    private void initializeDatabase() {
        try (Connection conn = dbConnection.getConnection()) {
            extendCategoriesTable(conn);
            createCategoryAssignmentsTable(conn);
        } catch (SQLException e) {
            System.err.println("Error initializing Category database: " + e.getMessage());
        }
    }

    /**
     * Extend the categories table with new columns
     */
    private void extendCategoriesTable(Connection conn) {
        String[] alterStatements = {
            "ALTER TABLE categories ADD COLUMN IF NOT EXISTS category_administrator_id BIGINT REFERENCES users(id) ON DELETE SET NULL",
            "ALTER TABLE categories ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW()",
            "ALTER TABLE categories ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT NOW()"
        };

        for (String sql : alterStatements) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            } catch (SQLException e) {
                // Column might already exist
                System.err.println("Note: " + e.getMessage());
            }
        }
        System.out.println("Categories table extended with admin columns.");
    }

    /**
     * Create the category_assignments table
     */
    private void createCategoryAssignmentsTable(Connection conn) {
        String sql = """
            CREATE TABLE IF NOT EXISTS category_assignments (
                id SERIAL PRIMARY KEY,
                category_id BIGINT NOT NULL REFERENCES categories(category_id) ON DELETE CASCADE,
                user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                assigned_at TIMESTAMP DEFAULT NOW(),
                removed_at TIMESTAMP,
                is_active BOOLEAN DEFAULT TRUE
            )
            """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Category assignments table created successfully.");
        } catch (SQLException e) {
            System.err.println("Error creating category_assignments table: " + e.getMessage());
        }
    }

    /**
     * Synchronize the category_id sequence with the maximum existing ID
     * This prevents duplicate key errors when inserting new categories
     */
    private void synchronizeCategorySequence() {
        String sql = """
            SELECT setval('categories_category_id_seq', 
                          COALESCE((SELECT MAX(category_id) FROM categories), 0) + 1, 
                          false)
            """;

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Category sequence synchronized.");
        } catch (SQLException e) {
            System.err.println("Error synchronizing category sequence: " + e.getMessage());
        }
    }

    @Override
    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        String sql = """
            SELECT c.*, 
                   u.username as admin_username,
                   u.full_name as admin_full_name,
                   (SELECT COUNT(*) FROM groups g WHERE g.category_id = c.category_id) as groups_count
            FROM categories c
            LEFT JOIN users u ON c.category_administrator_id = u.id
            ORDER BY c.name
            """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                categories.add(mapResultSetToCategory(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error getting all categories: " + e.getMessage());
        }

        return categories;
    }

    @Override
    public Category getCategoryById(Long categoryId) {
        String sql = """
            SELECT c.*, 
                   u.username as admin_username,
                   u.full_name as admin_full_name,
                   (SELECT COUNT(*) FROM groups g WHERE g.category_id = c.category_id) as groups_count
            FROM categories c
            LEFT JOIN users u ON c.category_administrator_id = u.id
            WHERE c.category_id = ?
            """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, categoryId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCategory(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error getting category by ID: " + e.getMessage());
        }

        return null;
    }

    @Override
    public Category createCategory(Category category) {
        // First, ensure the sequence is synchronized with existing data
        synchronizeCategorySequence();

        String sql = """
            INSERT INTO categories (category_id, name, description, icon, color, category_administrator_id, created_at, updated_at)
            VALUES (nextval('categories_category_id_seq'), ?, ?, ?, ?, ?, NOW(), NOW())
            RETURNING category_id
            """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, category.getName());
            pstmt.setString(2, category.getDescription());
            pstmt.setString(3, category.getIcon());
            pstmt.setString(4, category.getColor());

            if (category.getCategoryAdministratorId() != null) {
                pstmt.setLong(5, category.getCategoryAdministratorId());
            } else {
                pstmt.setNull(5, Types.BIGINT);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    category.setCategoryId(rs.getLong("category_id"));
                    category.setCreatedAt(LocalDateTime.now());
                    category.setUpdatedAt(LocalDateTime.now());

                    // Create assignment record if admin was set
                    if (category.getCategoryAdministratorId() != null) {
                        createAssignmentRecord(conn, category.getCategoryId(), category.getCategoryAdministratorId());
                    }

                    System.out.println("Category '" + category.getName() + "' created successfully with ID: " + category.getCategoryId());
                    return category;
                }
            }

        } catch (SQLException e) {
            System.err.println("Error creating category: " + e.getMessage());
        }

        return null;
    }

    @Override
    public boolean updateCategory(Category category) {
        String sql = """
            UPDATE categories 
            SET name = ?, description = ?, icon = ?, color = ?, category_administrator_id = ?, updated_at = NOW()
            WHERE category_id = ?
            """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, category.getName());
            pstmt.setString(2, category.getDescription());
            pstmt.setString(3, category.getIcon());
            pstmt.setString(4, category.getColor());

            if (category.getCategoryAdministratorId() != null) {
                pstmt.setLong(5, category.getCategoryAdministratorId());
            } else {
                pstmt.setNull(5, Types.BIGINT);
            }

            pstmt.setLong(6, category.getCategoryId());

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Category '" + category.getName() + "' updated successfully.");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error updating category: " + e.getMessage());
        }

        return false;
    }

    @Override
    public boolean deleteCategory(Long categoryId) {
        String sql = "DELETE FROM categories WHERE category_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, categoryId);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Category with ID " + categoryId + " deleted successfully.");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error deleting category: " + e.getMessage());
        }

        return false;
    }

    @Override
    public boolean isCategoryNameUnique(String name, Long excludeCategoryId) {
        String sql = "SELECT COUNT(*) FROM categories WHERE LOWER(name) = LOWER(?)";

        if (excludeCategoryId != null) {
            sql += " AND category_id != ?";
        }

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);

            if (excludeCategoryId != null) {
                pstmt.setLong(2, excludeCategoryId);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) == 0;
                }
            }

        } catch (SQLException e) {
            System.err.println("Error checking category name uniqueness: " + e.getMessage());
        }

        return false;
    }

    @Override
    public boolean hasCategoryGroups(Long categoryId) {
        return getCategoryGroupCount(categoryId) > 0;
    }

    @Override
    public int getCategoryGroupCount(Long categoryId) {
        String sql = "SELECT COUNT(*) FROM groups WHERE category_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, categoryId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error getting category group count: " + e.getMessage());
        }

        return 0;
    }

    @Override
    public List<Group> getGroupsInCategory(Long categoryId) {
        List<Group> groups = new ArrayList<>();
        String sql = """
            SELECT g.*, c.category_id as cat_id, c.name as cat_name, 
                   c.description as cat_desc, c.icon as cat_icon, c.color as cat_color
            FROM groups g
            LEFT JOIN categories c ON g.category_id = c.category_id
            WHERE g.category_id = ?
            ORDER BY g.name
            """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, categoryId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    groups.add(mapResultSetToGroup(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error getting groups in category: " + e.getMessage());
        }

        return groups;
    }

    @Override
    public boolean assignCategoryAdministrator(Long categoryId, Long userId) {
        // First, deactivate any existing assignment
        deactivateCurrentAssignment(categoryId);

        // Update the category
        String updateSql = "UPDATE categories SET category_administrator_id = ?, updated_at = NOW() WHERE category_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {

            pstmt.setLong(1, userId);
            pstmt.setLong(2, categoryId);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                // Create new assignment record
                createAssignmentRecord(conn, categoryId, userId);
                System.out.println("Category administrator assigned successfully.");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error assigning category administrator: " + e.getMessage());
        }

        return false;
    }

    @Override
    public boolean removeCategoryAdministrator(Long categoryId, Long previousAdminId) {
        // Deactivate the current assignment
        deactivateCurrentAssignment(categoryId);

        // Update the category
        String updateSql = "UPDATE categories SET category_administrator_id = NULL, updated_at = NOW() WHERE category_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {

            pstmt.setLong(1, categoryId);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Category administrator removed successfully.");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error removing category administrator: " + e.getMessage());
        }

        return false;
    }

    @Override
    public CategoryAssignment getCategoryAssignment(Long categoryId) {
        String sql = """
            SELECT * FROM category_assignments 
            WHERE category_id = ? AND is_active = TRUE
            ORDER BY assigned_at DESC
            LIMIT 1
            """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, categoryId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    CategoryAssignment assignment = new CategoryAssignment();
                    assignment.setId(rs.getLong("id"));
                    assignment.setCategoryId(rs.getLong("category_id"));
                    assignment.setUserId(rs.getLong("user_id"));
                    assignment.setAssignedAt(rs.getTimestamp("assigned_at").toLocalDateTime());
                    assignment.setActive(rs.getBoolean("is_active"));

                    Timestamp removedAt = rs.getTimestamp("removed_at");
                    if (removedAt != null) {
                        assignment.setRemovedAt(removedAt.toLocalDateTime());
                    }

                    return assignment;
                }
            }

        } catch (SQLException e) {
            System.err.println("Error getting category assignment: " + e.getMessage());
        }

        return null;
    }

    @Override
    public List<Group> getAllGroups() {
        List<Group> groups = new ArrayList<>();
        String sql = """
            SELECT g.*, c.category_id as cat_id, c.name as cat_name, 
                   c.description as cat_desc, c.icon as cat_icon, c.color as cat_color
            FROM groups g
            LEFT JOIN categories c ON g.category_id = c.category_id
            ORDER BY g.name
            """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                groups.add(mapResultSetToGroup(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error getting all groups: " + e.getMessage());
        }

        return groups;
    }

    @Override
    public List<Group> getUnassignedGroups() {
        List<Group> groups = new ArrayList<>();
        String sql = """
            SELECT g.*, NULL as cat_id, NULL as cat_name, 
                   NULL as cat_desc, NULL as cat_icon, NULL as cat_color
            FROM groups g
            WHERE g.category_id IS NULL
            ORDER BY g.name
            """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                groups.add(mapResultSetToGroup(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error getting unassigned groups: " + e.getMessage());
        }

        return groups;
    }

    @Override
    public boolean assignGroupToCategory(Long groupId, Long categoryId) {
        String sql = "UPDATE groups SET category_id = ? WHERE group_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (categoryId != null) {
                pstmt.setLong(1, categoryId);
            } else {
                pstmt.setNull(1, Types.BIGINT);
            }
            pstmt.setLong(2, groupId);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Group " + groupId + " assigned to category " + categoryId);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error assigning group to category: " + e.getMessage());
        }

        return false;
    }

    @Override
    public boolean removeGroupFromCategory(Long groupId) {
        return assignGroupToCategory(groupId, null);
    }

    @Override
    public List<Category> searchCategories(String searchTerm) {
        List<Category> categories = new ArrayList<>();
        String sql = """
            SELECT c.*, 
                   u.username as admin_username,
                   u.full_name as admin_full_name,
                   (SELECT COUNT(*) FROM groups g WHERE g.category_id = c.category_id) as groups_count
            FROM categories c
            LEFT JOIN users u ON c.category_administrator_id = u.id
            WHERE LOWER(c.name) LIKE LOWER(?) OR LOWER(c.description) LIKE LOWER(?)
            ORDER BY c.name
            """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String pattern = "%" + searchTerm + "%";
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    categories.add(mapResultSetToCategory(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error searching categories: " + e.getMessage());
        }

        return categories;
    }

    // Private helper methods

    private void deactivateCurrentAssignment(Long categoryId) {
        String sql = """
            UPDATE category_assignments 
            SET is_active = FALSE, removed_at = NOW() 
            WHERE category_id = ? AND is_active = TRUE
            """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, categoryId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error deactivating current assignment: " + e.getMessage());
        }
    }

    private void createAssignmentRecord(Connection conn, Long categoryId, Long userId) {
        String sql = """
            INSERT INTO category_assignments (category_id, user_id, assigned_at, is_active)
            VALUES (?, ?, NOW(), TRUE)
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, categoryId);
            pstmt.setLong(2, userId);
            pstmt.executeUpdate();
            System.out.println("Category assignment record created.");
        } catch (SQLException e) {
            System.err.println("Error creating assignment record: " + e.getMessage());
        }
    }

    private Category mapResultSetToCategory(ResultSet rs) throws SQLException {
        Category category = new Category();
        category.setCategoryId(rs.getLong("category_id"));
        category.setName(rs.getString("name"));
        category.setDescription(rs.getString("description"));
        category.setIcon(rs.getString("icon"));
        category.setColor(rs.getString("color"));

        Long adminId = rs.getLong("category_administrator_id");
        if (!rs.wasNull()) {
            category.setCategoryAdministratorId(adminId);
        }

        // Try to get admin name
        try {
            String adminName = rs.getString("admin_full_name");
            if (adminName == null || adminName.isEmpty()) {
                adminName = rs.getString("admin_username");
            }
            category.setCategoryAdministratorName(adminName);
        } catch (SQLException e) {
            // Column might not exist in this query
        }

        // Try to get timestamps
        try {
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                category.setCreatedAt(createdAt.toLocalDateTime());
            }

            Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                category.setUpdatedAt(updatedAt.toLocalDateTime());
            }
        } catch (SQLException e) {
            // Timestamps might not exist
        }

        // Try to get groups count
        try {
            category.setGroupsCount(rs.getInt("groups_count"));
        } catch (SQLException e) {
            // Groups count might not be in this query
        }

        return category;
    }

    private Group mapResultSetToGroup(ResultSet rs) throws SQLException {
        Group group = new Group();
        group.setGroupId(rs.getLong("group_id"));
        group.setName(rs.getString("name"));
        group.setDescription(rs.getString("description"));
        group.setCreatorId(rs.getLong("creator_id"));
        group.setMemberCount(rs.getInt("member_count"));
        group.setIcon(rs.getString("icon"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            group.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp lastActivity = rs.getTimestamp("last_activity");
        if (lastActivity != null) {
            group.setLastActivity(lastActivity.toLocalDateTime());
        }

        // Map category if exists
        try {
            Long catId = rs.getLong("cat_id");
            if (!rs.wasNull()) {
                Category category = new Category();
                category.setCategoryId(catId);
                category.setName(rs.getString("cat_name"));
                category.setDescription(rs.getString("cat_desc"));
                category.setIcon(rs.getString("cat_icon"));
                category.setColor(rs.getString("cat_color"));
                group.setCategory(category);
            }
        } catch (SQLException e) {
            // Category columns might not exist
        }

        return group;
    }
}

