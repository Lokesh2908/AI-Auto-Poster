package com.aiautoposter.controller;

import com.aiautoposter.entity.ApprovalRequest;
import com.aiautoposter.service.ApprovalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/approvals")
@CrossOrigin(origins = "*")
public class ApprovalController {
    
    @Autowired
    private ApprovalService approvalService;
    
    @GetMapping
    public ResponseEntity<List<java.util.Map<String, Object>>> getAllApprovalRequests() {
        try {
            System.out.println("=== GET ALL APPROVALS DEBUG (ADMIN) ===");
            
            List<ApprovalRequest> requests = approvalService.findAll();
            System.out.println("Found " + requests.size() + " total approval requests");
            
            // Convert to safe JSON format to avoid Hibernate serialization issues
            List<java.util.Map<String, Object>> safeRequests = new java.util.ArrayList<>();
            
            for (ApprovalRequest request : requests) {
                System.out.println("Approval Request ID: " + request.getId() + 
                                 ", Post ID: " + request.getPostId() + 
                                 ", Status: " + request.getStatus() +
                                 ", Assigned To: " + request.getAssignedTo());
                
                java.util.Map<String, Object> safeRequest = new java.util.HashMap<>();
                safeRequest.put("id", request.getId());
                safeRequest.put("postId", request.getPostId());
                safeRequest.put("assignedTo", request.getAssignedTo());
                safeRequest.put("status", request.getStatus().toString());
                safeRequest.put("createdAt", request.getCreatedAt());
                safeRequest.put("reviewedAt", request.getReviewedAt());
                safeRequest.put("feedback", request.getFeedback());
                
                // Get post details safely
                try {
                    com.aiautoposter.entity.Post post = request.getPost();
                    if (post != null) {
                        java.util.Map<String, Object> postData = new java.util.HashMap<>();
                        postData.put("id", post.getId());
                        postData.put("title", post.getTitle());
                        postData.put("sourceDiscussion", post.getSourceDiscussion());
                        postData.put("targetPlatforms", post.getTargetPlatforms());
                        postData.put("currentStatus", post.getCurrentStatus().toString());
                        postData.put("createdBy", post.getCreatedBy());
                        postData.put("createdAt", post.getCreatedAt());
                        safeRequest.put("post", postData);
                    }
                } catch (Exception e) {
                    System.err.println("Error getting post details for approval " + request.getId() + ": " + e.getMessage());
                    safeRequest.put("post", null);
                }
                
                safeRequests.add(safeRequest);
            }
            
            System.out.println("Returning " + safeRequests.size() + " safe approval requests for admin");
            return new ResponseEntity<>(safeRequests, HttpStatus.OK);
            
        } catch (Exception e) {
            System.err.println("Error getting all approvals: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApprovalRequest> getApprovalRequestById(@PathVariable Long id) {
        Optional<ApprovalRequest> request = approvalService.findById(id);
        return request.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/manager/{managerId}")
    public ResponseEntity<List<ApprovalRequest>> getApprovalRequestsByManager(@PathVariable Long managerId) {
        List<ApprovalRequest> requests = approvalService.getApprovalHistory(managerId);
        return new ResponseEntity<>(requests, HttpStatus.OK);
    }
    
    @GetMapping("/manager/{managerId}/pending")
    public ResponseEntity<List<java.util.Map<String, Object>>> getPendingApprovalsByManager(@PathVariable Long managerId) {
        try {
            System.out.println("=== GET PENDING APPROVALS DEBUG ===");
            System.out.println("Manager ID: " + managerId);
            
            List<ApprovalRequest> requests = approvalService.getPendingApprovals(managerId);
            System.out.println("Found " + requests.size() + " pending approval requests for manager " + managerId);
            
            // Convert to safe JSON format to avoid Hibernate serialization issues
            List<java.util.Map<String, Object>> safeRequests = new java.util.ArrayList<>();
            
            for (ApprovalRequest request : requests) {
                System.out.println("Approval Request ID: " + request.getId() + 
                                 ", Post ID: " + request.getPostId() + 
                                 ", Status: " + request.getStatus() +
                                 ", Assigned To: " + request.getAssignedTo());
                
                java.util.Map<String, Object> safeRequest = new java.util.HashMap<>();
                safeRequest.put("id", request.getId());
                safeRequest.put("postId", request.getPostId());
                safeRequest.put("assignedTo", request.getAssignedTo());
                safeRequest.put("status", request.getStatus().toString());
                safeRequest.put("createdAt", request.getCreatedAt());
                safeRequest.put("reviewedAt", request.getReviewedAt());
                safeRequest.put("feedback", request.getFeedback());
                
                // Get post details safely
                try {
                    com.aiautoposter.entity.Post post = request.getPost();
                    if (post != null) {
                        java.util.Map<String, Object> postData = new java.util.HashMap<>();
                        postData.put("id", post.getId());
                        postData.put("title", post.getTitle());
                        postData.put("sourceDiscussion", post.getSourceDiscussion());
                        postData.put("targetPlatforms", post.getTargetPlatforms());
                        postData.put("currentStatus", post.getCurrentStatus().toString());
                        postData.put("createdBy", post.getCreatedBy());
                        postData.put("createdAt", post.getCreatedAt());
                        safeRequest.put("post", postData);
                    }
                } catch (Exception e) {
                    System.err.println("Error getting post details for approval " + request.getId() + ": " + e.getMessage());
                    safeRequest.put("post", null);
                }
                
                safeRequests.add(safeRequest);
            }
            
            System.out.println("Returning " + safeRequests.size() + " safe approval requests");
            return new ResponseEntity<>(safeRequests, HttpStatus.OK);
            
        } catch (Exception e) {
            System.err.println("Error getting pending approvals: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<ApprovalRequest>> getApprovalRequestsByPost(@PathVariable Long postId) {
        List<ApprovalRequest> requests = approvalService.getApprovalRequestsByPost(postId);
        return new ResponseEntity<>(requests, HttpStatus.OK);
    }
    
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApprovalRequest> approvePost(@PathVariable Long id) {
        try {
            ApprovalRequest request = approvalService.approvePost(id);
            return new ResponseEntity<>(request, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApprovalRequest> rejectPost(@PathVariable Long id, 
                                                    @RequestParam String feedback) {
        try {
            ApprovalRequest request = approvalService.rejectPost(id, feedback);
            return new ResponseEntity<>(request, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApprovalRequest> updateApprovalRequest(@PathVariable Long id, 
                                                               @Valid @RequestBody ApprovalRequest request) {
        try {
            request.setId(id);
            ApprovalRequest updatedRequest = approvalService.updateApprovalRequest(request);
            return new ResponseEntity<>(updatedRequest, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApprovalRequest(@PathVariable Long id) {
        try {
            approvalService.deleteApprovalRequest(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @GetMapping("/manager/{managerId}/pending/count")
    public ResponseEntity<Long> getPendingApprovalCount(@PathVariable Long managerId) {
        Long count = approvalService.countPendingApprovals(managerId);
        return new ResponseEntity<>(count, HttpStatus.OK);
    }
}
