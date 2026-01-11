package com.syncstudy.BL.ProfileManager;

import com.syncstudy.BL.AbstractFactory;
import com.syncstudy.PL.PostgresFactory;

import java.util.List;

/**
 * Singleton ProfileManager handling profile-related business logic
 */
public class ProfileManager {
    private static ProfileManager instance;
    private ProfileDAO profileDAO;

    private ProfileManager() {
        // Initialize with concrete factory (can be changed for other DB types)
        AbstractFactory factory = new PostgresFactory();
        this.profileDAO = factory.createProfileDAO();
    }

    /**
     * Get the singleton instance of ProfileManager
     * @return ProfileManager instance
     */
    public static ProfileManager getInstance() {
        if (instance == null) {
            synchronized (ProfileManager.class) {
                if (instance == null) {
                    instance = new ProfileManager();
                }
            }
        }
        return instance;
    }

    /**
     * Create a UserProfile with the given credentials
     * @param userId given user id
     * @param firstname given firstname
     * @param lastname given lastname
     * @return the id of the created profile
     */
    public Long createProfile(Long userId, String firstname, String lastname) {
        if (userId == null ||
                firstname == null || firstname.trim().isEmpty() ||
                lastname == null || lastname.trim().isEmpty()) {
            return null;
        }
        return profileDAO.createProfile(userId,firstname,lastname);
    }

    /**
     * Update a profile with the given credentials
     * @param profileId profile id used to find the profile to update
     * @param userId user id also used to find the profile
     * @param firstname given firstname
     * @param lastname given lastname
     * @return true if update completed successfully, false otherwise
     */
    public boolean updateProfile(Long profileId, Long userId, String firstname, String lastname) {
        if (profileId == null ||
                userId == null ||
                firstname == null || firstname.trim().isEmpty() ||
                lastname == null || lastname.trim().isEmpty()) {
            return false;
        }
        return profileDAO.updateProfile(profileId,userId,firstname,lastname);
    }

    /**
     * Find a profile by user id
     * @param userId the user id used to find the profile
     * @return the corresponding UserProfile object
     */
    public UserProfile findProfileByUserId(Long userId) {
        return profileDAO.findProfileByUserId(userId);
    }

    /**
     *
     * @param searchQuery the search bar query
     * @param sortBy the filter selected
     * @param page the number of pages
     * @param pageSize the size of one page of users
     * @return a list of all the profiles available
     */
    public List<UserProfile> findAllProfiles(String searchQuery, String sortBy, int page, int pageSize) {
        return profileDAO.findAllProfiles(searchQuery, sortBy, page, pageSize);
    }

    /**
     * Delete a profile by id
     * @param userId the id to delete the user from
     * @return true if deletion successful, false otherwise
     */
    public boolean deleteProfile(Long userId) {
        if (userId == null) {
            return false;
        }
        return profileDAO.deleteProfile(userId);
    }

    /**
     * Get total number of profiles in database that match the query
     * @param searchQuery the search query
     * @return the number of matching profiles
     */
    public int getTotalProfilesCount(String searchQuery) {
        return profileDAO.getTotalProfilesCount(searchQuery != null ? searchQuery.trim() : "");
    }
}
