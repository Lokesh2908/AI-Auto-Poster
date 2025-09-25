package com.aiautoposter.repository;

import com.aiautoposter.entity.SocialMediaApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SocialMediaAppRepository extends JpaRepository<SocialMediaApp, Long> {
    
    List<SocialMediaApp> findByPlatformAndIsActiveTrue(SocialMediaApp.Platform platform);
    
    List<SocialMediaApp> findByIsActiveTrue();
    
    Optional<SocialMediaApp> findByPlatformAndIsDefaultTrue(SocialMediaApp.Platform platform);
    
    @Query("SELECT s FROM SocialMediaApp s WHERE s.platform = :platform AND s.isActive = true ORDER BY s.isDefault DESC, s.name ASC")
    List<SocialMediaApp> findActivePlatformAppsOrderedByDefault(@Param("platform") SocialMediaApp.Platform platform);
    
    @Query("SELECT s FROM SocialMediaApp s WHERE s.createdBy = :userId AND s.isActive = true")
    List<SocialMediaApp> findByCreatedByAndIsActiveTrue(@Param("userId") Long userId);
    
    boolean existsByPlatformAndIsDefaultTrue(SocialMediaApp.Platform platform);
    
    @Query("SELECT COUNT(s) FROM SocialMediaApp s WHERE s.platform = :platform AND s.isActive = true")
    long countActivePlatformApps(@Param("platform") SocialMediaApp.Platform platform);
}
