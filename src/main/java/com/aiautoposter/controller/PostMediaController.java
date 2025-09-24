package com.aiautoposter.controller;

import com.aiautoposter.entity.PostMedia;
import com.aiautoposter.entity.Media;
import com.aiautoposter.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts/{postId}/media")
public class PostMediaController {

    @Autowired
    private PostService postService;

    @PostMapping("/attach")
    public ResponseEntity<PostMedia> attachMediaToPost(
            @PathVariable Long postId,
            @RequestBody Map<String, Object> request) {
        
        Long mediaId = Long.valueOf(request.get("mediaId").toString());
        Integer displayOrder = request.containsKey("displayOrder") ? 
            Integer.valueOf(request.get("displayOrder").toString()) : 0;
        
        PostMedia postMedia = postService.attachMediaToPost(postId, mediaId, displayOrder);
        return ResponseEntity.ok(postMedia);
    }

    @DeleteMapping("/detach/{mediaId}")
    public ResponseEntity<Void> detachMediaFromPost(
            @PathVariable Long postId,
            @PathVariable Long mediaId) {
        
        postService.detachMediaFromPost(postId, mediaId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getPostMedia(@PathVariable Long postId) {
        List<PostMedia> postMediaList = postService.getPostMedia(postId);
        
        // Convert to the format expected by frontend
        List<Map<String, Object>> mediaResponse = postMediaList.stream()
            .map(postMedia -> {
                Map<String, Object> mediaMap = new HashMap<>();
                Media media = postMedia.getMedia();
                if (media != null) {
                    mediaMap.put("id", media.getId());
                    mediaMap.put("fileName", media.getStoredFilename());
                    mediaMap.put("originalName", media.getOriginalFilename());
                    mediaMap.put("fileUrl", media.getFileUrl());
                    mediaMap.put("contentType", media.getContentType());
                    mediaMap.put("displayOrder", postMedia.getDisplayOrder());
                }
                return mediaMap;
            })
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(mediaResponse);
    }
}
