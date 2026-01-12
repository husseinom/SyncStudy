package com.syncstudy.BL.StudySessionManager;

import com.syncstudy.BL.StudySessionManager.StudySession.SessionStatus;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class StudySessionFacade {
    private static StudySessionFacade instance;
    private final StudySessionManager sessionManager;

    private StudySessionFacade() {
        this.sessionManager = StudySessionManager.getInstance();
    }

    public static StudySessionFacade getInstance() {
        if (instance == null) {
            instance = new StudySessionFacade();
        }
        return instance;
    }

    // Session Creation
    public StudySession createSession(StudySession session) throws IllegalArgumentException {
        validateSession(session);

        try {
            Long sessionId = sessionManager.createSession(session);
            session.setId(sessionId);

            // If proposed session, add proposed dates
            if (session.getStatus() == SessionStatus.PROPOSED && session.getProposedDates() != null) {
                for (ProposedDate proposedDate : session.getProposedDates()) {
                    sessionManager.addProposedDate(sessionId, proposedDate);
                }
            }

            return session;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create session: " + e.getMessage(), e);
        }
    }

    // Session Retrieval
    public StudySession getSession(Long sessionId) {
        try {
            StudySession session = sessionManager.getSessionById(sessionId);
            if (session != null && session.getStatus() == SessionStatus.PROPOSED) {
                List<ProposedDate> proposedDates = sessionManager.getProposedDates(sessionId);
                session.setProposedDates(proposedDates);
            }
            return session;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve session: " + e.getMessage(), e);
        }
    }

    public List<ProposedDate> getProposedDates(Long sessionId) {
        try {
            return sessionManager.getProposedDates(sessionId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get proposed dates: " + e.getMessage(), e);
        }
    }

    public ProposedDate addProposedDate(Long sessionId, ProposedDate proposedDate) {
        try {
            sessionManager.addProposedDate(sessionId, proposedDate);
            proposedDate.setId(proposedDate.getId());
            return proposedDate;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add proposed date: " + e.getMessage(), e);
        }
    }


    public List<StudySession> getGroupSessions(Long groupId, String filter) {
        try {
            List<StudySession> sessions = sessionManager.getSessionsByGroupId(groupId);

            // Load proposed dates for ALL sessions (not just proposed ones)
            for (StudySession session : sessions) {
                List<ProposedDate> proposedDates = sessionManager.getProposedDates(session.getId());
                session.setProposedDates(proposedDates);
            }

            // Apply filter AFTER loading proposed dates
            if (filter != null && !filter.equalsIgnoreCase("all")) {
                sessions = sessions.stream()
                        .filter(s -> matchesFilter(s, filter))
                        .collect(Collectors.toList());
            }

            return sessions;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load group sessions: " + e.getMessage(), e);
        }
    }


    public List<StudySession> getUserSessions(Long userId) {
        try {
            List<StudySession> registered = sessionManager.getUserRegisteredSessions(userId);
            List<StudySession> created = sessionManager.getUserCreatedSessions(userId);

            // Merge and remove duplicates
            created.addAll(registered);
            List<StudySession> uniqueSessions = created.stream()
                    .distinct()
                    .sorted((s1, s2) -> {
                        if (s1.getSessionDate() == null) return 1;
                        if (s2.getSessionDate() == null) return -1;
                        return s1.getSessionDate().compareTo(s2.getSessionDate());
                    })
                    .collect(Collectors.toList());

            // Load proposed dates for proposed sessions
            for (StudySession session : uniqueSessions) {
                if (session.getStatus() == SessionStatus.PROPOSED) {
                    List<ProposedDate> dates = sessionManager.getProposedDates(session.getId());
                    session.setProposedDates(dates);
                }
            }

            return uniqueSessions;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load user sessions: " + e.getMessage(), e);
        }
    }

    // Session Modification
    public void updateSession(StudySession session, Long userId, boolean isAdmin)
            throws IllegalArgumentException, SecurityException {
        validateSession(session);

        try {
            if (!sessionManager.canUserModifySession(session.getId(), userId, isAdmin)) {
                throw new SecurityException("User does not have permission to modify this session");
            }

            sessionManager.updateSession(session);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update session: " + e.getMessage(), e);
        }
    }

    public void cancelSession(Long sessionId, Long userId, boolean isAdmin, String reason)
            throws SecurityException {
        try {
            if (!sessionManager.canUserModifySession(sessionId, userId, isAdmin)) {
                throw new SecurityException("User does not have permission to cancel this session");
            }

            sessionManager.cancelSession(sessionId, reason);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to cancel session: " + e.getMessage(), e);
        }
    }

    // Participant Management
    public boolean registerForSession(Long sessionId, Long userId) {
        try {
            if (!sessionManager.canRegisterForSession(sessionId)) {
                throw new IllegalStateException("Cannot register for this session");
            }

            return sessionManager.registerParticipant(sessionId, userId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to register: " + e.getMessage(), e);
        }
    }

    public void unregisterFromSession(Long sessionId, Long userId) {
        try {
            sessionManager.unregisterParticipant(sessionId, userId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to unregister: " + e.getMessage(), e);
        }
    }

    public boolean isUserRegistered(Long sessionId, Long userId) {
        try {
            return sessionManager.isUserRegistered(sessionId, userId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check registration: " + e.getMessage(), e);
        }
    }

    public List<Long> getSessionParticipants(Long sessionId) {
        try {
            return sessionManager.getSessionParticipantIds(sessionId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get participants: " + e.getMessage(), e);
        }
    }

    // Voting Management
    public boolean voteForDate(Long proposedDateId, Long userId, Long sessionId) {
        try {
            StudySession session = sessionManager.getSessionById(sessionId);
            if (session == null || session.getStatus() != SessionStatus.PROPOSED) {
                throw new IllegalStateException("Session is not accepting votes");
            }

            if (session.getVotingDeadline() != null &&
                    LocalDateTime.now().isAfter(session.getVotingDeadline())) {
                throw new IllegalStateException("Voting deadline has passed");
            }

            return sessionManager.addVote(proposedDateId, userId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to vote: " + e.getMessage(), e);
        }
    }

    public boolean hasUserVoted(Long sessionId, Long userId) {
        try {
            return sessionManager.hasUserVoted(sessionId, userId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check vote status: " + e.getMessage(), e);
        }
    }

    public List<Long> getUserVotes(Long sessionId, Long userId) {
        try {
            return sessionManager.getUserVotes(sessionId, userId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get user votes: " + e.getMessage(), e);
        }
    }

    public ProposedDate getMostVotedDate(Long sessionId) {
        try {
            return sessionManager.getMostVotedDate(sessionId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get most voted date: " + e.getMessage(), e);
        }
    }

    public void finalizeProposedSession(Long sessionId, Long selectedDateId, Long userId, boolean isAdmin)
            throws SecurityException {
        try {
            if (!sessionManager.canUserModifySession(sessionId, userId, isAdmin)) {
                throw new SecurityException("User does not have permission to finalize this session");
            }

            StudySession session = sessionManager.getSessionById(sessionId);
            if (session == null || session.getStatus() != SessionStatus.PROPOSED) {
                throw new IllegalStateException("Session cannot be finalized");
            }

            // Find selected proposed date
            List<ProposedDate> proposedDates = sessionManager.getProposedDates(sessionId);
            ProposedDate selectedDate = proposedDates.stream()
                    .filter(pd -> pd.getId().equals(selectedDateId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid proposed date selected"));

            // Update session to confirmed status
            session.setStatus(SessionStatus.CONFIRMED);
            session.setSessionDate(selectedDate.getProposedDateTime());
            session.setStartTime(selectedDate.getProposedDateTime());
            session.setEndTime(selectedDate.getProposedDateTime().plusHours(2)); // Default 2 hours

            sessionManager.updateSession(session);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to finalize session: " + e.getMessage(), e);
        }
    }

    // Validation
    private void validateSession(StudySession session) throws IllegalArgumentException {
        if (session.getTitle() == null || session.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Session title is required");
        }

        if (session.getTitle().length() > 255) {
            throw new IllegalArgumentException("Session title must not exceed 255 characters");
        }

        if (session.getStatus() == SessionStatus.CONFIRMED) {
            if (session.getSessionDate() == null) {
                throw new IllegalArgumentException("Session date is required for confirmed sessions");
            }

            if (session.getSessionDate().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Session date must be in the future");
            }

            if (session.getStartTime() == null || session.getEndTime() == null) {
                throw new IllegalArgumentException("Start and end times are required");
            }

            if (!session.getEndTime().isAfter(session.getStartTime())) {
                throw new IllegalArgumentException("End time must be after start time");
            }
        }

        if (session.getLocation() == null || session.getLocation().getDisplayValue() == null) {
            throw new IllegalArgumentException("Location is required");
        }

        if (session.getStatus() == SessionStatus.PROPOSED) {
            if (session.getProposedDates() == null || session.getProposedDates().size() < 2) {
                throw new IllegalArgumentException("At least 2 proposed dates are required");
            }
            if (session.getProposedDates().size() > 5) {
                throw new IllegalArgumentException("Maximum 5 proposed dates allowed");
            }

            // Validate all proposed dates are in the future
            for (ProposedDate proposedDate : session.getProposedDates()) {
                if (proposedDate.getProposedDateTime().isBefore(LocalDateTime.now())) {
                    throw new IllegalArgumentException("All proposed dates must be in the future");
                }
            }
        }

        if (session.getMaxParticipants() < 0) {
            throw new IllegalArgumentException("Maximum participants cannot be negative");
        }
    }

    private boolean matchesFilter(StudySession session, String filter) {
        switch (filter.toLowerCase()) {
            case "proposed":
                return session.getStatus() == SessionStatus.PROPOSED;
            case "confirmed":
                return session.getStatus() == SessionStatus.CONFIRMED;
            case "cancelled":
                return session.getStatus() == SessionStatus.CANCELLED;
            case "upcoming":
                return session.getStatus() == SessionStatus.CONFIRMED &&
                        session.getSessionDate() != null &&
                        session.getSessionDate().isAfter(LocalDateTime.now());
            case "past":
                return session.getSessionDate() != null &&
                        session.getSessionDate().isBefore(LocalDateTime.now());
            default:
                return true;
        }
    }

    // Permission Checks
    public boolean canUserModifySession(Long sessionId, Long userId, boolean isAdmin) {
        try {
            return sessionManager.canUserModifySession(sessionId, userId, isAdmin);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check permissions: " + e.getMessage(), e);
        }
    }

    public boolean canUserRegister(Long sessionId) {
        try {
            return sessionManager.canRegisterForSession(sessionId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check registration eligibility: " + e.getMessage(), e);
        }
    }
}

