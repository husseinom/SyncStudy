package com.syncstudy.BL.ProfileManager;

/**
 * UserProfile entity representing a profile in the system
 */
public class UserProfile {
    private Long id;
    private Long userId;
    private String firstname;
    private String lastname;

    public UserProfile() {}

    public UserProfile(Long id, Long userId, String firstname, String lastname) {
        this.id = id;
        this.userId = userId;
        this.firstname = firstname;
        this.lastname = lastname;
    }

    //getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    /**
     * Get the full name from the profile
     */
    public String getFullName() {
        return this.firstname + " " + this.lastname;
    }

    @Override
    public String toString() {
        return "UserProfile{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", firstname='" + firstname + '\'' +
                ", lastname=" + lastname +
                '}';
    }
}
