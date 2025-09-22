package com.aiautoposter.service;

import com.aiautoposter.entity.Media;
import com.aiautoposter.repository.MediaRepository;
import org.hibernate.type.ImageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.DefaultUriBuilderFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class MediaService {

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private AIContentGenerationService aiContentGenerationService;

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl;

    private static final String STATIC_DIR = "uploads";

    // Standardize on PNG for all images
    private static final String STANDARD_FORMAT = "PNG";
    private static final String STANDARD_EXTENSION = ".png";
    private static final String STANDARD_CONTENT_TYPE = "image/png";

    public String generateImage(String prompt, String title)
    {
        String url  = aiContentGenerationService.tryAzureOpenAIImageGeneration(prompt,title);
        return saveAiImageToStatic(url,title,title);
    }

    public String saveFileToStatic(MultipartFile file, String title, String description) {
        try {
            // Create static uploads directory
            Path staticPath = Paths.get(STATIC_DIR);
            if (!Files.exists(staticPath)) {
                Files.createDirectories(staticPath);
            }

            // Read original image
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                throw new RuntimeException("Invalid image file");
            }

            // Generate unique filename with standard extension
            String storedFilename = generateUniqueFilename(file.getOriginalFilename());
            Path filePath = staticPath.resolve(storedFilename);

            // Convert and save as PNG
            ImageIO.write(originalImage, STANDARD_FORMAT, filePath.toFile());

            // File URL (directly accessible)
            String fileUrl = baseUrl + "/uploads/" + storedFilename;

            // Save to database
            Media media = new Media();
            media.setOriginalFilename(file.getOriginalFilename());
            media.setStoredFilename(storedFilename);
            media.setFileUrl(fileUrl);
            media.setContentType(STANDARD_CONTENT_TYPE); // Always PNG
            media.setFileSize(Files.size(filePath)); // Actual file size after conversion
            media.setMediaType("PNG");
            media.setTitle(title != null ? title : file.getOriginalFilename());
            media.setAltText(description);
            media.setCreatedAt(LocalDateTime.now());
            media.setUpdatedAt(LocalDateTime.now());

            mediaRepository.save(media);

            return fileUrl;

        } catch (IOException e) {
            throw new RuntimeException("Failed to save and convert image: " + e.getMessage());
        }
    }

    // For AI-generated images
    public String saveAiImageToStatic(String imageUrl, String title, String description) {
        try {
            System.out.println("Attempting to download from URL: " + imageUrl);

            // Create RestTemplate with NO URL encoding
            RestTemplate restTemplate = new RestTemplate();

            // CRITICAL: Disable URL encoding to prevent double-encoding
            DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory();
            defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
            restTemplate.setUriTemplateHandler(defaultUriBuilderFactory);

            // Use URI object instead of String to prevent encoding
            URI uri = URI.create(imageUrl);

            ResponseEntity<byte[]> response = restTemplate.getForEntity(uri, byte[].class);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("HTTP Error: " + response.getStatusCode());
            }

            byte[] imageData = response.getBody();
            if (imageData == null || imageData.length == 0) {
                throw new RuntimeException("Empty response body");
            }

            System.out.println("Successfully downloaded " + imageData.length + " bytes");

            // Validate the image
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
            if (originalImage == null) {
                throw new RuntimeException("Invalid image data");
            }

            System.out.println("Image validation successful: " + originalImage.getWidth() + "x" + originalImage.getHeight());

            // Create static uploads directory
            Path staticPath = Paths.get(STATIC_DIR);
            if (!Files.exists(staticPath)) {
                Files.createDirectories(staticPath);
            }

            // Generate filename
            String storedFilename = generateUniqueFilename("ai-generated-image.png");
            Path filePath = staticPath.resolve(storedFilename);

            // Save as PNG
            ImageIO.write(originalImage, STANDARD_FORMAT, filePath.toFile());

            System.out.println("Image saved to: " + filePath.toAbsolutePath());

            // Create file URL
            String fileUrl = baseUrl + "/uploads/" + storedFilename;

            // Save to database
            Media media = new Media();
            media.setOriginalFilename("ai-generated-image.png");
            media.setStoredFilename(storedFilename);
            media.setFileUrl(fileUrl);
            media.setContentType(STANDARD_CONTENT_TYPE);
            media.setFileSize(Files.size(filePath));
            media.setMediaType("PNG");
            media.setTitle(title != null ? title : "AI Generated Image");
            media.setAltText(description);
            media.setCreatedAt(LocalDateTime.now());
            media.setUpdatedAt(LocalDateTime.now());

            mediaRepository.save(media);

            System.out.println("Media saved to database with URL: " + fileUrl);

            return fileUrl;

        } catch (Exception e) {
            System.err.println("ERROR: Failed to save AI image");
            System.err.println("URL: " + imageUrl);
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save AI image: " + e.getMessage(), e);
        }
    }


    private String generateUniqueFilename(String originalFilename) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String nameWithoutExtension = originalFilename.substring(0,
                originalFilename.lastIndexOf('.') > 0 ? originalFilename.lastIndexOf('.') : originalFilename.length());

        // Always use .png extension
        return timestamp + "_" + nameWithoutExtension.replaceAll("[^a-zA-Z0-9]", "_") + STANDARD_EXTENSION;
    }

    public List<Media> getAllMedia() {
        return mediaRepository.findAllByOrderByCreatedAtDesc();
    }

    public void deleteMedia(Long id) {
        try {
            Optional<Media> media = mediaRepository.findById(id);
            if (media.isPresent()) {
                // Delete file from filesystem
                Path filePath = Paths.get(STATIC_DIR, media.get().getStoredFilename());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }
                // Delete from database
                mediaRepository.deleteById(id);
            } else {
                throw new RuntimeException("Media not found");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + e.getMessage());
        }
    }

}
