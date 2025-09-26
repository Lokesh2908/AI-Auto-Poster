package com.aiautoposter.service;

import com.aiautoposter.entity.LinkedInUser;
import com.aiautoposter.entity.PostContent;
import com.aiautoposter.repository.LinkedInUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class PersistentLinkedInService {

    @Autowired
    private LinkedInUserRepository linkedInUserRepository;

    @Autowired
    private LinkedInService linkedInService;

    // Store user with refresh token for long-term access
    public LinkedInUser storeUserWithRefreshToken(String code) {
        try {
            // Exchange code for tokens
            Map<String, Object> tokenResponse = linkedInService.exchangeCodeForToken(code);

            String accessToken = (String) tokenResponse.get("access_token");
            String refreshToken = (String) tokenResponse.get("refresh_token"); // This is key!
            Integer accessExpiresIn = (Integer) tokenResponse.get("expires_in");
            Integer refreshExpiresIn = (Integer) tokenResponse.get("refresh_token_expires_in");

            // Get user profile
            Map<String, Object> profile = linkedInService.getUserProfile(accessToken);

            String linkedinUserId = (String) profile.get("sub");
            String personUrn = "urn:li:person:" + linkedinUserId;

            // Check if user exists
            Optional<LinkedInUser> existingUser = linkedInUserRepository.findByLinkedinUserId(linkedinUserId);

            LinkedInUser linkedInUser;
            if (existingUser.isPresent()) {
                linkedInUser = existingUser.get();
            } else {
                linkedInUser = new LinkedInUser();
                linkedInUser.setLinkedinUserId(linkedinUserId);
                linkedInUser.setPersonUrn(personUrn);
                linkedInUser.setName((String) profile.get("name"));
                linkedInUser.setEmail((String) profile.get("email"));
                linkedInUser.setCreatedAt(LocalDateTime.now());
            }

            // Update tokens and expiration
            linkedInUser.setAccessToken(accessToken);
            linkedInUser.setRefreshToken(refreshToken);
            linkedInUser.setAccessTokenExpiresAt(LocalDateTime.now().plusSeconds(accessExpiresIn));
            linkedInUser.setRefreshTokenExpiresAt(LocalDateTime.now().plusSeconds(refreshExpiresIn != null ? refreshExpiresIn : 31536000)); // 1 year default
            linkedInUser.setUpdatedAt(LocalDateTime.now());

            return linkedInUserRepository.save(linkedInUser);

        } catch (Exception e) {
            throw new RuntimeException("Failed to store user with refresh token", e);
        }
    }

    // Get valid access token (refresh if needed)
    public String getValidAccessToken(String linkedinUserId) {
        Optional<LinkedInUser> userOpt = linkedInUserRepository.findByLinkedinUserId(linkedinUserId);

        if (!userOpt.isPresent()) {
            throw new RuntimeException("LinkedIn user not found");
        }

        LinkedInUser user = userOpt.get();

        // Check if refresh token has expired
        if (user.getRefreshTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expired. User needs to re-authenticate.");
        }

        // Check if access token is still valid
        if (user.getAccessTokenExpiresAt().isAfter(LocalDateTime.now().plusMinutes(5))) {
            return user.getAccessToken(); // Still valid
        }

        // Access token expired, refresh it
        try {
            Map<String, Object> tokenResponse = linkedInService.refreshAccessToken(user.getRefreshToken());

            String newAccessToken = (String) tokenResponse.get("access_token");
            Integer expiresIn = (Integer) tokenResponse.get("expires_in");

            // Update stored token
            user.setAccessToken(newAccessToken);
            user.setAccessTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
            user.setUpdatedAt(LocalDateTime.now());

            linkedInUserRepository.save(user);

            return newAccessToken;

        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh access token", e);
        }
    }

    // Post using stored credentials (for scheduling)
    public boolean schedulePost(String linkedinUserId, PostContent content) {
        try {
            Optional<LinkedInUser> userOpt = linkedInUserRepository.findByLinkedinUserId(linkedinUserId);
            if (!userOpt.isPresent()) {
                return false;
            }

            LinkedInUser user = userOpt.get();

            // Get valid access token (automatically refreshes if needed)
            String accessToken = getValidAccessToken(linkedinUserId);

            // Create post using persistent authentication
            String postId ="0";// linkedInService.createLinkedInPost(accessToken, user.getPersonUrn(), content);

            return postId != null;

        } catch (Exception e) {
            System.err.println("Failed to schedule post: " + e.getMessage());
            return false;
        }
    }
}
