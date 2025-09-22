package com.aiautoposter.service;

import com.aiautoposter.entity.Post;
import com.aiautoposter.entity.PostContent;
import com.aiautoposter.entity.User;
import com.aiautoposter.repository.PostContentRepository;
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
    private UserService userService;

    // Generate auth URL with user state for linking
    public String getAuthorizationUrl(String username) {
        User user = userService.findByUsername(username);
        String state = Base64.getEncoder().encodeToString(
                ("user:" + user.getId()).getBytes(StandardCharsets.UTF_8)
        );

        return "https://www.linkedin.com/oauth/v2/authorization" +
                "?response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode("openid profile email w_member_social", StandardCharsets.UTF_8) +
                "&state=" + state;
    }

    public Long extractUserIdFromState(String state) {
        if (state == null) {
            throw new IllegalArgumentException("State parameter is required");
        }

        try {
            String decoded = new String(Base64.getDecoder().decode(state), StandardCharsets.UTF_8);
            if (decoded.startsWith("user:")) {
                return Long.parseLong(decoded.substring(5));
            }
            throw new IllegalArgumentException("Invalid state format");
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid state parameter", e);
        }
    }

    // Updated to actually publish to LinkedIn
    public boolean publishPost(Post post, String accessToken, String personUrn) {
        try {
            // Get LinkedIn content for the post
            List<PostContent> postContents = postContentRepository.findByPostIdAndPlatform(
                    post.getId(), "linkedin");

            if (postContents.isEmpty()) {
                throw new RuntimeException("No LinkedIn content found for post");
            }

            PostContent linkedinContent = postContents.get(0);

            // Create the actual LinkedIn post
            String postId = createLinkedInPost(accessToken, personUrn, linkedinContent);

            if (postId != null) {
                System.out.println("Successfully published to LinkedIn with ID: " + postId);
                return true;
            }

            return false;

        } catch (Exception e) {
            System.err.println("Error publishing to LinkedIn: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String createLinkedInPost(String accessToken, String personUrn, PostContent content) {
        String postUrl = "https://api.linkedin.com/v2/ugcPosts";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        headers.set("X-Restli-Protocol-Version", "2.0.0");

        // Build post content
        String fullContent = buildPostContent(content);

        // Create payload using Java 8 compatible HashMap approach
        Map<String, Object> payload = new HashMap<>();
        payload.put("author", "urn:li:person:" + personUrn);
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
            ResponseEntity<String> response = restTemplate.postForEntity(postUrl, entity, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                return response.getHeaders().getFirst("X-RestLi-Id");
            }

            return null;

        } catch (Exception e) {
            System.err.println("Error creating LinkedIn post: " + e.getMessage());
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
                return response.getBody();
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
                // Add the person URN for convenience
                if (profile != null && profile.containsKey("sub")) {
                    profile.put("personUrn", "urn:li:person:" + profile.get("sub"));
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

    // Refresh expired access token using refresh token
    public Map<String, Object> refreshAccessToken(String refreshToken) {
        try {
            String tokenUrl = "https://www.linkedin.com/oauth/v2/accessToken";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "refresh_token");
            params.add("refresh_token", refreshToken);
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);

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
            registerUploadRequest.put("owner", "urn:li:person:" + personUrn);

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
        payload.put("author", "urn:li:person:" + personUrn);
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
