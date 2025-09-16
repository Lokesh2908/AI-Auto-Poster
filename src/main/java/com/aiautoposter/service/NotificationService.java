package com.aiautoposter.service;

import com.aiautoposter.entity.Notification;
import com.aiautoposter.entity.User;
import com.aiautoposter.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private UserService userService;
    
    public Notification createNotification(Long postId, Long userId, Notification.NotificationType type, String message) {
        Notification notification = new Notification(postId, userId, type, message);
        Notification savedNotification = notificationRepository.save(notification);
        
        // Send email notification
        sendEmailNotification(userId, type, message);
        
        return savedNotification;
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
        } catch (Exception e) {
            System.err.println("Error sending email notification: " + e.getMessage());
        }
    }
}
