package com.aiautoposter.repository;

import com.aiautoposter.entity.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostMediaRepository extends JpaRepository<PostMedia, Long> {
    
    List<PostMedia> findByPostIdOrderByDisplayOrder(Long postId);
    
    List<PostMedia> findByMediaId(Long mediaId);
    
    void deleteByPostId(Long postId);
    
    void deleteByPostIdAndMediaId(Long postId, Long mediaId);
    
    @Query("SELECT pm FROM PostMedia pm JOIN FETCH pm.media WHERE pm.postId = :postId ORDER BY pm.displayOrder")
    List<PostMedia> findByPostIdWithMediaOrderByDisplayOrder(@Param("postId") Long postId);
}
