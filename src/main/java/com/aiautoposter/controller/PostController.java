package com.aiautoposter.controller;

import com.aiautoposter.entity.Post;
import com.aiautoposter.entity.PostContent;
import com.aiautoposter.entity.Image;
import com.aiautoposter.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            post.setId(id);
            Post updatedPost = postService.updatePost(post);
            return new ResponseEntity<>(updatedPost, HttpStatus.OK);
        } catch (Exception e) {
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
    public ResponseEntity<Post> submitForApproval(@PathVariable Long id) {
        try {
            Post post = postService.submitForApproval(id);
            return new ResponseEntity<>(post, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @PostMapping("/{id}/approve")
    public ResponseEntity<Post> approvePost(@PathVariable Long id) {
        try {
            Post post = postService.approvePost(id);
            return new ResponseEntity<>(post, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @PostMapping("/{id}/reject")
    public ResponseEntity<Post> rejectPost(@PathVariable Long id, @RequestParam String feedback) {
        try {
            Post post = postService.rejectPost(id, feedback);
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
    public ResponseEntity<List<PostContent>> getPostContents(@PathVariable Long id) {
        List<PostContent> contents = postService.getPostContents(id);
        return new ResponseEntity<>(contents, HttpStatus.OK);
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
}
