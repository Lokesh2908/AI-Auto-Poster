package com.aiautoposter.controller;

import com.aiautoposter.entity.Post;
import com.aiautoposter.entity.PostContent;
import com.aiautoposter.entity.PostVersion;
import com.aiautoposter.entity.User;
import com.aiautoposter.service.PostService;
import com.aiautoposter.service.PostVersionService;
import com.aiautoposter.service.UserService;
import com.aiautoposter.security.JwtTokenUtil;
import com.aiautoposter.entity.Image;
import com.aiautoposter.dto.PostVersionRequest;
import com.aiautoposter.dto.RestoreVersionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "*")
public class PostController {
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private PostVersionService postVersionService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
    @Autowired
    private DataSource dataSource;
    
    @PostMapping
    public ResponseEntity<Post> createPost(@Valid @RequestBody Post post) {
        try {
            Post createdPost = postService.createPost(post);
            return new ResponseEntity<>(createdPost, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping
    public ResponseEntity<List<Post>> getAllPosts() {
        List<Post> posts = postService.findAll();
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Post> getPostById(@PathVariable Long id) {
        Optional<Post> post = postService.findById(id);
        return post.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Post>> getPostsByUser(@PathVariable Long userId) {
        List<Post> posts = postService.findByCreatedBy(userId);
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }
    
    @GetMapping("/manager/{managerId}")
    public ResponseEntity<List<Post>> getPostsByManager(@PathVariable Long managerId) {
        try {
            System.out.println("=== GET POSTS BY MANAGER DEBUG ===");
            System.out.println("Manager ID: " + managerId);
            
            List<Post> posts = postService.findPostsByManager(managerId);
            System.out.println("Found " + posts.size() + " posts for manager " + managerId);
            
            for (Post post : posts) {
                System.out.println("Post ID: " + post.getId() + 
                                 ", Title: " + post.getTitle() + 
                                 ", Created By: " + post.getCreatedBy() +
                                 ", Status: " + post.getCurrentStatus());
            }
            
            return new ResponseEntity<>(posts, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Error getting posts by manager: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Post>> getPostsByStatus(@PathVariable Post.PostStatus status) {
        List<Post> posts = postService.findByCurrentStatus(status);
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }
    
    @GetMapping("/user/{userId}/status/{status}")
    public ResponseEntity<List<Post>> getPostsByUserAndStatus(@PathVariable Long userId, 
                                                            @PathVariable Post.PostStatus status) {
        List<Post> posts = postService.findByCreatedByAndCurrentStatus(userId, status);
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }
    
    @GetMapping("/date-range")
    public ResponseEntity<List<Post>> getPostsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<Post> posts = postService.findByCreatedAtBetween(startDate, endDate);
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }
    
    @GetMapping("/statuses")
    public ResponseEntity<List<Post>> getPostsByStatuses(@RequestParam List<Post.PostStatus> statuses) {
        List<Post> posts = postService.findByCurrentStatusIn(statuses);
        return new ResponseEntity<>(posts, HttpStatus.OK);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Post> updatePost(@PathVariable Long id, @Valid @RequestBody Post post) {
        try {
            System.out.println("=== UPDATE POST CONTROLLER ===");
            System.out.println("Updating post ID: " + id);
            System.out.println("New title: " + post.getTitle());
            System.out.println("New source discussion: " + post.getSourceDiscussion());
            System.out.println("New target platforms: " + post.getTargetPlatforms());
            
            post.setId(id);
            Post updatedPost = postService.updatePost(post);
            
            System.out.println("Post updated successfully. ID: " + updatedPost.getId());
            System.out.println("=== END UPDATE POST CONTROLLER ===");
            
            return new ResponseEntity<>(updatedPost, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("UPDATE POST ERROR: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<Post> updatePostStatus(@PathVariable Long id, 
                                               @RequestParam Post.PostStatus status) {
        try {
            Post post = postService.updatePostStatus(id, status);
            return new ResponseEntity<>(post, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @PostMapping("/{id}/submit-approval")
    public ResponseEntity<?> submitForApproval(@PathVariable Long id) {
        try {
            System.out.println("=== SUBMIT FOR APPROVAL CONTROLLER ===");
            System.out.println("Submitting post ID: " + id + " for approval");
            
            Post post = postService.submitForApproval(id);
            
            System.out.println("Post submitted successfully. New status: " + post.getCurrentStatus());
            System.out.println("=== END SUBMIT FOR APPROVAL CONTROLLER ===");
            
            return new ResponseEntity<>(post, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("SUBMIT FOR APPROVAL ERROR: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    
    @PostMapping("/{id}/approve")
    public ResponseEntity<Post> approvePost(@PathVariable Long id, HttpServletRequest request) {
        try {
            // Get current user from JWT token
            String authHeader = request.getHeader("Authorization");
            String username = null;
            Long currentUserId = null;
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                username = jwtTokenUtil.getUsernameFromToken(token);
                if (username != null) {
                    User currentUser = userService.findByEmail(username).orElse(null);
                    if (currentUser != null) {
                        currentUserId = currentUser.getId();
                    }
                }
            }
            
            Post post = postService.approvePost(id, currentUserId);
            return new ResponseEntity<>(post, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @PostMapping("/{id}/reject")
    public ResponseEntity<Post> rejectPost(@PathVariable Long id, @RequestParam String feedback, HttpServletRequest request) {
        try {
            // Get current user from JWT token
            String authHeader = request.getHeader("Authorization");
            String username = null;
            Long currentUserId = null;
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                username = jwtTokenUtil.getUsernameFromToken(token);
                if (username != null) {
                    User currentUser = userService.findByEmail(username).orElse(null);
                    if (currentUser != null) {
                        currentUserId = currentUser.getId();
                    }
                }
            }
            
            Post post = postService.rejectPost(id, feedback, currentUserId);
            return new ResponseEntity<>(post, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @PostMapping("/{id}/content")
    public ResponseEntity<PostContent> addPostContent(@PathVariable Long id, 
                                                    @Valid @RequestBody PostContent postContent) {
        try {
            postContent.setPostId(id);
            PostContent savedContent = postService.addPostContent(postContent);
            return new ResponseEntity<>(savedContent, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/{id}/content")
    public ResponseEntity<?> getPostContents(@PathVariable Long id) {
        try {
            System.out.println("=== POST CONTENT CONTROLLER ===");
            System.out.println("Getting content for post ID: " + id);
            System.out.println("Authentication: " + org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
            
            List<PostContent> contents = postService.getPostContents(id);
            System.out.println("Found " + contents.size() + " content items");
            
            // Create a simple map to avoid serialization issues
            java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
            for (PostContent content : contents) {
                java.util.Map<String, Object> contentMap = new java.util.HashMap<>();
                contentMap.put("id", content.getId());
                contentMap.put("postId", content.getPostId());
                contentMap.put("platform", content.getPlatform());
                contentMap.put("title", content.getTitle());
                contentMap.put("content", content.getContent());
                contentMap.put("aiConfidenceScore", content.getAiConfidenceScore());
                contentMap.put("hashtags", content.getHashtags());
                contentMap.put("createdAt", content.getCreatedAt());
                result.add(contentMap);
                
                System.out.println("Content ID: " + content.getId());
                System.out.println("Platform: " + content.getPlatform());
                System.out.println("Content preview: " + content.getContent().substring(0, Math.min(50, content.getContent().length())) + "...");
                System.out.println("Hashtags: " + content.getHashtags());
            }
            
            System.out.println("=== END POST CONTENT CONTROLLER ===");
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("POST CONTENT CONTROLLER ERROR: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/{id}/images")
    public ResponseEntity<Image> addImage(@PathVariable Long id, @Valid @RequestBody Image image) {
        try {
            image.setPostId(id);
            Image savedImage = postService.addImage(image);
            return new ResponseEntity<>(savedImage, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping("/{id}/images")
    public ResponseEntity<List<Image>> getImages(@PathVariable Long id) {
        List<Image> images = postService.getImages(id);
        return new ResponseEntity<>(images, HttpStatus.OK);
    }
    
    @GetMapping("/{id}/workflow-steps")
    public ResponseEntity<List<com.aiautoposter.entity.AIWorkflowStep>> getWorkflowSteps(@PathVariable Long id) {
        List<com.aiautoposter.entity.AIWorkflowStep> steps = postService.getAIWorkflowSteps(id);
        return new ResponseEntity<>(steps, HttpStatus.OK);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        try {
            postService.deletePost(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @GetMapping("/stats/user/{userId}/status/{status}")
    public ResponseEntity<Long> getPostCountByUserAndStatus(@PathVariable Long userId, 
                                                           @PathVariable Post.PostStatus status) {
        Long count = postService.countByCreatedByAndCurrentStatus(userId, status);
        return new ResponseEntity<>(count, HttpStatus.OK);
    }
    
    // Fix post creator endpoint
    @PostMapping("/{id}/fix-creator")
    public ResponseEntity<?> fixPostCreator(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        try {
            System.out.println("=== FIX POST CREATOR ===");
            System.out.println("Fixing creator for post ID: " + id);
            
            // Get current user from JWT token
            String token = authHeader.replace("Bearer ", "");
            String username = jwtTokenUtil.getUsernameFromToken(token);
            User currentUser = userService.findByEmail(username).orElse(null);
            
            if (currentUser == null) {
                return new ResponseEntity<>("User not found", HttpStatus.BAD_REQUEST);
            }
            
            // Update the post
            Post post = postService.findById(id).orElse(null);
            if (post == null) {
                return new ResponseEntity<>("Post not found", HttpStatus.NOT_FOUND);
            }
            
            System.out.println("Assigning post to user: " + currentUser.getEmail() + " (ID: " + currentUser.getId() + ")");
            post.setCreatedBy(currentUser.getId());
            Post updatedPost = postService.updatePost(post);
            
            System.out.println("Post creator updated successfully");
            return new ResponseEntity<>(updatedPost, HttpStatus.OK);
            
        } catch (Exception e) {
            System.err.println("Fix creator error: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Simple test endpoint for approval without complex dependencies
    @PostMapping("/{id}/test-approval")
    public ResponseEntity<?> testSubmitForApproval(@PathVariable Long id) {
        try {
            System.out.println("=== TEST APPROVAL ENDPOINT ===");
            System.out.println("Testing approval for post ID: " + id);
            
            // Just update the post status without approval request or notification
            Post post = postService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Post not found"));
            
            System.out.println("Found post: " + post.getTitle());
            System.out.println("Current status: " + post.getCurrentStatus());
            System.out.println("Created by: " + post.getCreatedBy());
            
            post.setCurrentStatus(Post.PostStatus.PENDING_APPROVAL);
            Post updatedPost = postService.updatePostStatus(id, Post.PostStatus.PENDING_APPROVAL);
            
            System.out.println("Updated post status to: " + updatedPost.getCurrentStatus());
            System.out.println("=== END TEST APPROVAL ENDPOINT ===");
            
            return new ResponseEntity<>(updatedPost, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("TEST APPROVAL ERROR: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Test endpoint that returns JSON like the authenticated one but without auth
    @GetMapping("/test/{id}/content")
    public ResponseEntity<?> testGetPostContents(@PathVariable Long id) {
        try {
            System.out.println("=== TEST ENDPOINT ===");
            System.out.println("Testing content retrieval for post ID: " + id);
            
            List<PostContent> contents = postService.getPostContents(id);
            System.out.println("Test endpoint returning " + contents.size() + " items");
            
            return new ResponseEntity<>(contents, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("TEST ENDPOINT ERROR: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // Debug endpoint to check content without authentication
    @GetMapping("/debug/{id}/content")
    public ResponseEntity<String> debugGetPostContents(@PathVariable Long id) {
        try {
            List<PostContent> contents = postService.getPostContents(id);
            StringBuilder debug = new StringBuilder();
            debug.append("Post ID: ").append(id).append("\n");
            debug.append("Content count: ").append(contents.size()).append("\n");
            for (int i = 0; i < contents.size(); i++) {
                PostContent content = contents.get(i);
                debug.append("Content ").append(i + 1).append(":\n");
                debug.append("  Platform: ").append(content.getPlatform()).append("\n");
                debug.append("  Title: ").append(content.getTitle()).append("\n");
                debug.append("  Content length: ").append(content.getContent().length()).append("\n");
                debug.append("  Hashtags: ").append(content.getHashtags()).append("\n");
                debug.append("  Created: ").append(content.getCreatedAt()).append("\n");
            }
            return new ResponseEntity<>(debug.toString(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // Update post content endpoint
    @PutMapping("/{postId}/content/{contentId}")
    public ResponseEntity<?> updatePostContent(@PathVariable Long postId, 
                                             @PathVariable Long contentId,
                                             @RequestBody Map<String, Object> contentData,
                                             HttpServletRequest request) {
        try {
            System.out.println("=== UPDATE POST CONTENT CONTROLLER ===");
            System.out.println("Updating content ID: " + contentId + " for post ID: " + postId);
            
            // Get current user from JWT token
            String authHeader = request.getHeader("Authorization");
            String username = null;
            Long currentUserId = null;
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                username = jwtTokenUtil.getUsernameFromToken(token);
                if (username != null) {
                    User currentUser = userService.findByEmail(username).orElse(null);
                    if (currentUser != null) {
                        currentUserId = currentUser.getId();
                    }
                }
            }
            
            if (currentUserId == null) {
                return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
            }
            
            // Update content using the post service
            PostContent updatedContent = postService.updatePostContent(postId, contentId, contentData, currentUserId);
            
            System.out.println("Content updated successfully");
            System.out.println("=== END UPDATE POST CONTENT CONTROLLER ===");
            
            return new ResponseEntity<>(updatedContent, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("UPDATE POST CONTENT ERROR: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Get LinkedIn-ready content (HTML stripped)
    @GetMapping("/{postId}/content/{contentId}/linkedin-ready")
    public ResponseEntity<String> getLinkedInReadyContent(@PathVariable Long postId, 
                                                        @PathVariable Long contentId) {
        try {
            System.out.println("=== GET LINKEDIN-READY CONTENT ===");
            System.out.println("Getting LinkedIn-ready content for content ID: " + contentId);
            
            String linkedInContent = postService.getLinkedInReadyContent(contentId);
            
            System.out.println("LinkedIn-ready content retrieved successfully");
            return new ResponseEntity<>(linkedInContent, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("GET LINKEDIN-READY CONTENT ERROR: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Regenerate post content endpoint
    @PostMapping("/{id}/regenerate-content")
    public ResponseEntity<?> regeneratePostContent(@PathVariable Long id, HttpServletRequest request) {
        try {
            System.out.println("=== REGENERATE CONTENT CONTROLLER ===");
            System.out.println("Regenerating content for post ID: " + id);
            
            // Get current user from JWT token
            String authHeader = request.getHeader("Authorization");
            String username = null;
            Long currentUserId = null;
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                username = jwtTokenUtil.getUsernameFromToken(token);
                if (username != null) {
                    User currentUser = userService.findByEmail(username).orElse(null);
                    if (currentUser != null) {
                        currentUserId = currentUser.getId();
                    }
                }
            }
            
            if (currentUserId == null) {
                return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
            }
            
            // Regenerate content using the post service
            Post post = postService.regeneratePostContent(id, currentUserId);
            
            System.out.println("Content regenerated successfully for post: " + post.getTitle());
            System.out.println("=== END REGENERATE CONTENT CONTROLLER ===");
            
            return new ResponseEntity<>(post, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("REGENERATE CONTENT ERROR: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // ==================== VERSION MANAGEMENT ENDPOINTS ====================
    
    /**
     * Save a new version of a post (for thread-based refinement system)
     */
    @PostMapping("/{postId}/versions")
    public ResponseEntity<?> savePostVersion(@PathVariable Long postId, 
                                           @Valid @RequestBody PostVersionRequest request,
                                           HttpServletRequest httpRequest) {
        try {
            System.out.println("=== SAVE POST VERSION ===");
            System.out.println("Saving version for post ID: " + postId);
            System.out.println("Version type: " + request.getVersionType());
            System.out.println("Change description: " + request.getChangeDescription());
            
            // Get current user from JWT token
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
            }
            
            String token = authHeader.substring(7);
            String username = jwtTokenUtil.getUsernameFromToken(token);
            if (username == null) {
                return new ResponseEntity<>("Invalid token", HttpStatus.UNAUTHORIZED);
            }
            
            PostVersion savedVersion = postVersionService.saveVersion(postId, request);
            System.out.println("Version saved successfully with ID: " + savedVersion.getId());
            
            return new ResponseEntity<>(savedVersion, HttpStatus.CREATED);
        } catch (Exception e) {
            System.err.println("SAVE VERSION ERROR: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * Get all versions for a post (for thread history display)
     */
    @GetMapping("/{postId}/versions")
    public ResponseEntity<?> getPostVersions(@PathVariable Long postId,
                                           HttpServletRequest httpRequest) {
        try {
            System.out.println("=== GET POST VERSIONS ===");
            System.out.println("Getting versions for post ID: " + postId);
            
            // Get current user from JWT token
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
            }
            
            String token = authHeader.substring(7);
            String username = jwtTokenUtil.getUsernameFromToken(token);
            if (username == null) {
                return new ResponseEntity<>("Invalid token", HttpStatus.UNAUTHORIZED);
            }
            
            List<PostVersion> versions = postVersionService.getVersionHistory(postId);
            System.out.println("Found " + versions.size() + " versions");
            
            return new ResponseEntity<>(versions, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("GET VERSIONS ERROR: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * Restore post to a previous version
     */
    @PostMapping("/{postId}/restore-version")
    public ResponseEntity<?> restorePostVersion(@PathVariable Long postId,
                                              @Valid @RequestBody RestoreVersionRequest request,
                                              HttpServletRequest httpRequest) {
        try {
            System.out.println("=== RESTORE POST VERSION ===");
            System.out.println("Restoring post ID: " + postId + " to version ID: " + request.getVersionId());
            
            // Get current user from JWT token
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
            }
            
            String token = authHeader.substring(7);
            String username = jwtTokenUtil.getUsernameFromToken(token);
            if (username == null) {
                return new ResponseEntity<>("Invalid token", HttpStatus.UNAUTHORIZED);
            }
            
            Post restoredPost = postVersionService.restoreVersion(postId, request);
            System.out.println("Post restored successfully");
            
            return new ResponseEntity<>(restoredPost, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("RESTORE VERSION ERROR: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * Get version count for a post
     */
    @GetMapping("/{postId}/versions/count")
    public ResponseEntity<?> getVersionCount(@PathVariable Long postId,
                                           HttpServletRequest httpRequest) {
        try {
            // Get current user from JWT token
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
            }
            
            String token = authHeader.substring(7);
            String username = jwtTokenUtil.getUsernameFromToken(token);
            if (username == null) {
                return new ResponseEntity<>("Invalid token", HttpStatus.UNAUTHORIZED);
            }
            
            Long count = postVersionService.getVersionCount(postId);
            return new ResponseEntity<>(count, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * Debug endpoint to check if database migration V7 executed
     */
    @GetMapping("/debug/database/check-migration")
    public ResponseEntity<?> checkDatabaseMigration() {
        try {
            System.out.println("=== CHECKING DATABASE MIGRATION V7 ===");
            
            // Try to access a version with the new fields
            List<com.aiautoposter.entity.PostVersion> versions = postVersionService.getVersionHistory(1L);
            
            StringBuilder result = new StringBuilder();
            result.append("Migration V7 Status: ");
            
            if (versions.isEmpty()) {
                result.append("No versions found to test\n");
            } else {
                com.aiautoposter.entity.PostVersion firstVersion = versions.get(0);
                try {
                    // Try to access the new fields
                    String savedContent = firstVersion.getSavedContent();
                    String contentSummary = firstVersion.getContentSummary();
                    
                    result.append("SUCCESS - V7 fields accessible\n");
                    result.append("Saved Content Field: ").append(savedContent != null ? "EXISTS" : "NULL").append("\n");
                    result.append("Content Summary Field: ").append(contentSummary != null ? "EXISTS" : "NULL").append("\n");
                } catch (Exception e) {
                    result.append("FAILED - V7 fields not accessible: ").append(e.getMessage()).append("\n");
                }
            }
            
            return new ResponseEntity<>(result.toString(), HttpStatus.OK);
            
        } catch (Exception e) {
            return new ResponseEntity<>("Migration check failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Manual database migration endpoint (use only if V7 didn't execute)
     */
    @PostMapping("/debug/database/run-migration-v7")
    public ResponseEntity<?> runMigrationV7() {
        try {
            System.out.println("=== RUNNING MANUAL MIGRATION V7 ===");
            
            // Use the injected DataSource
            if (dataSource != null) {
                try (java.sql.Connection conn = dataSource.getConnection();
                     java.sql.Statement stmt = conn.createStatement()) {
                    
                    // Check if columns already exist
                    try (java.sql.ResultSet rs = stmt.executeQuery("SHOW COLUMNS FROM post_versions LIKE 'saved_content'")) {
                        if (rs.next()) {
                            return new ResponseEntity<>("Migration V7 already executed - saved_content column exists", HttpStatus.OK);
                        }
                    }
                    
                    // Execute the migration
                    stmt.executeUpdate("ALTER TABLE post_versions ADD COLUMN saved_content LONGTEXT COMMENT 'JSON array of PostContent objects saved at this version'");
                    stmt.executeUpdate("ALTER TABLE post_versions ADD COLUMN content_summary TEXT COMMENT 'Brief summary of content for display purposes'");
                    stmt.executeUpdate("CREATE INDEX idx_post_versions_content_summary ON post_versions(content_summary(100))");
                    
                    return new ResponseEntity<>("✅ Migration V7 executed successfully!", HttpStatus.OK);
                }
            }
            
            return new ResponseEntity<>("❌ Could not get database connection", HttpStatus.INTERNAL_SERVER_ERROR);
            
        } catch (Exception e) {
            System.err.println("Manual migration error: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("❌ Migration failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Debug endpoint to test version system without authentication
     */
    @GetMapping("/debug/versions/test")
    public ResponseEntity<?> debugVersionSystem() {
        try {
            System.out.println("=== DEBUG VERSION SYSTEM ===");
            
            // Test if PostVersionService is properly injected
            if (postVersionService == null) {
                return new ResponseEntity<>("PostVersionService is null", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            
            // Test database connection by trying to count versions for post 1
            Long count = postVersionService.getVersionCount(1L);
            System.out.println("Version count for post 1: " + count);
            
            return new ResponseEntity<>("Version system working. Count for post 1: " + count, HttpStatus.OK);
            
        } catch (Exception e) {
            System.err.println("DEBUG VERSION ERROR: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Debug error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Debug endpoint to check if versions are saving content properly
     */
    @GetMapping("/debug/versions/{postId}")
    public ResponseEntity<?> debugVersionContent(@PathVariable Long postId) {
        try {
            System.out.println("=== DEBUG VERSION CONTENT FOR POST " + postId + " ===");
            
            List<com.aiautoposter.entity.PostVersion> versions = postVersionService.getVersionHistory(postId);
            
            StringBuilder debug = new StringBuilder();
            debug.append("Post ID: ").append(postId).append("\n");
            debug.append("Total versions: ").append(versions.size()).append("\n\n");
            
            for (int i = 0; i < versions.size(); i++) {
                com.aiautoposter.entity.PostVersion version = versions.get(i);
                debug.append("=== VERSION ").append(i + 1).append(" ===\n");
                debug.append("ID: ").append(version.getId()).append("\n");
                debug.append("Type: ").append(version.getVersionType()).append("\n");
                debug.append("Description: ").append(version.getChangeDescription()).append("\n");
                debug.append("Title: ").append(version.getTitle()).append("\n");
                debug.append("Saved Content: ").append(version.getSavedContent() != null ? "YES (" + version.getSavedContent().length() + " chars)" : "NO").append("\n");
                debug.append("Content Summary: ").append(version.getContentSummary()).append("\n");
                debug.append("Created: ").append(version.getCreatedAt()).append("\n\n");
            }
            
            return new ResponseEntity<>(debug.toString(), HttpStatus.OK);
            
        } catch (Exception e) {
            System.err.println("DEBUG VERSION CONTENT ERROR: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Debug error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Debug endpoint to check current post content
     */
    @GetMapping("/debug/post/{postId}/current-content")
    public ResponseEntity<?> debugCurrentContent(@PathVariable Long postId) {
        try {
            System.out.println("=== DEBUG CURRENT CONTENT FOR POST " + postId + " ===");
            
            List<com.aiautoposter.entity.PostContent> contents = postService.getPostContents(postId);
            
            StringBuilder debug = new StringBuilder();
            debug.append("Post ID: ").append(postId).append("\n");
            debug.append("Current content items: ").append(contents.size()).append("\n\n");
            
            for (int i = 0; i < contents.size(); i++) {
                com.aiautoposter.entity.PostContent content = contents.get(i);
                debug.append("=== CONTENT ").append(i + 1).append(" ===\n");
                debug.append("ID: ").append(content.getId()).append("\n");
                debug.append("Platform: ").append(content.getPlatform()).append("\n");
                debug.append("Title: ").append(content.getTitle()).append("\n");
                debug.append("Content: ").append(content.getContent().substring(0, Math.min(200, content.getContent().length()))).append("...\n");
                debug.append("Hashtags: ").append(content.getHashtags()).append("\n");
                debug.append("Created: ").append(content.getCreatedAt()).append("\n\n");
            }
            
            return new ResponseEntity<>(debug.toString(), HttpStatus.OK);
            
        } catch (Exception e) {
            System.err.println("DEBUG CURRENT CONTENT ERROR: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Debug error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
