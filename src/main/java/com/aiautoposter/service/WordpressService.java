package com.aiautoposter.service;

import com.aiautoposter.entity.PostContent;
import com.aiautoposter.entity.WordPressPostResponse;
import com.aiautoposter.entity.Post;
import com.aiautoposter.entity.PostMedia;
import com.aiautoposter.entity.Media;
import com.aiautoposter.entity.SocialMediaApp;
import com.aiautoposter.repository.PostMediaRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WordpressService {

    private final RestTemplate restTemplate = new RestTemplate();


    @Autowired
    private PostMediaRepository postMediaRepository;

    // New method to publish post with app-specific configuration
    public WordPressPostResponse publishPost(Post post, PostContent content, SocialMediaApp app) {
        if (app == null || !app.getPlatform().equals(SocialMediaApp.Platform.WORDPRESS)) {
            throw new RuntimeException("Invalid WordPress app configuration");
        }

        try {
            // Get media files associated with the post
            List<PostMedia> postMediaList = postMediaRepository.findByPostIdWithMediaOrderByDisplayOrder(post.getId());
            
            // Extract existing images from HTML content
            List<String> htmlImageUrls = extractImageUrls(content.getContent());
            
            // Collect all media URLs (from database + HTML)
            List<String> allImageUrls = new ArrayList<>();
            
            // Add media from database
            for (PostMedia postMedia : postMediaList) {
                Media media = postMedia.getMedia();
                if (media != null && ("IMAGE".equalsIgnoreCase(media.getMediaType()) || "PNG".equalsIgnoreCase(media.getMediaType()))) {
                    allImageUrls.add(media.getFileUrl());
                }
            }
            
            // Add images from HTML content
            allImageUrls.addAll(htmlImageUrls);

            // Upload all images to WordPress using app-specific config
            Map<String, Integer> urlToWpMediaId = uploadImages(allImageUrls, app);

            // Replace local image URLs with WordPress URLs in content
            String wpContent = replaceImageUrls(content.getContent(), urlToWpMediaId, app);
            
            // If no images in HTML content but we have media from database, add them to content
            if (htmlImageUrls.isEmpty() && !postMediaList.isEmpty()) {
                wpContent = addMediaToContent(wpContent, postMediaList, urlToWpMediaId, app);
            }

            // Create WordPress post using app-specific config
            return createPost(content.getTitle(), wpContent, urlToWpMediaId, app);

        } catch (Exception e) {
            throw new RuntimeException("Failed to publish to WordPress: " + e.getMessage(), e);
        }
    }


    private Map<String, Integer> uploadImages(List<String> imageUrls, SocialMediaApp app) {
        Map<String, Integer> urlToMediaId = new HashMap<>();

        for (String imageUrl : imageUrls) {
            try {
                Integer mediaId = uploadImage(imageUrl, app);
                if (mediaId != null) {
                    urlToMediaId.put(imageUrl, mediaId);
                }
            } catch (Exception e) {
                System.err.println("Failed to upload image: " + imageUrl);
            }
        }

        return urlToMediaId;
    }

    private Integer uploadImage(String imageUrl, SocialMediaApp app) throws IOException {
        String uploadUrl = app.getUrl() + "/wp-json/wp/v2/media";

        byte[] imageData = downloadImageData(imageUrl);
        String fileName = extractFileName(imageUrl);
        String mimeType = getMimeType(fileName);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", getBasicAuthHeader(app));
        headers.set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        headers.setContentType(MediaType.parseMediaType(mimeType));

        HttpEntity<byte[]> entity = new HttpEntity<>(imageData, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(uploadUrl, entity, Map.class);

        if (response.getStatusCode() == HttpStatus.CREATED) {
            return (Integer) response.getBody().get("id");
        }

        return null;
    }

    private WordPressPostResponse createPost(String title, String content, Map<String, Integer> mediaIds, SocialMediaApp app) {
        String postUrl = app.getUrl() + "/wp-json/wp/v2/posts";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", getBasicAuthHeader(app));

        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("content", content);
        payload.put("status", "publish");

        // Set featured image (first image)
        if (!mediaIds.isEmpty()) {
            payload.put("featured_media", mediaIds.values().iterator().next());
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(postUrl, entity, Map.class);

        if (response.getStatusCode() == HttpStatus.CREATED) {
            Map<String, Object> responseBody = response.getBody();
            return new WordPressPostResponse(
                    (Integer) responseBody.get("id"),
                    (String) responseBody.get("link"),
                    "success"
            );
        }

        throw new RuntimeException("Failed to create WordPress post");
    }

    private String getBasicAuthHeader(SocialMediaApp app) {
        return "Basic " + Base64.getEncoder().encodeToString((app.getUsername() + ":" + app.getAppPassword()).getBytes(StandardCharsets.UTF_8));
    }

    private List<String> extractImageUrls(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        return doc.select("img")
                .stream()
                .map(img -> img.attr("src"))
                .filter(src -> !src.isEmpty())
                .collect(Collectors.toList());
    }

    private String replaceImageUrls(String htmlContent, Map<String, Integer> urlToMediaId, SocialMediaApp app) {
        Document doc = Jsoup.parse(htmlContent);
        Elements images = doc.select("img");

        for (Element img : images) {
            String originalSrc = img.attr("src");
            Integer mediaId = urlToMediaId.get(originalSrc);

            if (mediaId != null) {
                String wpMediaUrl = getMediaUrl(mediaId, app);
                if (wpMediaUrl != null) {
                    img.attr("src", wpMediaUrl);
                }
            }
        }

        return doc.body().html();
    }


    private String getMediaUrl(Integer mediaId, SocialMediaApp app) {
        try {
            String mediaUrl = app.getUrl() + "/wp-json/wp/v2/media/" + mediaId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", getBasicAuthHeader(app));

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(mediaUrl, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return (String) response.getBody().get("source_url");
            }
        } catch (Exception e) {
            System.err.println("Error getting media URL: " + e.getMessage());
        }

        return null;
    }

    private byte[] downloadImageData(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        try (InputStream inputStream = url.openStream()) {
            return inputStream.readAllBytes();
        }
    }

    private String extractFileName(String imageUrl) {
        String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        if (fileName.contains("?")) {
            fileName = fileName.substring(0, fileName.indexOf("?"));
        }
        return fileName.isEmpty() ? "image.jpg" : fileName;
    }

    private String getMimeType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "webp": return "image/webp";
            default: return "image/jpeg";
        }
    }

    // Helper method to add media to content when no images exist in HTML (app-specific)
    private String addMediaToContent(String content, List<PostMedia> postMediaList, Map<String, Integer> urlToWpMediaId, SocialMediaApp app) {
        StringBuilder contentBuilder = new StringBuilder(content);
        
        for (PostMedia postMedia : postMediaList) {
            Media media = postMedia.getMedia();
            if (media != null && ("IMAGE".equalsIgnoreCase(media.getMediaType()) || "PNG".equalsIgnoreCase(media.getMediaType()))) {
                Integer wpMediaId = urlToWpMediaId.get(media.getFileUrl());
                if (wpMediaId != null) {
                    String wpMediaUrl = getMediaUrl(wpMediaId, app);
                    if (wpMediaUrl != null) {
                        // Add image to content with proper WordPress image block format
                        String imageBlock = String.format(
                            "\n\n<!-- wp:image {\"id\":%d} -->\n" +
                            "<figure class=\"wp-block-image\"><img src=\"%s\" alt=\"%s\" class=\"wp-image-%d\"/></figure>\n" +
                            "<!-- /wp:image -->\n",
                            wpMediaId, wpMediaUrl, 
                            media.getAltText() != null ? media.getAltText() : media.getTitle(),
                            wpMediaId
                        );
                        contentBuilder.append(imageBlock);
                    }
                }
            }
        }
        
        return contentBuilder.toString();
    }

}
