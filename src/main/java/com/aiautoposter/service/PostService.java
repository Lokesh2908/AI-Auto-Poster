package com.aiautoposter.service;

import com.aiautoposter.entity.Post;
import com.aiautoposter.entity.PostContent;
import com.aiautoposter.entity.Image;
import com.aiautoposter.entity.AIWorkflowStep;
import com.aiautoposter.entity.User;
import com.aiautoposter.entity.PostMedia;
import com.aiautoposter.entity.Media;
import com.aiautoposter.repository.PostRepository;
import com.aiautoposter.repository.PostContentRepository;
import com.aiautoposter.repository.ImageRepository;
import com.aiautoposter.repository.AIWorkflowStepRepository;
import com.aiautoposter.repository.UserRepository;
import com.aiautoposter.repository.PostMediaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Transactional
public class PostService {
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private AIContentGenerationService aiContentGenerationService;
    
    @Autowired
    private ApprovalService approvalService;
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * Get a post by its ID
     * @param postId the ID of the post to retrieve
     * @return the Post entity if found, null otherwise
     */
    public Post getPostById(Long postId) {
        if (postId == null) {
            return null;
        }
        return postRepository.findById(postId).orElse(null);
    }
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostMediaRepository postMediaRepository;
    
    @Autowired
    private PostContentRepository postContentRepository;
    
    @Autowired
    private ImageRepository imageRepository;
    
    @Autowired
    private AIWorkflowStepRepository aiWorkflowStepRepository;
    
    /**
     * Get all posts for a specific user by their username (email)
     * @param username The email of the user
     * @return List of posts created by the user, or empty list if user not found
     */
    public List<Post> getPostsByUsername(String username) {
        Optional<User> userOptional = userRepository.findByEmail(username);
        if (userOptional.isPresent()) {
            return postRepository.findByCreatedByOrderByCreatedAtDesc(userOptional.get().getId());
        }
        return List.of();
    }
    
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
        System.out.println("Note: Content not regenerated - use regeneratePostContent() method for that");
        
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
    
    // Media attachment methods
    public PostMedia attachMediaToPost(Long postId, Long mediaId, Integer displayOrder) {
        PostMedia postMedia = new PostMedia(postId, mediaId, displayOrder);
        return postMediaRepository.save(postMedia);
    }
    
    public void detachMediaFromPost(Long postId, Long mediaId) {
        postMediaRepository.deleteByPostIdAndMediaId(postId, mediaId);
    }
    
    public List<PostMedia> getPostMedia(Long postId) {
        return postMediaRepository.findByPostIdWithMediaOrderByDisplayOrder(postId);
    }
    
    public AIWorkflowStep addAIWorkflowStep(AIWorkflowStep workflowStep) {
        return aiWorkflowStepRepository.save(workflowStep);
    }
    
    public List<AIWorkflowStep> getAIWorkflowSteps(Long postId) {
        return aiWorkflowStepRepository.findByPostId(postId);
    }
    
