package com.syncstudy.PL.GroupMembership;

import com.syncstudy.BL.GroupMembership.GroupMembershipDAO;
import com.syncstudy.BL.GroupMembership.JoinRequest;
import com.syncstudy.BL.GroupMembership.GroupMember;
import com.syncstudy.PL.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * GroupMembershipDAOPostgres - PostgreSQL implementation for group membership operations
 * UC03 - Manage Group Membership
 */
public class GroupMembershipDAOPostgres extends GroupMembershipDAO {
    private DatabaseConnection dbConnection;
    
    public GroupMembershipDAOPostgres() {
        this.dbConnection = DatabaseConnection.getInstance();
        initializeDatabase();
    }
    
    private void initializeDatabase() {
        try (Connection conn = dbConnection.getConnection()) {
            createTableJoinRequests(conn);
            createTableGroupMembers(conn);
            System.out.println("GroupMembership database tables initialized successfully.");
        } catch (SQLException e) {
            System.err.println("Error initializing GroupMembership database: " + e.getMessage());
        }
    }
    
    private void createTableJoinRequests(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS join_requests (
                request_id SERIAL PRIMARY KEY,
                user_id BIGINT NOT NULL,
                group_id BIGINT NOT NULL,
                request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                status VARCHAR(20) DEFAULT 'Pending' CHECK (status IN ('Pending', 'Approved', 'Rejected')),
                message TEXT,
                rejection_reason TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(user_id, group_id, status) -- Prevent duplicate pending requests
            )
        """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
        
        // Create indexes for performance
        String indexSql = """
            CREATE INDEX IF NOT EXISTS idx_join_requests_group_status 
            ON join_requests(group_id, status);
            CREATE INDEX IF NOT EXISTS idx_join_requests_user 
            ON join_requests(user_id);
        """;
        
        try (PreparedStatement stmt = conn.prepareStatement(indexSql)) {
            stmt.executeUpdate();
        }
    }
    
    private void createTableGroupMembers(Connection conn) throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS group_members (
                member_id SERIAL PRIMARY KEY,
                user_id BIGINT NOT NULL,
                group_id BIGINT NOT NULL,
                role VARCHAR(50) DEFAULT 'Member' CHECK (role IN ('Member', 'Group Admin', 'Administrator')),
                joined_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                is_banned BOOLEAN DEFAULT FALSE,
                ban_reason TEXT,
                ban_date TIMESTAMP,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(user_id, group_id) -- One membership per user per group
            )
        """;
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
        
        // Create indexes for performance
        String indexSql = """
            CREATE INDEX IF NOT EXISTS idx_group_members_group 
            ON group_members(group_id);
            CREATE INDEX IF NOT EXISTS idx_group_members_user 
            ON group_members(user_id);
            CREATE INDEX IF NOT EXISTS idx_group_members_role 
            ON group_members(group_id, role);
        """;
        
        try (PreparedStatement stmt = conn.prepareStatement(indexSql)) {
            stmt.executeUpdate();
        }
    }
    
    // ====================================================
    // JOIN REQUESTS IMPLEMENTATION
    // ====================================================
    
