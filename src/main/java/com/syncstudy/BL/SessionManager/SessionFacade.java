package com.syncstudy.BL.SessionManager;


import com.syncstudy.BL.ProfileManager.ProfileManager;
import com.syncstudy.BL.ProfileManager.UserProfile;

import java.sql.SQLException;
import java.util.List;

/**
 * Singleton Facade providing simplified interface for session management
 */
public class SessionFacade {
    private static SessionFacade instance;
    private UserManager userManager;
    private ProfileManager profileManager;
    private Long loggedUserId;

    private SessionFacade() {
        this.userManager = UserManager.getInstance();
        this.profileManager = ProfileManager.getInstance();
    }

    /**
     * Get the singleton instance of SessionFacade
     * @return SessionFacade instance
     */
    public static SessionFacade getInstance() {
        if (instance == null) {
            synchronized (SessionFacade.class) {
                if (instance == null) {
                    instance = new SessionFacade();
                }
            }
        }
        return instance;
    }

    /**
     * Attempt to login with provided credentials
     * @param username the username
     * @param password the password
     * @return true if login successful, false otherwise
     */
    public boolean login(String username, String password) {
        try {
            return userManager.checkCredentials(username, password);
        } catch (Exception e) {
            System.err.println("Login error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the current logged-in user
     * @return the current User object
     * @throws IllegalStateException if no user is logged in
     */
    public User getCurrentUser() {
        return userManager.getCurrentUser();
    }

    public void setCurrentUser(User user) {
        userManager.setCurrentUser(user);
    }

    public boolean logout() {
        userManager.setCurrentUser(null);
        return true;
    }

    public boolean isLoggedIn() {
        return userManager.getCurrentUser() != null;
    }


    public void setLoggedUserId(Long loggedUserId) {
        this.loggedUserId = loggedUserId;
    }

    /**
     * Creates an account with given credentials (aka registers a user and a userprofile)
     * @param username the provided username
     * @param passwordhash the provided password hash
     * @param email the provided email
     * @param firstname the provided firstname
     * @param lastname the provided lastname
     * @param university the provided university
     * @param department the provided department
     * @return true if both operations issued correctly, false otherwise
     */
    public boolean createAccount(String username, String passwordhash, String email, String firstname, String lastname, String university, String department) {
        String fullname = (firstname + " " + lastname.toUpperCase());
        Long userId = userManager.createUser(username,passwordhash,email,fullname,university,department);
        Long profileId = profileManager.createProfile(userId,firstname,lastname);
        return !(userId == null || profileId == null);
    }

    /**
     * Updates a profile with given credentials
     * @param firstname provided firstname
     * @param lastname provided lastname
     * @return true if operation issued correctly, false otherwise
     */
    public boolean updateAccount(String username, String passwordHash, String email, String firstname, String lastname, String university, String department) {
        Long userId = this.loggedUserId;

        UserProfile profile = this.profileManager.findProfileByUserId(userId);

        if (profile == null) {
            System.err.println("No profile found for user ID: " + userId);
            return false;
        }

        Long profileId = this.profileManager.findProfileByUserId(userId).getId();
        String fullname = firstname + " " + lastname;
        boolean profileOk = profileManager.updateProfile(profileId,userId,firstname,lastname);
        boolean userOk = userManager.updateUser(userId, username, passwordHash, email, fullname, university, department);
        return (profileOk && userOk);
    }

    /**
     * Finds the currently logged user's profile
     * @return UserProfile object if found, null otherwise
     */
    public UserProfile findProfile() {
        return this.profileManager.findProfileByUserId(this.loggedUserId);
    }

    /**
     * Finds all profiles corresponding to the parameters
     * @param searchQuery a research term
     * @param sortBy filter
     * @param page page number
     * @param pageSize size of pagination
     * @return a list of UserProfile objects
     */
    public List<UserProfile> findAllProfiles(String searchQuery, String sortBy, int page, int pageSize) {
        return this.profileManager.findAllProfiles(searchQuery,sortBy,page,pageSize);
    }

    /**
     * Deletes the currently logged user's account
     * @return true if deletion issued correctly, false otherwise
     */
    public boolean deleteAccount() {
        boolean profileOk = this.profileManager.deleteProfile(this.loggedUserId);
        boolean userOk = this.userManager.deleteUser(this.loggedUserId);
        return (profileOk && userOk);
    }

    /**
     * Get total profiles count for pagination
     * @param searchQuery search term
     * @return total count
     */
    public int getTotalProfilesCount(String searchQuery) {
        return profileManager.getTotalProfilesCount(searchQuery);
    }

    /**
     * Find a user by id
     * @param userId the id used to find the user
     * @return a corresponding User object
     */
    public User findUserById(Long userId) {
        return userManager.findUserById(userId);
    }
}