package com.aiautoposter.service;

import com.aiautoposter.entity.Post;
import com.aiautoposter.entity.PostAnalytics;
import com.aiautoposter.repository.PostAnalyticsRepository;
import com.aiautoposter.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LinkedInAnalyticsService {
    
    private static final Logger log = LoggerFactory.getLogger(LinkedInAnalyticsService.class);
    private static final String LINKEDIN_API_BASE = "https://api.linkedin.com/v2";
    
    @Value("${linkedin.client.id}")
    private String clientId;
    
    @Value("${linkedin.client.secret}")
    private String clientSecret;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private PostAnalyticsRepository postAnalyticsRepository;
    
    @Autowired
    private LinkedInService linkedInService;
    
    /**
     * Fetches analytics for a specific post from LinkedIn
     */
    public PostAnalytics fetchPostAnalytics(Post post, String accessToken) {
        try {
            if (accessToken == null) {
                log.warn("Cannot fetch analytics for post {}: No access token provided", post.getId());
                return null;
            }
            
            // Get the LinkedIn URN for this post
            String postUrn = extractLinkedInUrn(post);
            if (postUrn == null) {
                log.warn("No LinkedIn URN found for post ID: {}", post.getId());
                return null;
            }
            
            // Build the URL for the LinkedIn API
            String url = String.format("%s/socialActions/%s", LINKEDIN_API_BASE, postUrn);
            
            // Set up headers with OAuth token
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Restli-Protocol-Version", "2.0.0");
            
            // Make the API request
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return processAnalyticsResponse(post, response.getBody());
            }
            
            log.error("Failed to fetch LinkedIn analytics for post {}: {}", post.getId(), response.getStatusCode());
            return null;
            
        } catch (Exception e) {
            log.error("Error fetching LinkedIn analytics for post {}: {}", post.getId(), e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Processes the analytics response from LinkedIn and saves it
     */
    private PostAnalytics processAnalyticsResponse(Post post, Map<String, Object> analyticsData) {
        try {
            PostAnalytics analytics = new PostAnalytics();
            analytics.setPost(post);
            analytics.setPlatform("LINKEDIN");
            
            // Extract basic metrics
            Map<String, Object> likes = (Map<String, Object>) analyticsData.get("likes");
            Map<String, Object> comments = (Map<String, Object>) analyticsData.get("comments");
            Map<String, Object> shares = (Map<String, Object>) analyticsData.get("shares");
            
            // Set the metrics
            if (likes != null) {
                analytics.setLikes(((Number) likes.get("count")).intValue());
            }
            
            if (comments != null) {
                analytics.setComments(((Number) comments.get("count")).intValue());
            }
            
            if (shares != null) {
                analytics.setShares(((Number) shares.get("count")).intValue());
            }
            
            // Get impressions and clicks if available (requires additional API call)
            fetchEngagementMetrics(post, analytics);
            
            // Save and return the analytics
            return postAnalyticsRepository.save(analytics);
            
        } catch (Exception e) {
            log.error("Error processing LinkedIn analytics response: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Fetches additional engagement metrics (impressions, clicks) for a post
     */
    private void fetchEngagementMetrics(Post post, PostAnalytics analytics) {
        try {
            String postUrn = extractLinkedInUrn(post);
            if (postUrn == null) {
                log.debug("No LinkedIn URN found for post {}", post.getId());
                return;
            }
            
            // Always use the manager's LinkedIn account (ID: 2)
            final Long MANAGER_USER_ID = 2L;
            
            // Check if manager has a connected LinkedIn account
            if (!linkedInService.isLinkedInConnected(MANAGER_USER_ID)) {
                log.error("Cannot fetch engagement metrics: Manager (ID: {}) does not have a connected LinkedIn account", 
                         MANAGER_USER_ID);
                return;
            }
            
            // Get access token for the manager
            String accessToken = linkedInService.getAccessTokenForUser(MANAGER_USER_ID);
            if (accessToken == null) {
                log.error("No access token available for manager (ID: {}) to fetch engagement metrics", 
                         MANAGER_USER_ID);
                return;
            }
            
            log.debug("Using manager's LinkedIn account (ID: {}) to fetch engagement metrics for post {}", 
                     MANAGER_USER_ID, post.getId());
            
            // Build the URL for the LinkedIn API
            String url = String.format("%s/organizationalEntityShareStatistics?q=organizationalEntity&organizationalEntity=%s", 
                                     LINKEDIN_API_BASE, postUrn);
            
            // Set up headers with OAuth token
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("X-Restli-Protocol-Version", "2.0.0");
            
            // Make the API request
            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Process the engagement metrics
                Map<String, Object> elements = (Map<String, Object>) response.getBody();
                if (elements != null && elements.containsKey("elements")) {
                    List<Map<String, Object>> elementsList = (List<Map<String, Object>>) elements.get("elements");
                    if (elementsList != null && !elementsList.isEmpty()) {
                        Map<String, Object> stats = elementsList.get(0);
                        
                        if (stats.containsKey("totalShareStatistics")) {
                            Map<String, Object> shareStats = (Map<String, Object>) stats.get("totalShareStatistics");
                            if (shareStats != null) {
                                analytics.setImpressions(((Number) shareStats.get("impressionCount")).intValue());
                                analytics.setClicks(((Number) shareStats.get("clickCount")).intValue());
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error fetching engagement metrics for post {}: {}", post.getId(), e.getMessage());
        }
    }
    
    /**
     * Extracts the LinkedIn URN from the post's content or other fields
     */
    private String extractUrnFromPostContent(Post post) {
        // Try to find the URN in the post content or other fields
        // This is a fallback method when target_platforms is not in JSON format
        
        // Example: Check if the post content contains a LinkedIn URL
        if (post.getSourceDiscussion() != null) {
            String content = post.getSourceDiscussion().toLowerCase();
            
            // Look for LinkedIn post URL pattern
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("https?://(www\\.)?linkedin\\.com/.*/activity/.*");
            java.util.regex.Matcher matcher = pattern.matcher(content);
            
            if (matcher.find()) {
                String url = matcher.group();
                // Convert the URL to a URN if possible
                return convertLinkedInUrlToUrn(url);
            }
        }
        
        log.debug("Could not extract LinkedIn URN from post content for post ID: {}", post.getId());
        return null;
    }
    
    /**
     * Converts a LinkedIn post URL to a URN
     */
    private String convertLinkedInUrlToUrn(String url) {
        try {
            // Extract the activity ID from the URL
            // Example: https://www.linkedin.com/feed/update/urn:li:activity:1234567890/
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("activity/([^/?]+)");
            java.util.regex.Matcher matcher = pattern.matcher(url);
            
            if (matcher.find()) {
                String activityId = matcher.group(1);
                // If it's not already a URN, format it as one
                if (!activityId.startsWith("urn:li:")) {
                    return "urn:li:activity:" + activityId;
                }
                return activityId;
            }
        } catch (Exception e) {
            log.warn("Error converting LinkedIn URL to URN: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Extracts the LinkedIn URN from the post's target platforms or content
     */
    private String extractLinkedInUrn(Post post) {
        if (post == null) {
            return null;
        }
        
        String targetPlatforms = post.getTargetPlatforms();
        if (targetPlatforms == null || targetPlatforms.trim().isEmpty()) {
            return null;
        }
        
        // First, try to parse as JSON
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode platformsNode = objectMapper.readTree(targetPlatforms);
            
            // Handle array format
            if (platformsNode.isArray()) {
                for (JsonNode platform : platformsNode) {
                    // Handle string elements
                    if (platform.isTextual()) {
                        String text = platform.asText();
                        if (text.startsWith("urn:li:share:")) {
                            return text;
                        }
                    }
                    // Handle object elements
                    if (platform.isObject() && platform.has("platform") && 
                        "LINKEDIN".equalsIgnoreCase(platform.get("platform").asText())) {
                        return platform.has("urn") ? platform.get("urn").asText() : null;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not parse targetPlatforms as JSON for post {}: {}", 
                     post.getId(), e.getMessage());
            // Check if it's a simple "linkedin" string
            if ("linkedin".equalsIgnoreCase(targetPlatforms.trim())) {
                log.debug("Found simple 'linkedin' string for post {}", post.getId());
                // Try to get URN from post content or other fields
                return extractUrnFromPostContent(post);
            }
        }
        try {
            if (targetPlatforms.contains("urn:li:share:")) {
                // First extract the URN pattern
                String urnPattern = "urn:li:share:[^\\s\"]+";
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(urnPattern);
                java.util.regex.Matcher matcher = pattern.matcher(targetPlatforms);
                if (matcher.find()) {
                    String urn = matcher.group();
                    // Remove any unwanted characters
                    return urn.replaceAll("[\"'\\[\\]]", "");
                }
            }
        } catch (Exception e) {
            log.warn("Error extracting URN from string for post {}: {}", 
                    post.getId(), e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Scheduled job to fetch analytics for all published LinkedIn posts
     * Runs every 2 hours
     */
    @Scheduled(fixedRate = 60 * 1000) // Every minute
    public void fetchAllLinkedInAnalytics() {
        log.info("Starting scheduled LinkedIn analytics fetch");
        
        // Get all posts that have been published to LinkedIn
        // You'll need to implement this method in your PostRepository
        List<Post> linkedInPosts = postRepository.findByTargetPlatformsContaining("LINKEDIN");
        
        if (linkedInPosts == null || linkedInPosts.isEmpty()) {
            log.info("No LinkedIn posts found for analytics");
            return;
        }
        
        log.info("Found {} LinkedIn posts to fetch analytics for", linkedInPosts.size());
        
        // Get the manager's LinkedIn account (ID: 2)
        final Long MANAGER_USER_ID = 2L;
        
        // Check if manager has a connected LinkedIn account
        if (!linkedInService.isLinkedInConnected(MANAGER_USER_ID)) {
            log.error("Manager (ID: {}) does not have a connected LinkedIn account", MANAGER_USER_ID);
            return;
        }
        
        // Get access token for the manager
        String accessToken = linkedInService.getAccessTokenForUser(MANAGER_USER_ID);
        if (accessToken == null) {
            log.error("No LinkedIn access token found for manager (ID: {})", MANAGER_USER_ID);
            return;
        }
        
        log.info("Using manager's LinkedIn account (ID: {}) for fetching analytics", MANAGER_USER_ID);
        
        // Process all posts with the manager's LinkedIn account
        for (Post post : linkedInPosts) {
            if (post == null) continue;
            
            try {
                log.debug("Fetching analytics for post {} (created by user {})", post.getId(), post.getCreatedBy());
                fetchPostAnalytics(post, accessToken);
                
                // Add a small delay to avoid rate limiting
                Thread.sleep(1000);
            } catch (Exception e) {
                log.error("Error fetching analytics for post {}: {}", post.getId(), e.getMessage(), e);
            }
        }
        
        log.info("Completed LinkedIn analytics fetch");
    }
    
    /**
     * Gets the latest analytics for a post
     */
    public PostAnalytics getLatestAnalyticsForPost(Long postId) {
        return postAnalyticsRepository
            .findFirstByPostIdAndPlatformOrderByCollectedAtDesc(postId, "LINKEDIN")
            .orElse(null);
    }
    
    /**
     * Gets analytics history for a post
     */
    public List<PostAnalytics> getAnalyticsHistory(Long postId, LocalDateTime startDate) {
        return postAnalyticsRepository.findByPostIdAndPlatformAndCollectedAtAfter(
            postId, "LINKEDIN", startDate);
    }
    
    /**
     * Gets analytics for multiple posts
     */
    public Map<Long, PostAnalytics> getAnalyticsForPosts(List<Long> postIds) {
        // Get analytics from the last 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        
        return postAnalyticsRepository
            .findByPostIdInAndPlatformAndCollectedAtAfter(postIds, "LINKEDIN", thirtyDaysAgo)
            .stream()
            .collect(Collectors.toMap(
                analytics -> analytics.getPost().getId(),
                analytics -> analytics,
                (existing, replacement) -> {
                    // If there are multiple entries for the same post, keep the most recent one
                    return existing.getCollectedAt().isAfter(replacement.getCollectedAt()) 
                        ? existing 
                        : replacement;
                }
            ));
    }
}
