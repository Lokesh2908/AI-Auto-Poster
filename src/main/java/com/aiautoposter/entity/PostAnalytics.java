package com.aiautoposter.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "post_analytics")
public class PostAnalytics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    
    @Column(name = "platform", nullable = false)
    private String platform; // e.g., "LINKEDIN"
    
    @Column(name = "platform_post_id")
    private String platformPostId; // The ID returned by LinkedIn
    
    @Column(name = "impressions")
    private Integer impressions;
    
    @Column(name = "likes")
    private Integer likes;
    
    @Column(name = "comments")
    private Integer comments;
    
    @Column(name = "shares")
    private Integer shares;
    
    @Column(name = "clicks")
    private Integer clicks;
    
    @Column(name = "engagement_rate")
    private Double engagementRate;
    
    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;
    
    @PrePersist
    protected void onCreate() {
        this.collectedAt = LocalDateTime.now();
        calculateEngagementRate();
    }
    
    @PreUpdate
    protected void onUpdate() {
        calculateEngagementRate();
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

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getPlatformPostId() {
        return platformPostId;
    }

    public void setPlatformPostId(String platformPostId) {
        this.platformPostId = platformPostId;
    }

    public Integer getImpressions() {
        return impressions;
    }

    public void setImpressions(Integer impressions) {
        this.impressions = impressions;
    }

    public Integer getLikes() {
        return likes;
    }

    public void setLikes(Integer likes) {
        this.likes = likes;
    }

    public Integer getComments() {
        return comments;
    }

    public void setComments(Integer comments) {
        this.comments = comments;
    }

    public Integer getShares() {
        return shares;
    }

    public void setShares(Integer shares) {
        this.shares = shares;
    }

    public Integer getClicks() {
        return clicks;
    }

    public void setClicks(Integer clicks) {
        this.clicks = clicks;
    }

    public Double getEngagementRate() {
        return engagementRate;
    }

    public void setEngagementRate(Double engagementRate) {
        this.engagementRate = engagementRate;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }

    @Override
    public String toString() {
        return "PostAnalytics{" +
                "id=" + id +
                ", post=" + (post != null ? post.getId() : null) +
                ", platform='" + platform + '\'' +
                ", platformPostId='" + platformPostId + '\'' +
                ", impressions=" + impressions +
                ", likes=" + likes +
                ", comments=" + comments +
                ", shares=" + shares +
                ", clicks=" + clicks +
                ", engagementRate=" + engagementRate +
                ", collectedAt=" + collectedAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostAnalytics that = (PostAnalytics) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(platform, that.platform) &&
                Objects.equals(platformPostId, that.platformPostId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, platform, platformPostId);
    }

    private void calculateEngagementRate() {
        if (impressions != null && impressions > 0) {
            double engagements = (likes != null ? likes : 0) + 
                               (comments != null ? comments : 0) + 
                               (shares != null ? shares : 0);
            this.engagementRate = (engagements / impressions) * 100;
        } else {
            this.engagementRate = 0.0;
        }
    }
}
