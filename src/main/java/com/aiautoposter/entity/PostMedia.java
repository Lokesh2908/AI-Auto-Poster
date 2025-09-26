package com.aiautoposter.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "post_media")
public class PostMedia {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "post_id", nullable = false)
    private Long postId;
    
    @Column(name = "media_id", nullable = false)
    private Long mediaId;
    
    @Column(name = "display_order")
    private Integer displayOrder;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    private Post post;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", insertable = false, updatable = false)
    private Media media;

    // Constructors
    public PostMedia() {
        this.createdAt = LocalDateTime.now();
    }
    
    public PostMedia(Long postId, Long mediaId, Integer displayOrder) {
        this();
        this.postId = postId;
        this.mediaId = mediaId;
        this.displayOrder = displayOrder;
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
    
    public Long getMediaId() {
        return mediaId;
    }
    
    public void setMediaId(Long mediaId) {
        this.mediaId = mediaId;
    }
    
    public Integer getDisplayOrder() {
        return displayOrder;
    }
    
    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Post getPost() {
        return post;
    }
    
    public void setPost(Post post) {
        this.post = post;
    }
    
    public Media getMedia() {
        return media;
    }
    
    public void setMedia(Media media) {
        this.media = media;
    }

}
