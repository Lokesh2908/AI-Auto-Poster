package com.aiautoposter.service;

import com.aiautoposter.entity.Notification;
import com.aiautoposter.entity.User;
import com.aiautoposter.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class NotificationService {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired(required = false)
    private JavaMailSender mailSender;
    
    @Autowired
    private UserService userService;
    
    @Value("${spring.mail.enabled:false}")
    private boolean mailEnabled;
    
    public Notification createNotification(Long postId, Long userId, Notification.NotificationType type, String message) {
        return createNotification(postId, userId, type, message, null, null, null, null);
    }
    
    public Notification createNotification(Long postId, Long userId, Notification.NotificationType type, String message, 
                                         Long approvedBy, Long rejectedBy, String approvalFeedback, String rejectionFeedback) {
        try {
            System.out.println("NotificationService - Creating notification for user ID: " + userId);
            System.out.println("NotificationService - Post ID: " + postId + ", Type: " + type + ", Message: " + message);
            
            Notification notification = new Notification(postId, userId, type, message);
            
            // Set approval/rejection details
            if (approvedBy != null) {
                notification.setApprovedBy(approvedBy);
                // Fetch approver email and role
                try {
                    User approver = userService.findById(approvedBy).orElse(null);
                    if (approver != null) {
                        if (approver.getEmail() != null) {
                            notification.setApprovedByEmail(approver.getEmail());
                            System.out.println("NotificationService - Set approved by email: " + approver.getEmail());
                        }
                        if (approver.getRole() != null) {
                            notification.setApprovedByRole(approver.getRole().toString());
                            System.out.println("NotificationService - Set approved by role: " + approver.getRole());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("NotificationService - Error fetching approver details: " + e.getMessage());
                }
            }
            if (rejectedBy != null) {
                notification.setRejectedBy(rejectedBy);
                // Fetch rejector email and role
                try {
                    User rejector = userService.findById(rejectedBy).orElse(null);
                    if (rejector != null) {
                        if (rejector.getEmail() != null) {
                            notification.setRejectedByEmail(rejector.getEmail());
                            System.out.println("NotificationService - Set rejected by email: " + rejector.getEmail());
                        }
                        if (rejector.getRole() != null) {
                            notification.setRejectedByRole(rejector.getRole().toString());
                            System.out.println("NotificationService - Set rejected by role: " + rejector.getRole());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("NotificationService - Error fetching rejector details: " + e.getMessage());
                }
            }
            if (approvalFeedback != null) {
                notification.setApprovalFeedback(approvalFeedback);
            }
            if (rejectionFeedback != null) {
                notification.setRejectionFeedback(rejectionFeedback);
            }
            
            Notification savedNotification = notificationRepository.save(notification);
            System.out.println("NotificationService - Saved notification with ID: " + savedNotification.getId());
            
            // Send email notification
            try {
                sendEmailNotification(userId, type, message);
                System.out.println("NotificationService - Email notification sent successfully");
            } catch (Exception e) {
                System.err.println("NotificationService - Error sending email: " + e.getMessage());
                // Continue without email
            }
            
            return savedNotification;
            
        } catch (Exception e) {
            System.err.println("NotificationService - Error creating notification: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findUnreadByUserIdOrderByCreatedAtDesc(userId);
    }
    
    public List<Notification> getNotificationsByPost(Long postId) {
        return notificationRepository.findByPostId(postId);
    }
    
    public List<Notification> getNotificationsByType(Notification.NotificationType type) {
        return notificationRepository.findByType(type);
    }
    
    public List<Notification> getUserNotificationsByType(Long userId, Notification.NotificationType type) {
        return notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type);
    }
    
    public Long getUnreadNotificationCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }
    
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setReadStatus(true);
        return notificationRepository.save(notification);
    }
    
    public Notification markAsUnread(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setReadStatus(false);
        return notificationRepository.save(notification);
    }
    
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = getUnreadNotifications(userId);
        for (Notification notification : unreadNotifications) {
            notification.setReadStatus(true);
            notificationRepository.save(notification);
        }
    }
    
    public Optional<Notification> findById(Long id) {
        return notificationRepository.findById(id);
    }
    
    public List<Notification> findAll() {
        return notificationRepository.findAll();
    }
    
    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }
    
    public void deleteOldNotifications(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        List<Notification> oldNotifications = notificationRepository.findAll().stream()
                .filter(n -> n.getCreatedAt().isBefore(cutoffDate))
                .collect(java.util.stream.Collectors.toList());
        
        for (Notification notification : oldNotifications) {
            notificationRepository.delete(notification);
        }
    }
    
    private void sendEmailNotification(Long userId, Notification.NotificationType type, String message) {
        // Skip email sending if mail is disabled or mailSender is not available
        if (!mailEnabled || mailSender == null) {
            System.out.println("Email notifications are disabled. Skipping email for userId: " + userId);
            return;
        }
        try {
            // Fetch the user's actual email from database
            User user = userService.findById(userId).orElse(null);
            if (user == null || user.getEmail() == null) {
                System.err.println("Cannot send email notification: User not found or email is null for userId: " + userId);
                return;
            }
            
            String userEmail = user.getEmail();
            
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(userEmail);
            mailMessage.setSubject("AI Auto Poster - " + type.toString().replace("_", " "));
            mailMessage.setText(message);
            mailMessage.setFrom("noreply@aiautoposter.com");
            
            mailSender.send(mailMessage);
            System.out.println("Email notification sent successfully to: " + userEmail);
        } catch (Exception e) {
            System.err.println("Error sending email notification: " + e.getMessage());
            // Don't throw exception, just log and continue
        }
    }
}
