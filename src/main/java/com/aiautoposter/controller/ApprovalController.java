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
    public ResponseEntity<List<ApprovalRequest>> getAllApprovalRequests() {
        List<ApprovalRequest> requests = approvalService.findAll();
        return new ResponseEntity<>(requests, HttpStatus.OK);
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
    public ResponseEntity<List<ApprovalRequest>> getPendingApprovalsByManager(@PathVariable Long managerId) {
        List<ApprovalRequest> requests = approvalService.getPendingApprovals(managerId);
        return new ResponseEntity<>(requests, HttpStatus.OK);
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
