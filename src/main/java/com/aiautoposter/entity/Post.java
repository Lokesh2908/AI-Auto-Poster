package com.aiautoposter.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "posts")
public class Post {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "created_by", nullable = true)
    private Long createdBy;
    
    @NotBlank
    @Column(nullable = false)
    private String title;
    
    @Column(name = "source_discussion", columnDefinition = "TEXT")
    private String sourceDiscussion;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false)
    private PostStatus currentStatus;
    
    @Column(name = "target_platforms", columnDefinition = "TEXT")
    private String targetPlatforms; // JSON string of platforms
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "post_url")
    private String postUrl;
    
    // Relationships
    @ManyToOne
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    private User creator;
    

    
    // Constructors
    public Post() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.currentStatus = PostStatus.DRAFT;
    }
    
    public Post(Long createdBy, String title, String sourceDiscussion, String targetPlatforms) {
        this();
        this.createdBy = createdBy;
        this.title = title;
        this.sourceDiscussion = sourceDiscussion;
        this.targetPlatforms = targetPlatforms;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getSourceDiscussion() {
        return sourceDiscussion;
    }
    
    public void setSourceDiscussion(String sourceDiscussion) {
        this.sourceDiscussion = sourceDiscussion;
    }
    
    public PostStatus getCurrentStatus() {
        return currentStatus;
    }
    
    public void setCurrentStatus(PostStatus currentStatus) {
        this.currentStatus = currentStatus;
    }
    
    public String getTargetPlatforms() {
        return targetPlatforms;
    }
    
    public void setTargetPlatforms(String targetPlatforms) {
        this.targetPlatforms = targetPlatforms;
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
    
    public User getCreator() {
        return creator;
    }
    
    public void setCreator(User creator) {
        this.creator = creator;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getPostUrl() {
        return postUrl;
    }

    public void setPostUrl(String postUrl) {
        this.postUrl = postUrl;
    }

    public enum PostStatus {
        DRAFT, PENDING_APPROVAL, APPROVED, REJECTED, SCHEDULED, PUBLISHED, FAILED
    }
}
