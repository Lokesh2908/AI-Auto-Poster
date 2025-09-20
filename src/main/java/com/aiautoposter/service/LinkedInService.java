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
}
