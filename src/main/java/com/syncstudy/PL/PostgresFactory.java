package com.syncstudy.PL;

import com.syncstudy.BL.AbstractFactory;
import com.syncstudy.BL.AdminManager.AdminDAO;
import com.syncstudy.BL.FileManager.SharedFileDAO;
import com.syncstudy.BL.ProfileManager.ProfileDAO;
import com.syncstudy.BL.SessionManager.UserDAO;
import com.syncstudy.BL.GroupManager.GroupDAO;
<<<<<<< HEAD
import com.syncstudy.BL.GroupManager.CategoryDAO;
import com.syncstudy.BL.ReportsManager.ReportsDAO;
||||||| parent of 622575d (feat:pl bl)
=======
import com.syncstudy.BL.GroupMembership.GroupMembershipDAO;
>>>>>>> 622575d (feat:pl bl)
import com.syncstudy.PL.AdminManager.AdminDAOPostgres;
import com.syncstudy.PL.FileManager.SharedFileDAOPostgres;
import com.syncstudy.PL.ProfileManager.ProfileDAOPostgres;
import com.syncstudy.PL.SessionManager.UserDAOPostgres;
import com.syncstudy.PL.GroupManager.GroupDAOPostgres;
<<<<<<< HEAD
import com.syncstudy.PL.GroupManager.CategoryDAOPostgres;
import com.syncstudy.PL.ReportsManager.ReportsDAOPostgres;
||||||| parent of 622575d (feat:pl bl)
=======
import com.syncstudy.PL.GroupMembership.GroupMembershipDAOPostgres;
>>>>>>> 622575d (feat:pl bl)

/**
 * Concrete Factory for creating PostgreSQL DAO instances
 */
public class PostgresFactory extends AbstractFactory {

    @Override
    public UserDAO createUserDAO() {
        return new UserDAOPostgres();
    }

    @Override
    public AdminDAO createAdminDAO() {
        return new AdminDAOPostgres();
    }
    
    @Override
    public GroupDAO createGroupDAO() {
        return new GroupDAOPostgres();
    }
<<<<<<< HEAD

    @Override
    public ProfileDAO createProfileDAO() {
        return new ProfileDAOPostgres(); }

    @Override
    public CategoryDAO createCategoryDAO() {
        return new CategoryDAOPostgres();
    }

    public SharedFileDAO createSharedFileDAO() {return new SharedFileDAOPostgres();}
    
    @Override
    public ReportsDAO createReportsDAO() {
        return new ReportsDAOPostgres();
    }
||||||| parent of 622575d (feat:pl bl)
=======
    
    @Override
    public GroupMembershipDAO createGroupMembershipDAO() {
        return new GroupMembershipDAOPostgres();
    }
>>>>>>> 622575d (feat:pl bl)
}