package com.syncstudy.BL.GroupMembership;

import java.time.LocalDateTime;

/**
 * JoinRequest - Entity for managing group join requests
 * UC03 - Manage Group Membership
 */
public class JoinRequest {
    private Long requestId;
    private Long userId;
    private Long groupId;
    private LocalDateTime requestDate;
    private String status; // "Pending", "Approved", "Rejected"
    private String message; // Message from user requesting to join
    private String rejectionReason;
    
    // Default constructor
    public JoinRequest() {
        this.requestDate = LocalDateTime.now();
        this.status = "Pending";
    }
    
    // Constructor for creating new request
    public JoinRequest(Long userId, Long groupId, String message) {
        this.userId = userId;
        this.groupId = groupId;
        this.message = message;
        this.status = "Pending";
        this.requestDate = LocalDateTime.now();
    }
    
    // Full constructor
    public JoinRequest(Long requestId, Long userId, Long groupId, LocalDateTime requestDate, 
                      String status, String message, String rejectionReason) {
        this.requestId = requestId;
        this.userId = userId;
        this.groupId = groupId;
        this.requestDate = requestDate;
        this.status = status;
        this.message = message;
        this.rejectionReason = rejectionReason;
    }
    
    // Getters and Setters
    public Long getRequestId() {
        return requestId;
    }
    
    public void setRequestId(Long requestId) {
        this.requestId = requestId;
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
    
    public LocalDateTime getRequestDate() {
        return requestDate;
    }
    
    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getRejectionReason() {
        return rejectionReason;
    }
    
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
    
    // Utility methods
    public boolean isPending() {
        return "Pending".equals(status);
    }
    
    public boolean isApproved() {
        return "Approved".equals(status);
    }
    
    public boolean isRejected() {
        return "Rejected".equals(status);
    }
    
    @Override
    public String toString() {
        return "JoinRequest{" +
                "requestId=" + requestId +
                ", userId=" + userId +
                ", groupId=" + groupId +
                ", status='" + status + '\'' +
                ", requestDate=" + requestDate +
                '}';
    }
}
