package com.aiautoposter.service;

import com.aiautoposter.dto.PostVersionRequest;
import com.aiautoposter.dto.RestoreVersionRequest;
import com.aiautoposter.entity.Post;
import com.aiautoposter.entity.PostContent;
import com.aiautoposter.entity.PostVersion;
import com.aiautoposter.repository.PostRepository;
import com.aiautoposter.repository.PostVersionRepository;
import com.aiautoposter.repository.PostContentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    
    @Autowired
    private PostContentRepository postContentRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
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
        
        // Store the actual post content (this is the key fix!)
        try {
            List<PostContent> currentContent = postContentRepository.findByPostId(postId);
            if (currentContent != null && !currentContent.isEmpty()) {
                // Convert to JSON string for storage
                String contentJson = objectMapper.writeValueAsString(currentContent);
                version.setSavedContent(contentJson);
                
                // Create a summary for display
                StringBuilder summary = new StringBuilder();
                for (PostContent content : currentContent) {
                    if (summary.length() > 0) summary.append("; ");
                    summary.append(content.getPlatform()).append(": ")
                           .append(content.getContent().substring(0, Math.min(100, content.getContent().length())))
                           .append("...");
                }
                version.setContentSummary(summary.toString());
            }
        } catch (Exception e) {
            System.err.println("Error saving post content to version: " + e.getMessage());
            e.printStackTrace();
            // Continue without failing the version save
        }
        
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
        
        // *** KEY FIX: Restore the actual post content ***
        try {
            if (version.getSavedContent() != null && !version.getSavedContent().trim().isEmpty()) {
                System.out.println("🔄 Restoring post content from version " + version.getId());
                
                // Parse the saved content JSON
                PostContent[] savedContentArray = objectMapper.readValue(version.getSavedContent(), PostContent[].class);
                List<PostContent> savedContentList = java.util.Arrays.asList(savedContentArray);
                
                // Delete current post content
                List<PostContent> currentContent = postContentRepository.findByPostId(postId);
                if (!currentContent.isEmpty()) {
                    System.out.println("🗑️ Deleting " + currentContent.size() + " current content items");
                    postContentRepository.deleteAll(currentContent);
                }
                
                // Restore the saved content
                for (PostContent content : savedContentList) {
                    // Create new content with same data but new ID
                    PostContent restoredContent = new PostContent();
                    restoredContent.setPostId(postId);
                    restoredContent.setPlatform(content.getPlatform());
                    restoredContent.setTitle(content.getTitle());
                    restoredContent.setContent(content.getContent());
                    restoredContent.setHashtags(content.getHashtags());
                    restoredContent.setAiConfidenceScore(content.getAiConfidenceScore());
                    restoredContent.setCreatedAt(LocalDateTime.now()); // Use current time for restored content
                    
                    postContentRepository.save(restoredContent);
                }
                
                System.out.println("✅ Restored " + savedContentList.size() + " content items from version");
            } else {
                System.out.println("⚠️ No saved content found in version " + version.getId());
            }
        } catch (Exception e) {
            System.err.println("❌ Error restoring post content from version: " + e.getMessage());
            e.printStackTrace();
            // Don't fail the entire restore operation if content restore fails
        }
        
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
