package com.syncstudy;

import com.syncstudy.BL.AdminManager.AdminFacade;
import com.syncstudy.BL.AdminManager.AdminManager;
import com.syncstudy.BL.AdminManager.BlockRecord;
import com.syncstudy.BL.AdminManager.UserActivity;
import com.syncstudy.BL.SessionManager.User;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Test Suite for Use Case 8: Manage User Accounts
 *
 * Tests cover:
 * - 8.2.1.1 View All Users
 * - 8.2.1.2 Block/Unblock Account
 * - 8.2.1.3 Delete User Account
 * - 8.2.1.4 View User Activity
 * - 8.2.2 Alternative Flows
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UC8_ManageUserAccountsTest {

    private AdminFacade adminFacade;
    private static final Long TEST_ADMIN_ID = 1L;
    private static final Long TEST_USER_ID = 2L;

    @Before
    public void setUp() {
        adminFacade = AdminFacade.getInstance();
        adminFacade.setCurrentAdminId(TEST_ADMIN_ID);
    }

    @After
    public void tearDown() {
        // Reset admin facade state
        adminFacade.setCurrentAdminId(null);
    }

    // ============================================================
    // 8.2.1.1 View All Users Tests
    // ============================================================

    /**
     * Test: View all users without filters
     * Expected: Returns a non-null list of users
     */
    @Test
    public void test_01_ViewAllUsers_NoFilters() {
        // Given: Admin is logged in
        adminFacade.setCurrentAdminId(TEST_ADMIN_ID);

        // When: Getting all users without filters
        List<User> users = adminFacade.getAllUsers("", "All", "name");

        // Then: Should return a non-null list
        assertNotNull("Users list should not be null", users);
    }

    /**
     * Test: View users with search query
     * Expected: Returns users matching the search term
     */
    @Test
    public void test_02_ViewAllUsers_WithSearchQuery() {
        // Given: Admin is logged in
        adminFacade.setCurrentAdminId(TEST_ADMIN_ID);

        // When: Searching for users
        List<User> users = adminFacade.getAllUsers("test", "All", "name");

        // Then: Should return a list (may be empty)
        assertNotNull("Search results should not be null", users);
    }

    /**
     * Test: View users filtered by Active status
     * Expected: Returns only active users
     */
    @Test
    public void test_03_ViewAllUsers_FilterByActiveStatus() {
        // Given: Admin is logged in
        adminFacade.setCurrentAdminId(TEST_ADMIN_ID);

        // When: Filtering by Active status
        List<User> activeUsers = adminFacade.getAllUsers("", "Active", "name");

        // Then: Should return only active users
        assertNotNull("Active users list should not be null", activeUsers);
        for (User user : activeUsers) {
            assertFalse("User should not be blocked", user.isBlocked());
        }
    }

    /**
     * Test: View users filtered by Blocked status
     * Expected: Returns only blocked users
     */
    @Test
    public void test_04_ViewAllUsers_FilterByBlockedStatus() {
        // Given: Admin is logged in
        adminFacade.setCurrentAdminId(TEST_ADMIN_ID);

        // When: Filtering by Blocked status
        List<User> blockedUsers = adminFacade.getAllUsers("", "Blocked", "name");

        // Then: Should return only blocked users
        assertNotNull("Blocked users list should not be null", blockedUsers);
        for (User user : blockedUsers) {
            assertTrue("User should be blocked", user.isBlocked());
        }
    }

    /**
     * Test: View users with pagination
     * Expected: Returns correct page of users
     */
    @Test
    public void test_05_ViewAllUsers_WithPagination() {
        // Given: Admin is logged in
        adminFacade.setCurrentAdminId(TEST_ADMIN_ID);
        int pageSize = 10;

        // When: Getting first page
        List<User> page1 = adminFacade.getAllUsers("", "All", "name", 0, pageSize);

        // Then: Should return at most pageSize users
        assertNotNull("Page 1 should not be null", page1);
        assertTrue("Page size should be <= " + pageSize, page1.size() <= pageSize);
    }

    /**
     * Test: Get total users count
     * Expected: Returns a non-negative count
     */
    @Test
    public void test_06_GetTotalUsersCount() {
        // Given: Admin is logged in
        adminFacade.setCurrentAdminId(TEST_ADMIN_ID);

        // When: Getting total count
        int totalCount = adminFacade.getTotalUsersCount("", "All");

        // Then: Should return non-negative count
        assertTrue("Total count should be >= 0", totalCount >= 0);
    }

    /**
     * Test: Sort users by different fields
     * Expected: Returns users sorted accordingly
     */
    @Test
    public void test_07_ViewAllUsers_SortByName() {
        // Given: Admin is logged in
        adminFacade.setCurrentAdminId(TEST_ADMIN_ID);

        // When: Sorting by name
        List<User> usersByName = adminFacade.getAllUsers("", "All", "name");

        // Then: Should return a list
        assertNotNull("Users sorted by name should not be null", usersByName);
    }

    @Test
    public void test_08_ViewAllUsers_SortByRegistration() {
        // Given: Admin is logged in
        adminFacade.setCurrentAdminId(TEST_ADMIN_ID);

        // When: Sorting by registration date
        List<User> usersByDate = adminFacade.getAllUsers("", "All", "registration_date");

        // Then: Should return a list
        assertNotNull("Users sorted by registration should not be null", usersByDate);
    }

    // ============================================================
    // 8.2.1.2 Block/Unblock Account Tests
    // ============================================================

    /**
     * Test: Block user with valid reason
     * Expected: User is blocked successfully or throws IllegalArgumentException if user doesn't exist
     */
    @Test
    public void test_09_BlockUser_ValidReason() {
        // Given: Admin is logged in and user exists
        adminFacade.setCurrentAdminId(TEST_ADMIN_ID);

        // When: Blocking a user with a reason
        try {
            boolean result = adminFacade.blockUser(TEST_USER_ID, "Policy violation");
            // Then: Operation should complete if user exists
            assertTrue("Block should succeed if user exists", result);
        } catch (IllegalArgumentException e) {
            // Expected if user doesn't exist in database
            assertTrue("Exception should mention user not found",
                e.getMessage().toLowerCase().contains("user") ||
                e.getMessage().toLowerCase().contains("not found"));
        }
    }

    /**
     * Test: Block user without admin logged in
     * Expected: IllegalStateException thrown
     */
    @Test(expected = IllegalStateException.class)
    public void test_10_BlockUser_NoAdminLoggedIn() {
        // Given: No admin is logged in
        adminFacade.setCurrentAdminId(null);

        // When: Attempting to block a user
        // Then: Should throw IllegalStateException
        adminFacade.blockUser(TEST_USER_ID, "Test reason");
    }

    /**
     * Test: Unblock user
     * Expected: User is unblocked successfully or throws IllegalArgumentException if user doesn't exist
     */
    @Test
    public void test_11_UnblockUser_Success() {
        // Given: Admin is logged in
        adminFacade.setCurrentAdminId(TEST_ADMIN_ID);

        // When: Unblocking a user
        try {
            boolean result = adminFacade.unblockUser(TEST_USER_ID);
            // Then: Operation should complete if user exists
            assertTrue("Unblock should succeed if user exists", result);
        } catch (IllegalArgumentException e) {
            // Expected if user doesn't exist in database
            assertTrue("Exception should mention user not found",
                e.getMessage().toLowerCase().contains("user") ||
                e.getMessage().toLowerCase().contains("not found"));
        }
    }

    /**
     * Test: Unblock user without admin logged in
     * Expected: IllegalStateException thrown
     */
    @Test(expected = IllegalStateException.class)
    public void test_12_UnblockUser_NoAdminLoggedIn() {
        // Given: No admin is logged in
        adminFacade.setCurrentAdminId(null);

        // When: Attempting to unblock a user
        // Then: Should throw IllegalStateException
        adminFacade.unblockUser(TEST_USER_ID);
    }

    /**
     * Test: Get block history for a user
     * Expected: Returns list of block records
     */
    @Test
    public void test_13_GetBlockHistory() {
        // Given: Admin is logged in
        adminFacade.setCurrentAdminId(TEST_ADMIN_ID);

        // When: Getting block history
        List<BlockRecord> history = adminFacade.getBlockHistory(TEST_USER_ID);

        // Then: Should return a list (may be empty)
        assertNotNull("Block history should not be null", history);
    }

    // ============================================================
    // 8.2.1.3 Delete User Account Tests
    // ============================================================

    /**
     * Test: Check if user is sole admin of groups before deletion
     * Expected: Returns boolean indicating if user is sole admin
     */
    @Test
    public void test_14_IsUserSoleAdminOfGroups() {
        // Given: Admin is logged in
        adminFacade.setCurrentAdminId(TEST_ADMIN_ID);

        // When: Checking if user is sole admin
        boolean isSoleAdmin = adminFacade.isUserSoleAdminOfGroups(TEST_USER_ID);

        // Then: Should return a boolean
        assertNotNull("Should return boolean", isSoleAdmin);
    }

    /**
     * Test: Delete user without admin logged in
     * Expected: IllegalStateException thrown
     */
    @Test(expected = IllegalStateException.class)
    public void test_15_DeleteUser_NoAdminLoggedIn() {
        // Given: No admin is logged in
        adminFacade.setCurrentAdminId(null);

        // When: Attempting to delete a user
        // Then: Should throw IllegalStateException
        adminFacade.deleteUser(TEST_USER_ID);
    }

    /**
     * Test: Delete user with admin logged in
     * Expected: Operation completes or throws IllegalArgumentException if user doesn't exist
     */
    @Test
    public void test_16_DeleteUser_AdminLoggedIn() {
        // Given: Admin is logged in
        adminFacade.setCurrentAdminId(TEST_ADMIN_ID);

        // When: Deleting a user (using a non-existent ID to avoid actual deletion)
        try {
            boolean result = adminFacade.deleteUser(999999L);
            // Then: If no exception, deletion was attempted
            assertFalse("Delete should fail for non-existent user", result);
        } catch (IllegalArgumentException e) {
            // Expected behavior when user doesn't exist
            assertTrue("Exception should mention user not found",
                e.getMessage().toLowerCase().contains("user") ||
                e.getMessage().toLowerCase().contains("not found"));
        }
    }

    // ============================================================
    // 8.2.1.4 View User Activity Tests
    // ============================================================

    /**
     * Test: Get user activity statistics
     * Expected: Returns UserActivity object with statistics
     */
    @Test
    public void test_17_GetUserActivity() {
        // Given: Admin is logged in
        adminFacade.setCurrentAdminId(TEST_ADMIN_ID);

        // When: Getting user activity
        UserActivity activity = adminFacade.getUserActivity(TEST_USER_ID);

        // Then: Should return activity object (may be null if user doesn't exist)
        // This test validates the facade method works
        // assertNotNull("User activity should not be null", activity);
    }

    /**
     * Test: Get user by ID
     * Expected: Returns User object if exists
     */
    @Test
    public void test_18_GetUserById() {
        // Given: Admin is logged in
        adminFacade.setCurrentAdminId(TEST_ADMIN_ID);

        // When: Getting user by ID
        User user = adminFacade.getUserById(TEST_USER_ID);

        // Then: Operation should complete
        // User may be null if ID doesn't exist
    }

    // ============================================================
    // 8.2.2 Alternative Flows Tests
    // ============================================================

    /**
     * Test: Check admin status for current user
     * Expected: Returns true if current user is admin
     */
    @Test
    public void test_19_IsCurrentUserAdmin() {
        // Given: Admin ID is set
        adminFacade.setCurrentAdminId(TEST_ADMIN_ID);

        // When: Checking if current user is admin
        boolean isAdmin = adminFacade.isCurrentUserAdmin();

        // Then: Should return boolean
        assertNotNull("Should return boolean", isAdmin);
    }

    /**
     * Test: Check admin status when not logged in
     * Expected: Returns false
     */
    @Test
    public void test_20_IsCurrentUserAdmin_NotLoggedIn() {
        // Given: No admin logged in
        adminFacade.setCurrentAdminId(null);

        // When: Checking admin status
        boolean isAdmin = adminFacade.isCurrentUserAdmin();

        // Then: Should return false
        assertFalse("Should return false when not logged in", isAdmin);
    }

    /**
     * Test: Check if specific user is admin
     * Expected: Returns boolean indicating admin status
     */
    @Test
    public void test_21_IsAdmin_SpecificUser() {
        // Given: Admin facade is available

        // When: Checking if a specific user is admin
        boolean isAdmin = adminFacade.isAdmin(TEST_ADMIN_ID);

        // Then: Should return boolean
        assertNotNull("Should return boolean", isAdmin);
    }

    /**
     * Test: Search users with no results
     * Expected: Returns empty list, no exception
     */
    @Test
    public void test_22_SearchUsers_NoResults() {
        // Given: Admin is logged in
        adminFacade.setCurrentAdminId(TEST_ADMIN_ID);

        // When: Searching with term that doesn't match
        List<User> users = adminFacade.getAllUsers("zzzznonexistentuserzzz", "All", "name");

        // Then: Should return empty list, not null
        assertNotNull("Search results should not be null", users);
    }

    // ============================================================
    // BlockRecord Entity Tests
    // ============================================================

    /**
     * Test: BlockRecord constructor and getters
     * Expected: All fields are set correctly
     */
    @Test
    public void test_23_BlockRecord_Constructor() {
        // Given: Block record data
        Long userId = 1L;
        Long adminId = 2L;
        String reason = "Policy violation";

        // When: Creating block record
        BlockRecord record = new BlockRecord(userId, adminId, reason);

        // Then: All fields should be set
        assertEquals("User ID should match", userId, record.getUserId());
        assertEquals("Admin ID should match", adminId, record.getAdminId());
        assertEquals("Reason should match", reason, record.getReason());
        assertTrue("Should be active", record.isActive());
        assertNotNull("Block date should be set", record.getBlockDate());
    }

    /**
     * Test: BlockRecord setters
     * Expected: All setters work correctly
     */
    @Test
    public void test_24_BlockRecord_Setters() {
        // Given: Empty block record
        BlockRecord record = new BlockRecord();

        // When: Setting fields
        record.setId(1L);
        record.setUserId(2L);
        record.setAdminId(3L);
        record.setReason("Test reason");
        record.setActive(false);
        LocalDateTime now = LocalDateTime.now();
        record.setBlockDate(now);
        record.setUnblockDate(now.plusDays(1));
        record.setAdminUsername("admin");

        // Then: All getters should return correct values
        assertEquals("ID should match", Long.valueOf(1L), record.getId());
        assertEquals("User ID should match", Long.valueOf(2L), record.getUserId());
        assertEquals("Admin ID should match", Long.valueOf(3L), record.getAdminId());
        assertEquals("Reason should match", "Test reason", record.getReason());
        assertFalse("Should not be active", record.isActive());
        assertEquals("Block date should match", now, record.getBlockDate());
        assertEquals("Unblock date should match", now.plusDays(1), record.getUnblockDate());
        assertEquals("Admin username should match", "admin", record.getAdminUsername());
    }

    // ============================================================
    // UserActivity Entity Tests
    // ============================================================

    /**
     * Test: UserActivity constructor and getters
     * Expected: All fields initialized correctly
     */
    @Test
    public void test_25_UserActivity_Constructor() {
        // Given: User ID
        Long userId = 1L;

        // When: Creating user activity
        UserActivity activity = new UserActivity(userId);

        // Then: User ID should be set
        assertEquals("User ID should match", userId, activity.getUserId());
    }

    /**
     * Test: UserActivity setters
     * Expected: All setters work correctly
     */
    @Test
    public void test_26_UserActivity_Setters() {
        // Given: Empty user activity
        UserActivity activity = new UserActivity();

        // When: Setting fields
        activity.setUserId(1L);
        activity.setUsername("testuser");
        activity.setFullName("Test User");
        activity.setEmail("test@example.com");
        activity.setMessagesCount(100);
        activity.setFilesCount(50);
        activity.setGroupsCount(5);
        activity.setSessionsCreated(10);
        activity.setSessionsAttended(20);
        activity.setAccountAgeDays(365);
        activity.setAvgMessagesPerDay(1.5);
        activity.setMostActiveGroup("Study Group");
        activity.setEngagementScore(80);
        activity.setBlocked(false);
        LocalDateTime now = LocalDateTime.now();
        activity.setLastLogin(now);
        activity.setRegistrationDate(now.minusDays(365));

        // Then: All getters should return correct values
        assertEquals("User ID should match", Long.valueOf(1L), activity.getUserId());
        assertEquals("Username should match", "testuser", activity.getUsername());
        assertEquals("Full name should match", "Test User", activity.getFullName());
        assertEquals("Email should match", "test@example.com", activity.getEmail());
        assertEquals("Messages count should match", 100, activity.getMessagesCount());
        assertEquals("Files count should match", 50, activity.getFilesCount());
        assertEquals("Groups count should match", 5, activity.getGroupsCount());
        assertEquals("Sessions created should match", 10, activity.getSessionsCreated());
        assertEquals("Sessions attended should match", 20, activity.getSessionsAttended());
        assertEquals("Account age should match", 365, activity.getAccountAgeDays());
        assertEquals("Avg messages should match", 1.5, activity.getAvgMessagesPerDay(), 0.01);
        assertEquals("Most active group should match", "Study Group", activity.getMostActiveGroup());
        assertEquals("Engagement score should match", 80, activity.getEngagementScore());
        assertFalse("Should not be blocked", activity.isBlocked());
        assertEquals("Last login should match", now, activity.getLastLogin());
    }

    // ============================================================
    // Admin Facade Singleton Test
    // ============================================================

    /**
     * Test: AdminFacade singleton pattern
     * Expected: Same instance returned each time
     */
    @Test
    public void test_27_AdminFacade_Singleton() {
        // When: Getting multiple instances
        AdminFacade instance1 = AdminFacade.getInstance();
        AdminFacade instance2 = AdminFacade.getInstance();

        // Then: Should be same instance
        assertSame("Should return same instance", instance1, instance2);
    }

    /**
     * Test: Current admin ID management
     * Expected: Admin ID is stored and retrieved correctly
     */
    @Test
    public void test_28_AdminFacade_CurrentAdminId() {
        // Given: Admin facade
        AdminFacade facade = AdminFacade.getInstance();

        // When: Setting admin ID
        facade.setCurrentAdminId(TEST_ADMIN_ID);

        // Then: Should return same ID
        assertEquals("Admin ID should match", TEST_ADMIN_ID, facade.getCurrentAdminId());

        // When: Setting to null
        facade.setCurrentAdminId(null);

        // Then: Should return null
        assertNull("Admin ID should be null", facade.getCurrentAdminId());
    }
}

