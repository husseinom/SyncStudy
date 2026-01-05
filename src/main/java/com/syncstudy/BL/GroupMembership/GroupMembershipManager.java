package com.syncstudy.BL.GroupMembership;

import com.syncstudy.BL.AbstractFactory;
import com.syncstudy.PL.PostgresFactory;
import com.syncstudy.BL.GroupManager.GroupDAO;
import java.util.List;

/**
 * GroupMembershipManager - Singleton manager for group membership operations
 * UC03 - Manage Group Membership
 */
public class GroupMembershipManager {
    private static GroupMembershipManager instance;
    private GroupMembershipDAO membershipDAO;
    private GroupDAO groupDAO;
    
    // Private constructor for singleton
    private GroupMembershipManager() {
        // Initialize DAOs using factory
        AbstractFactory factory = new PostgresFactory();
        this.membershipDAO = factory.createGroupMembershipDAO();
        this.groupDAO = factory.createGroupDAO();
    }
    
    // Singleton getInstance method
    public static GroupMembershipManager getInstance() {
        if (instance == null) {
            instance = new GroupMembershipManager();
        }
        return instance;
    }
    
    // ====================================================
    // UC03.1 - REQUEST TO JOIN GROUP
    // ====================================================
    
    /**
     * User requests to join a group
     * @param userId User ID requesting to join
     * @param groupId Group ID to join
     * @param message Optional message from user
     * @return Created join request
     * @throws IllegalStateException if user cannot join
     */
    public JoinRequest requestToJoinGroup(Long userId, Long groupId, String message) 
            throws IllegalStateException {
        
        // Check if user is already member
        if (membershipDAO.isUserMember(userId, groupId)) {
            throw new IllegalStateException("You are already a member of this group.");
        }
        
        // Check if user is banned
        if (membershipDAO.isUserBanned(userId, groupId)) {
            throw new IllegalStateException("You cannot join this group. You have been banned by an administrator.");
        }
        
        // Check if user already has pending request
        if (membershipDAO.hasPendingRequest(userId, groupId)) {
            throw new IllegalStateException("You already have a pending request for this group. Please wait for administrator review.");
        }
        
        // Create and save the request
        JoinRequest request = new JoinRequest(userId, groupId, message);
        return membershipDAO.createJoinRequest(request);
    }
    
    // ====================================================
    // UC03.2 - APPROVE JOIN REQUEST
    // ====================================================
    
    /**
     * Group admin approves a join request
     * @param requestId Request ID to approve
     * @param approverId Admin user ID
     * @param groupId Group ID
     * @throws SecurityException if user doesn't have permission
     */
    public void approveJoinRequest(Long requestId, Long approverId, Long groupId) 
            throws SecurityException {
        
        // Check if approver is group admin
        if (!membershipDAO.canManageGroup(approverId, groupId)) {
            throw new SecurityException("You do not have permission to perform this action. Group admin access required.");
        }
        
        // Get the request to find user
        JoinRequest request = membershipDAO.getJoinRequestByUserAndGroup(null, groupId);
        if (request != null && request.getRequestId().equals(requestId)) {
            // Add user to group as member
            GroupMember member = new GroupMember(request.getUserId(), groupId);
            membershipDAO.addGroupMember(member);
            
            // Update request status
            membershipDAO.updateJoinRequestStatus(requestId, "Approved", null);
            
            // Note: Group member count will be updated automatically by database triggers or in PL layer
        }
    }
    
    // ====================================================
    // UC03.3 - REJECT JOIN REQUEST
    // ====================================================
    
    /**
     * Group admin rejects a join request
     * @param requestId Request ID to reject
     * @param reason Reason for rejection
     * @param rejecterId Admin user ID
     * @param groupId Group ID
     * @throws SecurityException if user doesn't have permission
     */
    public void rejectJoinRequest(Long requestId, String reason, Long rejecterId, Long groupId) 
            throws SecurityException {
        
        if (!membershipDAO.canManageGroup(rejecterId, groupId)) {
            throw new SecurityException("You do not have permission to perform this action. Group admin access required.");
        }
        
        membershipDAO.updateJoinRequestStatus(requestId, "Rejected", reason);
    }
    
    // ====================================================
    // UC03.4 - LEAVE GROUP
    // ====================================================
    
    /**
     * User leaves a group
     * @param userId User ID leaving the group
     * @param groupId Group ID to leave
     * @throws IllegalStateException if user is not member
     */
    public void leaveGroup(Long userId, Long groupId) throws IllegalStateException {
        
        if (!membershipDAO.isUserMember(userId, groupId)) {
            throw new IllegalStateException("You are not a member of this group.");
        }
        
        membershipDAO.removeGroupMember(userId, groupId);
        // Note: Group member count will be updated automatically
    }
    
    // ====================================================
    // UC03.5 - BAN OR UNBAN MEMBER (System Admin only)
    // ====================================================
    
