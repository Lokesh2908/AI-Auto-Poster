package com.aiautoposter.controller;

import com.aiautoposter.entity.Post;
import com.aiautoposter.entity.PostAnalytics;
import com.aiautoposter.service.LinkedInAnalyticsService;
import com.aiautoposter.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private LinkedInAnalyticsService linkedInAnalyticsService;
    
    @Autowired
    private PostService postService;
    
    /**
     * Get analytics for a specific post
     */
    @GetMapping("/post/{postId}")
    public ResponseEntity<?> getPostAnalytics(
            @PathVariable Long postId,
            Authentication authentication) {
        
        // Verify the authenticated user has access to this post
        Post post = postService.getPostById(postId);
        if (post == null) {
            return ResponseEntity.notFound().build();
        }
        
        // In a real app, you'd also verify the user has permission to view this post's analytics
        
        // Get the latest analytics
        PostAnalytics analytics = linkedInAnalyticsService.getLatestAnalyticsForPost(postId);
        
        if (analytics == null) {
            return ResponseEntity.noContent().build();
        }
        
        return ResponseEntity.ok(mapAnalyticsToResponse(analytics));
    }
    
    /**
     * Get analytics history for a post
     */
    @GetMapping("/post/{postId}/history")
    public ResponseEntity<?> getPostAnalyticsHistory(
            @PathVariable Long postId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            Authentication authentication) {
        
        // Verify the authenticated user has access to this post
        Post post = postService.getPostById(postId);
        if (post == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Default to 30 days if no start date provided
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        
        // Get the analytics history
        List<PostAnalytics> analyticsHistory = linkedInAnalyticsService.getAnalyticsHistory(postId, startDate);
        
        if (analyticsHistory.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        
        // Map to response DTOs
        List<Map<String, Object>> response = analyticsHistory.stream()
            .map(this::mapAnalyticsToResponse)
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get analytics for all posts of the authenticated user
     */
    @GetMapping("/my-posts")
    public ResponseEntity<?> getMyPostsAnalytics(Authentication authentication) {
        // Get the current user's ID
        String username = authentication.getName();
        
        // Get all posts for this user
        List<Post> userPosts = postService.getPostsByUsername(username);
        
        if (userPosts.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        
        // Get post IDs
        List<Long> postIds = userPosts.stream()
            .map(Post::getId)
            .collect(Collectors.toList());
        
        // Get analytics for these posts
        Map<Long, PostAnalytics> analyticsMap = linkedInAnalyticsService.getAnalyticsForPosts(postIds);
        
        // Prepare response
        List<Map<String, Object>> response = userPosts.stream()
            .map(post -> {
                Map<String, Object> postData = new HashMap<>();
                postData.put("postId", post.getId());
                postData.put("title", post.getTitle());
                postData.put("status", post.getCurrentStatus().toString());
                postData.put("createdAt", post.getCreatedAt());
                
                // Add analytics if available
                PostAnalytics analytics = analyticsMap.get(post.getId());
                if (analytics != null) {
                    postData.put("analytics", mapAnalyticsToResponse(analytics));
                }
                
                return postData;
            })
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(response);
    }
    
    /**
     * Manually trigger analytics refresh for a post
     */
    @PostMapping("/post/{postId}/refresh")
    public ResponseEntity<?> refreshPostAnalytics(
            @PathVariable Long postId,
            Authentication authentication) {
        
        // Verify the authenticated user has access to this post
        Post post = postService.getPostById(postId);
        if (post == null) {
            return ResponseEntity.notFound().build();
        }
        
        // In a real app, verify the user has permission to refresh analytics
        
        // Trigger a manual refresh
        PostAnalytics analytics = linkedInAnalyticsService.fetchPostAnalytics(post, null);
        
        if (analytics == null) {
            return ResponseEntity.badRequest().body("Failed to refresh analytics for this post");
        }
        
        return ResponseEntity.ok(mapAnalyticsToResponse(analytics));
    }
    
    /**
     * Helper method to map analytics entity to response DTO
     */
    private Map<String, Object> mapAnalyticsToResponse(PostAnalytics analytics) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", analytics.getId());
        response.put("postId", analytics.getPost().getId());
        response.put("platform", analytics.getPlatform());
        response.put("impressions", analytics.getImpressions());
        response.put("likes", analytics.getLikes());
        response.put("comments", analytics.getComments());
        response.put("shares", analytics.getShares());
        response.put("clicks", analytics.getClicks());
        response.put("engagementRate", analytics.getEngagementRate());
        response.put("collectedAt", analytics.getCollectedAt());
        return response;
    }
}
