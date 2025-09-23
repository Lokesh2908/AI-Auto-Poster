package com.aiautoposter.controller;

import com.aiautoposter.entity.Post;
import com.aiautoposter.entity.PostContent;
import com.aiautoposter.entity.User;
import com.aiautoposter.service.PostService;
import com.aiautoposter.service.UserService;
import com.aiautoposter.security.JwtTokenUtil;
import com.aiautoposter.entity.Image;
import com.aiautoposter.entity.Image;
import com.aiautoposter.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "*")
public class PostController {
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
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
}
