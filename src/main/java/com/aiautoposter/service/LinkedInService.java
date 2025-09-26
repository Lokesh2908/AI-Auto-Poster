package com.aiautoposter.service;

import com.aiautoposter.entity.Post;
import com.aiautoposter.entity.PostContent;
import com.aiautoposter.entity.User;
import com.aiautoposter.entity.PostMedia;
import com.aiautoposter.entity.LinkedInUser;
import com.aiautoposter.entity.Media;
import com.aiautoposter.repository.LinkedInUserRepository;
import com.aiautoposter.repository.PostContentRepository;
import com.aiautoposter.repository.PostMediaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import org.slf4j.LoggerFactory;

@Service
public class LinkedInService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LinkedInService.class);

    @Value("${linkedin.client.id}")
    private String clientId;

    @Value("${linkedin.client.secret}")
    private String clientSecret;

    @Value("${linkedin.client.redirect-uri}")
    private String redirectUri;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PostContentRepository postContentRepository;

    @Autowired
    private PostMediaRepository postMediaRepository;

    @Autowired
    private UserService userService;
    
    @Autowired
    private LinkedInUserRepository linkedInUserRepository;
    
    /**
     * Check if a user has connected their LinkedIn account
     * @param userId The ID of the user to check
     * @return true if the user has a connected LinkedIn account, false otherwise
     */
    public boolean isLinkedInConnected(Long userId) {
        try {
            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isEmpty()) {
                log.warn("User with ID {} not found", userId);
                return false;
            }
            
            Optional<LinkedInUser> linkedInUser = linkedInUserRepository.findByUser(userOpt.get());
            boolean isConnected = linkedInUser.isPresent() && 
                               linkedInUser.get().getAccessToken() != null && 
                               !linkedInUser.get().getAccessToken().isBlank();
            
            log.debug("LinkedIn connection status for user {}: {}", userId, isConnected ? "CONNECTED" : "NOT CONNECTED");
            return isConnected;
            
        } catch (Exception e) {
            log.error("Error checking LinkedIn connection status for user {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get the LinkedIn authorization URL for a user
     * @param userId The ID of the user
     * @return The authorization URL, or null if user not found
     */
    public String getAuthorizationUrl(Long userId) {
        try {
            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isEmpty()) {
                log.error("User with ID {} not found", userId);
                return null;
            }
            
            // Generate a state parameter with user ID for security
            String state = Base64.getEncoder().encodeToString(
                String.format("user:%d:ts:%d", userId, System.currentTimeMillis())
                    .getBytes(StandardCharsets.UTF_8)
            );
            
            // Build the authorization URL
            return UriComponentsBuilder.fromHttpUrl("https://www.linkedin.com/oauth/v2/authorization")
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .queryParam("scope", "r_liteprofile r_emailaddress w_member_social")
                .build()
                .toUriString();
                
        } catch (Exception e) {
            log.error("Error generating LinkedIn authorization URL for user {}: {}", userId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get the access token for a specific user
     * @param userId The ID of the user
     * @return The access token if found, null otherwise
     */
    /**
     * Get the access token for a specific user with enhanced logging
     * @param userId The ID of the user
     * @return The access token if found, null otherwise
     */
    public String getAccessTokenForUser(Long userId) {
        try {
            log.info("🔑 Attempting to get LinkedIn access token for user ID: {}", userId);
            
            // Get the user from the database
            Optional<User> userOptional = userService.findById(userId);
            if (userOptional.isEmpty()) {
                log.error("❌ User not found with ID: {}", userId);
                return null;
            }

            User user = userOptional.get();
            log.debug("✅ Found user: {} (ID: {})", user.getEmail(), user.getId());
            
            // Get the LinkedIn user associated with this user
            Optional<LinkedInUser> linkedInUserOpt = linkedInUserRepository.findByUser(user);
            if (linkedInUserOpt.isEmpty()) {
                log.error("❌ No LinkedIn account connected for user: {} (ID: {})", 
                         user.getEmail(), user.getId());
                return null;
            }

            LinkedInUser linkedInUser = linkedInUserOpt.get();
            log.debug("✅ Found LinkedIn user ID: {}", linkedInUser.getLinkedinUserId());
            
            // Check if token is expired
            LocalDateTime now = LocalDateTime.now();
            if (linkedInUser.getAccessTokenExpiresAt() != null) {
                log.debug("ℹ️ Token expires at: {}", linkedInUser.getAccessTokenExpiresAt());
                
                if (linkedInUser.getAccessTokenExpiresAt().isBefore(now)) {
                    log.info("🔄 Access token expired. Attempting to refresh...");
                    
                    // Token is expired, try to refresh it
                    Map<String, Object> tokenResponse = refreshAccessToken(linkedInUser.getRefreshToken());
                    
                    if (tokenResponse != null && tokenResponse.containsKey("access_token")) {
                        log.info("🔄 Successfully refreshed access token");
                        
                        // Update the LinkedIn user with new tokens
                        String newAccessToken = tokenResponse.get("access_token").toString();
                        linkedInUser.setAccessToken(newAccessToken);
                        
                        // Update refresh token if a new one was provided
                        if (tokenResponse.containsKey("refresh_token")) {
                            String newRefreshToken = tokenResponse.get("refresh_token").toString();
                            linkedInUser.setRefreshToken(newRefreshToken);
                            log.debug("✅ Updated refresh token");
                        }
                        
                        // Update expiration time (default to 1 hour if not provided)
                        int expiresIn = tokenResponse.containsKey("expires_in") ? 
                            Integer.parseInt(tokenResponse.get("expires_in").toString()) : 3600;
                        LocalDateTime newExpiry = now.plusSeconds(expiresIn);
                        linkedInUser.setAccessTokenExpiresAt(newExpiry);
                        
                        // Save the updated tokens
                        linkedInUserRepository.save(linkedInUser);
                        log.info("✅ Successfully updated tokens. New expiry: {}", newExpiry);
                        
                        return newAccessToken;
                    } else {
                        log.error("❌ Failed to refresh access token for user {}. Response: {}", 
                                userId, tokenResponse);
                        return null;
                    }
                } else {
                    log.info("✅ Using existing valid access token");
                }
            } else {
                log.warn("⚠️ No token expiration date set. Using existing token.");
            }
            
            return linkedInUser.getAccessToken();
            
        } catch (Exception e) {
            log.error("❌ Error getting LinkedIn access token for user {}: {}", 
                     userId, e.getMessage(), e);
            return null;
        }
    }

    // Generate auth URL with user state for linking
    public String getAuthorizationUrl(String username) {
        return getAuthorizationUrl(username, false);
    }

    // Generate auth URL with optional force-login (prompt=login)
    public String getAuthorizationUrl(String username, boolean forceLogin) {
        User user = userService.findByUsername(username);
        String state = Base64.getEncoder().encodeToString(
                ("user:" + user.getId() + ":nonce:" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8)
        );

        UriComponentsBuilder b = UriComponentsBuilder
                .fromHttpUrl("https://www.linkedin.com/oauth/v2/authorization")
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "openid profile email w_member_social")
                .queryParam("state", state)
                .queryParam("nonce", UUID.randomUUID().toString())
                .queryParam("cb", System.currentTimeMillis());

        if (forceLogin) {
            b.queryParam("prompt", "login");
        }

        return b.toUriString();
    }

    public Long extractUserIdFromState(String state) {
        if (state == null) {
            throw new IllegalArgumentException("State parameter is required");
        }

        try {
            String decoded = new String(Base64.getDecoder().decode(state), StandardCharsets.UTF_8);
            if (!decoded.startsWith("user:")) {
                throw new IllegalArgumentException("Invalid state format");
            }
            // Support both "user:<id>" and "user:<id>:nonce:<uuid>"
            String[] parts = decoded.split(":");
            if (parts.length >= 2) {
                return Long.parseLong(parts[1]);
            }
            throw new IllegalArgumentException("Invalid state format (missing user id)");
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid state parameter", e);
        }
    }

    /**
     * Publish a post to LinkedIn with media support and enhanced error handling
     * @param post The post to publish
     * @param accessToken The LinkedIn access token
     * @param personUrn The LinkedIn user's URN (e.g., "urn:li:person:abc123")
     * @return true if published successfully, false otherwise
     */
    public boolean publishPost(Post post, String accessToken, String personUrn) {
        log.info("🚀 Starting LinkedIn post publication for post ID: {}", post.getId());
        
        if (accessToken == null || accessToken.isBlank()) {
            log.error("❌ Cannot publish post: Access token is null or empty");
            return false;
        }
        
        if (personUrn == null || personUrn.isBlank()) {
            log.error("❌ Cannot publish post: Person URN is null or empty");
            return false;
        }
        
        try {
            // Get the LinkedIn user to check token expiration
            Optional<LinkedInUser> linkedInUserOpt = linkedInUserRepository.findByUser(
                userService.findById(post.getCreatedBy())
                    .orElseThrow(() -> new RuntimeException("User not found"))
            );
            
            if (linkedInUserOpt.isEmpty()) {
                log.error("❌ No LinkedIn account found for post creator");
                return false;
            }
            
            LinkedInUser linkedInUser = linkedInUserOpt.get();
            
            // Check if token is expired or about to expire soon (within 5 minutes)
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = linkedInUser.getAccessTokenExpiresAt();
            
            if (expiresAt != null && (expiresAt.isBefore(now) || expiresAt.isBefore(now.plusMinutes(5)))) {
                log.info("🔄 Access token expired or about to expire, attempting to refresh...");
                Map<String, Object> tokenResponse = refreshAccessToken(linkedInUser.getRefreshToken());
                
                if (tokenResponse != null && tokenResponse.containsKey("access_token")) {
                    // Update the access token and retry
                    String newAccessToken = tokenResponse.get("access_token").toString();
                    linkedInUser.setAccessToken(newAccessToken);
                    
                    // Update refresh token if a new one was provided
                    if (tokenResponse.containsKey("refresh_token")) {
                        linkedInUser.setRefreshToken(tokenResponse.get("refresh_token").toString());
                    }
                    
                    // Update expiration time (default to 1 hour if not provided)
                    int expiresIn = tokenResponse.containsKey("expires_in") ? 
                        Integer.parseInt(tokenResponse.get("expires_in").toString()) : 3600;
                    linkedInUser.setAccessTokenExpiresAt(now.plusSeconds(expiresIn));
                    
                    // Save the updated tokens
                    linkedInUserRepository.save(linkedInUser);
                    log.info("✅ Successfully refreshed access token");
                    
                    // Update the access token for this request
                    accessToken = newAccessToken;
                } else {
                    log.error("❌ Failed to refresh access token");
                    return false;
                }
            }
            
            log.info("🔍 Fetching LinkedIn content for post ID: {}", post.getId());
            List<PostContent> postContents = postContentRepository.findByPostIdAndPlatform(
                    post.getId(), "linkedin");

            if (postContents.isEmpty()) {
                log.error("❌ No LinkedIn content found for post ID: {}", post.getId());
                return false;
            }

            PostContent linkedinContent = postContents.get(0);
            log.debug("✅ Found LinkedIn content for post ID: {}", post.getId());
            
            // Get media files associated with the post
            log.debug("🖼️ Fetching media files for post ID: {}", post.getId());
            List<PostMedia> postMediaList = postMediaRepository.findByPostIdWithMediaOrderByDisplayOrder(post.getId());
            List<String> mediaUrls = new ArrayList<>();
            
            for (PostMedia postMedia : postMediaList) {
                Media media = postMedia.getMedia();
                if (media != null && ("IMAGE".equalsIgnoreCase(media.getMediaType()) || 
                                    "PNG".equalsIgnoreCase(media.getMediaType()) ||
                                    "JPEG".equalsIgnoreCase(media.getMediaType()) ||
                                    "JPG".equalsIgnoreCase(media.getMediaType()))) {
                    mediaUrls.add(media.getFileUrl());
                    log.debug("➕ Added media to post: {}", media.getFileUrl());
                }
            }
            
            // Try to create the LinkedIn post
            String postId = null;
            Exception lastError = null;
            
            // Try with the provided URN first
            log.info("📤 Attempting to publish post with URN: {}", personUrn);
            try {
                if (!mediaUrls.isEmpty()) {
                    log.info("🖼️ Creating post with {} images", mediaUrls.size());
                    postId = createPostWithImages(accessToken, personUrn, linkedinContent, mediaUrls);
                } else {
                    log.info("📝 Creating text-only post");
                    postId = createLinkedInPost(accessToken, personUrn, linkedinContent);
                }
            } catch (Exception e) {
                lastError = e;
                log.warn("⚠️ First attempt failed with URN {}: {}", personUrn, e.getMessage());
                
                // Try with alternative URN format
                String alternativeUrn = null;
                if (personUrn.contains("urn:li:person:")) {
                    // Extract the ID and try with member format
                    String id = personUrn.replace("urn:li:person:", "");
                    alternativeUrn = "urn:li:member:" + id;
                    log.info("🔄 Trying alternative URN format: {}", alternativeUrn);
                } else if (personUrn.contains("urn:li:member:")) {
                    // Extract the ID and try with person format
                    String id = personUrn.replace("urn:li:member:", "");
                    alternativeUrn = "urn:li:person:" + id;
                    log.info("🔄 Trying alternative URN format: {}", alternativeUrn);
                }
                
                if (alternativeUrn != null) {
                    try {
                        if (!mediaUrls.isEmpty()) {
                            postId = createPostWithImages(accessToken, alternativeUrn, linkedinContent, mediaUrls);
                        } else {
                            postId = createLinkedInPost(accessToken, alternativeUrn, linkedinContent);
                        }
                        log.info("✅ Successfully published with alternative URN format");
                    } catch (Exception ex) {
                        lastError = ex;
                        log.error("❌ Failed to publish with alternative URN format: {}", ex.getMessage());
                    }
                }
            }

            if (postId != null) {
                log.info("✅ Successfully published to LinkedIn! Post ID: {}", postId);
                return true;
            } else {
                log.error("❌ Failed to publish post to LinkedIn. No post ID returned.");
                if (lastError != null) {
                    log.error("Last error: {}", lastError.getMessage(), lastError);
                }
                return false;
            }

        } catch (Exception e) {
            log.error("❌ Error publishing to LinkedIn: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Create a text-only LinkedIn post
     * @param accessToken The LinkedIn access token
     * @param personUrn The LinkedIn user's URN
     * @param content The post content
     * @return The post ID if successful, null otherwise
     */
    private String createLinkedInPost(String accessToken, String personUrn, PostContent content) {
        final String postUrl = "https://api.linkedin.com/v2/ugcPosts";
        log.info("📝 Creating LinkedIn text post for URN: {}", personUrn);
        
        try {
            // Build headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            headers.set("X-Restli-Protocol-Version", "2.0.0");
            headers.set("LinkedIn-Version", "202304"); // Specify API version

            // Build post content
            String fullContent = buildPostContent(content);
            log.debug("📄 Post content prepared ({} chars)", fullContent.length());
            
            if (fullContent.length() > 3000) {
                log.warn("⚠️ Post content exceeds 3000 characters ({}), it may be truncated by LinkedIn", 
                        fullContent.length());
            }

            // Build request payload
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("author", personUrn);
            payload.put("lifecycleState", "PUBLISHED");

            // Build share content
            Map<String, Object> shareCommentary = new LinkedHashMap<>();
            shareCommentary.put("text", fullContent);

            Map<String, Object> shareContent = new LinkedHashMap<>();
            shareContent.put("shareCommentary", shareCommentary);
            shareContent.put("shareMediaCategory", "NONE");

            Map<String, Object> specificContent = new LinkedHashMap<>();
            specificContent.put("com.linkedin.ugc.ShareContent", shareContent);

            Map<String, Object> visibility = new LinkedHashMap<>();
            visibility.put("com.linkedin.ugc.MemberNetworkVisibility", "PUBLIC");

            payload.put("specificContent", specificContent);
            payload.put("visibility", visibility);

            // Log request details
            if (log.isDebugEnabled()) {
                log.debug("🔗 API URL: {}", postUrl);
                log.debug("🔑 Using access token: {}...", accessToken.substring(0, Math.min(10, accessToken.length())));
                log.debug("👤 Author URN: {}", personUrn);
                log.trace("📦 Request payload: {}", payload);
            }

            // Create and send the request
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            log.info("🚀 Sending post creation request to LinkedIn API...");
            
            long startTime = System.currentTimeMillis();
            ResponseEntity<Map> response = restTemplate.exchange(
                postUrl, 
                HttpMethod.POST, 
                entity, 
                Map.class
            );
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("📥 Received LinkedIn API response in {} ms. Status: {}", 
                    duration, response.getStatusCode());
            
            // Process the response
            if (response.getStatusCode() == HttpStatus.CREATED) {
                String postId = response.getHeaders().getFirst("X-RestLi-Id");
                if (postId != null && !postId.isEmpty()) {
                    log.info("✅ Successfully created LinkedIn post! ID: {}", postId);
                    return postId;
                } else {
                    log.warn("⚠️ Success status but no post ID in response headers. Full response: {}", response);
                    // Try to extract ID from response body as fallback
                    if (response.getBody() != null && response.getBody().containsKey("id")) {
                        postId = response.getBody().get("id").toString();
                        log.info("✅ Extracted post ID from response body: {}", postId);
                        return postId;
                    }
                }
            } else {
                log.error("❌ Failed to create LinkedIn post. Status: {}. Response: {}", 
                         response.getStatusCodeValue(), response.getBody());
            }
            
            return null;

        } catch (HttpClientErrorException e) {
            log.error("❌ LinkedIn API client error: {}", e.getResponseBodyAsString());
            log.error("❌ Status code: {}", e.getStatusCode());
            log.error("❌ Response headers: {}", e.getResponseHeaders());
            throw new RuntimeException("Failed to create LinkedIn post: " + e.getMessage(), e);
            
        } catch (HttpServerErrorException e) {
            log.error("❌ LinkedIn server error: {}", e.getResponseBodyAsString());
            log.error("❌ Status code: {}", e.getStatusCode());
            throw new RuntimeException("LinkedIn server error: " + e.getMessage(), e);
            
        } catch (RestClientException e) {
            log.error("❌ Error communicating with LinkedIn API: {}", e.getMessage());
            throw new RuntimeException("Network error while connecting to LinkedIn: " + e.getMessage(), e);
            
        } catch (Exception e) {
            log.error("❌ Unexpected error creating LinkedIn post: {}", e.getMessage(), e);
            throw new RuntimeException("Unexpected error creating LinkedIn post: " + e.getMessage(), e);
        }
    }


    private String buildPostContent(PostContent content) {
        StringBuilder fullContent = new StringBuilder();

        if (content.getTitle() != null && !content.getTitle().trim().isEmpty()) {
            fullContent.append(content.getTitle().trim()).append("\n\n");
        }

        if (content.getContent() != null && !content.getContent().trim().isEmpty()) {
            fullContent.append(content.getContent().trim());
        }

        return fullContent.toString();
    }

    // Updated authorization URL with proper scope
    public String getAuthorizationUrl() {
        return UriComponentsBuilder
                .fromHttpUrl("https://www.linkedin.com/oauth/v2/authorization")
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", UUID.randomUUID().toString())
                .queryParam("scope", "openid profile email w_member_social")
                .toUriString();
    }

    // Updated token exchange method
    public Map<String, Object> exchangeCodeForToken(String code) {
        try {
            String tokenUrl = "https://www.linkedin.com/oauth/v2/accessToken";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("code", code);
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("redirect_uri", redirectUri);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> tokenResponse = response.getBody();
                System.out.println("LinkedIn token exchange response: " + tokenResponse);
                
                // Log what tokens are actually provided
                System.out.println("Access token present: " + tokenResponse.containsKey("access_token"));
                System.out.println("Refresh token present: " + tokenResponse.containsKey("refresh_token"));
                System.out.println("Token type: " + tokenResponse.get("token_type"));
                System.out.println("Expires in: " + tokenResponse.get("expires_in"));
                
                return tokenResponse;
            }

            return null;

        } catch (Exception e) {
            System.err.println("Error exchanging code for token: " + e.getMessage());
            return null;
        }
    }

    // New method to get user profile and person URN
    public Map<String, Object> getUserProfile(String accessToken) {
        try {
            String profileUrl = "https://api.linkedin.com/v2/userinfo";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    profileUrl, HttpMethod.GET, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> profile = response.getBody();
                // Also get the person info to get the correct URN
                try {
                    String personUrl = "https://api.linkedin.com/v2/people/~";
                    ResponseEntity<Map> personResponse = restTemplate.exchange(
                            personUrl, HttpMethod.GET, request, Map.class);
                    
                    if (personResponse.getStatusCode() == HttpStatus.OK && personResponse.getBody() != null) {
                        Map<String, Object> personInfo = personResponse.getBody();
                        String personId = (String) personInfo.get("id");
                        if (personId != null) {
                            profile.put("personUrn", "urn:li:person:" + personId);
                            profile.put("personId", personId);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error getting person info, falling back to sub: " + e.getMessage());
                    // Fallback to using sub
                    if (profile != null && profile.containsKey("sub")) {
                        profile.put("personUrn", "urn:li:person:" + profile.get("sub"));
                    }
                }
                return profile;
            }

            return null;

        } catch (Exception e) {
            System.err.println("Error getting user profile: " + e.getMessage());
            return null;
        }
    }

    /**
     * Validates if the provided LinkedIn access token is valid
     * @param accessToken The access token to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean validateAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("⚠️ Access token validation failed: Token is null or empty");
            return false;
        }
        
        try {
            log.debug("🔍 Validating LinkedIn access token...");
            
            // First, check if the token is expired using the stored expiration time
            try {
                // Try to get the LinkedIn user for this token
                Optional<LinkedInUser> userOpt = linkedInUserRepository.findByAccessToken(accessToken);
                if (userOpt.isPresent()) {
                    LinkedInUser user = userOpt.get();
                    if (user.getAccessTokenExpiresAt() != null && 
                        user.getAccessTokenExpiresAt().isBefore(LocalDateTime.now())) {
                        log.warn("⚠️ Access token is expired (expired at: {})", user.getAccessTokenExpiresAt());
                        return false;
                    }
                }
            } catch (Exception e) {
                log.debug("Could not check token expiration from database: {}", e.getMessage());
                // Continue with API validation if we can't check the database
            }
            
            // Then validate with LinkedIn's API
            Map<String, Object> profile = getUserProfile(accessToken);
            boolean isValid = profile != null && profile.containsKey("sub");
            
            if (isValid) {
                log.debug("✅ LinkedIn access token is valid");
                return true;
            } else {
                log.warn("⚠️ LinkedIn access token validation failed: Invalid or expired token");
                return false;
            }
            
        } catch (HttpClientErrorException e) {
            String errorMessage = e.getResponseBodyAsString();
            log.error("❌ LinkedIn API client error during token validation: {}", e.getStatusCode());
            log.error("❌ Error response: {}", errorMessage);
            
            // Check for specific error conditions
            if (errorMessage != null) {
                if (errorMessage.contains("expired")) {
                    log.error("❌ Access token has expired");
                } else if (errorMessage.contains("invalid_token")) {
                    log.error("❌ Access token is invalid or malformed");
                } else if (errorMessage.contains("insufficient_scope")) {
                    log.error("❌ Insufficient scope for this operation");
                }
            }
            
            return false;
            
        } catch (HttpServerErrorException e) {
            log.error("❌ LinkedIn server error during token validation: {}", e.getStatusCode());
            log.error("❌ Error response: {}", e.getResponseBodyAsString());
            return false;
            
        } catch (Exception e) {
            log.error("❌ Error validating LinkedIn access token: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * @deprecated Use validateAccessToken instead
     */
    @Deprecated
    public boolean validateToken(String accessToken) {
        return validateAccessToken(accessToken);
    }

    // New method for testing posts
    public String createTestPost(String accessToken, String personUrn, String content) {
        return createLinkedInPost(accessToken, personUrn,
                new PostContent() {{
                    setContent(content);
                    setTitle("");
                }});
    }

    /**
     * Refresh an expired access token using a refresh token
     * @param refreshToken The refresh token to use
     * @return Map containing the new tokens, or null if refresh failed
     */
    public Map<String, Object> refreshAccessToken(String refreshToken) {
        final String tokenUrl = "https://www.linkedin.com/oauth/v2/accessToken";
        log.info("🔄 Attempting to refresh LinkedIn access token...");
        
        if (refreshToken == null || refreshToken.isBlank()) {
            log.error("❌ Cannot refresh token: Refresh token is null or empty");
            return null;
        }
        
        try {
            // Log the first few characters of the refresh token for debugging (but not the whole thing)
            String tokenPreview = refreshToken.length() > 8 
                ? refreshToken.substring(0, 4) + "..." + refreshToken.substring(refreshToken.length() - 4)
                : "[invalid token]";
            log.debug("🔑 Using refresh token: {}", tokenPreview);
            
            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            // Build form parameters
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "refresh_token");
            params.add("refresh_token", refreshToken);
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            
            // Log request details (excluding sensitive data)
            if (log.isDebugEnabled()) {
                log.debug("🔗 Token endpoint: {}", tokenUrl);
                log.debug("🔑 Client ID: {}", clientId);
                log.debug("🔄 Refresh token: {}", tokenPreview);
                log.debug("📦 Sending token refresh request...");
            }

            // Create and send the request
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            
            long startTime = System.currentTimeMillis();
            ResponseEntity<Map> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                request,
                Map.class
            );
            long duration = System.currentTimeMillis() - startTime;
            
            // Process the response
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> tokenResponse = response.getBody();
                
                // Log token refresh success (without logging actual tokens)
                log.info("✅ Successfully refreshed LinkedIn access token. Took {} ms", duration);
                
                // Log token metadata (but not the actual tokens)
                if (log.isDebugEnabled()) {
                    log.debug("📝 Token response keys: {}", tokenResponse.keySet());
                    tokenResponse.forEach((key, value) -> {
                        if (key.toLowerCase().contains("token")) {
                            log.debug("   {}: {}... [truncated]", key, 
                                value.toString().substring(0, Math.min(10, value.toString().length())));
                        } else {
                            log.debug("   {}: {}", key, value);
                        }
                    });
                }
                
                return tokenResponse;
                
            } else {
                log.error("❌ Failed to refresh token. Status: {}. Response: {}", 
                         response.getStatusCodeValue(), response.getBody());
                return null;
            }

        } catch (HttpClientErrorException e) {
            log.error("❌ LinkedIn API client error during token refresh: {}", e.getStatusCode());
            log.error("❌ Error response: {}", e.getResponseBodyAsString());
            log.error("❌ Response headers: {}", e.getResponseHeaders());
            return null;
            
        } catch (HttpServerErrorException e) {
            log.error("❌ LinkedIn server error during token refresh: {}", e.getStatusCode());
            log.error("❌ Error response: {}", e.getResponseBodyAsString());
            return null;
            
        } catch (RestClientException e) {
            log.error("❌ Network error while refreshing LinkedIn token: {}", e.getMessage());
            return null;
            
        } catch (Exception e) {
            log.error("❌ Unexpected error refreshing LinkedIn token: {}", e.getMessage(), e);
            return null;
        }
    }

    //Image post


    private String createPostWithImages(String accessToken, String personUrn, PostContent content, List<String> imageUrls) {
        try {
            // Step 1: Upload all images and get their asset URNs
            List<String> assetUrns = new ArrayList<>();
            for (String imageUrl : imageUrls) {
                String assetUrn = uploadImageToLinkedIn(accessToken, personUrn, imageUrl);
                if (assetUrn != null) {
                    assetUrns.add(assetUrn);
                }
            }

            if (assetUrns.isEmpty()) {
                throw new RuntimeException("Failed to upload any images to LinkedIn");
            }

            // Step 2: Create post with uploaded images
            return createUGCPostWithMedia(accessToken, personUrn, content, assetUrns);

        } catch (Exception e) {
            System.err.println("Error creating LinkedIn post with images: " + e.getMessage());
            throw e;
        }
    }

    private String uploadImageToLinkedIn(String accessToken, String personUrn, String imageUrl) {
        try {
            // Step 1: Register upload
            String registerUrl = "https://api.linkedin.com/v2/assets?action=registerUpload";

            HttpHeaders registerHeaders = new HttpHeaders();
            registerHeaders.setContentType(MediaType.APPLICATION_JSON);
            registerHeaders.setBearerAuth(accessToken);
            registerHeaders.set("X-Restli-Protocol-Version", "2.0.0");

            Map<String, Object> registerPayload = new HashMap<>();
            Map<String, Object> registerUploadRequest = new HashMap<>();

            List<String> recipes = new ArrayList<>();
            recipes.add("urn:li:digitalmediaRecipe:feedshare-image");
            registerUploadRequest.put("recipes", recipes);
            registerUploadRequest.put("owner", personUrn);

            List<Map<String, Object>> serviceRelationships = new ArrayList<>();
            Map<String, Object> relationship = new HashMap<>();
            relationship.put("relationshipType", "OWNER");
            relationship.put("identifier", "urn:li:userGeneratedContent");
            serviceRelationships.add(relationship);
            registerUploadRequest.put("serviceRelationships", serviceRelationships);

            registerPayload.put("registerUploadRequest", registerUploadRequest);

            HttpEntity<Map<String, Object>> registerEntity = new HttpEntity<>(registerPayload, registerHeaders);
            ResponseEntity<Map> registerResponse = restTemplate.postForEntity(registerUrl, registerEntity, Map.class);

            if (registerResponse.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Failed to register image upload");
            }

            Map<String, Object> responseBody = registerResponse.getBody();
            Map<String, Object> value = (Map<String, Object>) responseBody.get("value");
            String assetUrn = (String) value.get("asset");

            Map<String, Object> uploadMechanism = (Map<String, Object>) value.get("uploadMechanism");
            Map<String, Object> mediaUploadRequest = (Map<String, Object>)
                    uploadMechanism.get("com.linkedin.digitalmedia.uploading.MediaUploadHttpRequest");
            String uploadUrl = (String) mediaUploadRequest.get("uploadUrl");

            // Step 2: Upload image binary data
            byte[] imageData = downloadImageData(imageUrl);

            HttpHeaders uploadHeaders = new HttpHeaders();
            uploadHeaders.setBearerAuth(accessToken);
            uploadHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            HttpEntity<byte[]> uploadEntity = new HttpEntity<>(imageData, uploadHeaders);
            ResponseEntity<String> uploadResponse = restTemplate.exchange(
                    uploadUrl, HttpMethod.PUT, uploadEntity, String.class);

            if (uploadResponse.getStatusCode() == HttpStatus.CREATED) {
                return assetUrn;
            } else {
                throw new RuntimeException("Failed to upload image data");
            }

        } catch (Exception e) {
            System.err.println("Error uploading image to LinkedIn: " + e.getMessage());
            return null;
        }
    }

    private String createUGCPostWithMedia(String accessToken, String personUrn, PostContent content, List<String> assetUrns) {
        String postUrl = "https://api.linkedin.com/v2/ugcPosts";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        headers.set("X-Restli-Protocol-Version", "2.0.0");

        String fullContent = buildPostContent(content);

        // Build media array
        List<Map<String, Object>> mediaList = new ArrayList<>();
        for (String assetUrn : assetUrns) {
            Map<String, Object> mediaItem = new HashMap<>();
            mediaItem.put("status", "READY");
            mediaItem.put("media", assetUrn);
            mediaList.add(mediaItem);
        }

        // Build payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("author", personUrn);
        payload.put("lifecycleState", "PUBLISHED");

        Map<String, Object> shareCommentary = new HashMap<>();
        shareCommentary.put("text", fullContent);

        Map<String, Object> shareContent = new HashMap<>();
        shareContent.put("shareCommentary", shareCommentary);
        shareContent.put("shareMediaCategory", "IMAGE");
        shareContent.put("media", mediaList);

        Map<String, Object> specificContent = new HashMap<>();
        specificContent.put("com.linkedin.ugc.ShareContent", shareContent);

        Map<String, Object> visibility = new HashMap<>();
        visibility.put("com.linkedin.ugc.MemberNetworkVisibility", "PUBLIC");

        payload.put("specificContent", specificContent);
        payload.put("visibility", visibility);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(postUrl, entity, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                return response.getHeaders().getFirst("X-RestLi-Id");
            }
            return null;

        } catch (Exception e) {
            System.err.println("Error creating LinkedIn UGC post: " + e.getMessage());
            throw e;
        }
    }

    private byte[] downloadImageData(String imageUrl) throws IOException {
        try {
            URL url = new URL(imageUrl);
            try (InputStream inputStream = url.openStream()) {
                return inputStream.readAllBytes();
            }
        } catch (Exception e) {
            throw new IOException("Failed to download image from: " + imageUrl, e);
        }
    }
}
