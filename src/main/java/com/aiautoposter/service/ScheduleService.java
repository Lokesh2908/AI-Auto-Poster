package com.aiautoposter.service;

import com.aiautoposter.entity.Schedule;
import com.aiautoposter.entity.Post;
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
    
    @Scheduled(fixedRate = 60000) // Run every minute
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
        
        // Publish to LinkedIn
        boolean published = linkedInService.publishPost(post);
        
        if (published) {
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
                String.format("Your post '%s' has been successfully published", post.getTitle())
            );
        } else {
            throw new RuntimeException("Failed to publish post to LinkedIn");
        }
    }
    
    public void deleteSchedule(Long id) {
        scheduleRepository.deleteById(id);
    }
}
