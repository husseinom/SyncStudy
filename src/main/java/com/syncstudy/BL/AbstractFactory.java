package com.syncstudy.BL;

import com.syncstudy.BL.FileManager.SharedFileDAO;
import com.syncstudy.BL.ProfileManager.ProfileDAO;
import com.syncstudy.BL.SessionManager.UserDAO;
import com.syncstudy.BL.AdminManager.AdminDAO;
import com.syncstudy.BL.GroupManager.GroupDAO;
import com.syncstudy.BL.GroupManager.CategoryDAO;
import com.syncstudy.BL.GroupMembership.GroupMembershipDAO;
import com.syncstudy.BL.GroupManager.CategoryDAO;
import com.syncstudy.BL.ReportsManager.ReportsDAO;
import com.syncstudy.BL.ProfileManager.ProfileDAO;
import com.syncstudy.BL.FileManager.SharedFileDAO;

/**
 * Abstract Factory for creating DAO instances
 * Allows switching between different persistence implementations
 */
public abstract class AbstractFactory {

    /**
     * Creates a UserDAO instance
     * @return UserDAO implementation
     */
    public abstract UserDAO createUserDAO();

    /**
     * Creates an AdminDAO instance
     * @return AdminDAO implementation
     */
    public abstract AdminDAO createAdminDAO();
    
    /**
     * Creates a GroupDAO instance
     * @return GroupDAO implementation
     */
    public abstract GroupDAO createGroupDAO();

    public abstract ProfileDAO createProfileDAO();
    public abstract CategoryDAO createCategoryDAO();
    public abstract SharedFileDAO createSharedFileDAO();

    /**
     * Creates a ReportsDAO instance
     * @return ReportsDAO implementation
     */
    public abstract ReportsDAO createReportsDAO();

    /**
     * Creates a GroupMembershipDAO instance
     * @return GroupMembershipDAO implementation
     */
    public abstract GroupMembershipDAO createGroupMembershipDAO();
}