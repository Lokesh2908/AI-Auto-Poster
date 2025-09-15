package com.aiautoposter.service;

import com.aiautoposter.entity.Post;
import com.aiautoposter.entity.PostContent;
import com.aiautoposter.entity.Image;
import com.aiautoposter.entity.AIWorkflowStep;
import com.aiautoposter.repository.PostRepository;
import com.aiautoposter.repository.PostContentRepository;
import com.aiautoposter.repository.ImageRepository;
import com.aiautoposter.repository.AIWorkflowStepRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PostService {
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private PostContentRepository postContentRepository;
    
    @Autowired
    private ImageRepository imageRepository;
    
    @Autowired
    private AIWorkflowStepRepository aiWorkflowStepRepository;
    
    @Autowired
    private AIContentGenerationService aiContentGenerationService;
    
    @Autowired
    private ApprovalService approvalService;
    
    @Autowired
    private NotificationService notificationService;
    
    public Post createPost(Post post) {
        Post savedPost = postRepository.save(post);
        
        // Generate AI content for the post
        generateAIContent(savedPost);
        
        // Create approval request if user has a manager
        if (post.getCreatedBy() != null) {
            approvalService.createApprovalRequest(savedPost);
        }
        
        return savedPost;
    }
    
    public Post updatePost(Post post) {
        return postRepository.save(post);
    }
    
    public Optional<Post> findById(Long id) {
        return postRepository.findById(id);
    }
    
    public List<Post> findByCreatedBy(Long createdBy) {
        return postRepository.findByCreatedBy(createdBy);
    }
    
    public List<Post> findByCurrentStatus(Post.PostStatus status) {
        return postRepository.findByCurrentStatus(status);
    }
    
    public List<Post> findByCreatedByAndCurrentStatus(Long createdBy, Post.PostStatus status) {
        return postRepository.findByCreatedByAndCurrentStatus(createdBy, status);
    }
    
    public List<Post> findAll() {
        return postRepository.findAll();
    }
    
    public List<Post> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return postRepository.findByCreatedAtBetween(startDate, endDate);
    }
    
    public List<Post> findByCurrentStatusIn(List<Post.PostStatus> statuses) {
        return postRepository.findByCurrentStatusIn(statuses);
    }
    
    public Long countByCreatedByAndCurrentStatus(Long createdBy, Post.PostStatus status) {
        return postRepository.countByCreatedByAndCurrentStatus(createdBy, status);
    }
    
    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }
    
    public Post updatePostStatus(Long postId, Post.PostStatus newStatus) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setCurrentStatus(newStatus);
        return postRepository.save(post);
    }
    
    public PostContent addPostContent(PostContent postContent) {
        return postContentRepository.save(postContent);
    }
    
    public List<PostContent> getPostContents(Long postId) {
        return postContentRepository.findByPostId(postId);
    }
    
    public Image addImage(Image image) {
        return imageRepository.save(image);
    }
    
    public List<Image> getImages(Long postId) {
        return imageRepository.findByPostId(postId);
    }
    
    public AIWorkflowStep addAIWorkflowStep(AIWorkflowStep workflowStep) {
        return aiWorkflowStepRepository.save(workflowStep);
    }
    
    public List<AIWorkflowStep> getAIWorkflowSteps(Long postId) {
        return aiWorkflowStepRepository.findByPostId(postId);
    }
    
    private void generateAIContent(Post post) {
        try {
            // Generate content for LinkedIn platform
            String linkedinContent = aiContentGenerationService.generateLinkedInContent(
                post.getSourceDiscussion(), post.getTitle()
            );
            
            // Create post content
            PostContent postContent = new PostContent(
                post.getId(),
                "linkedin",
                post.getTitle(),
                linkedinContent,
                0.85 // AI confidence score
            );
            addPostContent(postContent);
            
            // Record AI workflow step
            AIWorkflowStep workflowStep = new AIWorkflowStep(
                post.getId(),
                AIWorkflowStep.AgentType.CONTENT_GENERATOR,
                1,
                post.getSourceDiscussion(),
                linkedinContent
            );
            addAIWorkflowStep(workflowStep);
            
        } catch (Exception e) {
            // Log error and continue
            System.err.println("Error generating AI content: " + e.getMessage());
        }
    }
    
    public Post submitForApproval(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        post.setCurrentStatus(Post.PostStatus.PENDING_APPROVAL);
        Post updatedPost = postRepository.save(post);
        
        // Create approval request
        approvalService.createApprovalRequest(updatedPost);
        
        // Send notification
        notificationService.createNotification(
            updatedPost.getId(),
            updatedPost.getCreatedBy(),
            com.aiautoposter.entity.Notification.NotificationType.APPROVAL_REQUEST,
            "Your post has been submitted for approval"
        );
        
        return updatedPost;
    }
    
    public Post approvePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        post.setCurrentStatus(Post.PostStatus.APPROVED);
        Post updatedPost = postRepository.save(post);
        
        // Update approval request
        approvalService.approvePost(postId);
        
        // Send notification
        notificationService.createNotification(
            updatedPost.getId(),
            updatedPost.getCreatedBy(),
            com.aiautoposter.entity.Notification.NotificationType.APPROVED,
            "Your post has been approved"
        );
        
        return updatedPost;
    }
    
    public Post rejectPost(Long postId, String feedback) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        post.setCurrentStatus(Post.PostStatus.REJECTED);
        Post updatedPost = postRepository.save(post);
        
        // Update approval request
        approvalService.rejectPost(postId, feedback);
        
        // Send notification
        notificationService.createNotification(
            updatedPost.getId(),
            updatedPost.getCreatedBy(),
            com.aiautoposter.entity.Notification.NotificationType.REJECTED,
            "Your post has been rejected: " + feedback
        );
        
        return updatedPost;
    }
}