    private void generateAIContent(Post post) {
        try {
            System.out.println("PostService - Generating AI content for platforms: " + post.getTargetPlatforms());
            
            // Parse target platforms (comma-separated string)
            String[] platforms = post.getTargetPlatforms().split(",");
            
            for (String platform : platforms) {
                platform = platform.trim().toLowerCase();
                System.out.println("PostService - Generating content for platform: " + platform);
                
                String content;
                if ("linkedin".equals(platform)) {
                    // Generate LinkedIn content (plain text friendly)
                    content = aiContentGenerationService.generateLinkedInContent(
                        post.getSourceDiscussion(), post.getTitle()
                    );
                    // Convert any HTML to plain text for LinkedIn
                    content = convertHtmlToLinkedInText(content);
                } else if ("wordpress".equals(platform)) {
                    // Generate WordPress content (can include HTML)
                    content = generateWordPressContent(post.getSourceDiscussion(), post.getTitle());
                } else {
                    // Default content generation
                    content = aiContentGenerationService.generateLinkedInContent(
                        post.getSourceDiscussion(), post.getTitle()
                    );
                }
                
                // Extract hashtags from the content
                String hashtags = extractHashtags(content);
                
                // Create post content for this platform
                PostContent postContent = new PostContent(
                    post.getId(),
                    platform,
                    post.getTitle(),
                    content,
                    0.85 // AI confidence score
                );
                // TODO: Add hashtags back after database is updated
                // postContent.setHashtags(hashtags);
                addPostContent(postContent);
                
                System.out.println("PostService - Created content for " + platform + " platform");
            }
            
            // Record AI workflow step (use first platform for workflow tracking)
            String firstPlatform = platforms[0].trim();
            List<PostContent> allContent = postContentRepository.findByPostId(post.getId());
            String combinedContent = allContent.stream()
                .map(PostContent::getContent)
                .reduce("", (a, b) -> a + "\n\n--- " + b);
                
            AIWorkflowStep workflowStep = new AIWorkflowStep(
                post.getId(),
                AIWorkflowStep.AgentType.CONTENT_GENERATOR,
                1,
                post.getSourceDiscussion(),
                combinedContent
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
    
    public Post regeneratePostContent(Long postId, Long userId) {
        try {
            System.out.println("PostService - Regenerating content for post ID: " + postId);
            
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("Post not found"));
            
            System.out.println("PostService - Found post: " + post.getTitle());
            System.out.println("PostService - Current status: " + post.getCurrentStatus());
            
            // Verify user has permission to regenerate this post
            if (!post.getCreatedBy().equals(userId)) {
                throw new RuntimeException("User does not have permission to regenerate this post");
            }
            
            // Delete existing content for this post
            System.out.println("PostService - Deleting existing content...");
            List<PostContent> existingContent = postContentRepository.findByPostId(postId);
            postContentRepository.deleteAll(existingContent);
            System.out.println("PostService - Deleted " + existingContent.size() + " existing content items");
            
            // Generate new AI content
            System.out.println("PostService - Generating new AI content...");
            try {
                generateAIContent(post);
                System.out.println("PostService - AI content generation completed");
            } catch (Exception e) {
                System.err.println("PostService - Error generating AI content: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Failed to generate new content: " + e.getMessage());
            }
            
            // Reset post status to DRAFT if it was rejected
            if (post.getCurrentStatus() == Post.PostStatus.REJECTED) {
                System.out.println("PostService - Resetting rejected post status to DRAFT");
                post.setCurrentStatus(Post.PostStatus.DRAFT);
                post = postRepository.save(post);
            }
            
            System.out.println("PostService - Content regeneration completed successfully");
            return post;
            
        } catch (Exception e) {
            System.err.println("PostService - Error in regeneratePostContent: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    public PostContent updatePostContent(Long postId, Long contentId, Map<String, Object> contentData, Long userId) {
        try {
            System.out.println("PostService - Updating content ID: " + contentId + " for post ID: " + postId);
            
            // Verify the post exists and user has permission
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("Post not found"));
            
            if (!post.getCreatedBy().equals(userId)) {
                throw new RuntimeException("User does not have permission to update this post content");
            }
            
            // Find the existing content
            PostContent existingContent = postContentRepository.findById(contentId)
                    .orElseThrow(() -> new RuntimeException("Post content not found"));
            
            // Verify the content belongs to the specified post
            if (!existingContent.getPostId().equals(postId)) {
                throw new RuntimeException("Content does not belong to the specified post");
            }
            
            // Update the content fields
            if (contentData.containsKey("content")) {
                existingContent.setContent((String) contentData.get("content"));
                System.out.println("PostService - Updated content text");
            }
            
            if (contentData.containsKey("title")) {
                existingContent.setTitle((String) contentData.get("title"));
                System.out.println("PostService - Updated content title");
            }
            
            if (contentData.containsKey("hashtags")) {
                existingContent.setHashtags((String) contentData.get("hashtags"));
                System.out.println("PostService - Updated content hashtags");
            }
            
            // Save the updated content
            PostContent savedContent = postContentRepository.save(existingContent);
            
            System.out.println("PostService - Content updated successfully");
            return savedContent;
            
        } catch (Exception e) {
            System.err.println("PostService - Error updating post content: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    // Utility method to convert HTML content to LinkedIn-friendly text
    public String convertHtmlToLinkedInText(String htmlContent) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return htmlContent;
        }
        
        try {
            System.out.println("PostService - Converting HTML to LinkedIn text");
            System.out.println("Original HTML: " + htmlContent);
            
            String result = htmlContent;
            
            // Convert common HTML formatting to LinkedIn equivalents
            // Bold text: <strong> or <b> -> keep text, add emphasis with formatting
            result = result.replaceAll("</?strong>", "**");
            result = result.replaceAll("</?b>", "**");
            
            // Italic text: <em> or <i> -> keep text, could use underscores but LinkedIn doesn't support
            result = result.replaceAll("</?em>", "");
            result = result.replaceAll("</?i>", "");
            
            // Headings: <h1>, <h2>, etc. -> add line breaks and emphasis
            result = result.replaceAll("<h[1-6][^>]*>", "\n\n**");
            result = result.replaceAll("</h[1-6]>", "**\n");
            
            // Paragraphs: <p> -> add line breaks
            result = result.replaceAll("<p[^>]*>", "");
            result = result.replaceAll("</p>", "\n\n");
            
            // Line breaks: <br> -> newline
            result = result.replaceAll("<br[^>]*>", "\n");
            
            // Lists: <ul>, <ol>, <li>
            result = result.replaceAll("<ul[^>]*>", "");
            result = result.replaceAll("</ul>", "\n");
            result = result.replaceAll("<ol[^>]*>", "");
            result = result.replaceAll("</ol>", "\n");
            result = result.replaceAll("<li[^>]*>", "• ");
            result = result.replaceAll("</li>", "\n");
            
            // Links: <a href="url">text</a> -> text (url)
            Pattern linkPattern = Pattern.compile("<a[^>]*href=[\"']([^\"']*)[\"'][^>]*>([^<]*)</a>");
            result = linkPattern.matcher(result).replaceAll("$2 ($1)");
            
            // Remove any remaining HTML tags
            result = result.replaceAll("<[^>]+>", "");
            
            // Clean up extra whitespace and line breaks
            result = result.replaceAll("\\n\\s*\\n\\s*\\n", "\n\n"); // Max 2 consecutive line breaks
            result = result.replaceAll("^\\s+|\\s+$", ""); // Trim start and end
            result = result.replaceAll("[ \\t]+", " "); // Multiple spaces to single space
            
            // Decode HTML entities
            result = result.replaceAll("&nbsp;", " ");
            result = result.replaceAll("&amp;", "&");
            result = result.replaceAll("&lt;", "<");
            result = result.replaceAll("&gt;", ">");
            result = result.replaceAll("&quot;", "\"");
            result = result.replaceAll("&#39;", "'");
            
            System.out.println("Converted text: " + result);
            return result;
            
        } catch (Exception e) {
            System.err.println("PostService - Error converting HTML to text: " + e.getMessage());
            // Return original content if conversion fails
            return htmlContent;
        }
    }
    
    // Method to get LinkedIn-ready content (strips HTML)
    public String getLinkedInReadyContent(Long contentId) {
        try {
            PostContent content = postContentRepository.findById(contentId)
                    .orElseThrow(() -> new RuntimeException("Content not found"));
            
            return convertHtmlToLinkedInText(content.getContent());
        } catch (Exception e) {
            System.err.println("PostService - Error getting LinkedIn-ready content: " + e.getMessage());
            throw e;
        }
    }
    
    // Generate WordPress-specific content (can include HTML formatting)
    private String generateWordPressContent(String sourceDiscussion, String title) {
        try {
            System.out.println("PostService - Generating WordPress content");
            
            // For now, use the same AI service but format for WordPress
            String baseContent = aiContentGenerationService.generateLinkedInContent(sourceDiscussion, title);
            
            // Format content for WordPress (add some HTML structure)
            String wordpressContent = formatContentForWordPress(baseContent, title);
            
            System.out.println("PostService - WordPress content generated");
            return wordpressContent;
            
        } catch (Exception e) {
            System.err.println("PostService - Error generating WordPress content: " + e.getMessage());
            // Fallback to basic content
            return String.format("<h2>%s</h2>\n\n<p>%s</p>", title, sourceDiscussion);
        }
    }
    
    // Format content specifically for WordPress with HTML structure
    private String formatContentForWordPress(String content, String title) {
        try {
            // Basic WordPress formatting
            StringBuilder formatted = new StringBuilder();
            
            // Add title as H2 if not already present
            if (!content.toLowerCase().contains("<h") && !content.toLowerCase().contains(title.toLowerCase())) {
                formatted.append("<h2>").append(title).append("</h2>\n\n");
            }
            
            // Split content into paragraphs and wrap in <p> tags
            String[] paragraphs = content.split("\n\n");
            for (String paragraph : paragraphs) {
                paragraph = paragraph.trim();
                if (!paragraph.isEmpty()) {
                    // Check if it's already wrapped in HTML tags
                    if (!paragraph.startsWith("<")) {
                        // Convert bullet points to HTML lists
                        if (paragraph.contains("•") || paragraph.contains("-")) {
                            String[] lines = paragraph.split("\n");
                            boolean inList = false;
                            for (String line : lines) {
                                line = line.trim();
                                if (line.startsWith("•") || line.startsWith("-")) {
                                    if (!inList) {
                                        formatted.append("<ul>\n");
                                        inList = true;
                                    }
                                    formatted.append("<li>").append(line.substring(1).trim()).append("</li>\n");
                                } else {
                                    if (inList) {
                                        formatted.append("</ul>\n\n");
                                        inList = false;
                                    }
                                    if (!line.isEmpty()) {
                                        formatted.append("<p>").append(line).append("</p>\n\n");
                                    }
                                }
                            }
                            if (inList) {
                                formatted.append("</ul>\n\n");
                            }
                        } else {
                            // Regular paragraph
                            formatted.append("<p>").append(paragraph).append("</p>\n\n");
                        }
                    } else {
                        // Already has HTML tags
                        formatted.append(paragraph).append("\n\n");
                    }
                }
            }
            
            return formatted.toString().trim();
            
        } catch (Exception e) {
            System.err.println("PostService - Error formatting WordPress content: " + e.getMessage());
            return "<p>" + content + "</p>";
        }
    }
}
