package com.syncstudy.BL.SessionManager;
import com.syncstudy.BL.AbstractFactory;
import com.syncstudy.PL.PostgresFactory;

import java.sql.SQLException;

/**
 * Singleton UserManager handling user-related business logic
 */
public class UserManager {
    private static UserManager instance = new UserManager();
    private UserDAO userDAO;
    private User currentUser;
    private UserManager() {
    }

    /**
     * Ensure the DAO (and any other resources) are initialized.
     * Safe to call multiple times.
     */
    private void ensureInitialized() {
        if (userDAO == null) {
            synchronized (this) {
                if (userDAO == null) {
                    AbstractFactory factory = new PostgresFactory();
                    this.userDAO = factory.createUserDAO();
                }
            }
        }
    }

    /**
     * Force initialization from application or server startup.
     */
    public static void init() {
        instance.ensureInitialized();
    }

    /**
     * Get the singleton instance of UserManager
     * @return UserManager instance
     */
    public static UserManager getInstance() {
        if (instance == null) {
            synchronized (UserManager.class) {
                if (instance == null) {
                    instance = new UserManager();
                }
            }
        }
        return instance;
    }

    /**
     * Check if the provided credentials are valid
     * @param username the username
     * @param password the password
     * @return true if credentials are valid, false otherwise
     */
    public boolean checkCredentials(String username, String password) {
        ensureInitialized();
        if (username == null || username.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            return false;
        }
        // First verify credentials
        boolean valid = userDAO.checkCredentials(username, password);
        if (!valid) {
            return false;
        }
        User user = userDAO.findUserByUsername(username);
        setCurrentUser(user);  // Set the current user after successful authentication
        System.out.println("Current User : "+getCurrentUser());
        return true;
    }

    /**
     * Get the current logged-in user
     * @return the current User object
     * @throws IllegalStateException if no user is logged in
     */
    public User getCurrentUser() {
        if (currentUser == null) {
            throw new IllegalStateException("No user is currently logged in");
        }
        return currentUser;
    }

    public User setCurrentUser(User user) {
        this.currentUser = user; return this.currentUser;
    }

    /**
     * Logout the current user
     */
    public boolean logout() {
        this.currentUser = null;
        System.out.println("User logged out successfully.");
        return true;
    }

    /**
     * Find a user by username
     * @param username the username to search for
     * @return User object if found, null otherwise
     */
    public User findUserByUsername(String username) {
        return userDAO.findUserByUsername(username);
    }

    /**
     * Create a user with given data
     * @param username the username of the user we want to create
     * @param passwordHash a password hash for the new user
     * @param email the new user's email
     * @param fullName the new user's full name
     * @param university the new user's university
     * @param department the new user's department
     * @return true if user creation is successful, false otherwise
     */
    public Long createUser(String username, String passwordHash, String email, String fullName, String university, String department) {
        if (username == null || username.trim().isEmpty() ||
                passwordHash == null || passwordHash.trim().isEmpty() ||
                email == null || email.trim().isEmpty() ||
                fullName == null || fullName.trim().isEmpty() ||
                university == null || university.trim().isEmpty() ||
                department == null || department.trim().isEmpty()) {
            return null;
        }
        return userDAO.createUser(username,passwordHash,email,fullName,university,department);
    }

    /**
     * Delete a user by id
     * @param userId the id to delete the user from
     * @return true if deletion successful, false otherwise
     */
    public boolean deleteUser(Long userId) {
        if (userId == null) {
            return false;
        }
        return userDAO.deleteUser(userId);
    }

    /**
     * Find a user by id
     * @param userId the id to search for
     * @return User object if found, null otherwise
     */
    public User findUserById(Long userId) {
        return userDAO.findUserById(userId);
    }

    /**
     * Update a user with the given credentials
     * @param userId the id of the user to update
     * @param username the new username
     * @param passwordHash the new password hash
     * @param email the new email
     * @param fullname the new name
     * @param university the new university
     * @param department the new department
     * @return true if update successful, false otherwise
     */
    public boolean updateUser(Long userId, String username, String passwordHash, String email, String fullname, String university, String department) {
        return userDAO.updateUser(userId, username,passwordHash,email,fullname,university,department);
    }

}
