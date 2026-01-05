package com.syncstudy.BL.GroupMembership;

import java.util.List;

/**
 * GroupMembershipFacade - Singleton facade for group membership operations
 * UC03 - Manage Group Membership
 */
public class GroupMembershipFacade {
    private static GroupMembershipFacade instance;
    private GroupMembershipManager membershipManager;
    
    // Private constructor for singleton
    private GroupMembershipFacade() {
        this.membershipManager = GroupMembershipManager.getInstance();
    }
    
    // Singleton getInstance method
    public static GroupMembershipFacade getInstance() {
        if (instance == null) {
            instance = new GroupMembershipFacade();
        }
        return instance;
    }
    
    // ====================================================
    // FACADE METHODS - Delegate to Manager
    // ====================================================
    
    /**
     * Request to join a group
     * @param userId User ID requesting to join
     * @param groupId Group ID to join
     * @param message Optional message from user
     * @return Created join request
     */
    public JoinRequest requestToJoinGroup(Long userId, Long groupId, String message) {
        return membershipManager.requestToJoinGroup(userId, groupId, message);
    }
    
    /**
     * Approve a join request
     * @param requestId Request ID to approve
     * @param approverId Admin user ID
     * @param groupId Group ID
     */
    public void approveJoinRequest(Long requestId, Long approverId, Long groupId) {
        membershipManager.approveJoinRequest(requestId, approverId, groupId);
    }
    
    /**
     * Reject a join request
     * @param requestId Request ID to reject
     * @param reason Reason for rejection
     * @param rejecterId Admin user ID
     * @param groupId Group ID
     */
    public void rejectJoinRequest(Long requestId, String reason, Long rejecterId, Long groupId) {
        membershipManager.rejectJoinRequest(requestId, reason, rejecterId, groupId);
    }
    
    /**
     * Leave a group
     * @param userId User ID leaving the group
     * @param groupId Group ID to leave
     */
    public void leaveGroup(Long userId, Long groupId) {
        membershipManager.leaveGroup(userId, groupId);
    }
    
    /**
     * Ban a member from group
     * @param targetUserId User ID to ban
     * @param groupId Group ID
     * @param reason Ban reason
     * @param adminId System admin ID
     */
    public void banMember(Long targetUserId, Long groupId, String reason, Long adminId) {
        membershipManager.banMember(targetUserId, groupId, reason, adminId);
    }
    
    /**
     * Unban a member
     * @param targetUserId User ID to unban
     * @param groupId Group ID
     * @param adminId System admin ID
     */
    public void unbanMember(Long targetUserId, Long groupId, Long adminId) {
        membershipManager.unbanMember(targetUserId, groupId, adminId);
    }
    
    /**
     * Assign role to member
     * @param targetUserId User ID to assign role
     * @param groupId Group ID
     * @param newRole New role to assign
     * @param adminId System admin ID
     */
    public void assignRole(Long targetUserId, Long groupId, String newRole, Long adminId) {
        membershipManager.assignRole(targetUserId, groupId, newRole, adminId);
    }
    
    /**
     * Get pending requests for group
     * @param groupId Group ID
     * @param requesterId Admin user ID
     * @return List of pending join requests
     */
    public List<JoinRequest> getPendingRequests(Long groupId, Long requesterId) {
        return membershipManager.getPendingRequests(groupId, requesterId);
    }
    
    /**
     * Get group members
     * @param groupId Group ID
     * @return List of group members
     */
    public List<GroupMember> getGroupMembers(Long groupId) {
        return membershipManager.getGroupMembers(groupId);
    }
    
    /**
     * Get banned members
     * @param groupId Group ID
     * @param requesterId Admin user ID
     * @return List of banned members
     */
    public List<GroupMember> getBannedMembers(Long groupId, Long requesterId) {
        return membershipManager.getBannedMembers(groupId, requesterId);
    }
    
    /**
     * Check if user can join group
     * @param userId User ID
     * @param groupId Group ID
     * @return true if user can join, false otherwise
     */
    public boolean canUserJoinGroup(Long userId, Long groupId) {
        return membershipManager.canUserJoinGroup(userId, groupId);
    }
    
    /**
     * Get user's membership status for group
     * @param userId User ID
     * @param groupId Group ID
     * @return Membership status string
     */
    public String getUserMembershipStatus(Long userId, Long groupId) {
        return membershipManager.getUserMembershipStatus(userId, groupId);
    }
}
