package com.syncstudy.BL.GroupMembership;

import java.time.LocalDateTime;

/**
 * GroupMember - Entity for managing group membership
 * UC03 - Manage Group Membership
 */
public class GroupMember {
    private Long memberId;
    private Long userId;
    private Long groupId;
    private String role; // "Member", "Group Admin", "Administrator"
    private LocalDateTime joinedDate;
    private boolean isBanned;
    private String banReason;
    private LocalDateTime banDate;
    
    // Default constructor
    public GroupMember() {
        this.role = "Member";
        this.joinedDate = LocalDateTime.now();
        this.isBanned = false;
    }
    
    // Constructor for new member
    public GroupMember(Long userId, Long groupId) {
        this.userId = userId;
        this.groupId = groupId;
        this.role = "Member";
        this.joinedDate = LocalDateTime.now();
        this.isBanned = false;
    }
    
    // Constructor with role
    public GroupMember(Long userId, Long groupId, String role) {
        this.userId = userId;
        this.groupId = groupId;
        this.role = role;
        this.joinedDate = LocalDateTime.now();
        this.isBanned = false;
    }
    
    // Full constructor
    public GroupMember(Long memberId, Long userId, Long groupId, String role, 
                      LocalDateTime joinedDate, boolean isBanned, String banReason, LocalDateTime banDate) {
        this.memberId = memberId;
        this.userId = userId;
        this.groupId = groupId;
        this.role = role;
        this.joinedDate = joinedDate;
        this.isBanned = isBanned;
        this.banReason = banReason;
        this.banDate = banDate;
    }
    
    // Getters and Setters
    public Long getMemberId() {
        return memberId;
    }
    
    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getGroupId() {
        return groupId;
    }
    
    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public LocalDateTime getJoinedDate() {
        return joinedDate;
    }
    
    public void setJoinedDate(LocalDateTime joinedDate) {
        this.joinedDate = joinedDate;
    }
    
    public boolean isBanned() {
        return isBanned;
    }
    
    public void setBanned(boolean banned) {
        isBanned = banned;
        if (banned && banDate == null) {
            banDate = LocalDateTime.now();
        }
    }
    
    public String getBanReason() {
        return banReason;
    }
    
    public void setBanReason(String banReason) {
        this.banReason = banReason;
    }
    
    public LocalDateTime getBanDate() {
        return banDate;
    }
    
    public void setBanDate(LocalDateTime banDate) {
        this.banDate = banDate;
    }
    
    // Role checking methods
    public boolean isMember() {
        return "Member".equals(role);
    }
    
    public boolean isGroupAdmin() {
        return "Group Admin".equals(role);
    }
    
    public boolean isAdministrator() {
        return "Administrator".equals(role);
    }
    
    public boolean canManageRequests() {
        return isGroupAdmin() || isAdministrator();
    }
    
    public boolean canManageMembers() {
        return isAdministrator();
    }
    
    @Override
    public String toString() {
        return "GroupMember{" +
                "memberId=" + memberId +
                ", userId=" + userId +
                ", groupId=" + groupId +
                ", role='" + role + '\'' +
                ", joinedDate=" + joinedDate +
                ", isBanned=" + isBanned +
                '}';
    }
}
