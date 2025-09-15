package com.aiautoposter.service;

import com.aiautoposter.entity.Post;
import com.aiautoposter.entity.PostContent;
import com.aiautoposter.repository.PostContentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    public boolean publishPost(Post post) {
        try {
            // Get LinkedIn content for the post
            List<PostContent> postContents = postContentRepository.findByPostIdAndPlatform(
                post.getId(), "linkedin");
            
            if (postContents.isEmpty()) {
                throw new RuntimeException("No LinkedIn content found for post");
            }
            
            PostContent linkedinContent = postContents.get(0);
            
            // In a real implementation, you would:
            // 1. Get the user's LinkedIn access token
            // 2. Use LinkedIn API to publish the post
            // 3. Handle the response
            
            // For now, we'll simulate a successful publication
            System.out.println("Publishing to LinkedIn:");
            System.out.println("Title: " + linkedinContent.getTitle());
            System.out.println("Content: " + linkedinContent.getContent());
            
            // Simulate API call delay
            Thread.sleep(1000);
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Error publishing to LinkedIn: " + e.getMessage());
            return false;
        }
    }
    
    public String getAuthorizationUrl() {
        return String.format(
            "https://www.linkedin.com/oauth/v2/authorization?response_type=code&client_id=%s&redirect_uri=%s&state=random_state&scope=w_member_social",
            clientId, redirectUri
        );
    }
    
    public String exchangeCodeForToken(String code) {
        try {
            String tokenUrl = "https://www.linkedin.com/oauth/v2/accessToken";
            
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("grant_type", "authorization_code");
            requestBody.put("code", code);
            requestBody.put("client_id", clientId);
            requestBody.put("client_secret", clientSecret);
            requestBody.put("redirect_uri", redirectUri);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                return (String) responseBody.get("access_token");
            }
            
            return null;
            
        } catch (Exception e) {
            System.err.println("Error exchanging code for token: " + e.getMessage());
            return null;
        }
    }
    
    public boolean validateToken(String accessToken) {
        try {
            String profileUrl = "https://api.linkedin.com/v2/me";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                profileUrl, HttpMethod.GET, request, Map.class);
            
            return response.getStatusCode() == HttpStatus.OK;
            
        } catch (Exception e) {
            System.err.println("Error validating token: " + e.getMessage());
            return false;
        }
    }
}
