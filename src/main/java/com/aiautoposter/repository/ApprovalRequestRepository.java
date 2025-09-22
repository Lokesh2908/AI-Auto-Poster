package com.aiautoposter.repository;

import com.aiautoposter.entity.ApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    
    List<ApprovalRequest> findByPostId(Long postId);
    
    List<ApprovalRequest> findByAssignedTo(Long assignedTo);
    
    List<ApprovalRequest> findByStatus(ApprovalRequest.ApprovalStatus status);
    
    List<ApprovalRequest> findByAssignedToAndStatus(Long assignedTo, ApprovalRequest.ApprovalStatus status);
    
    @Query("SELECT ar FROM ApprovalRequest ar WHERE ar.assignedTo = :assignedTo ORDER BY ar.createdAt DESC")
    List<ApprovalRequest> findByAssignedToOrderByCreatedAtDesc(@Param("assignedTo") Long assignedTo);
    
    @Query("SELECT ar FROM ApprovalRequest ar WHERE ar.status = :status ORDER BY ar.createdAt ASC")
    List<ApprovalRequest> findByStatusOrderByCreatedAtAsc(@Param("status") ApprovalRequest.ApprovalStatus status);
    
    @Query("SELECT COUNT(ar) FROM ApprovalRequest ar WHERE ar.assignedTo = :assignedTo AND ar.status = :status")
    Long countByAssignedToAndStatus(@Param("assignedTo") Long assignedTo, 
                                  @Param("status") ApprovalRequest.ApprovalStatus status);
    
    @Query("SELECT ar FROM ApprovalRequest ar WHERE ar.postId = :postId AND ar.status = :status")
    ApprovalRequest findByPostIdAndStatus(@Param("postId") Long postId, 
                                        @Param("status") ApprovalRequest.ApprovalStatus status);
}
