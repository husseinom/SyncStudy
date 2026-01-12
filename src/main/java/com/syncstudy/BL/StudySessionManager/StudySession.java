package com.syncstudy.BL.StudySessionManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StudySession {
    private Long id;
    private Long groupId;
    private Long creatorId;
    private String creatorUsername;
    private String creatorFullName;
    private String title;
    private String description;
    private LocalDateTime sessionDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private SessionLocation location;
    private Integer maxParticipants; // 0 = unlimited
    private SessionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String cancellationReason;
    private LocalDateTime votingDeadline;
    private int participantCount;
    private List<ProposedDate> proposedDates;

    public enum SessionStatus {
        PROPOSED, CONFIRMED, CANCELLED, COMPLETED
    }

    public enum LocationType {
        PHYSICAL, ONLINE
    }

    public static class SessionLocation {
        private LocationType type;
        private String address; // for physical
        private String meetingLink; // for online

        public SessionLocation(LocationType type, String value) {
            this.type = type;
            if (type == LocationType.PHYSICAL) {
                this.address = value;
            } else {
                this.meetingLink = value;
            }
        }

        // Getters and setters
        public LocationType getType() { return type; }
        public void setType(LocationType type) { this.type = type; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getMeetingLink() { return meetingLink; }
        public void setMeetingLink(String meetingLink) { this.meetingLink = meetingLink; }

        public String getDisplayValue() {
            return type == LocationType.PHYSICAL ? address : meetingLink;
        }
    }

    // Constructor
    public StudySession() {
        this.proposedDates = new ArrayList<>();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }

    public String getCreatorUsername() { return creatorUsername; }
    public void setCreatorUsername(String creatorUsername) { this.creatorUsername = creatorUsername; }

    public String getCreatorFullName() { return creatorFullName; }
    public void setCreatorFullName(String creatorFullName) { this.creatorFullName = creatorFullName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDateTime sessionDate) { this.sessionDate = sessionDate; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public SessionLocation getLocation() { return location; }
    public void setLocation(SessionLocation location) { this.location = location; }

    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }

    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public LocalDateTime getVotingDeadline() { return votingDeadline; }
    public void setVotingDeadline(LocalDateTime votingDeadline) { this.votingDeadline = votingDeadline; }

    public int getParticipantCount() { return participantCount; }
    public void setParticipantCount(int participantCount) { this.participantCount = participantCount; }

    public List<ProposedDate> getProposedDates() { return proposedDates; }
    public void setProposedDates(List<ProposedDate> proposedDates) { this.proposedDates = proposedDates; }

    // Helper methods
    public boolean isFull() {
        return maxParticipants > 0 && participantCount >= maxParticipants;
    }

    public boolean isPast() {
        return sessionDate != null && sessionDate.isBefore(LocalDateTime.now());
    }

    public String getStatusColor() {
        switch (status) {
            case PROPOSED: return "#FF9800";
            case CONFIRMED: return "#4CAF50";
            case CANCELLED: return "#F44336";
            case COMPLETED: return "#9E9E9E";
            default: return "#000000";
        }
    }
}

