package com.syncstudy.BL.StudySessionManager;

import com.syncstudy.BL.AbstractFactory;
import com.syncstudy.PL.PostgresFactory;

import java.sql.SQLException;
import java.util.List;

public class StudySessionManager {
    private static StudySessionManager instance;
    private final StudySessionDAO sessionDAO;

    private StudySessionManager() {
        AbstractFactory factory = new PostgresFactory();
        this.sessionDAO = factory.createStudySessionDAO();
    }

    public static StudySessionManager getInstance() {
        if (instance == null) {
            instance = new StudySessionManager();
        }
        return instance;
    }

    public Long createSession(StudySession session) throws SQLException {
        return sessionDAO.createSession(session);
    }

    public StudySession getSessionById(Long sessionId) throws SQLException {
        return sessionDAO.getSessionById(sessionId);
    }

    public List<StudySession> getSessionsByGroupId(Long groupId) throws SQLException {
        return sessionDAO.getSessionsByGroupId(groupId);
    }

    public void updateSession(StudySession session) throws SQLException {
        sessionDAO.updateSession(session);
    }

    public void cancelSession(Long sessionId, String reason) throws SQLException {
        sessionDAO.cancelSession(sessionId, reason);
    }

    public void addProposedDate(Long sessionId, ProposedDate proposedDate) throws SQLException {
        sessionDAO.addProposedDate(sessionId, proposedDate);
    }

    public List<ProposedDate> getProposedDates(Long sessionId) throws SQLException {
        return sessionDAO.getProposedDates(sessionId);
    }

    public boolean registerParticipant(Long sessionId, Long userId) throws SQLException {
        StudySession session = getSessionById(sessionId);
        if (session != null && session.isFull()) {
            throw new SQLException("Session is full");
        }
        return sessionDAO.registerParticipant(sessionId, userId);
    }

    public void unregisterParticipant(Long sessionId, Long userId) throws SQLException {
        sessionDAO.unregisterParticipant(sessionId, userId);
    }

    public boolean isUserRegistered(Long sessionId, Long userId) throws SQLException {
        return sessionDAO.isUserRegistered(sessionId, userId);
    }

    public boolean addVote(Long proposedDateId, Long userId) throws SQLException {
        return sessionDAO.addVote(proposedDateId, userId);
    }

    public boolean hasUserVoted(Long sessionId, Long userId) throws SQLException {
        return sessionDAO.hasUserVoted(sessionId, userId);
    }

    public List<Long> getUserVotes(Long sessionId, Long userId) throws SQLException {
        return sessionDAO.getUserVotes(sessionId, userId);
    }

    public List<Long> getSessionParticipantIds(Long sessionId) throws SQLException {
        return sessionDAO.getSessionParticipantIds(sessionId);
    }

    public boolean canUserModifySession(Long sessionId, Long userId, boolean isAdmin) throws SQLException {
        StudySession session = getSessionById(sessionId);
        return session != null && (session.getCreatorId().equals(userId) || isAdmin);
    }

    public boolean canRegisterForSession(Long sessionId) throws SQLException {
        StudySession session = getSessionById(sessionId);
        if (session == null) return false;

        return session.getStatus() == StudySession.SessionStatus.CONFIRMED
                && !session.isFull()
                && !session.isPast();
    }

    public List<StudySession> getUserRegisteredSessions(Long userId) throws SQLException {
        return sessionDAO.getUserRegisteredSessions(userId);
    }

    public List<StudySession> getUserCreatedSessions(Long userId) throws SQLException {
        return sessionDAO.getUserCreatedSessions(userId);
    }

    public ProposedDate getMostVotedDate(Long sessionId) throws SQLException {
        List<ProposedDate> dates = getProposedDates(sessionId);
        if (dates.isEmpty()) return null;

        ProposedDate mostVoted = dates.get(0);
        for (ProposedDate date : dates) {
            if (date.getVoteCount() > mostVoted.getVoteCount()) {
                mostVoted = date;
            }
        }
        return mostVoted;
    }
}