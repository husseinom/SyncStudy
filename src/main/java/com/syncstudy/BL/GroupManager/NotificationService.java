package com.syncstudy.BL.GroupManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * NotificationService - Singleton service for sending notifications
 * Currently simulates notifications by logging to console
 * In production, this would send emails or in-app notifications
 */
public class NotificationService {

    // Singleton instance
    private static NotificationService instance;

    // Date formatter for logs
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Private constructor for singleton
    private NotificationService() {
        // Initialize notification service
        System.out.println("NotificationService initialized.");
    }

    /**
     * Get singleton instance
     * @return NotificationService instance
     */
    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    /**
     * Send a notification to a user
     * @param userId the user ID to notify
     * @param message the notification message
     */
    public void sendNotification(Long userId, String message) {
        if (userId == null || message == null) {
            return;
        }

        String timestamp = LocalDateTime.now().format(DATE_FORMAT);

        // Simulation: log to console
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("📧 NOTIFICATION [" + timestamp + "]");
        System.out.println("   To User ID: " + userId);
        System.out.println("   Message: " + message);
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // In production, this would:
        // 1. Look up user email from database
        // 2. Send email via SMTP or email service
        // 3. Create in-app notification record
        // 4. Potentially send push notification
    }

    /**
     * Send a notification to multiple users
     * @param userIds list of user IDs to notify
     * @param message the notification message
     */
    public void sendNotificationToMany(java.util.List<Long> userIds, String message) {
        if (userIds == null || message == null) {
            return;
        }

        for (Long userId : userIds) {
            sendNotification(userId, message);
        }
    }

    /**
     * Send an email notification (simulation)
     * @param userId the user ID
     * @param subject email subject
     * @param body email body
     */
    public void sendEmail(Long userId, String subject, String body) {
        if (userId == null || subject == null || body == null) {
            return;
        }

        String timestamp = LocalDateTime.now().format(DATE_FORMAT);

        // Simulation: log to console
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("📨 EMAIL [" + timestamp + "]");
        System.out.println("   To User ID: " + userId);
        System.out.println("   Subject: " + subject);
        System.out.println("   Body: " + body);
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}