    /**
     * System admin bans a member from group
     * @param targetUserId User ID to ban
     * @param groupId Group ID
     * @param reason Ban reason
     * @param adminId System admin ID
     * @throws SecurityException if user doesn't have permission
     */
    public void banMember(Long targetUserId, Long groupId, String reason, Long adminId) 
            throws SecurityException {
        
        // TODO: Check if adminId is system administrator (need SessionFacade integration)
        // For now, check if user can manage group
        if (!membershipDAO.canManageGroup(adminId, groupId)) {
            throw new SecurityException("You do not have permission to perform this action. System admin access required.");
        }
        
        // Cannot ban administrator
        GroupMember targetMember = membershipDAO.getGroupMember(targetUserId, groupId);
        if (targetMember != null && targetMember.isAdministrator()) {
            throw new SecurityException("You cannot ban an administrator. Please remove their admin role first.");
        }
        
        membershipDAO.banMember(targetUserId, groupId, reason);
        membershipDAO.removeGroupMember(targetUserId, groupId);
    }
    
    /**
     * System admin unbans a member
     * @param targetUserId User ID to unban
     * @param groupId Group ID
     * @param adminId System admin ID
     * @throws SecurityException if user doesn't have permission
     */
    public void unbanMember(Long targetUserId, Long groupId, Long adminId) 
            throws SecurityException {
        
        if (!membershipDAO.canManageGroup(adminId, groupId)) {
            throw new SecurityException("You do not have permission to perform this action. System admin access required.");
        }
        
        membershipDAO.unbanMember(targetUserId, groupId);
    }
    
    // ====================================================
    // UC03.6 - ASSIGN ROLE (System Admin only)
    // ====================================================
    
    /**
     * System admin assigns role to member
     * @param targetUserId User ID to assign role
     * @param groupId Group ID
     * @param newRole New role to assign
     * @param adminId System admin ID
     * @throws SecurityException if user doesn't have permission
     */
    public void assignRole(Long targetUserId, Long groupId, String newRole, Long adminId) 
            throws SecurityException {
        
        if (!membershipDAO.canManageGroup(adminId, groupId)) {
            throw new SecurityException("You do not have permission to perform this action. System admin access required.");
        }
        
        // Validate role
        if (!"Member".equals(newRole) && !"Group Admin".equals(newRole) && !"Administrator".equals(newRole)) {
            throw new IllegalArgumentException("Invalid role: " + newRole);
        }
        
        membershipDAO.updateMemberRole(targetUserId, groupId, newRole);
    }
    
    // ====================================================
    // QUERY METHODS
    // ====================================================
    
    /**
     * Get pending requests for group admin
     * @param groupId Group ID
     * @param requesterId Admin user ID
     * @return List of pending join requests
     * @throws SecurityException if user doesn't have permission
     */
    public List<JoinRequest> getPendingRequests(Long groupId, Long requesterId) 
            throws SecurityException {
        
        if (!membershipDAO.canManageGroup(requesterId, groupId)) {
            throw new SecurityException("You do not have permission to view requests. Group admin access required.");
        }
        
        return membershipDAO.getPendingJoinRequests(groupId);
    }
    
    /**
     * Get group members
     * @param groupId Group ID
     * @return List of group members
     */
    public List<GroupMember> getGroupMembers(Long groupId) {
        return membershipDAO.getGroupMembers(groupId);
    }
    
    /**
     * Get banned members
     * @param groupId Group ID
     * @param requesterId Admin user ID
     * @return List of banned members
     * @throws SecurityException if user doesn't have permission
     */
    public List<GroupMember> getBannedMembers(Long groupId, Long requesterId) 
            throws SecurityException {
        
        if (!membershipDAO.canManageGroup(requesterId, groupId)) {
            throw new SecurityException("You do not have permission to view banned members. Group admin access required.");
        }
        
        return membershipDAO.getBannedMembers(groupId);
    }
    
    /**
     * Check if user can join group
     * @param userId User ID
     * @param groupId Group ID
     * @return true if user can join, false otherwise
     */
    public boolean canUserJoinGroup(Long userId, Long groupId) {
        return !membershipDAO.isUserMember(userId, groupId) 
            && !membershipDAO.isUserBanned(userId, groupId)
            && !membershipDAO.hasPendingRequest(userId, groupId);
    }
    
    /**
     * Get user's group membership status
     * @param userId User ID
     * @param groupId Group ID
     * @return Membership status string
     */
    public String getUserMembershipStatus(Long userId, Long groupId) {
        if (membershipDAO.isUserMember(userId, groupId)) {
            return "Member";
        } else if (membershipDAO.isUserBanned(userId, groupId)) {
            return "Banned";
        } else if (membershipDAO.hasPendingRequest(userId, groupId)) {
            return "Pending";
        } else {
            return "Not Member";
        }
    }
}
