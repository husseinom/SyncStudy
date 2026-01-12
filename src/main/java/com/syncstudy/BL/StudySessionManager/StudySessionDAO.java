package com.syncstudy.BL.StudySessionManager;

import com.syncstudy.PL.StudySessionManager.StudySessionDAOPostgres;

import java.sql.SQLException;
import java.util.List;

public abstract class StudySessionDAO {

    // Abstract methods that must be implemented by concrete classes
    public abstract Long createSession(StudySession session) throws SQLException;

    public abstract StudySession getSessionById(Long sessionId) throws SQLException;

    public abstract List<StudySession> getSessionsByGroupId(Long groupId) throws SQLException;

    public abstract void updateSession(StudySession session) throws SQLException;

    public abstract void cancelSession(Long sessionId, String reason) throws SQLException;

    public abstract void addProposedDate(Long sessionId, ProposedDate proposedDate) throws SQLException;

    public abstract List<ProposedDate> getProposedDates(Long sessionId) throws SQLException;

    public abstract boolean registerParticipant(Long sessionId, Long userId) throws SQLException;

    public abstract void unregisterParticipant(Long sessionId, Long userId) throws SQLException;

    public abstract boolean isUserRegistered(Long sessionId, Long userId) throws SQLException;

    public abstract boolean addVote(Long proposedDateId, Long userId) throws SQLException;

    public abstract boolean hasUserVoted(Long sessionId, Long userId) throws SQLException;

    public abstract List<Long> getUserVotes(Long sessionId, Long userId) throws SQLException;

    public abstract List<Long> getSessionParticipantIds(Long sessionId) throws SQLException;

    public abstract List<StudySession> getUserRegisteredSessions(Long userId) throws SQLException;

    public abstract List<StudySession> getUserCreatedSessions(Long userId) throws SQLException;

    public abstract List<StudySession> getUpcomingSessions(Long groupId) throws SQLException;
}