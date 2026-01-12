package com.syncstudy.BL.StudySessionManager;

import java.time.LocalDateTime;

public class ProposedDate {
    private Long id;
    private Long sessionId;
    private LocalDateTime proposedDateTime;
    private int voteCount;

    public ProposedDate() {}

    public ProposedDate(Long sessionId, LocalDateTime proposedDateTime) {
        this.sessionId = sessionId;
        this.proposedDateTime = proposedDateTime;
        this.voteCount = 0;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public LocalDateTime getProposedDateTime() { return proposedDateTime; }
    public void setProposedDateTime(LocalDateTime proposedDateTime) { this.proposedDateTime = proposedDateTime; }

    public int getVoteCount() { return voteCount; }
    public void setVoteCount(int voteCount) { this.voteCount = voteCount; }

    public void incrementVoteCount() { this.voteCount++; }
    public void decrementVoteCount() { if (this.voteCount > 0) this.voteCount--; }
}

