package com.aiautoposter.repository;

import com.aiautoposter.entity.Post;
import com.aiautoposter.entity.PostAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostAnalyticsRepository extends JpaRepository<PostAnalytics, Long> {
    
    List<PostAnalytics> findByPostId(Long postId);
    
    List<PostAnalytics> findByPostIdAndPlatform(Long postId, String platform);
    
    Optional<PostAnalytics> findFirstByPostIdAndPlatformOrderByCollectedAtDesc(Long postId, String platform);
    
    @Query("SELECT pa FROM PostAnalytics pa WHERE pa.post.id = :postId AND pa.platform = :platform " +
           "AND pa.collectedAt = (SELECT MAX(pa2.collectedAt) FROM PostAnalytics pa2 WHERE pa2.post.id = :postId AND pa2.platform = :platform)")
    Optional<PostAnalytics> findLatestByPostIdAndPlatform(@Param("postId") Long postId, @Param("platform") String platform);
    
    @Query("SELECT pa FROM PostAnalytics pa WHERE pa.post.id IN :postIds AND pa.platform = :platform " +
           "AND pa.collectedAt >= :startDate")
    List<PostAnalytics> findByPostIdsAndPlatformAfterDate(
            @Param("postIds") List<Long> postIds,
            @Param("platform") String platform,
            @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT pa FROM PostAnalytics pa WHERE pa.platform = :platform AND pa.collectedAt >= :startDate")
    List<PostAnalytics> findByPlatformAfterDate(
            @Param("platform") String platform,
            @Param("startDate") LocalDateTime startDate);
            
    /**
     * Find analytics by multiple post IDs, platform, and collected after a specific date
     * This is an alternative to the findByPostIdsAndPlatformAfterDate method with a more JPA-naming-convention friendly name
     */
    @Query("SELECT pa FROM PostAnalytics pa WHERE pa.post.id IN :postIds AND pa.platform = :platform AND pa.collectedAt > :collectedAfter")
    List<PostAnalytics> findByPostIdInAndPlatformAndCollectedAtAfter(
            @Param("postIds") List<Long> postIds,
            @Param("platform") String platform,
            @Param("collectedAfter") LocalDateTime collectedAfter);
            
    /**
     * Find analytics by a single post ID, platform, and collected after a specific date
     */
    @Query("SELECT pa FROM PostAnalytics pa WHERE pa.post.id = :postId AND pa.platform = :platform AND pa.collectedAt > :collectedAfter")
    List<PostAnalytics> findByPostIdAndPlatformAndCollectedAtAfter(
            @Param("postId") Long postId,
            @Param("platform") String platform,
            @Param("collectedAfter") LocalDateTime collectedAfter);
}
