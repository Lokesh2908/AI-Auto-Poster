package com.aiautoposter.service;

import com.aiautoposter.entity.ApprovalRequest;
import com.aiautoposter.entity.Post;
import com.aiautoposter.entity.User;
import com.aiautoposter.repository.ApprovalRequestRepository;
import com.aiautoposter.repository.PostRepository;
import com.aiautoposter.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ApprovalService {
    
    @Autowired
    private ApprovalRequestRepository approvalRequestRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    public ApprovalRequest createApprovalRequest(Post post) {
        try {
            System.out.println("ApprovalService - Creating approval request for post ID: " + post.getId());
            System.out.println("ApprovalService - Post created by user ID: " + post.getCreatedBy());
            
            // Find the manager of the post creator
            User creator = userRepository.findById(post.getCreatedBy())
                    .orElseThrow(() -> new RuntimeException("Post creator not found"));
            
            System.out.println("ApprovalService - Found creator: " + creator.getEmail());
            System.out.println("ApprovalService - Creator's manager ID: " + creator.getManagerId());
            
            if (creator.getManagerId() == null) {
                // Handle users without managers (like admins)
                if ("ADMIN".equals(creator.getRole().toString())) {
                    System.out.println("ApprovalService - Admin user doesn't need approval, auto-approving post");
                    // For admin users, we could auto-approve or skip approval
                    throw new RuntimeException("Admin users don't require approval workflow");
                } else {
                    throw new RuntimeException("Post creator has no manager assigned");
                }
            }
            
            ApprovalRequest approvalRequest = new ApprovalRequest(post.getId(), creator.getManagerId());
            System.out.println("ApprovalService - Created approval request object");
            
            ApprovalRequest savedRequest = approvalRequestRepository.save(approvalRequest);
            System.out.println("ApprovalService - Saved approval request with ID: " + savedRequest.getId());
            
            // Send notification to manager
            try {
                Long managerId = creator.getManagerId();
                if (managerId != null) {
                    System.out.println("ApprovalService - Sending notification to manager ID: " + managerId);
                    notificationService.createNotification(
                        post.getId(),
                        managerId,
                        com.aiautoposter.entity.Notification.NotificationType.APPROVAL_REQUEST,
                        String.format("New post '%s' requires your approval", post.getTitle())
                    );
                    System.out.println("ApprovalService - Notification sent to manager successfully");
                } else {
                    System.err.println("ApprovalService - Cannot send notification: Manager ID is null");
                }
            } catch (Exception e) {
                System.err.println("ApprovalService - Error sending notification: " + e.getMessage());
                e.printStackTrace();
                // Continue without notification
            }
            
            return savedRequest;
            
        } catch (Exception e) {
            System.err.println("ApprovalService - Error creating approval request: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    public ApprovalRequest approvePost(Long postId) {
        ApprovalRequest approvalRequest = approvalRequestRepository.findByPostIdAndStatus(
            postId, ApprovalRequest.ApprovalStatus.PENDING);
        if (approvalRequest == null) {
            throw new RuntimeException("No pending approval request found for this post");
        }
        
        approvalRequest.setStatus(ApprovalRequest.ApprovalStatus.APPROVED);
        approvalRequest.setReviewedAt(LocalDateTime.now());
        
        return approvalRequestRepository.save(approvalRequest);
    }
    
    public ApprovalRequest rejectPost(Long postId, String feedback) {
        ApprovalRequest approvalRequest = approvalRequestRepository.findByPostIdAndStatus(
            postId, ApprovalRequest.ApprovalStatus.PENDING);
        if (approvalRequest == null) {
            throw new RuntimeException("No pending approval request found for this post");
        }
        
        approvalRequest.setStatus(ApprovalRequest.ApprovalStatus.REJECTED);
        approvalRequest.setFeedback(feedback);
        approvalRequest.setReviewedAt(LocalDateTime.now());
        
        return approvalRequestRepository.save(approvalRequest);
    }
    
    public List<ApprovalRequest> getPendingApprovals(Long managerId) {
        return approvalRequestRepository.findByAssignedToAndStatus(
            managerId, ApprovalRequest.ApprovalStatus.PENDING);
    }
    
    public List<ApprovalRequest> getApprovalHistory(Long managerId) {
        return approvalRequestRepository.findByAssignedToOrderByCreatedAtDesc(managerId);
    }
    
    public List<ApprovalRequest> getApprovalRequestsByPost(Long postId) {
        return approvalRequestRepository.findByPostId(postId);
    }
    
    public Optional<ApprovalRequest> findById(Long id) {
        return approvalRequestRepository.findById(id);
    }
    
    public List<ApprovalRequest> findAll() {
        return approvalRequestRepository.findAll();
    }
    
    public Long countPendingApprovals(Long managerId) {
        return approvalRequestRepository.countByAssignedToAndStatus(
            managerId, ApprovalRequest.ApprovalStatus.PENDING);
    }
    
    public ApprovalRequest updateApprovalRequest(ApprovalRequest approvalRequest) {
        return approvalRequestRepository.save(approvalRequest);
    }
    
    public void deleteApprovalRequest(Long id) {
        approvalRequestRepository.deleteById(id);
    }
}
