package com.syncstudy.BL.GroupMembership;

import java.util.List;

/**
 * GroupMembershipDAO - Abstract DAO for managing group membership operations
 * UC03 - Manage Group Membership
 */
public abstract class GroupMembershipDAO {
    
    // ====================================================
    // JOIN REQUESTS MANAGEMENT
    // ====================================================
    
    /**
     * Create a new join request
     * @param request JoinRequest to create
     * @return Created JoinRequest with ID
     */
    public abstract JoinRequest createJoinRequest(JoinRequest request);
    
    /**
     * Get all pending join requests for a group
     * @param groupId Group ID
     * @return List of pending join requests
     */
    public abstract List<JoinRequest> getPendingJoinRequests(Long groupId);
    
    /**
     * Get join request by user and group
     * @param userId User ID
     * @param groupId Group ID
     * @return JoinRequest if exists, null otherwise
     */
    public abstract JoinRequest getJoinRequestByUserAndGroup(Long userId, Long groupId);
    
    /**
     * Update join request status
     * @param requestId Request ID
     * @param status New status ("Approved", "Rejected")
     * @param rejectionReason Reason for rejection (if applicable)
     */
    public abstract void updateJoinRequestStatus(Long requestId, String status, String rejectionReason);
    
    /**
     * Get all join requests for a user
     * @param userId User ID
     * @return List of join requests
     */
    public abstract List<JoinRequest> getUserJoinRequests(Long userId);
    
    // ====================================================
    // GROUP MEMBERS MANAGEMENT
    // ====================================================
    
    /**
     * Add a new group member
     * @param member GroupMember to add
     * @return Created GroupMember with ID
     */
    public abstract GroupMember addGroupMember(GroupMember member);
    
    /**
     * Get all members of a group
     * @param groupId Group ID
     * @return List of group members
     */
    public abstract List<GroupMember> getGroupMembers(Long groupId);
    
    /**
     * Get specific group member
     * @param userId User ID
     * @param groupId Group ID
     * @return GroupMember if exists, null otherwise
     */
    public abstract GroupMember getGroupMember(Long userId, Long groupId);
    
    /**
     * Remove a member from group
     * @param userId User ID
     * @param groupId Group ID
     */
    public abstract void removeGroupMember(Long userId, Long groupId);
    
    /**
     * Update member role
     * @param userId User ID
     * @param groupId Group ID
     * @param newRole New role to assign
     */
    public abstract void updateMemberRole(Long userId, Long groupId, String newRole);
    
    /**
     * Ban a member from group
     * @param userId User ID
     * @param groupId Group ID
     * @param reason Ban reason
     */
    public abstract void banMember(Long userId, Long groupId, String reason);
    
    /**
     * Unban a member from group
     * @param userId User ID
     * @param groupId Group ID
     */
    public abstract void unbanMember(Long userId, Long groupId);
    
    /**
     * Get all banned members of a group
     * @param groupId Group ID
     * @return List of banned members
     */
    public abstract List<GroupMember> getBannedMembers(Long groupId);
    
    // ====================================================
    // VERIFICATION METHODS
    // ====================================================
    
    /**
     * Check if user is member of group
     * @param userId User ID
     * @param groupId Group ID
     * @return true if user is member, false otherwise
     */
    public abstract boolean isUserMember(Long userId, Long groupId);
    
    /**
     * Check if user is banned from group
     * @param userId User ID
     * @param groupId Group ID
     * @return true if user is banned, false otherwise
     */
    public abstract boolean isUserBanned(Long userId, Long groupId);
    
    /**
     * Check if user has pending join request
     * @param userId User ID
     * @param groupId Group ID
     * @return true if user has pending request, false otherwise
     */
    public abstract boolean hasPendingRequest(Long userId, Long groupId);
    
    /**
     * Check if user is group admin
     * @param userId User ID
     * @param groupId Group ID
     * @return true if user is group admin, false otherwise
     */
    public abstract boolean isGroupAdmin(Long userId, Long groupId);
    
    /**
     * Check if user can manage group (is group admin or administrator)
     * @param userId User ID
     * @param groupId Group ID
     * @return true if user can manage group, false otherwise
     */
    public abstract boolean canManageGroup(Long userId, Long groupId);
    
    /**
     * Get member count for a group
     * @param groupId Group ID
     * @return Number of active members
     */
    public abstract int getMemberCount(Long groupId);
    
    /**
     * Get groups where user is member
     * @param userId User ID
     * @return List of group IDs where user is member
     */
    public abstract List<Long> getUserGroups(Long userId);
}