    @Override
    public JoinRequest createJoinRequest(JoinRequest request) {
        String sql = """
            INSERT INTO join_requests (user_id, group_id, message, status, request_date)
            VALUES (?, ?, ?, ?, ?) RETURNING request_id
        """;
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, request.getUserId());
            stmt.setLong(2, request.getGroupId());
            stmt.setString(3, request.getMessage());
            stmt.setString(4, request.getStatus());
            stmt.setTimestamp(5, Timestamp.valueOf(request.getRequestDate()));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    request.setRequestId(rs.getLong("request_id"));
                }
            }
            
            return request;
            
        } catch (SQLException e) {
            throw new RuntimeException("Error creating join request: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<JoinRequest> getPendingJoinRequests(Long groupId) {
        String sql = """
            SELECT request_id, user_id, group_id, request_date, status, message, rejection_reason
            FROM join_requests
            WHERE group_id = ? AND status = 'Pending'
            ORDER BY request_date ASC
        """;
        
        List<JoinRequest> requests = new ArrayList<>();
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, groupId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JoinRequest request = new JoinRequest(
                        rs.getLong("request_id"),
                        rs.getLong("user_id"),
                        rs.getLong("group_id"),
                        rs.getTimestamp("request_date").toLocalDateTime(),
                        rs.getString("status"),
                        rs.getString("message"),
                        rs.getString("rejection_reason")
                    );
                    requests.add(request);
                }
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error getting pending join requests: " + e.getMessage(), e);
        }
        
        return requests;
    }
    
    @Override
    public JoinRequest getJoinRequestByUserAndGroup(Long userId, Long groupId) {
        String sql = """
            SELECT request_id, user_id, group_id, request_date, status, message, rejection_reason
            FROM join_requests
            WHERE user_id = ? AND group_id = ? AND status = 'Pending'
            ORDER BY request_date DESC LIMIT 1
        """;
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            stmt.setLong(2, groupId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new JoinRequest(
                        rs.getLong("request_id"),
                        rs.getLong("user_id"),
                        rs.getLong("group_id"),
                        rs.getTimestamp("request_date").toLocalDateTime(),
                        rs.getString("status"),
                        rs.getString("message"),
                        rs.getString("rejection_reason")
                    );
                }
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error getting join request: " + e.getMessage(), e);
        }
        
        return null;
    }
    
    @Override
    public void updateJoinRequestStatus(Long requestId, String status, String rejectionReason) {
        String sql = """
            UPDATE join_requests 
            SET status = ?, rejection_reason = ?, updated_at = CURRENT_TIMESTAMP
            WHERE request_id = ?
        """;
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            stmt.setString(2, rejectionReason);
            stmt.setLong(3, requestId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("Join request not found with ID: " + requestId);
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error updating join request status: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<JoinRequest> getUserJoinRequests(Long userId) {
        String sql = """
            SELECT request_id, user_id, group_id, request_date, status, message, rejection_reason
            FROM join_requests
            WHERE user_id = ?
            ORDER BY request_date DESC
        """;
        
        List<JoinRequest> requests = new ArrayList<>();
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JoinRequest request = new JoinRequest(
                        rs.getLong("request_id"),
                        rs.getLong("user_id"),
                        rs.getLong("group_id"),
                        rs.getTimestamp("request_date").toLocalDateTime(),
                        rs.getString("status"),
                        rs.getString("message"),
                        rs.getString("rejection_reason")
                    );
                    requests.add(request);
                }
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error getting user join requests: " + e.getMessage(), e);
        }
        
        return requests;
    }
    
    // ====================================================
    // GROUP MEMBERS IMPLEMENTATION
    // ====================================================
    
    @Override
    public GroupMember addGroupMember(GroupMember member) {
        String sql = """
            INSERT INTO group_members (user_id, group_id, role, joined_date, is_banned)
            VALUES (?, ?, ?, ?, ?) RETURNING member_id
        """;
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, member.getUserId());
            stmt.setLong(2, member.getGroupId());
            stmt.setString(3, member.getRole());
            stmt.setTimestamp(4, Timestamp.valueOf(member.getJoinedDate()));
            stmt.setBoolean(5, member.isBanned());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    member.setMemberId(rs.getLong("member_id"));
                }
            }
            
            return member;
            
        } catch (SQLException e) {
            throw new RuntimeException("Error adding group member: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<GroupMember> getGroupMembers(Long groupId) {
        String sql = """
            SELECT member_id, user_id, group_id, role, joined_date, is_banned, ban_reason, ban_date
            FROM group_members
            WHERE group_id = ? AND is_banned = FALSE
            ORDER BY joined_date ASC
        """;
        
        List<GroupMember> members = new ArrayList<>();
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, groupId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    GroupMember member = new GroupMember(
                        rs.getLong("member_id"),
                        rs.getLong("user_id"),
                        rs.getLong("group_id"),
                        rs.getString("role"),
                        rs.getTimestamp("joined_date").toLocalDateTime(),
                        rs.getBoolean("is_banned"),
                        rs.getString("ban_reason"),
                        rs.getTimestamp("ban_date") != null ? 
                            rs.getTimestamp("ban_date").toLocalDateTime() : null
                    );
                    members.add(member);
                }
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error getting group members: " + e.getMessage(), e);
        }
        
        return members;
    }
    
    @Override
    public GroupMember getGroupMember(Long userId, Long groupId) {
        String sql = """
            SELECT member_id, user_id, group_id, role, joined_date, is_banned, ban_reason, ban_date
            FROM group_members
            WHERE user_id = ? AND group_id = ?
        """;
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            stmt.setLong(2, groupId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new GroupMember(
                        rs.getLong("member_id"),
                        rs.getLong("user_id"),
                        rs.getLong("group_id"),
                        rs.getString("role"),
                        rs.getTimestamp("joined_date").toLocalDateTime(),
                        rs.getBoolean("is_banned"),
                        rs.getString("ban_reason"),
                        rs.getTimestamp("ban_date") != null ? 
                            rs.getTimestamp("ban_date").toLocalDateTime() : null
                    );
                }
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error getting group member: " + e.getMessage(), e);
        }
        
        return null;
    }
    
    @Override
    public void removeGroupMember(Long userId, Long groupId) {
        String sql = "DELETE FROM group_members WHERE user_id = ? AND group_id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            stmt.setLong(2, groupId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("Group member not found: userId=" + userId + ", groupId=" + groupId);
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error removing group member: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void updateMemberRole(Long userId, Long groupId, String newRole) {
        String sql = """
            UPDATE group_members 
            SET role = ?, updated_at = CURRENT_TIMESTAMP
            WHERE user_id = ? AND group_id = ?
        """;
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, newRole);
            stmt.setLong(2, userId);
            stmt.setLong(3, groupId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("Group member not found for role update: userId=" + userId + ", groupId=" + groupId);
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error updating member role: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void banMember(Long userId, Long groupId, String reason) {
        String sql = """
            UPDATE group_members 
            SET is_banned = TRUE, ban_reason = ?, ban_date = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
            WHERE user_id = ? AND group_id = ?
        """;
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, reason);
            stmt.setLong(2, userId);
            stmt.setLong(3, groupId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("Group member not found for ban: userId=" + userId + ", groupId=" + groupId);
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error banning member: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void unbanMember(Long userId, Long groupId) {
        String sql = """
            UPDATE group_members 
            SET is_banned = FALSE, ban_reason = NULL, ban_date = NULL, updated_at = CURRENT_TIMESTAMP
            WHERE user_id = ? AND group_id = ?
        """;
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            stmt.setLong(2, groupId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("Group member not found for unban: userId=" + userId + ", groupId=" + groupId);
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error unbanning member: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<GroupMember> getBannedMembers(Long groupId) {
        String sql = """
            SELECT member_id, user_id, group_id, role, joined_date, is_banned, ban_reason, ban_date
            FROM group_members
            WHERE group_id = ? AND is_banned = TRUE
            ORDER BY ban_date DESC
        """;
        
        List<GroupMember> bannedMembers = new ArrayList<>();
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, groupId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    GroupMember member = new GroupMember(
                        rs.getLong("member_id"),
                        rs.getLong("user_id"),
                        rs.getLong("group_id"),
                        rs.getString("role"),
                        rs.getTimestamp("joined_date").toLocalDateTime(),
                        rs.getBoolean("is_banned"),
                        rs.getString("ban_reason"),
                        rs.getTimestamp("ban_date") != null ? 
                            rs.getTimestamp("ban_date").toLocalDateTime() : null
                    );
                    bannedMembers.add(member);
                }
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error getting banned members: " + e.getMessage(), e);
        }
        
        return bannedMembers;
    }
    
    // ====================================================
    // VERIFICATION METHODS IMPLEMENTATION
    // ====================================================
    
    @Override
    public boolean isUserMember(Long userId, Long groupId) {
        String sql = """
            SELECT 1 FROM group_members
            WHERE user_id = ? AND group_id = ? AND is_banned = FALSE
        """;
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            stmt.setLong(2, groupId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error checking user membership: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isUserBanned(Long userId, Long groupId) {
        String sql = """
            SELECT 1 FROM group_members
            WHERE user_id = ? AND group_id = ? AND is_banned = TRUE
        """;
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            stmt.setLong(2, groupId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error checking user ban status: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean hasPendingRequest(Long userId, Long groupId) {
        String sql = """
            SELECT 1 FROM join_requests
            WHERE user_id = ? AND group_id = ? AND status = 'Pending'
        """;
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            stmt.setLong(2, groupId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error checking pending request: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isGroupAdmin(Long userId, Long groupId) {
        String sql = """
            SELECT 1 FROM group_members
            WHERE user_id = ? AND group_id = ? AND role IN ('Group Admin', 'Administrator') AND is_banned = FALSE
        """;
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            stmt.setLong(2, groupId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error checking group admin status: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean canManageGroup(Long userId, Long groupId) {
        return isGroupAdmin(userId, groupId);
    }
    
    @Override
    public int getMemberCount(Long groupId) {
        String sql = """
            SELECT COUNT(*) FROM group_members
            WHERE group_id = ? AND is_banned = FALSE
        """;
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, groupId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error getting member count: " + e.getMessage(), e);
        }
        
        return 0;
    }
    
    @Override
    public List<Long> getUserGroups(Long userId) {
        String sql = """
            SELECT group_id FROM group_members
            WHERE user_id = ? AND is_banned = FALSE
        """;
        
        List<Long> groupIds = new ArrayList<>();
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    groupIds.add(rs.getLong("group_id"));
                }
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Error getting user groups: " + e.getMessage(), e);
        }
        
        return groupIds;
    }
}
