package com.aiautoposter.service;

import com.aiautoposter.entity.Post;
import com.aiautoposter.entity.PostContent;
import com.aiautoposter.entity.Image;
import com.aiautoposter.entity.AIWorkflowStep;
import com.aiautoposter.entity.User;
import com.aiautoposter.repository.PostRepository;
import com.aiautoposter.repository.PostContentRepository;
import com.aiautoposter.repository.ImageRepository;
import com.aiautoposter.repository.AIWorkflowStepRepository;
import com.aiautoposter.repository.UserRepository;
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
    
    @Autowired
    private UserRepository userRepository;
    public Post createPost(Post post) {
        Post savedPost = postRepository.save(post);
        
        // Generate AI content for the post
        generateAIContent(savedPost);
        
        // Create approval request if user has a manager
//        if (post.getCreatedBy() != null) {
//            approvalService.createApprovalRequest(savedPost);
//        }
        
        return savedPost;
    }
    
    public Post updatePost(Post post) {
        System.out.println("=== UPDATE POST SERVICE ===");
        System.out.println("Input post ID: " + post.getId());
        System.out.println("Input post title: " + post.getTitle());
        
        // Find the existing post first
        Post existingPost = postRepository.findById(post.getId())
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + post.getId()));
        
        System.out.println("Found existing post ID: " + existingPost.getId());
        System.out.println("Existing post createdBy: " + existingPost.getCreatedBy());
        System.out.println("Existing post createdAt: " + existingPost.getCreatedAt());
        
        // Update only the fields that should change
        existingPost.setTitle(post.getTitle());
        existingPost.setSourceDiscussion(post.getSourceDiscussion());
        existingPost.setTargetPlatforms(post.getTargetPlatforms());
        existingPost.setUpdatedAt(LocalDateTime.now());
        
        // Save the updated existing post (this will update, not create new)
        Post savedPost = postRepository.save(existingPost);
        
        System.out.println("Saved post ID: " + savedPost.getId());
        System.out.println("Saved post createdBy: " + savedPost.getCreatedBy());
        
        // Delete existing AI content and regenerate
        deleteExistingPostContent(savedPost.getId());
        generateAIContent(savedPost);
        
        System.out.println("=== END UPDATE POST SERVICE ===");
        return savedPost;
    }
    
    private void deleteExistingPostContent(Long postId) {
        try {
            List<PostContent> existingContent = postContentRepository.findByPostId(postId);
            if (!existingContent.isEmpty()) {
                System.out.println("Deleting " + existingContent.size() + " existing content items for post " + postId);
                postContentRepository.deleteAll(existingContent);
            }
        } catch (Exception e) {
            System.err.println("Error deleting existing post content: " + e.getMessage());
        }
    }
    
    public Optional<Post> findById(Long id) {
        return postRepository.findById(id);
    }
    
    public List<Post> findByCreatedBy(Long userId) {
        return postRepository.findByCreatedBy(userId);
    }
    
    public List<Post> findPostsByManager(Long managerId) {
        try {
            System.out.println("PostService - Finding posts for manager ID: " + managerId);
            
            // Find all users who report to this manager
            List<User> teamMembers = userRepository.findByManagerId(managerId);
            System.out.println("PostService - Found " + teamMembers.size() + " team members for manager " + managerId);
            
            List<Post> allPosts = new java.util.ArrayList<>();
            
            // Get posts from all team members
            for (User teamMember : teamMembers) {
                System.out.println("PostService - Getting posts for team member: " + teamMember.getEmail() + " (ID: " + teamMember.getId() + ")");
                List<Post> memberPosts = postRepository.findByCreatedBy(teamMember.getId());
                System.out.println("PostService - Found " + memberPosts.size() + " posts for " + teamMember.getEmail());
                allPosts.addAll(memberPosts);
            }
            
            // Also include manager's own posts
            System.out.println("PostService - Getting manager's own posts");
            List<Post> managerPosts = postRepository.findByCreatedBy(managerId);
            System.out.println("PostService - Found " + managerPosts.size() + " posts for manager");
            allPosts.addAll(managerPosts);
            
            System.out.println("PostService - Total posts for manager: " + allPosts.size());
            return allPosts;
            
        } catch (Exception e) {
            System.err.println("PostService - Error finding posts by manager: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
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
        try {
            System.out.println("PostService - Getting contents for post ID: " + postId);
            List<PostContent> contents = postContentRepository.findByPostId(postId);
            System.out.println("PostService - Repository returned " + contents.size() + " items");
            return contents;
        } catch (Exception e) {
            System.err.println("PostService ERROR - Failed to get contents for post ID: " + postId);
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
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
            // Extract hashtags from the content
            String hashtags = extractHashtags(linkedinContent);
            
            // Create post content (temporarily without hashtags)
            PostContent postContent = new PostContent(
                post.getId(),
                "linkedin",
                post.getTitle(),
                linkedinContent,
                0.85 // AI confidence score
            );
            // TODO: Add hashtags back after database is updated
            // postContent.setHashtags(hashtags);
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
    
    private String extractHashtags(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        
        // Extract hashtags using regex
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("#\\w+");
        java.util.regex.Matcher matcher = pattern.matcher(content);
        
        StringBuilder hashtags = new StringBuilder();
        while (matcher.find()) {
            if (hashtags.length() > 0) {
                hashtags.append(" ");
            }
            hashtags.append(matcher.group());
        }
        
        return hashtags.toString();
    }
    
    public Post submitForApproval(Long postId) {
        try {
            System.out.println("PostService - Submitting post ID: " + postId + " for approval");
            
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("Post not found"));
            
            System.out.println("PostService - Found post: " + post.getTitle());
            System.out.println("PostService - Current status: " + post.getCurrentStatus());
            System.out.println("PostService - Created by: " + post.getCreatedBy());
            
            // Fix posts that don't have createdBy set
            if (post.getCreatedBy() == null) {
                System.err.println("PostService - WARNING: Post has null createdBy, this will cause approval workflow to fail");
                System.err.println("PostService - This post needs to be updated with a valid createdBy value");
                throw new RuntimeException("Post has no creator assigned. Please update the post with a valid creator.");
            }
            
            post.setCurrentStatus(Post.PostStatus.PENDING_APPROVAL);
            Post updatedPost = postRepository.save(post);
            
            System.out.println("PostService - Updated post status to: " + updatedPost.getCurrentStatus());
            
            // Create approval request (only for users who need approval)
            try {
                System.out.println("PostService - Creating approval request...");
                
                // Check if user needs approval workflow
                User creator = userRepository.findById(updatedPost.getCreatedBy()).orElse(null);
                if (creator != null && creator.getManagerId() != null) {
                    approvalService.createApprovalRequest(updatedPost);
                    System.out.println("PostService - Approval request created successfully");
                } else if (creator != null && "ADMIN".equals(creator.getRole().toString())) {
                    System.out.println("PostService - Admin user, auto-approving post");
                    updatedPost.setCurrentStatus(Post.PostStatus.APPROVED);
                    updatedPost = postRepository.save(updatedPost);
                } else {
                    System.err.println("PostService - User has no manager, skipping approval workflow");
                }
            } catch (Exception e) {
                System.err.println("PostService - Error creating approval request: " + e.getMessage());
                e.printStackTrace();
                // Continue without approval request for now
            }
            
            // Send notification to the post creator
            try {
                System.out.println("PostService - Creating notification for post creator...");
                if (updatedPost.getCreatedBy() != null) {
                    notificationService.createNotification(
                        updatedPost.getId(),
                        updatedPost.getCreatedBy(),
                        com.aiautoposter.entity.Notification.NotificationType.APPROVAL_REQUEST,
                        "Your post has been submitted for approval"
                    );
                    System.out.println("PostService - Notification created successfully for creator");
                } else {
                    System.err.println("PostService - Cannot create notification: createdBy is null");
                }
            } catch (Exception e) {
                System.err.println("PostService - Error creating notification: " + e.getMessage());
                // Continue without notification for now
            }
            
            System.out.println("PostService - Submit for approval completed successfully");
            return updatedPost;
            
        } catch (Exception e) {
            System.err.println("PostService - Error in submitForApproval: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    public Post approvePost(Long postId) {
        return approvePost(postId, null);
    }
    
    public Post approvePost(Long postId, Long approverId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        post.setCurrentStatus(Post.PostStatus.APPROVED);
        Post updatedPost = postRepository.save(post);
        
        // Update approval request
        approvalService.approvePost(postId);
        
        notificationService.createNotification(
            updatedPost.getId(),
            updatedPost.getCreatedBy(),
            com.aiautoposter.entity.Notification.NotificationType.APPROVED,
            "Your post has been approved",
            approverId, // approvedBy
            null,       // rejectedBy
            "Post approved", // approvalFeedback
            null        // rejectionFeedback
        );
        
        return updatedPost;
    }
    
    public Post rejectPost(Long postId, String feedback) {
        return rejectPost(postId, feedback, null);
    }
    
    public Post rejectPost(Long postId, String feedback, Long rejecterId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        post.setCurrentStatus(Post.PostStatus.REJECTED);
        Post updatedPost = postRepository.save(post);
        
        // Update approval request
        approvalService.rejectPost(postId, feedback);
        notificationService.createNotification(
            updatedPost.getId(),
            updatedPost.getCreatedBy(),
            com.aiautoposter.entity.Notification.NotificationType.REJECTED,
            "Your post has been rejected: " + feedback,
            null,       // approvedBy
            rejecterId, // rejectedBy
            null,       // approvalFeedback
            feedback    // rejectionFeedback
        );
        
        return updatedPost;
    }
}
