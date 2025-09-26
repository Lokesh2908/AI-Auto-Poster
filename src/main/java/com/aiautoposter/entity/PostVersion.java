package com.aiautoposter.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "post_versions")
public class PostVersion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    
    @Column(name = "version_type", nullable = false, length = 50)
    private String versionType; // 'refinement' or 'full_regeneration'
    
    @Column(name = "change_description", columnDefinition = "TEXT")
    private String changeDescription;
    
    @Column(name = "refinement_instructions", columnDefinition = "TEXT")
    private String refinementInstructions;
    
    @Column(name = "previous_content", columnDefinition = "TEXT")
    private String previousContent;
    
    @Column(name = "enhancement_options", columnDefinition = "JSON")
    private String enhancementOptions; // JSON string
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "title")
    private String title;
    
    @Column(name = "source_discussion", columnDefinition = "TEXT")
    private String sourceDiscussion;
    
    @Column(name = "target_platforms")
    private String targetPlatforms;
    
    @Column(name = "saved_content", columnDefinition = "LONGTEXT")
    private String savedContent; // JSON array of PostContent objects
    
    @Column(name = "content_summary", columnDefinition = "TEXT")
    private String contentSummary; // Brief summary for display
    
    // Constructors
    public PostVersion() {
        this.createdAt = LocalDateTime.now();
    }
    
    public PostVersion(Post post, String versionType, String changeDescription) {
        this();
        this.post = post;
        this.versionType = versionType;
        this.changeDescription = changeDescription;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Post getPost() {
        return post;
    }
    
    public void setPost(Post post) {
        this.post = post;
    }
    
    public String getVersionType() {
        return versionType;
    }
    
    public void setVersionType(String versionType) {
        this.versionType = versionType;
    }
    
    public String getChangeDescription() {
        return changeDescription;
    }
    
    public void setChangeDescription(String changeDescription) {
        this.changeDescription = changeDescription;
    }
    
    public String getRefinementInstructions() {
        return refinementInstructions;
    }
    
    public void setRefinementInstructions(String refinementInstructions) {
        this.refinementInstructions = refinementInstructions;
    }
    
    public String getPreviousContent() {
        return previousContent;
    }
    
    public void setPreviousContent(String previousContent) {
        this.previousContent = previousContent;
    }
    
    public String getEnhancementOptions() {
        return enhancementOptions;
    }
    
    public void setEnhancementOptions(String enhancementOptions) {
        this.enhancementOptions = enhancementOptions;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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
    
    public String getTargetPlatforms() {
        return targetPlatforms;
    }
    
    public void setTargetPlatforms(String targetPlatforms) {
        this.targetPlatforms = targetPlatforms;
    }
    
    public String getSavedContent() {
        return savedContent;
    }
    
    public void setSavedContent(String savedContent) {
        this.savedContent = savedContent;
    }
    
    public String getContentSummary() {
        return contentSummary;
    }
    
    public void setContentSummary(String contentSummary) {
        this.contentSummary = contentSummary;
    }
}
