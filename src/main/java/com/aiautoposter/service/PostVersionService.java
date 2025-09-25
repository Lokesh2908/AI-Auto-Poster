package com.aiautoposter.service;

import com.aiautoposter.dto.PostVersionRequest;
import com.aiautoposter.dto.RestoreVersionRequest;
import com.aiautoposter.entity.Post;
import com.aiautoposter.entity.PostVersion;
import com.aiautoposter.repository.PostRepository;
import com.aiautoposter.repository.PostVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PostVersionService {
    
    @Autowired
    private PostVersionRepository postVersionRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    /**
     * Save a new version of a post
     */
    public PostVersion saveVersion(Long postId, PostVersionRequest request) {
        // Find the post
        Optional<Post> postOpt = postRepository.findById(postId);
        if (!postOpt.isPresent()) {
            throw new RuntimeException("Post not found with id: " + postId);
        }
        
        Post post = postOpt.get();
        
        // Create new version
        PostVersion version = new PostVersion();
        version.setPost(post);
        version.setVersionType(request.getVersionType());
        version.setChangeDescription(request.getChangeDescription());
        version.setRefinementInstructions(request.getRefinementInstructions());
        version.setPreviousContent(request.getPreviousContent());
        version.setEnhancementOptions(request.getEnhancementOptions());
        version.setCreatedAt(LocalDateTime.now());
        
        // Store current post state in the version
        version.setTitle(request.getCurrentTitle() != null ? request.getCurrentTitle() : post.getTitle());
        version.setSourceDiscussion(request.getCurrentSourceDiscussion() != null ? 
            request.getCurrentSourceDiscussion() : post.getSourceDiscussion());
        version.setTargetPlatforms(request.getCurrentTargetPlatforms() != null ? 
            request.getCurrentTargetPlatforms() : post.getTargetPlatforms());
        
        return postVersionRepository.save(version);
    }
    
    /**
     * Get all versions for a post
     */
    public List<PostVersion> getVersionHistory(Long postId) {
        // Verify post exists
        if (!postRepository.existsById(postId)) {
            throw new RuntimeException("Post not found with id: " + postId);
        }
        
        return postVersionRepository.findByPostIdOrderByCreatedAtDesc(postId);
    }
    
    /**
     * Get total version count for a post
     */
    public Long getVersionCount(Long postId) {
        return postVersionRepository.countByPostId(postId);
    }
    
    /**
     * Restore a post to a previous version
     */
    public Post restoreVersion(Long postId, RestoreVersionRequest request) {
        // Find the post
        Optional<Post> postOpt = postRepository.findById(postId);
        if (!postOpt.isPresent()) {
            throw new RuntimeException("Post not found with id: " + postId);
        }
        
        // Find the version to restore
        Optional<PostVersion> versionOpt = postVersionRepository.findById(request.getVersionId());
        if (!versionOpt.isPresent()) {
            throw new RuntimeException("Version not found with id: " + request.getVersionId());
        }
        
        Post post = postOpt.get();
        PostVersion version = versionOpt.get();
        
        // Verify the version belongs to this post
        if (!version.getPost().getId().equals(postId)) {
            throw new RuntimeException("Version does not belong to the specified post");
        }
        
        // Save current state as a new version before restoring
        PostVersionRequest backupRequest = new PostVersionRequest();
        backupRequest.setVersionType("backup_before_restore");
        backupRequest.setChangeDescription("Backup before restoring to version " + version.getId());
        backupRequest.setCurrentTitle(post.getTitle());
        backupRequest.setCurrentSourceDiscussion(post.getSourceDiscussion());
        backupRequest.setCurrentTargetPlatforms(post.getTargetPlatforms());
        saveVersion(postId, backupRequest);
        
        // Restore the post to the version state
        post.setTitle(version.getTitle());
        post.setSourceDiscussion(version.getSourceDiscussion());
        post.setTargetPlatforms(version.getTargetPlatforms());
        post.setUpdatedAt(LocalDateTime.now());
        
        Post restoredPost = postRepository.save(post);
        
        // Create a new version entry for the restore action
        PostVersionRequest restoreRequest = new PostVersionRequest();
        restoreRequest.setVersionType("restore");
        restoreRequest.setChangeDescription(request.getChangeDescription() != null ? 
            request.getChangeDescription() : "Restored to version from " + version.getCreatedAt());
        restoreRequest.setCurrentTitle(post.getTitle());
        restoreRequest.setCurrentSourceDiscussion(post.getSourceDiscussion());
        restoreRequest.setCurrentTargetPlatforms(post.getTargetPlatforms());
        saveVersion(postId, restoreRequest);
        
        return restoredPost;
    }
    
    /**
     * Get latest version for a post
     */
    public Optional<PostVersion> getLatestVersion(Long postId) {
        List<PostVersion> versions = postVersionRepository.findLatestByPostId(postId);
        return versions.isEmpty() ? Optional.empty() : Optional.of(versions.get(0));
    }
    
    /**
     * Get versions by type
     */
    public List<PostVersion> getVersionsByType(Long postId, String versionType) {
        return postVersionRepository.findByPostIdAndVersionType(postId, versionType);
    }
}
