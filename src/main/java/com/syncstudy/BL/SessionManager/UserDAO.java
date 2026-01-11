package com.syncstudy.BL.SessionManager;


import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Abstract Data Access Object for User operations
 * Defines the contract for persistence operations without specifying DB type
 */
public abstract class UserDAO {

    /**
     * Find a user by username
     * @param username the username to search for
     * @return User object if found, null otherwise
     */
    public abstract User findUserByUsername(String username);

    /**
     * Check if the provided credentials are valid
     * @param username the username
     * @param password the plain text password
     * @return true if credentials are valid, false otherwise
     */
    public abstract boolean checkCredentials(String username, String password);

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
    public abstract Long createUser(String username, String passwordHash, String email, String fullName, String university, String department);

    /**
     * Delete a user by id
     * @param id the id to delete the user from
     * @return true if deletion successful, false otherwise
     */
    public abstract boolean deleteUser(Long id);

    /**
     * Find a user by id
     * @param id the id to search for
     * @return User object if found, null otherwise
     */
    public abstract User findUserById(Long id);

    /**
     * Update a user's credentials
     * @param username
     * @param passwordHash
     * @param email
     * @param fullname
     * @param university
     * @param department
     * @return
     */
    public abstract boolean updateUser(Long userId, String username, String passwordHash, String email, String fullname, String university, String department);
}