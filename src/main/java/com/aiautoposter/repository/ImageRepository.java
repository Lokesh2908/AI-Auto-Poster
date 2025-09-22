package com.aiautoposter.repository;

import com.aiautoposter.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    
    List<Image> findByPostId(Long postId);
    
    @Query("SELECT i FROM Image i WHERE i.postId = :postId ORDER BY i.createdAt ASC")
    List<Image> findByPostIdOrderByCreatedAtAsc(@Param("postId") Long postId);
    
    @Query("SELECT COUNT(i) FROM Image i WHERE i.postId = :postId")
    Long countByPostId(@Param("postId") Long postId);
}
