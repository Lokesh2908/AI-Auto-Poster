package com.aiautoposter.repository;

import com.aiautoposter.entity.PostVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostVersionRepository extends JpaRepository<PostVersion, Long> {
    
    /**
     * Find all versions for a specific post, ordered by creation date (newest first)
     */
    @Query("SELECT pv FROM PostVersion pv WHERE pv.post.id = :postId ORDER BY pv.createdAt DESC")
    List<PostVersion> findByPostIdOrderByCreatedAtDesc(@Param("postId") Long postId);
    
    /**
     * Count total versions for a post
     */
    @Query("SELECT COUNT(pv) FROM PostVersion pv WHERE pv.post.id = :postId")
    Long countByPostId(@Param("postId") Long postId);
    
    /**
     * Find latest version for a post
     */
    @Query("SELECT pv FROM PostVersion pv WHERE pv.post.id = :postId ORDER BY pv.createdAt DESC")
    List<PostVersion> findLatestByPostId(@Param("postId") Long postId);
    
    /**
     * Find versions by type for a post
     */
    @Query("SELECT pv FROM PostVersion pv WHERE pv.post.id = :postId AND pv.versionType = :versionType ORDER BY pv.createdAt DESC")
    List<PostVersion> findByPostIdAndVersionType(@Param("postId") Long postId, @Param("versionType") String versionType);
}
