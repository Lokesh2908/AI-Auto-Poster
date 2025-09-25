package com.aiautoposter.entity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "schedule")
public class Schedule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "post_id", nullable = false)
    private Long postId;
    
    @NotNull
    @Column(name = "scheduled_for", nullable = false)
    private LocalDateTime scheduledFor;
    
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ScheduleStatus status = ScheduleStatus.PENDING;
    
    @Column(name = "social_media_app_id")
    private Long socialMediaAppId;
    
    @Column(name = "platform")
    @Enumerated(EnumType.STRING)
    private Platform platform;
    
    @Column(name = "linkedin_post_id")
    private String linkedInPostId;

    @Column(name = "post_url")
    private String postUrl;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    private Post post;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "social_media_app_id", insertable = false, updatable = false)
    private SocialMediaApp socialMediaApp;
    
    // Constructors
    public Schedule() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Schedule(Long postId, LocalDateTime scheduledFor) {
        this();
        this.postId = postId;
        this.scheduledFor = scheduledFor;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getPostId() {
        return postId;
    }
    
    public void setPostId(Long postId) {
        this.postId = postId;
    }
    
    public LocalDateTime getScheduledFor() {
        return scheduledFor;
    }
    
    public void setScheduledFor(LocalDateTime scheduledFor) {
        this.scheduledFor = scheduledFor;
    }
    
    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }
    
    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public ScheduleStatus getStatus() {
        return status;
    }
    
    public void setStatus(ScheduleStatus status) {
        this.status = status;
    }
    
    public Post getPost() {
        return post;
    }
    
    public void setPost(Post post) {
        this.post = post;
    }
    
    public Long getSocialMediaAppId() {
        return socialMediaAppId;
    }
    
    public void setSocialMediaAppId(Long socialMediaAppId) {
        this.socialMediaAppId = socialMediaAppId;
    }
    
    public Platform getPlatform() {
        return platform;
    }
    
    public void setPlatform(Platform platform) {
        this.platform = platform;
    }
    
    public String getLinkedInPostId() {
        return linkedInPostId;
    }
    
    public void setLinkedInPostId(String linkedInPostId) {
        this.linkedInPostId = linkedInPostId;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public SocialMediaApp getSocialMediaApp() {
        return socialMediaApp;
    }
    
    public void setSocialMediaApp(SocialMediaApp socialMediaApp) {
        this.socialMediaApp = socialMediaApp;
    }

    public String getPostUrl() {
        return postUrl;
    }

    public void setPostUrl(String postUrl) {
        this.postUrl = postUrl;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public enum ScheduleStatus {
        PENDING, PUBLISHED, FAILED, CANCELLED, SCHEDULED
    }
    
    public enum Platform {
        LINKEDIN("LinkedIn"),
        WORDPRESS("WordPress"),
        TWITTER("Twitter"),
        FACEBOOK("Facebook");
        
        private final String displayName;
        
        Platform(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }

}
