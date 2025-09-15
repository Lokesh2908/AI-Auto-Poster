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
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    private User creator;
    
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PostContent> postContents;
    
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Image> images;
    
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Schedule> schedules;
    
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Notification> notifications;
    
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AIWorkflowStep> aiWorkflowSteps;
    
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ApprovalRequest> approvalRequests;
    
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
    
    public List<PostContent> getPostContents() {
        return postContents;
    }
    
    public void setPostContents(List<PostContent> postContents) {
        this.postContents = postContents;
    }
    
    public List<Image> getImages() {
        return images;
    }
    
    public void setImages(List<Image> images) {
        this.images = images;
    }
    
    public List<Schedule> getSchedules() {
        return schedules;
    }
    
    public void setSchedules(List<Schedule> schedules) {
        this.schedules = schedules;
    }
    
    public List<Notification> getNotifications() {
        return notifications;
    }
    
    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
    }
    
    public List<AIWorkflowStep> getAiWorkflowSteps() {
        return aiWorkflowSteps;
    }
    
    public void setAiWorkflowSteps(List<AIWorkflowStep> aiWorkflowSteps) {
        this.aiWorkflowSteps = aiWorkflowSteps;
    }
    
    public List<ApprovalRequest> getApprovalRequests() {
        return approvalRequests;
    }
    
    public void setApprovalRequests(List<ApprovalRequest> approvalRequests) {
        this.approvalRequests = approvalRequests;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public enum PostStatus {
        DRAFT, PENDING_APPROVAL, APPROVED, REJECTED, SCHEDULED, PUBLISHED, FAILED
    }
}
