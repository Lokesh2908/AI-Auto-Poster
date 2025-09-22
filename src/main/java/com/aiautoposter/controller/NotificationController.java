package com.aiautoposter.controller;

import com.aiautoposter.entity.Notification;
import com.aiautoposter.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {
    
    @Autowired
    private NotificationService notificationService;
    
    @GetMapping
    public ResponseEntity<List<Notification>> getAllNotifications() {
        List<Notification> notifications = notificationService.findAll();
        return new ResponseEntity<>(notifications, HttpStatus.OK);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Notification> getNotificationById(@PathVariable Long id) {
        Optional<Notification> notification = notificationService.findById(id);
        return notification.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<java.util.Map<String, Object>>> getUserNotifications(@PathVariable Long userId) {
        try {
            System.out.println("=== GET USER NOTIFICATIONS DEBUG ===");
            System.out.println("User ID: " + userId);
            
            List<Notification> notifications = notificationService.getUserNotifications(userId);
            System.out.println("Found " + notifications.size() + " notifications for user " + userId);
            
            // Convert to safe JSON format to avoid Hibernate serialization issues
            List<java.util.Map<String, Object>> safeNotifications = new java.util.ArrayList<>();
            
            for (Notification notification : notifications) {
                java.util.Map<String, Object> safeNotification = new java.util.HashMap<>();
                safeNotification.put("id", notification.getId());
                safeNotification.put("postId", notification.getPostId());
                safeNotification.put("userId", notification.getUserId());
                safeNotification.put("type", notification.getType().toString());
                safeNotification.put("readStatus", notification.getReadStatus());
                safeNotification.put("message", notification.getMessage());
                safeNotification.put("createdAt", notification.getCreatedAt());
                safeNotification.put("updatedAt", notification.getUpdatedAt());
                
                // Add approval/rejection details
                safeNotification.put("approvedBy", notification.getApprovedBy());
                safeNotification.put("rejectedBy", notification.getRejectedBy());
                safeNotification.put("approvedByEmail", notification.getApprovedByEmail());
                safeNotification.put("rejectedByEmail", notification.getRejectedByEmail());
                safeNotification.put("approvedByRole", notification.getApprovedByRole());
                safeNotification.put("rejectedByRole", notification.getRejectedByRole());
                safeNotification.put("approvalFeedback", notification.getApprovalFeedback());
                safeNotification.put("rejectionFeedback", notification.getRejectionFeedback());
                
                safeNotifications.add(safeNotification);
            }
            
            System.out.println("Returning " + safeNotifications.size() + " safe notifications");
            return new ResponseEntity<>(safeNotifications, HttpStatus.OK);
            
        } catch (Exception e) {
            System.err.println("Error getting user notifications: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@PathVariable Long userId) {
        List<Notification> notifications = notificationService.getUnreadNotifications(userId);
        return new ResponseEntity<>(notifications, HttpStatus.OK);
    }
    
    @GetMapping("/user/{userId}/unread/count")
    public ResponseEntity<Long> getUnreadNotificationCount(@PathVariable Long userId) {
        Long count = notificationService.getUnreadNotificationCount(userId);
        return new ResponseEntity<>(count, HttpStatus.OK);
    }
    
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Notification>> getNotificationsByPost(@PathVariable Long postId) {
        List<Notification> notifications = notificationService.getNotificationsByPost(postId);
        return new ResponseEntity<>(notifications, HttpStatus.OK);
    }
    
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Notification>> getNotificationsByType(@PathVariable Notification.NotificationType type) {
        List<Notification> notifications = notificationService.getNotificationsByType(type);
        return new ResponseEntity<>(notifications, HttpStatus.OK);
    }
    
    @GetMapping("/user/{userId}/type/{type}")
    public ResponseEntity<List<Notification>> getUserNotificationsByType(@PathVariable Long userId, 
                                                                        @PathVariable Notification.NotificationType type) {
        List<Notification> notifications = notificationService.getUserNotificationsByType(userId, type);
        return new ResponseEntity<>(notifications, HttpStatus.OK);
    }
    
    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id) {
        try {
            Notification notification = notificationService.markAsRead(id);
            return new ResponseEntity<>(notification, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @PutMapping("/{id}/unread")
    public ResponseEntity<Notification> markAsUnread(@PathVariable Long id) {
        try {
            Notification notification = notificationService.markAsUnread(id);
            return new ResponseEntity<>(notification, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<Void> markAllAsRead(@PathVariable Long userId) {
        try {
            notificationService.markAllAsRead(userId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        try {
            notificationService.deleteNotification(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @DeleteMapping("/cleanup/{daysOld}")
    public ResponseEntity<Void> deleteOldNotifications(@PathVariable int daysOld) {
        try {
            notificationService.deleteOldNotifications(daysOld);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
