package com.aiautoposter.service;

import com.aiautoposter.entity.*;
import com.aiautoposter.repository.*;
import com.aiautoposter.repository.ScheduleRepository;
import com.aiautoposter.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ScheduleService {
    
    @Autowired
    private ScheduleRepository scheduleRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private LinkedInService linkedInService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private LinkedInUserRepository linkedInUserRepository;

    @Autowired
    private UserService userService;
    
    @Autowired
    private WordpressService wordpressService;
    
    @Autowired
    private PostService postService;
    public Schedule createSchedule(Long postId, LocalDateTime scheduledFor) {
        Schedule schedule = new Schedule(postId, scheduledFor);
        return scheduleRepository.save(schedule);
    }
    
    public Schedule updateSchedule(Schedule schedule) {
        return scheduleRepository.save(schedule);
    }
    
    public Optional<Schedule> findById(Long id) {
        return scheduleRepository.findById(id);
    }
    
    public List<Schedule> findByPostId(Long postId) {
        return scheduleRepository.findByPostId(postId);
    }
    
    public List<Schedule> findByStatus(Schedule.ScheduleStatus status) {
        return scheduleRepository.findByStatus(status);
    }
    
    public List<Schedule> findByScheduledForBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return scheduleRepository.findByScheduledForBetween(startTime, endTime);
    }
    
    public List<Schedule> findAll() {
        return scheduleRepository.findAll();
    }
    
    public List<Schedule> getPendingSchedules() {
        return scheduleRepository.findByStatus(Schedule.ScheduleStatus.PENDING);
    }
    
    public List<Schedule> getSchedulesToPublish() {
        return scheduleRepository.findPendingSchedulesToPublish(
            Schedule.ScheduleStatus.PENDING, LocalDateTime.now());
    }
    
    public Schedule schedulePost(Long postId, LocalDateTime scheduledFor) {
        // Verify post exists and is approved
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        if (post.getCurrentStatus() != Post.PostStatus.APPROVED) {
            throw new RuntimeException("Post must be approved before scheduling");
        }
        
        // Create schedule
        Schedule schedule = createSchedule(postId, scheduledFor);
        
        // Update post status
        post.setCurrentStatus(Post.PostStatus.SCHEDULED);
        postRepository.save(post);
        
        // Send notification
        notificationService.createNotification(
            postId,
            post.getCreatedBy(),
            com.aiautoposter.entity.Notification.NotificationType.SCHEDULED,
            String.format("Your post '%s' has been scheduled for %s", 
                post.getTitle(), scheduledFor.toString())
        );
        
        return schedule;
    }
    
    public Schedule cancelSchedule(Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));
        
        schedule.setStatus(Schedule.ScheduleStatus.CANCELLED);
        Schedule updatedSchedule = scheduleRepository.save(schedule);
        
        // Update post status back to approved
        Post post = postRepository.findById(schedule.getPostId())
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setCurrentStatus(Post.PostStatus.APPROVED);
        postRepository.save(post);
        
        return updatedSchedule;
    }
    
    @Scheduled(cron = "${scheduler.cron:0 * * * * *}") // Default: every minute
    public void processScheduledPosts() {
        List<Schedule> schedulesToPublish = getSchedulesToPublish();
        for (Schedule schedule : schedulesToPublish) {
            try {
                publishPost(schedule);
            } catch (Exception e) {
                System.err.println("Error publishing scheduled post: " + e.getMessage());
                schedule.setStatus(Schedule.ScheduleStatus.FAILED);
                scheduleRepository.save(schedule);
                
                // Send failure notification
                Post post = postRepository.findById(schedule.getPostId())
                        .orElseThrow(() -> new RuntimeException("Post not found"));
                
                notificationService.createNotification(
                    schedule.getPostId(),
                    post.getCreatedBy(),
                    com.aiautoposter.entity.Notification.NotificationType.FAILED,
                    String.format("Failed to publish scheduled post '%s'", post.getTitle())
                );
            }
        }
    }
    
    private void publishPost(Schedule schedule) {
        Post post = postRepository.findById(schedule.getPostId())
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        try {
            if (post.getCreatedBy() == null) {
                throw new RuntimeException("Post creator not set; cannot determine publishing accounts");
            }

            // Load creator
            User creator = userService.findById(post.getCreatedBy())
                    .orElseThrow(() -> new RuntimeException("Creator user not found"));

            // Get target platforms from post
            String[] platforms = post.getTargetPlatforms().split(",");
            boolean anyPublished = false;
            StringBuilder publishResults = new StringBuilder();
            
            for (String platform : platforms) {
                platform = platform.trim().toLowerCase();
                
                try {
                    if ("linkedin".equals(platform)) {
                        boolean linkedInPublished = publishToLinkedIn(post, creator);
                        if (linkedInPublished) {
                            anyPublished = true;
                            publishResults.append("LinkedIn: Success. ");
                        } else {
                            publishResults.append("LinkedIn: Failed. ");
                        }
                    } else if ("wordpress".equals(platform)) {
                        boolean wordPressPublished = publishToWordPress(post, creator);
                        if (wordPressPublished) {
                            anyPublished = true;
                            publishResults.append("WordPress: Success. ");
                        } else {
                            publishResults.append("WordPress: Failed. ");
                        }
                    }
                } catch (Exception e) {
                    publishResults.append(platform).append(": Error - ").append(e.getMessage()).append(". ");
                }
            }

            if (anyPublished) {
                schedule.setStatus(Schedule.ScheduleStatus.PUBLISHED);
                schedule.setPublishedAt(LocalDateTime.now());
                scheduleRepository.save(schedule);
                
                // Update post status
                post.setCurrentStatus(Post.PostStatus.PUBLISHED);
                postRepository.save(post);
                
                // Send success notification
                notificationService.createNotification(
                    schedule.getPostId(),
                    post.getCreatedBy(),
                    com.aiautoposter.entity.Notification.NotificationType.PUBLISHED,
                    String.format("Your post '%s' has been published. Results: %s", post.getTitle(), publishResults.toString())
                );
            } else {
                // All platforms failed
                schedule.setStatus(Schedule.ScheduleStatus.FAILED);
                scheduleRepository.save(schedule);
                
                notificationService.createNotification(
                    schedule.getPostId(),
                    post.getCreatedBy(),
                    com.aiautoposter.entity.Notification.NotificationType.FAILED,
                    String.format("Failed to publish '%s' to any platform. Results: %s", post.getTitle(), publishResults.toString())
                );
            }
            
        } catch (Exception ex) {
            schedule.setStatus(Schedule.ScheduleStatus.FAILED);
            scheduleRepository.save(schedule);
            
            notificationService.createNotification(
                schedule.getPostId(),
                post.getCreatedBy(),
                com.aiautoposter.entity.Notification.NotificationType.FAILED,
                String.format("Failed to publish '%s': %s", post.getTitle(), ex.getMessage())
            );
            
            throw new RuntimeException("Failed to publish scheduled post: " + ex.getMessage(), ex);
        }
    }
    
    private boolean publishToLinkedIn(Post post, User creator) {
        try {
            java.util.Optional<LinkedInUser> liUserOpt = linkedInUserRepository.findByUser(creator);
            if (!liUserOpt.isPresent()) {
                throw new RuntimeException("LinkedIn account not connected for the post creator");
            }

            LinkedInUser liUser = liUserOpt.get();
            String accessToken = liUser.getAccessToken();
            String personUrn = liUser.getPersonUrn();
            if (accessToken == null || personUrn == null) {
                throw new RuntimeException("Missing LinkedIn credentials for user");
            }

            // Expiry pre-check: if token is expired, fail gracefully
            java.time.LocalDateTime expiresAt = liUser.getAccessTokenExpiresAt();
            if (expiresAt != null && expiresAt.isBefore(java.time.LocalDateTime.now())) {
                throw new RuntimeException("LinkedIn token expired. Please reconnect LinkedIn.");
            }

            // Publish with media attachments support
            return linkedInService.publishPost(post, accessToken, personUrn);
            
        } catch (Exception e) {
            throw new RuntimeException("LinkedIn publishing failed: " + e.getMessage(), e);
        }
    }
    
    private boolean publishToWordPress(Post post, User creator) {
        try {
            // Get post content for WordPress
            List<PostContent> contentList = postService.getPostContents(post.getId());
            PostContent wordpressContent = contentList.stream()
                    .filter(content -> "wordpress".equals(content.getPlatform()))
                    .findFirst()
                    .orElse(null);
            
            if (wordpressContent == null) {
                throw new RuntimeException("No WordPress content found for post");
            }
            
            // Publish with media attachments support
            var response = wordpressService.publishPost(post, wordpressContent);
            return response != null && response.getStatus() != null;
            
        } catch (Exception e) {
            throw new RuntimeException("WordPress publishing failed: " + e.getMessage(), e);
        }
    }
    
    public void deleteSchedule(Long id) {
        scheduleRepository.deleteById(id);
    }
}
