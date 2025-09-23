package com.aiautoposter.service;

import com.aiautoposter.entity.PostContent;
import com.aiautoposter.entity.WordPressPostResponse;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WordpressService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${wordpress.name}")
    private String blogName;

    @Value("${wordpress.url}")
    private String blogUrl;

    @Value("${wordpress.username}")
    private String username;

    @Value("${wordpress.app-password}")
    private String appPassword;

    @Value("${wordpress.enabled}")
    private boolean enabled;

    public WordPressPostResponse publishPost(PostContent content) {
        if (!enabled) {
            throw new RuntimeException("WordPress publishing is disabled");
        }

        try {
            // Extract images from HTML content
            List<String> imageUrls = extractImageUrls(content.getContent());

            // Upload images to WordPress
            Map<String, Integer> urlToWpMediaId = uploadImages(imageUrls);

            // Replace local image URLs with WordPress URLs
            String wpContent = replaceImageUrls(content.getContent(), urlToWpMediaId);

            // Create WordPress post
            return createPost(content.getTitle(), wpContent, urlToWpMediaId);

        } catch (Exception e) {
            throw new RuntimeException("Failed to publish to WordPress: " + e.getMessage(), e);
        }
    }

    public boolean testConnection() {
        if (!enabled) return false;

        try {
            String testUrl = blogUrl + "/wp-json/wp/v2/posts?per_page=1";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", getBasicAuthHeader());

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(testUrl, HttpMethod.GET, entity, String.class);

            return response.getStatusCode() == HttpStatus.OK;

        } catch (Exception e) {
            System.err.println("WordPress connection failed: " + e.getMessage());
            return false;
        }
    }

    public Map<String, String> getBlogInfo() {
        return Map.of(
                "name", blogName,
                "url", blogUrl,
                "enabled", String.valueOf(enabled)
        );
    }

    private Map<String, Integer> uploadImages(List<String> imageUrls) {
        Map<String, Integer> urlToMediaId = new HashMap<>();

        for (String imageUrl : imageUrls) {
            try {
                Integer mediaId = uploadImage(imageUrl);
                if (mediaId != null) {
                    urlToMediaId.put(imageUrl, mediaId);
                }
            } catch (Exception e) {
                System.err.println("Failed to upload image: " + imageUrl);
            }
        }

        return urlToMediaId;
    }

    private Integer uploadImage(String imageUrl) throws IOException {
        String uploadUrl = blogUrl + "/wp-json/wp/v2/media";

        byte[] imageData = downloadImageData(imageUrl);
        String fileName = extractFileName(imageUrl);
        String mimeType = getMimeType(fileName);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", getBasicAuthHeader());
        headers.set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        headers.setContentType(MediaType.parseMediaType(mimeType));

        HttpEntity<byte[]> entity = new HttpEntity<>(imageData, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(uploadUrl, entity, Map.class);

        if (response.getStatusCode() == HttpStatus.CREATED) {
            return (Integer) response.getBody().get("id");
        }

        return null;
    }

    private WordPressPostResponse createPost(String title, String content, Map<String, Integer> mediaIds) {
        String postUrl = blogUrl + "/wp-json/wp/v2/posts";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", getBasicAuthHeader());

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

    private String getBasicAuthHeader() {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + appPassword).getBytes(StandardCharsets.UTF_8));
    }

    private List<String> extractImageUrls(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        return doc.select("img")
                .stream()
                .map(img -> img.attr("src"))
                .filter(src -> !src.isEmpty())
                .collect(Collectors.toList());
    }

    private String replaceImageUrls(String htmlContent, Map<String, Integer> urlToMediaId) {
        Document doc = Jsoup.parse(htmlContent);
        Elements images = doc.select("img");

        for (Element img : images) {
            String originalSrc = img.attr("src");
            Integer mediaId = urlToMediaId.get(originalSrc);

            if (mediaId != null) {
                String wpMediaUrl = getMediaUrl(mediaId);
                if (wpMediaUrl != null) {
                    img.attr("src", wpMediaUrl);
                }
            }
        }

        return doc.body().html();
    }

    private String getMediaUrl(Integer mediaId) {
        try {
            String mediaUrl = blogUrl + "/wp-json/wp/v2/media/" + mediaId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", getBasicAuthHeader());

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

}
