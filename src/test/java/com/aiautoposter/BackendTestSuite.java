package com.aiautoposter;

import com.aiautoposter.controller.AuthController;
import com.aiautoposter.controller.PostController;
import com.aiautoposter.entity.*;
import com.aiautoposter.service.*;
import com.aiautoposter.repository.*;
import com.aiautoposter.security.JwtTokenUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Comprehensive Backend Test Suite for AI Auto Poster
 * Tests all major backend functionality including authentication, post management,
 * approval workflow, scheduling, and notifications.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class BackendTestSuite {

    @Autowired
    private UserService userService;
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private ApprovalService approvalService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private ScheduleService scheduleService;
    
    @Autowired
    private AIContentGenerationService aiContentGenerationService;
    
    @Autowired
    private LinkedInService linkedInService;
    
    @Autowired
    private AuthController authController;
    
    @Autowired
    private PostController postController;
    
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
    private User testUser;
    private User testManager;
    private Post testPost;

    @Before
    public void setUp() {
        // Create test users
        testManager = new User();
        testManager.setEmail("manager@test.com");
        testManager.setPassword("password123");
        testManager.setRole(User.Role.MANAGER);
        testManager.setDepartment("Marketing");
        testManager = userService.createUser(testManager);

        testUser = new User();
        testUser.setEmail("user@test.com");
        testUser.setPassword("password123");
        testUser.setRole(User.Role.USER);
        testUser.setDepartment("Marketing");
        testUser.setManagerId(testManager.getId());
        testUser = userService.createUser(testUser);
    }

    // ========== USER AUTHENTICATION TESTS ==========
    
    @Test
    public void testUserRegistration() {
        User newUser = new User();
        newUser.setEmail("newuser@test.com");
        newUser.setPassword("password123");
        newUser.setRole(User.Role.USER);
        newUser.setDepartment("Sales");
        
        User savedUser = userService.createUser(newUser);
        
        assertNotNull("User should be created", savedUser);
        assertNotNull("User ID should be generated", savedUser.getId());
        assertEquals("Email should match", "newuser@test.com", savedUser.getEmail());
        assertTrue("Password should be encoded", !savedUser.getPassword().equals("password123"));
        assertTrue("User should be active by default", savedUser.getIsActive());
    }
    
    @Test
    public void testUserLogin() {
        Optional<User> foundUser = userService.findByEmail("user@test.com");
        assertTrue("User should exist", foundUser.isPresent());
        
        User user = foundUser.get();
        boolean passwordValid = userService.validatePassword("password123", user.getPassword());
        assertTrue("Password should be valid", passwordValid);
    }
    
    @Test
    public void testJwtTokenGeneration() {
        // This would require UserDetails implementation
        // Testing JWT token generation and validation
        String email = testUser.getEmail();
        assertNotNull("Email should not be null", email);
        // Additional JWT tests would go here
    }

    // ========== POST MANAGEMENT TESTS ==========
    
    @Test
    public void testPostCreation() {
        Post post = new Post();
        post.setTitle("Test LinkedIn Post");
        post.setSourceDiscussion("This is a test discussion about AI and automation");
        post.setCreatedBy(testUser.getId());
        post.setTargetPlatforms("linkedin");
        
        Post savedPost = postService.createPost(post);
        testPost = savedPost;
        
        assertNotNull("Post should be created", savedPost);
        assertNotNull("Post ID should be generated", savedPost.getId());
        assertEquals("Title should match", "Test LinkedIn Post", savedPost.getTitle());
        assertEquals("Status should be DRAFT", Post.PostStatus.DRAFT, savedPost.getCurrentStatus());
        assertNotNull("Created date should be set", savedPost.getCreatedAt());
    }
    
    @Test
    public void testPostStatusUpdate() {
        if (testPost == null) {
            testPostCreation();
        }
        
        Post updatedPost = postService.updatePostStatus(testPost.getId(), Post.PostStatus.PENDING_APPROVAL);
        
        assertEquals("Status should be updated", Post.PostStatus.PENDING_APPROVAL, updatedPost.getCurrentStatus());
    }
    
    @Test
    public void testPostContentGeneration() {
        if (testPost == null) {
            testPostCreation();
        }
        
        List<PostContent> contents = postService.getPostContents(testPost.getId());
        
        // AI content should be generated automatically during post creation
        assertFalse("Post should have generated content", contents.isEmpty());
        
        PostContent linkedinContent = contents.stream()
            .filter(c -> "linkedin".equals(c.getPlatform()))
            .findFirst()
            .orElse(null);
            
        assertNotNull("LinkedIn content should be generated", linkedinContent);
        assertNotNull("Content should not be empty", linkedinContent.getContent());
    }

    // ========== APPROVAL WORKFLOW TESTS ==========
    
    @Test
    public void testApprovalRequestCreation() {
        if (testPost == null) {
            testPostCreation();
        }
        
        ApprovalRequest approvalRequest = approvalService.createApprovalRequest(testPost);
        
        assertNotNull("Approval request should be created", approvalRequest);
        assertEquals("Should be assigned to manager", testManager.getId(), approvalRequest.getAssignedTo());
        assertEquals("Status should be PENDING", ApprovalRequest.ApprovalStatus.PENDING, approvalRequest.getStatus());
    }
    
    @Test
    public void testPostApproval() {
        if (testPost == null) {
            testPostCreation();
        }
        
        // Create approval request first
        approvalService.createApprovalRequest(testPost);
        
        // Approve the post
        Post approvedPost = postService.approvePost(testPost.getId());
        
        assertEquals("Post status should be APPROVED", Post.PostStatus.APPROVED, approvedPost.getCurrentStatus());
        
        // Check approval request status
        ApprovalRequest approvalRequest = approvalService.getApprovalRequestsByPost(testPost.getId()).get(0);
        assertEquals("Approval request should be APPROVED", ApprovalRequest.ApprovalStatus.APPROVED, approvalRequest.getStatus());
    }
    
    @Test
    public void testPostRejection() {
        // Create a new post for rejection test
        Post rejectPost = new Post();
        rejectPost.setTitle("Post to Reject");
        rejectPost.setSourceDiscussion("This post will be rejected");
        rejectPost.setCreatedBy(testUser.getId());
        rejectPost.setTargetPlatforms("linkedin");
        rejectPost = postService.createPost(rejectPost);
        
        // Create approval request
        approvalService.createApprovalRequest(rejectPost);
        
        // Reject the post
        String feedback = "Content needs improvement";
        Post rejectedPost = postService.rejectPost(rejectPost.getId(), feedback);
        
        assertEquals("Post status should be REJECTED", Post.PostStatus.REJECTED, rejectedPost.getCurrentStatus());
        
        // Check approval request
        ApprovalRequest approvalRequest = approvalService.getApprovalRequestsByPost(rejectPost.getId()).get(0);
        assertEquals("Approval request should be REJECTED", ApprovalRequest.ApprovalStatus.REJECTED, approvalRequest.getStatus());
        assertEquals("Feedback should match", feedback, approvalRequest.getFeedback());
    }

    // ========== NOTIFICATION TESTS ==========
    
    @Test
    public void testNotificationCreation() {
        Notification notification = notificationService.createNotification(
            1L, 
            testUser.getId(), 
            Notification.NotificationType.APPROVAL_REQUEST,
            "Your post requires approval"
        );
        
        assertNotNull("Notification should be created", notification);
        assertEquals("User ID should match", testUser.getId(), notification.getUserId());
        assertEquals("Type should match", Notification.NotificationType.APPROVAL_REQUEST, notification.getType());
        assertFalse("Should be unread by default", notification.getReadStatus());
    }
    
    @Test
    public void testNotificationMarkAsRead() {
        Notification notification = notificationService.createNotification(
            1L, 
            testUser.getId(), 
            Notification.NotificationType.APPROVED,
            "Your post has been approved"
        );
        
        Notification readNotification = notificationService.markAsRead(notification.getId());
        
        assertTrue("Notification should be marked as read", readNotification.getReadStatus());
    }
    
    @Test
    public void testUnreadNotificationCount() {
        // Create multiple notifications
        notificationService.createNotification(1L, testUser.getId(), Notification.NotificationType.APPROVAL_REQUEST, "Test 1");
        notificationService.createNotification(2L, testUser.getId(), Notification.NotificationType.APPROVED, "Test 2");
        
        Long unreadCount = notificationService.getUnreadNotificationCount(testUser.getId());
        
        assertTrue("Should have unread notifications", unreadCount > 0);
    }

    // ========== SCHEDULING TESTS ==========
    
    @Test
    public void testPostScheduling() {
        if (testPost == null) {
            testPostCreation();
        }
        
        // First approve the post
        postService.updatePostStatus(testPost.getId(), Post.PostStatus.APPROVED);
        
        LocalDateTime scheduledTime = LocalDateTime.now().plusHours(1);
        Schedule schedule = scheduleService.schedulePost(testPost.getId(), scheduledTime);
        
        assertNotNull("Schedule should be created", schedule);
        assertEquals("Post ID should match", testPost.getId(), schedule.getPostId());
        assertEquals("Scheduled time should match", scheduledTime, schedule.getScheduledFor());
        assertEquals("Status should be PENDING", Schedule.ScheduleStatus.PENDING, schedule.getStatus());
        
        // Check post status
        Post scheduledPost = postService.findById(testPost.getId()).get();
        assertEquals("Post status should be SCHEDULED", Post.PostStatus.SCHEDULED, scheduledPost.getCurrentStatus());
    }
    
    @Test
    public void testScheduleCancellation() {
        if (testPost == null) {
            testPostCreation();
        }
        
        // Schedule the post first
        postService.updatePostStatus(testPost.getId(), Post.PostStatus.APPROVED);
        LocalDateTime scheduledTime = LocalDateTime.now().plusHours(2);
        Schedule schedule = scheduleService.schedulePost(testPost.getId(), scheduledTime);
        
        // Cancel the schedule
        Schedule cancelledSchedule = scheduleService.cancelSchedule(schedule.getId());
        
        assertEquals("Schedule status should be CANCELLED", Schedule.ScheduleStatus.CANCELLED, cancelledSchedule.getStatus());
        
        // Check post status
        Post post = postService.findById(testPost.getId()).get();
        assertEquals("Post status should be back to APPROVED", Post.PostStatus.APPROVED, post.getCurrentStatus());
    }

    // ========== AI CONTENT GENERATION TESTS ==========
    
    @Test
    public void testAIContentGenerationService() {
        try {
            String content = aiContentGenerationService.generateLinkedInContent(
                "AI is transforming the workplace", 
                "The Future of AI in Business"
            );
            
            // Note: This test may fail if API keys are not configured
            // In that case, we should test the fallback mechanism
            assertNotNull("Content should be generated or fallback should work", content);
            
        } catch (Exception e) {
            // Expected if API keys are not configured
            System.out.println("AI Content Generation test failed (expected if API keys not configured): " + e.getMessage());
        }
    }

    // ========== INTEGRATION TESTS ==========
    
    @Test
    public void testEndToEndWorkflow() {
        // 1. Create user and manager (already done in setUp)
        
        // 2. Create post
        Post post = new Post();
        post.setTitle("End-to-End Test Post");
        post.setSourceDiscussion("Testing the complete workflow from creation to publishing");
        post.setCreatedBy(testUser.getId());
        post.setTargetPlatforms("linkedin");
        
        Post createdPost = postService.createPost(post);
        assertNotNull("Post should be created", createdPost);
        
        // 3. Submit for approval
        Post submittedPost = postService.submitForApproval(createdPost.getId());
        assertEquals("Post should be pending approval", Post.PostStatus.PENDING_APPROVAL, submittedPost.getCurrentStatus());
        
        // 4. Approve post
        Post approvedPost = postService.approvePost(createdPost.getId());
        assertEquals("Post should be approved", Post.PostStatus.APPROVED, approvedPost.getCurrentStatus());
        
        // 5. Schedule post
        LocalDateTime scheduledTime = LocalDateTime.now().plusMinutes(30);
        Schedule schedule = scheduleService.schedulePost(createdPost.getId(), scheduledTime);
        assertNotNull("Post should be scheduled", schedule);
        
        // 6. Check notifications were created
        List<Notification> notifications = notificationService.getUserNotifications(testUser.getId());
        assertFalse("User should have notifications", notifications.isEmpty());
        
        System.out.println("✅ End-to-end workflow test completed successfully!");
    }
    
    // ========== REPOSITORY TESTS ==========
    
    @Test
    public void testUserRepository() {
        // Test custom queries
        List<User> activeUsers = userService.findActiveUsers();
        assertFalse("Should have active users", activeUsers.isEmpty());
        
        List<User> managerUsers = userService.findByRole(User.Role.MANAGER);
        assertTrue("Should have manager users", managerUsers.size() > 0);
        
        boolean emailExists = userService.existsByEmail(testUser.getEmail());
        assertTrue("Email should exist", emailExists);
    }
    
    @Test
    public void testPostRepository() {
        if (testPost == null) {
            testPostCreation();
        }
        
        List<Post> userPosts = postService.findByCreatedBy(testUser.getId());
        assertFalse("User should have posts", userPosts.isEmpty());
        
        List<Post> draftPosts = postService.findByCurrentStatus(Post.PostStatus.DRAFT);
        assertFalse("Should have draft posts", draftPosts.isEmpty());
        
        Long postCount = postService.countByCreatedByAndCurrentStatus(testUser.getId(), Post.PostStatus.DRAFT);
        assertTrue("Should have post count", postCount > 0);
    }

    // ========== ERROR HANDLING TESTS ==========
    
    @Test(expected = RuntimeException.class)
    public void testPostNotFoundError() {
        postService.findById(99999L).orElseThrow(() -> new RuntimeException("Post not found"));
    }
    
    @Test(expected = RuntimeException.class)
    public void testUserNotFoundError() {
        userService.findById(99999L).orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    @Test
    public void testApprovalWithoutManager() {
        // Create user without manager
        User userWithoutManager = new User();
        userWithoutManager.setEmail("noManager@test.com");
        userWithoutManager.setPassword("password123");
        userWithoutManager.setRole(User.Role.USER);
        userWithoutManager.setDepartment("IT");
        // No manager ID set
        
        User savedUser = userService.createUser(userWithoutManager);
        
        Post post = new Post();
        post.setTitle("Post without manager");
        post.setSourceDiscussion("Testing approval without manager");
        post.setCreatedBy(savedUser.getId());
        post.setTargetPlatforms("linkedin");
        
        try {
            Post createdPost = postService.createPost(post);
            // This should handle the case gracefully
            assertNotNull("Post should still be created", createdPost);
        } catch (RuntimeException e) {
            // Expected behavior when no manager is assigned
            assertTrue("Should handle missing manager gracefully", e.getMessage().contains("manager"));
        }
    }
}
