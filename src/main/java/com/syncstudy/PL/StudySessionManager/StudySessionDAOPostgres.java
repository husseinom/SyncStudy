package com.syncstudy.PL.StudySessionManager;

import com.syncstudy.BL.StudySessionManager.ProposedDate;
import com.syncstudy.BL.StudySessionManager.StudySession;
import com.syncstudy.BL.StudySessionManager.StudySession.SessionStatus;
import com.syncstudy.BL.StudySessionManager.StudySession.SessionLocation;
import com.syncstudy.BL.StudySessionManager.StudySession.LocationType;
import com.syncstudy.BL.StudySessionManager.StudySessionDAO;
import com.syncstudy.PL.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StudySessionDAOPostgres extends StudySessionDAO {
    private final DatabaseConnection dbConnection;

    public StudySessionDAOPostgres() {
        this.dbConnection = DatabaseConnection.getInstance();
        initializeDatabase();
    }

    private void initializeDatabase() {
        try (Connection conn = dbConnection.getConnection()) {
            createTableStudySessions(conn);
            createTableSessionParticipants(conn);
            createTableProposedDates(conn);
            createTableDateVotes(conn);
            createIndexes(conn);
        } catch (SQLException e) {
            System.err.println("Error initializing study session tables: " + e.getMessage());
        }
    }

    private void createTableStudySessions(Connection conn) {
        String sql = "CREATE TABLE IF NOT EXISTS study_sessions (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "group_id BIGINT NOT NULL REFERENCES groups(group_id) ON DELETE CASCADE, " +
                "creator_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE, " +
                "title VARCHAR(255) NOT NULL, " +
                "description TEXT, " +
                "session_date TIMESTAMP, " +
                "start_time TIMESTAMP, " +
                "end_time TIMESTAMP, " +
                "location_type VARCHAR(20) NOT NULL, " +
                "location_value VARCHAR(500) NOT NULL, " +
                "max_participants INTEGER DEFAULT 0, " +
                "status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED', " +
                "cancellation_reason TEXT, " +
                "voting_deadline TIMESTAMP, " +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP)";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Table 'study_sessions' created or already exists.");
        } catch (SQLException e) {
            System.err.println("Error creating study_sessions table: " + e.getMessage());
        }
    }

    private void createTableSessionParticipants(Connection conn) {
        String sql = "CREATE TABLE IF NOT EXISTS session_participants (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "session_id BIGINT NOT NULL REFERENCES study_sessions(id) ON DELETE CASCADE, " +
                "user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE, " +
                "registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE (session_id, user_id))";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Table 'session_participants' created or already exists.");
        } catch (SQLException e) {
            System.err.println("Error creating session_participants table: " + e.getMessage());
        }
    }

    private void createTableProposedDates(Connection conn) {
        String sql = "CREATE TABLE IF NOT EXISTS proposed_dates (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "session_id BIGINT NOT NULL REFERENCES study_sessions(id) ON DELETE CASCADE, " +
                "proposed_datetime TIMESTAMP NOT NULL, " +
                "vote_count INTEGER DEFAULT 0)";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Table 'proposed_dates' created or already exists.");
        } catch (SQLException e) {
            System.err.println("Error creating proposed_dates table: " + e.getMessage());
        }
    }

    private void createTableDateVotes(Connection conn) {
        String sql = "CREATE TABLE IF NOT EXISTS date_votes (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "proposed_date_id BIGINT NOT NULL REFERENCES proposed_dates(id) ON DELETE CASCADE, " +
                "user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE, " +
                "voted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE (proposed_date_id, user_id))";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Table 'date_votes' created or already exists.");
        } catch (SQLException e) {
            System.err.println("Error creating date_votes table: " + e.getMessage());
        }
    }

    private void createIndexes(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_sessions_group ON study_sessions(group_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_sessions_creator ON study_sessions(creator_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_sessions_date ON study_sessions(session_date)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_participants_session ON session_participants(session_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_participants_user ON session_participants(user_id)");
            System.out.println("Study session indexes created or already exist.");
        } catch (SQLException e) {
            System.err.println("Error creating indexes: " + e.getMessage());
        }
    }

    @Override
    public Long createSession(StudySession session) throws SQLException {
        String sql = "INSERT INTO study_sessions (group_id, creator_id, title, description, " +
                "session_date, start_time, end_time, location_type, location_value, " +
                "max_participants, status, voting_deadline, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, session.getGroupId());
            stmt.setLong(2, session.getCreatorId()); // This should properly set the creator
            stmt.setString(3, session.getTitle());
            stmt.setString(4, session.getDescription());
            stmt.setTimestamp(5, session.getSessionDate() != null ? Timestamp.valueOf(session.getSessionDate()) : null);
            stmt.setTimestamp(6, session.getStartTime() != null ? Timestamp.valueOf(session.getStartTime()) : null);
            stmt.setTimestamp(7, session.getEndTime() != null ? Timestamp.valueOf(session.getEndTime()) : null);
            stmt.setString(8, session.getLocation().getType().name());
            stmt.setString(9, session.getLocation().getDisplayValue());
            stmt.setInt(10, session.getMaxParticipants());
            stmt.setString(11, session.getStatus().name());
            stmt.setTimestamp(12, session.getVotingDeadline() != null ? Timestamp.valueOf(session.getVotingDeadline()) : null);
            stmt.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating session: " + e.getMessage());
            throw e;
        }
        return null;
    }


    public StudySession getSessionById(Long sessionId) throws SQLException {
        String sql = "SELECT s.*, u.username, u.full_name, " +
                "(SELECT COUNT(*) FROM session_participants WHERE session_id = s.id) as participant_count " +
                "FROM study_sessions s " +
                "JOIN users u ON s.creator_id = u.id " +
                "WHERE s.id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, sessionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSession(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding session: " + e.getMessage());
        }
        return null;
    }

    public List<StudySession> getSessionsByGroupId(Long groupId) throws SQLException {
        String sql = "SELECT s.*, u.username, u.full_name, " +
                "(SELECT COUNT(*) FROM session_participants WHERE session_id = s.id) as participant_count " +
                "FROM study_sessions s " +
                "JOIN users u ON s.creator_id = u.id " +
                "WHERE s.group_id = ? AND s.status != 'COMPLETED' " +
                "ORDER BY s.session_date ASC, s.created_at DESC";

        List<StudySession> sessions = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, groupId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToSession(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding sessions for group: " + e.getMessage());
        }
        return sessions;
    }

    public void updateSession(StudySession session) throws SQLException {
        String sql = "UPDATE study_sessions SET title = ?, description = ?, session_date = ?, " +
                "start_time = ?, end_time = ?, location_type = ?, location_value = ?, " +
                "max_participants = ?, status = ?, voting_deadline = ?, updated_at = ? " +
                "WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, session.getTitle());
            stmt.setString(2, session.getDescription());
            stmt.setTimestamp(3, session.getSessionDate() != null ? Timestamp.valueOf(session.getSessionDate()) : null);
            stmt.setTimestamp(4, session.getStartTime() != null ? Timestamp.valueOf(session.getStartTime()) : null);
            stmt.setTimestamp(5, session.getEndTime() != null ? Timestamp.valueOf(session.getEndTime()) : null);
            stmt.setString(6, session.getLocation().getType().name());
            stmt.setString(7, session.getLocation().getDisplayValue());
            stmt.setInt(8, session.getMaxParticipants());
            stmt.setString(9, session.getStatus().name());
            stmt.setTimestamp(10, session.getVotingDeadline() != null ? Timestamp.valueOf(session.getVotingDeadline()) : null);
            stmt.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setLong(12, session.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating session: " + e.getMessage());
            throw e;
        }
    }

    public void cancelSession(Long sessionId, String reason) throws SQLException {
        String sql = "UPDATE study_sessions SET status = 'CANCELLED', cancellation_reason = ?, " +
                "updated_at = ? WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reason);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setLong(3, sessionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error canceling session: " + e.getMessage());
            throw e;
        }
    }

    public void addProposedDate(Long sessionId, ProposedDate proposedDate) throws SQLException {
        String sql = "INSERT INTO proposed_dates (session_id, proposed_datetime, vote_count) " +
                "VALUES (?, ?, 0) RETURNING id";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, sessionId);
            stmt.setTimestamp(2, Timestamp.valueOf(proposedDate.getProposedDateTime()));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    proposedDate.setId(rs.getLong("id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error adding proposed date: " + e.getMessage());
            throw e;
        }
    }

    public List<ProposedDate> getProposedDates(Long sessionId) throws SQLException {
        String sql = "SELECT id, session_id, proposed_datetime, vote_count " +
                "FROM proposed_dates WHERE session_id = ? ORDER BY proposed_datetime ASC";

        List<ProposedDate> dates = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, sessionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    dates.add(mapResultSetToProposedDate(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting proposed dates: " + e.getMessage());
        }
        return dates;
    }

    public boolean registerParticipant(Long sessionId, Long userId) throws SQLException {
        String sql = "INSERT INTO session_participants (session_id, user_id) VALUES (?, ?) " +
                "ON CONFLICT (session_id, user_id) DO NOTHING";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, sessionId);
            stmt.setLong(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error registering participant: " + e.getMessage());
        }
        return false;
    }

    public void unregisterParticipant(Long sessionId, Long userId) throws SQLException {
        String sql = "DELETE FROM session_participants WHERE session_id = ? AND user_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, sessionId);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error unregistering participant: " + e.getMessage());
            throw e;
        }
    }

    public boolean isUserRegistered(Long sessionId, Long userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM session_participants WHERE session_id = ? AND user_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, sessionId);
            stmt.setLong(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking registration: " + e.getMessage());
        }
        return false;
    }

    public boolean addVote(Long proposedDateId, Long userId) throws SQLException {
        String sql = "INSERT INTO date_votes (proposed_date_id, user_id) VALUES (?, ?) " +
                "ON CONFLICT (proposed_date_id, user_id) DO NOTHING";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, proposedDateId);
            stmt.setLong(2, userId);
            int rows = stmt.executeUpdate();

            if (rows > 0) {
                String updateSql = "UPDATE proposed_dates SET vote_count = vote_count + 1 WHERE id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setLong(1, proposedDateId);
                    updateStmt.executeUpdate();
                }
            }
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error adding vote: " + e.getMessage());
        }
        return false;
    }

    public boolean hasUserVoted(Long sessionId, Long userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM date_votes v " +
                "JOIN proposed_dates pd ON v.proposed_date_id = pd.id " +
                "WHERE pd.session_id = ? AND v.user_id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, sessionId);
            stmt.setLong(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking votes: " + e.getMessage());
        }
        return false;
    }

    public List<Long> getUserVotes(Long sessionId, Long userId) throws SQLException {
        String sql = "SELECT v.proposed_date_id FROM date_votes v " +
                "JOIN proposed_dates pd ON v.proposed_date_id = pd.id " +
                "WHERE pd.session_id = ? AND v.user_id = ?";

        List<Long> votes = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, sessionId);
            stmt.setLong(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    votes.add(rs.getLong("proposed_date_id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting user votes: " + e.getMessage());
        }
        return votes;
    }

    public List<Long> getSessionParticipantIds(Long sessionId) throws SQLException {
        String sql = "SELECT user_id FROM session_participants WHERE session_id = ?";

        List<Long> ids = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, sessionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong("user_id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting participant IDs: " + e.getMessage());
        }
        return ids;
    }

    public List<StudySession> getUserRegisteredSessions(Long userId) throws SQLException {
        String sql = "SELECT s.*, u.username, u.full_name, " +
                "(SELECT COUNT(*) FROM session_participants WHERE session_id = s.id) as participant_count " +
                "FROM study_sessions s " +
                "JOIN users u ON s.creator_id = u.id " +
                "JOIN session_participants sp ON s.id = sp.session_id " +
                "WHERE sp.user_id = ? ORDER BY s.session_date ASC";

        return executeSessionQuery(sql, userId);
    }

    public List<StudySession> getUserCreatedSessions(Long userId) throws SQLException {
        String sql = "SELECT s.*, u.username, u.full_name, " +
                "(SELECT COUNT(*) FROM session_participants WHERE session_id = s.id) as participant_count " +
                "FROM study_sessions s " +
                "JOIN users u ON s.creator_id = u.id " +
                "WHERE s.creator_id = ? ORDER BY s.created_at DESC";

        return executeSessionQuery(sql, userId);
    }

    public List<StudySession> getUpcomingSessions(Long groupId) throws SQLException {
        String sql = "SELECT s.*, u.username, u.full_name, " +
                "(SELECT COUNT(*) FROM session_participants WHERE session_id = s.id) as participant_count " +
                "FROM study_sessions s " +
                "JOIN users u ON s.creator_id = u.id " +
                "WHERE s.group_id = ? AND s.session_date > NOW() AND s.status = 'CONFIRMED' " +
                "ORDER BY s.session_date ASC";

        return executeSessionQuery(sql, groupId);
    }

    private List<StudySession> executeSessionQuery(String sql, Long param) throws SQLException {
        List<StudySession> sessions = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, param);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSetToSession(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error executing session query: " + e.getMessage());
        }
        return sessions;
    }

    private StudySession mapResultSetToSession(ResultSet rs) throws SQLException {
        StudySession session = new StudySession();
        session.setId(rs.getLong("id"));
        session.setGroupId(rs.getLong("group_id"));
        session.setCreatorId(rs.getLong("creator_id"));
        session.setCreatorUsername(rs.getString("username"));
        session.setCreatorFullName(rs.getString("full_name"));
        session.setTitle(rs.getString("title"));
        session.setDescription(rs.getString("description"));

        Timestamp sessionDate = rs.getTimestamp("session_date");
        session.setSessionDate(sessionDate != null ? sessionDate.toLocalDateTime() : null);

        Timestamp startTime = rs.getTimestamp("start_time");
        session.setStartTime(startTime != null ? startTime.toLocalDateTime() : null);

        Timestamp endTime = rs.getTimestamp("end_time");
        session.setEndTime(endTime != null ? endTime.toLocalDateTime() : null);

        LocationType locationType = LocationType.valueOf(rs.getString("location_type"));
        String locationValue = rs.getString("location_value");
        session.setLocation(new SessionLocation(locationType, locationValue));

        session.setMaxParticipants(rs.getInt("max_participants"));
        session.setStatus(SessionStatus.valueOf(rs.getString("status")));

        Timestamp createdAt = rs.getTimestamp("created_at");
        session.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : null);

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        session.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);

        session.setCancellationReason(rs.getString("cancellation_reason"));

        Timestamp votingDeadline = rs.getTimestamp("voting_deadline");
        session.setVotingDeadline(votingDeadline != null ? votingDeadline.toLocalDateTime() : null);

        session.setParticipantCount(rs.getInt("participant_count"));

        return session;
    }

    private ProposedDate mapResultSetToProposedDate(ResultSet rs) throws SQLException {
        ProposedDate date = new ProposedDate();
        date.setId(rs.getLong("id"));
        date.setSessionId(rs.getLong("session_id"));
        date.setProposedDateTime(rs.getTimestamp("proposed_datetime").toLocalDateTime());
        date.setVoteCount(rs.getInt("vote_count"));
        return date;
    }
}