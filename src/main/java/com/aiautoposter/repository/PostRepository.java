package com.aiautoposter.repository;

import com.aiautoposter.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    List<Post> findByCreatedBy(Long createdBy);
    
    List<Post> findByCurrentStatus(Post.PostStatus status);
    
    List<Post> findByCreatedByAndCurrentStatus(Long createdBy, Post.PostStatus status);
    
    @Query("SELECT p FROM Post p WHERE p.createdBy = :createdBy ORDER BY p.createdAt DESC")
    List<Post> findByCreatedByOrderByCreatedAtDesc(@Param("createdBy") Long createdBy);
    
    @Query("SELECT p FROM Post p WHERE p.currentStatus = :status ORDER BY p.createdAt DESC")
    List<Post> findByCurrentStatusOrderByCreatedAtDesc(@Param("status") Post.PostStatus status);
    
    @Query("SELECT p FROM Post p WHERE p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    List<Post> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                    @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT p FROM Post p WHERE p.currentStatus IN :statuses ORDER BY p.createdAt DESC")
    List<Post> findByCurrentStatusIn(@Param("statuses") List<Post.PostStatus> statuses);
    
    @Query("SELECT COUNT(p) FROM Post p WHERE p.createdBy = :createdBy AND p.currentStatus = :status")
    Long countByCreatedByAndCurrentStatus(@Param("createdBy") Long createdBy, 
                                        @Param("status") Post.PostStatus status);
    
    /**
     * Find posts where targetPlatforms contains the given platform (case-insensitive)
     * @param platform The platform to search for (e.g., "LINKEDIN")
     * @return List of posts containing the platform in their targetPlatforms
     */
    @Query("SELECT p FROM Post p WHERE LOWER(p.targetPlatforms) LIKE LOWER(concat('%', :platform, '%'))")
    List<Post> findByTargetPlatformsContaining(@Param("platform") String platform);
}
