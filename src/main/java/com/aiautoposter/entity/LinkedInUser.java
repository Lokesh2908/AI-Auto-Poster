package com.aiautoposter.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "linkedin_users")
public class LinkedInUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String linkedinUserId;

    @Column(unique = true)
    private String personUrn;

    private String name;
    private String email;

    @Column(length = 1000)
    private String accessToken;

    @Column(length = 1000)
    private String refreshToken; // Store refresh token for long-term access

    private LocalDateTime accessTokenExpiresAt;
    private LocalDateTime refreshTokenExpiresAt; // Refresh token expires in 1 year

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToOne
    private User user;

    @ManyToOne
    @JoinColumn(name = "social_media_app_id")
    private SocialMediaApp socialMediaApp;

    // Getters and setters...

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLinkedinUserId() {
        return linkedinUserId;
    }

    public void setLinkedinUserId(String linkedinUserId) {
        this.linkedinUserId = linkedinUserId;
    }

    public String getPersonUrn() {
        return personUrn;
    }

    public void setPersonUrn(String personUrn) {
        this.personUrn = personUrn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public LocalDateTime getAccessTokenExpiresAt() {
        return accessTokenExpiresAt;
    }

    public void setAccessTokenExpiresAt(LocalDateTime accessTokenExpiresAt) {
        this.accessTokenExpiresAt = accessTokenExpiresAt;
    }

    public LocalDateTime getRefreshTokenExpiresAt() {
        return refreshTokenExpiresAt;
    }

    public void setRefreshTokenExpiresAt(LocalDateTime refreshTokenExpiresAt) {
        this.refreshTokenExpiresAt = refreshTokenExpiresAt;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
    // Utility methods
    public boolean isAccessTokenExpired() {
        return accessTokenExpiresAt != null && accessTokenExpiresAt.isBefore(LocalDateTime.now());
    }
    
    public boolean hasRefreshToken() {
        return refreshToken != null && !refreshToken.trim().isEmpty();
    }
    
    public boolean isRefreshTokenExpired() {
        return refreshTokenExpiresAt != null && refreshTokenExpiresAt.isBefore(LocalDateTime.now());
    }
    
    public boolean needsReauthorization() {
        return isAccessTokenExpired() && (!hasRefreshToken() || isRefreshTokenExpired());
    }

    public SocialMediaApp getSocialMediaApp() {
        return socialMediaApp;
    }

    public void setSocialMediaApp(SocialMediaApp socialMediaApp) {
        this.socialMediaApp = socialMediaApp;
    }
}
