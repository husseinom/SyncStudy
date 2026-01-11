package com.syncstudy.BL.ChatManager;

import com.syncstudy.BL.GroupManager.Group;
import com.syncstudy.BL.GroupManager.GroupManager;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Singleton Facade providing simplified interface for chat management
 */
public class ChatFacade {
    private static ChatFacade instance;
    private MessageManager messageManager;

    private ChatFacade() {
        this.messageManager = new MessageManager();
    }

    /**
     * Get the singleton instance of ChatFacade
     * @return ChatFacade instance
     */
    public static ChatFacade getInstance() {
        if (instance == null) {
            synchronized (ChatFacade.class) {
                if (instance == null) {
                    instance = new ChatFacade();
                }
            }
        }
        return instance;
    }

    /**
     * Get all messages for a group
     * @param groupId the group ID
     * @return list of messages
     */
    public List<Message> getMessages(Long groupId) {
        try {
            return messageManager.getMessages(groupId);
        } catch (Exception e) {
            System.err.println("Error fetching messages: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Get paginated messages for infinite scroll
     * @param groupId the group ID
     * @param beforeTimestamp load messages before this timestamp
     * @param limit number of messages to load
     * @return list of messages
     */
    public List<Message> getMessagesWithPagination(Long groupId, LocalDateTime beforeTimestamp, int limit) {
        try {
            return messageManager.getMessagesWithPagination(groupId, beforeTimestamp, limit);
        } catch (Exception e) {
            System.err.println("Error fetching paginated messages: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Send a new message to a group
     * @param senderId the sender's user ID
     * @param groupId the group ID
     * @param content the message content
     * @return the created message, or null if failed
     */
    public Message sendMessage(Long senderId, Long groupId, String content) {
        try {
            return messageManager.sendMessage(senderId, groupId, content);
        } catch (IllegalArgumentException e) {
            System.err.println("Validation error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Error sending message: " + e.getMessage());
            return null;
        }
    }

    /**
     * Edit an existing message
     * @param messageId the message ID
     * @param userId the user ID attempting the edit
     * @param newContent the new message content
     * @return true if edit successful, false otherwise
     */
    public boolean editMessage(Long messageId, Long userId, String newContent) {
        try {
            return messageManager.editMessage(messageId, userId, newContent);
        } catch (IllegalArgumentException | SecurityException | IllegalStateException e) {
            System.err.println("Edit message error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Error editing message: " + e.getMessage());
            return false;
        }
    }

    /**
     * Delete a message
     * @param messageId the message ID
     * @param userId the user ID attempting the deletion
     * @param isAdmin whether the user is an admin
     * @return true if deletion successful, false otherwise
     */
    public boolean deleteMessage(Long messageId, Long userId, boolean isAdmin) {
        try {
            return messageManager.deleteMessage(messageId, userId, isAdmin);
        } catch (SecurityException | IllegalStateException e) {
            System.err.println("Delete message error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Error deleting message: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get a specific message by ID
     * @param messageId the message ID
     * @return the message, or null if not found
     */
    public Message getMessage(Long messageId) {
        try {
            return messageManager.getMessage(messageId);
        } catch (Exception e) {
            System.err.println("Error fetching message: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if a user can edit a specific message
     * @param messageId the message ID
     * @param userId the user ID
     * @return true if user can edit, false otherwise
     */
    public boolean canEditMessage(Long messageId, Long userId) {
        try {
            Message message = messageManager.getMessage(messageId);
            return message != null && message.getSenderId().equals(userId);
        } catch (Exception e) {
            System.err.println("Error checking edit permission: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if a user can delete a specific message
     * @param messageId the message ID
     * @param userId the user ID
     * @param isAdmin whether the user is an admin
     * @return true if user can delete, false otherwise
     */
    public boolean canDeleteMessage(Long messageId, Long userId, boolean isAdmin) {
        if (isAdmin) {
            return true;
        }
        return canEditMessage(messageId, userId);
    }
    public String getGroupName(Long groupId) {
        if (groupId == null) return "Unknown Group";
        try {
            Group group = GroupManager.getInstance().findGroupById(groupId);
            if (group != null) {
                String name = group.getName();
                if (name != null && !name.isBlank()) return name;
            }
            return "Unknown Group";
        } catch (Exception e) {
            System.err.println("Error fetching group name: " + e.getMessage());
            return "Unknown Group";
        }
    }
}
