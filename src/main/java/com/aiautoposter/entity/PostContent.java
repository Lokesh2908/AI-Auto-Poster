package com.aiautoposter.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "post_content")
public class PostContent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "post_id", nullable = false)
    private Long postId;
    
    @NotBlank
    @Column(nullable = false)
    private String platform;
    
    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;
    
    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "ai_confidence_score")
    private Double aiConfidenceScore;
    
    // Temporarily commented out to fix 500 error - will add back after database is updated
    // @Column(name = "hashtags", columnDefinition = "TEXT", nullable = true)
    // private String hashtags;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relationships - temporarily commented out to fix serialization issue
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "post_id", insertable = false, updatable = false)
    // @JsonIgnore
    // private Post post;
    
    // Constructors
    public PostContent() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public PostContent(Long postId, String platform, String title, String content, Double aiConfidenceScore) {
        this();
        this.postId = postId;
        this.platform = platform;
        this.title = title;
        this.content = content;
        this.aiConfidenceScore = aiConfidenceScore;
    }
    
    // Temporarily commented out - will add back after database is updated
    /*
    public PostContent(Long postId, String platform, String title, String content, Double aiConfidenceScore, String hashtags) {
        this();
        this.postId = postId;
        this.platform = platform;
        this.title = title;
        this.content = content;
        this.aiConfidenceScore = aiConfidenceScore;
        this.hashtags = hashtags;
    }
    */
    
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
    
    public String getPlatform() {
        return platform;
    }
    
    public void setPlatform(String platform) {
        this.platform = platform;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Double getAiConfidenceScore() {
        return aiConfidenceScore;
    }
    
    public void setAiConfidenceScore(Double aiConfidenceScore) {
        this.aiConfidenceScore = aiConfidenceScore;
    }
    
    // Temporarily commented out - will add back after database is updated
    public String getHashtags() {
        return ""; // hashtags != null ? hashtags : "";
    }
    
    public void setHashtags(String hashtags) {
        // this.hashtags = hashtags;
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
    
    // Temporarily commented out to fix serialization issue
    // public Post getPost() {
    //     return post;
    // }
    // 
    // public void setPost(Post post) {
    //     this.post = post;
    // }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
