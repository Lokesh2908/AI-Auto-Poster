package com.aiautoposter.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "social_media_apps")
public class SocialMediaApp {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "description")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    private Platform platform;
    
    @Column(name = "client_id")
    private String clientId;
    
    @Column(name = "client_secret")
    private String clientSecret;
    
    // WordPress-specific fields
    @Column(name = "url")
    private String url;
    
    @Column(name = "username")
    private String username;
    
    @Column(name = "app_password")
    private String appPassword;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;
    
    @Column(name = "created_by")
    private Long createdBy;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    
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
    
    // Constructors
    public SocialMediaApp() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public SocialMediaApp(String name, Platform platform, String clientId, String clientSecret) {
        this();
        this.name = name;
        this.platform = platform;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Platform getPlatform() {
        return platform;
    }
    
    public void setPlatform(Platform platform) {
        this.platform = platform;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getClientSecret() {
        return clientSecret;
    }
    
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getAppPassword() {
        return appPassword;
    }
    
    public void setAppPassword(String appPassword) {
        this.appPassword = appPassword;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Boolean getIsDefault() {
        return isDefault;
    }
    
    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
    
    public Long getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
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
    
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "SocialMediaApp{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", platform=" + platform +
                ", isActive=" + isActive +
                ", isDefault=" + isDefault +
                '}';
    }
}
