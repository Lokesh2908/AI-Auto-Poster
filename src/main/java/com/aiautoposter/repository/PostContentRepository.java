package com.aiautoposter.repository;

import com.aiautoposter.entity.PostContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostContentRepository extends JpaRepository<PostContent, Long> {
    
    List<PostContent> findByPostId(Long postId);
    
    List<PostContent> findByPlatform(String platform);
    
    PostContent findByPostIdAndPlatform(Long postId, String platform);
    
    @Query("SELECT pc FROM PostContent pc WHERE pc.postId = :postId ORDER BY pc.createdAt DESC")
    List<PostContent> findByPostIdOrderByCreatedAtDesc(@Param("postId") Long postId);
    
    @Query("SELECT pc FROM PostContent pc WHERE pc.aiConfidenceScore >= :minScore ORDER BY pc.aiConfidenceScore DESC")
    List<PostContent> findByAiConfidenceScoreGreaterThanEqual(@Param("minScore") Double minScore);
    
    @Query("SELECT AVG(pc.aiConfidenceScore) FROM PostContent pc WHERE pc.postId = :postId")
    Double findAverageConfidenceScoreByPostId(@Param("postId") Long postId);
}
