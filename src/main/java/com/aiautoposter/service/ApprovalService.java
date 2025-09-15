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
        // Find the manager of the post creator
        User creator = userRepository.findById(post.getCreatedBy())
                .orElseThrow(() -> new RuntimeException("Post creator not found"));
        
        if (creator.getManagerId() == null) {
            throw new RuntimeException("Post creator has no manager assigned");
        }
        
        ApprovalRequest approvalRequest = new ApprovalRequest(post.getId(), creator.getManagerId());
        ApprovalRequest savedRequest = approvalRequestRepository.save(approvalRequest);
        
        // Send notification to manager
        notificationService.createNotification(
            post.getId(),
            creator.getManagerId(),
            com.aiautoposter.entity.Notification.NotificationType.APPROVAL_REQUEST,
            String.format("New post '%s' requires your approval", post.getTitle())
        );
        
        return savedRequest;
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
