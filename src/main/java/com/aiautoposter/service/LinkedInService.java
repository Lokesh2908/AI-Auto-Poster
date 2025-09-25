package com.aiautoposter.service;

import com.aiautoposter.entity.Post;
import com.aiautoposter.entity.PostContent;
import com.aiautoposter.entity.User;
import com.aiautoposter.entity.PostMedia;
import com.aiautoposter.entity.Media;
import com.aiautoposter.entity.SocialMediaApp;
import com.aiautoposter.repository.PostContentRepository;
import com.aiautoposter.repository.PostMediaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class LinkedInService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PostContentRepository postContentRepository;

    @Autowired
    private PostMediaRepository postMediaRepository;

    @Autowired
    private UserService userService;

    // Generate auth URL with user state for linking using specific app configuration
    public String getAuthorizationUrl(String username, SocialMediaApp app) {
        return getAuthorizationUrl(username, app, false);
    }

    // Generate auth URL with optional force-login (prompt=login) using specific app configuration
    public String getAuthorizationUrl(String username, SocialMediaApp app, boolean forceLogin) {
        User user = userService.findByUsername(username);
        String state = Base64.getEncoder().encodeToString(
                ("user:" + user.getId() + ":app:" + app.getId() + ":nonce:" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8)
        );

        String redirectUri = "http://localhost:8080/api/linkedin/callback";

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl("https://www.linkedin.com/oauth/v2/authorization")
                .queryParam("response_type", "code")
                .queryParam("client_id", app.getClientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "openid profile email w_member_social") // Fixed: single scope parameter
                .queryParam("state", state);
        // Removed nonce parameter - LinkedIn doesn't support it properly
        // Removed cb parameter - not needed

        if (forceLogin) {
            builder.queryParam("prompt", "login");
        }

        return builder.toUriString();
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
            // Support format "user:<id>:app:<appId>:nonce:<uuid>"
            String[] parts = decoded.split(":");
            if (parts.length >= 2) {
                return Long.parseLong(parts[1]);
            }
            throw new IllegalArgumentException("Invalid state format (missing user id)");
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid state parameter", e);
        }
    }

    public Long extractAppIdFromState(String state) {
        if (state == null) {
            throw new IllegalArgumentException("State parameter is required");
        }

        try {
            String decoded = new String(Base64.getDecoder().decode(state), StandardCharsets.UTF_8);
            // Format: "user:<id>:app:<appId>:nonce:<uuid>"
            String[] parts = decoded.split(":");
            for (int i = 0; i < parts.length - 1; i++) {
                if ("app".equals(parts[i]) && i + 1 < parts.length) {
                    return Long.parseLong(parts[i + 1]);
                }
            }
            throw new IllegalArgumentException("Invalid state format (missing app id)");
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid state parameter", e);
        }
    }

    // Updated to actually publish to LinkedIn with media support
    public String publishPost(Post post, String accessToken, String personUrn) {
        try {
            // Get LinkedIn content for the post
            PostContent linkedinContent = postContentRepository.findByPostIdAndPlatform(
                    post.getId(), "linkedin");

            if (linkedinContent==null) {
                throw new RuntimeException("No LinkedIn content found for post");
            }

            
            // Get media files associated with the post
            List<PostMedia> postMediaList = postMediaRepository.findByPostIdWithMediaOrderByDisplayOrder(post.getId());
            List<String> mediaUrls = new ArrayList<>();
            
            for (PostMedia postMedia : postMediaList) {
                Media media = postMedia.getMedia();
                if (media != null && "IMAGE".equalsIgnoreCase(media.getMediaType()) || "PNG".equalsIgnoreCase(media.getMediaType())) {
                    mediaUrls.add(media.getFileUrl());
                }
            }
            
            // Try to create the LinkedIn post
            String postId = null;
            try {
                if (!mediaUrls.isEmpty()) {
                    // Create post with images
                    postId = createPostWithImages(accessToken, personUrn, linkedinContent, mediaUrls);
                } else {
                    // Create text-only post
                    postId = createLinkedInPost(accessToken, personUrn, linkedinContent);
                }
            } catch (Exception e) {
                System.err.println("First attempt failed, trying alternative URN format: " + e.getMessage());
                
                // Try with different URN formats
                String alternativeUrn = null;
                if (personUrn.contains("urn:li:person:")) {
                    // Extract the ID and try with member format
                    String id = personUrn.replace("urn:li:person:", "");
                    alternativeUrn = "urn:li:member:" + id;
                } else if (personUrn.contains("urn:li:member:")) {
                    // Extract the ID and try with person format
                    String id = personUrn.replace("urn:li:member:", "");
                    alternativeUrn = "urn:li:person:" + id;
                }
                
                if (alternativeUrn != null) {
                    System.out.println("Trying alternative URN format: " + alternativeUrn);
                    if (!mediaUrls.isEmpty()) {
                        postId = createPostWithImages(accessToken, alternativeUrn, linkedinContent, mediaUrls);
                    } else {
                        postId = createLinkedInPost(accessToken, alternativeUrn, linkedinContent);
                    }
                }
            }

            if (postId != null) {
                System.out.println("Successfully published to LinkedIn with ID: " + postId);
                return postId;
            }

            return null;

        } catch (Exception e) {
            System.err.println("Error publishing to LinkedIn: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public String createLinkedInPost(String accessToken, String personUrn, PostContent content) {
        String postUrl = "https://api.linkedin.com/v2/ugcPosts";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        headers.set("X-Restli-Protocol-Version", "2.0.0");

        // Build post content
        String fullContent = buildPostContent(content);
        // Log the URN being used for debugging
        System.out.println("Creating LinkedIn post with author URN: " + personUrn);

        // Create payload using Java 8 compatible HashMap approach
        Map<String, Object> payload = new HashMap<>();
        payload.put("author", personUrn);
        payload.put("lifecycleState", "PUBLISHED");

        // Create nested maps step by step
        Map<String, Object> shareCommentary = new HashMap<>();
        shareCommentary.put("text", fullContent);

        Map<String, Object> shareContent = new HashMap<>();
        shareContent.put("shareCommentary", shareCommentary);
        shareContent.put("shareMediaCategory", "NONE");

        Map<String, Object> specificContent = new HashMap<>();
        specificContent.put("com.linkedin.ugc.ShareContent", shareContent);

        Map<String, Object> visibility = new HashMap<>();
        visibility.put("com.linkedin.ugc.MemberNetworkVisibility", "PUBLIC");

        payload.put("specificContent", specificContent);
        payload.put("visibility", visibility);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            // Log the full request for debugging
            System.out.println("LinkedIn API Request URL: " + postUrl);
            System.out.println("LinkedIn API Request Headers: " + headers);
            System.out.println("LinkedIn API Request Payload: " + payload);
            
            ResponseEntity<String> response = restTemplate.postForEntity(postUrl, entity, String.class);

            System.out.println("LinkedIn API Response Status: " + response.getStatusCode());
            System.out.println("LinkedIn API Response Body: " + response.getBody());

            if (response.getStatusCode() == HttpStatus.CREATED) {
                String postId = response.getHeaders().getFirst("X-RestLi-Id");
                System.out.println("LinkedIn post created successfully with ID: " + postId);
                return postId;
            }

            return null;

        } catch (Exception e) {
            System.err.println("Error creating LinkedIn post: " + e.getMessage());
            System.err.println("Request payload was: " + payload);
            throw e;
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

    // Updated authorization URL with proper scope using specific app configuration
    public String getAuthorizationUrl(SocialMediaApp app) {
        String redirectUri = "http://localhost:8080/auth/linkedin/callback";
        return UriComponentsBuilder
                .fromHttpUrl("https://www.linkedin.com/oauth/v2/authorization")
                .queryParam("response_type", "code")
                .queryParam("client_id", app.getClientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", app.getId().toString())
                .queryParam("scope", "openid profile email w_member_social")
                .toUriString();
    }

    // Updated token exchange method using specific app configuration
    public Map<String, Object> exchangeCodeForToken(String code, SocialMediaApp app) {
        try {
            String tokenUrl = "https://www.linkedin.com/oauth/v2/accessToken";
            String redirectUri = "http://localhost:8080/api/linkedin/callback";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("code", code);
            params.add("client_id", app.getClientId());
            params.add("client_secret", app.getClientSecret());
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

    // Updated token validation
    public boolean validateToken(String accessToken) {
        try {
            Map<String, Object> profile = getUserProfile(accessToken);
            return profile != null && profile.containsKey("sub");

        } catch (Exception e) {
            System.err.println("Error validating token: " + e.getMessage());
            return false;
        }
    }

    // New method for testing posts
    public String createTestPost(String accessToken, String personUrn, String content) {
        return createLinkedInPost(accessToken, personUrn,
                new PostContent() {{
                    setContent(content);
                    setTitle("");
                }});
    }

    // Refresh expired access token using refresh token with specific app configuration
    public Map<String, Object> refreshAccessToken(String refreshToken, SocialMediaApp app) {
        try {
            String tokenUrl = "https://www.linkedin.com/oauth/v2/accessToken";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "refresh_token");
            params.add("refresh_token", refreshToken);
            params.add("client_id", app.getClientId());
            params.add("client_secret", app.getClientSecret());

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

            return response.getBody(); // New access_token with same refresh_token

        } catch (Exception e) {
            System.err.println("Error refreshing token: " + e.getMessage());
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
    
    /**
     * Publish post using specific social media app configuration
     */
    public String publishPostWithApp(Post post, String accessToken, String personUrn, SocialMediaApp socialMediaApp) {
        try {
            // Get LinkedIn-specific content
            PostContent linkedInContent = postContentRepository.findByPostIdAndPlatform(post.getId(), "linkedin");
            if (linkedInContent == null) {
                throw new RuntimeException("No LinkedIn content found for post");
            }

            String content = linkedInContent.getContent();
            
            // Get media attachments
            List<PostMedia> mediaAttachments = postMediaRepository.findByPostIdOrderByDisplayOrder(post.getId());
            
            // Create LinkedIn post payload
            Map<String, Object> postData = new HashMap<>();
            postData.put("author", personUrn);
            
            Map<String, Object> lifecycleState = new HashMap<>();
            lifecycleState.put("lifecycleState", "PUBLISHED");
            postData.put("lifecycleState", lifecycleState.get("lifecycleState"));
            
            Map<String, Object> specificContent = new HashMap<>();
            Map<String, Object> shareContent = new HashMap<>();
            Map<String, Object> shareCommentary = new HashMap<>();
            shareCommentary.put("text", content);
            shareContent.put("shareCommentary", shareCommentary);
            
            // Add media if present
            if (!mediaAttachments.isEmpty()) {
                List<Map<String, Object>> mediaList = new ArrayList<>();
                for (PostMedia postMedia : mediaAttachments) {
                    Media media = postMedia.getMedia();
                    if (media != null) {
                        // Upload image to LinkedIn and get asset URN
                        String assetUrn = uploadImageToLinkedIn(media, accessToken, socialMediaApp);
                        if (assetUrn != null) {
                            Map<String, Object> mediaItem = new HashMap<>();
                            mediaItem.put("status", "READY");
                            mediaItem.put("media", assetUrn);
                            
                            Map<String, Object> title = new HashMap<>();
                            title.put("text", media.getTitle() != null ? media.getTitle() : "");
                            mediaItem.put("title", title);
                            
                            mediaList.add(mediaItem);
                        }
                    }
                }
                shareContent.put("media", mediaList);
            }
            
            specificContent.put("com.linkedin.ugc.ShareContent", shareContent);
            postData.put("specificContent", specificContent);
            
            Map<String, Object> visibility = new HashMap<>();
            visibility.put("com.linkedin.ugc.MemberNetworkVisibility", "PUBLIC");
            postData.put("visibility", visibility);

            // Make API call using the social media app's configuration
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
           // headers.set("LinkedIn-Version", socialMediaApp.getApiVersion() != null ? socialMediaApp.getApiVersion() : "202304");
            headers.set("X-Restli-Protocol-Version", "2.0.0");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(postData, headers);
            
            String apiUrl = "https://api.linkedin.com" + "/v2/ugcPosts";
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && responseBody.containsKey("id")) {
                    return responseBody.get("id").toString();
                }
            }
            
            return null;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish to LinkedIn with app configuration: " + e.getMessage(), e);
        }
    }
    
    private String uploadImageToLinkedIn(Media media, String accessToken, SocialMediaApp socialMediaApp) {
        try {
            // Implementation for uploading image to LinkedIn using the app configuration
            // This would use the socialMediaApp's clientId and other settings
            // For now, return a placeholder
            return "urn:li:digitalmediaAsset:" + media.getId();
        } catch (Exception e) {
            System.err.println("Failed to upload image to LinkedIn: " + e.getMessage());
            return null;
        }
    }
}
