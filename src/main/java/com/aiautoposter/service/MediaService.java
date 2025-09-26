package com.aiautoposter.service;

import com.aiautoposter.entity.Media;
import com.aiautoposter.repository.MediaRepository;
import org.hibernate.type.ImageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
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


    public String generateImage(String prompt, String title) {
        try {
            System.out.println("MediaService - Starting image generation");
            System.out.println("Prompt: " + prompt);
            System.out.println("Title: " + title);
            
            if (prompt == null || prompt.trim().isEmpty()) {
                throw new RuntimeException("Prompt cannot be empty");
            }

            prompt = aiContentGenerationService.getPromptForImageGen(prompt,title);
            
            String url = aiContentGenerationService.tryAzureOpenAIImageGeneration(prompt, title);
            System.out.println("AI service returned URL: " + url);
            
            if (url == null || url.trim().isEmpty()) {
                throw new RuntimeException("AI service failed to generate image URL");
            }
            
            String savedUrl = saveAiImageToStatic(url, title, title);
            System.out.println("Image saved with URL: " + savedUrl);
            
            return savedUrl;
        } catch (Exception e) {
            System.err.println("Error in generateImage: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to generate image: " + e.getMessage(), e);
        }
    }

    public String saveFileToStatic(MultipartFile file, String title, String description) {
        try {
            System.out.println("MediaService - Starting file upload");
            System.out.println("Original filename: " + file.getOriginalFilename());
            System.out.println("File size: " + file.getSize() + " bytes");
            
            if (file.isEmpty()) {
                throw new RuntimeException("File cannot be empty");
            }
            
            // Create static uploads directory with absolute path
            Path staticPath = Paths.get(STATIC_DIR).toAbsolutePath();
            System.out.println("Upload directory: " + staticPath.toString());
            
            if (!Files.exists(staticPath)) {
                Files.createDirectories(staticPath);
                System.out.println("Created upload directory: " + staticPath.toString());
            }

            // Read original image
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                throw new RuntimeException("Invalid image file - cannot read image data");
            }
            
            System.out.println("Image dimensions: " + originalImage.getWidth() + "x" + originalImage.getHeight());

            // Generate unique filename with standard extension
            String storedFilename = generateUniqueFilename(file.getOriginalFilename());
            Path filePath = staticPath.resolve(storedFilename);
            System.out.println("Saving to: " + filePath.toString());

            // Convert and save as PNG
            boolean saved = ImageIO.write(originalImage, STANDARD_FORMAT, filePath.toFile());
            if (!saved) {
                throw new RuntimeException("Failed to save image file");
            }
            
            System.out.println("File saved successfully, size: " + Files.size(filePath) + " bytes");

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
            media.setTitle(title != null && !title.trim().isEmpty() ? title : file.getOriginalFilename());
            media.setAltText(description);
            media.setCreatedAt(LocalDateTime.now());
            media.setUpdatedAt(LocalDateTime.now());

            Media savedMedia = mediaRepository.save(media);
            System.out.println("Media saved to database with ID: " + savedMedia.getId());

            return fileUrl;

        } catch (IOException e) {
            System.err.println("IOException in saveFileToStatic: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save and convert image: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("Exception in saveFileToStatic: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to process image: " + e.getMessage(), e);
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

    public byte[] generateDiagramImage(String mermaidCode) {
        try {
            String url = "https://kroki.io/mermaid/png";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);

            HttpEntity<String> entity = new HttpEntity<>(mermaidCode, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<byte[]> response = restTemplate.postForEntity(url, entity, byte[].class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }

            throw new RuntimeException("Failed to generate diagram");

        } catch (Exception e) {
            throw new RuntimeException("Error generating diagram: " + e.getMessage());
        }
    }

    public byte[] generateLatexDiagramImage(String latexCode) {
        try {
            // Change URL to use tikz endpoint for LaTeX
            String url = "https://kroki.io/tikz/png";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);

            // Use latexCode instead of mermaidCode
            HttpEntity<String> entity = new HttpEntity<>(latexCode, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<byte[]> response = restTemplate.postForEntity(url, entity, byte[].class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }

            throw new RuntimeException("Failed to generate diagram");

        } catch (Exception e) {
            throw new RuntimeException("Error generating diagram: " + e.getMessage());
        }
    }


    public String storeDiagramAsMedia(String prompt, String title, String description, String type) {
        try {
            // Generate diagram image
            String code=null;
            byte[] imageBytes=null;
            if(type.equals("diagram")) {
                code = aiContentGenerationService.createDiagramUsingAI(prompt);
                code = cleanMermaidCode(code);
                imageBytes = generateDiagramImage(code);
            }
            else {
                code = aiContentGenerationService.createLatexDiagramUsingAI(prompt);
                code = clearLatexCode(code);
                imageBytes = generateLatexDiagramImage(code);
            }

            // Create unique filename
            String fileName = "diagram_" + System.currentTimeMillis() + ".png";
            String filePath = "uploads/" + fileName;

            // Save to file system
            Path uploadPath = Paths.get(filePath);
            Files.createDirectories(uploadPath.getParent());
            Files.write(uploadPath, imageBytes);

            // Create Media entity
            Media media = new Media();
            media.setOriginalFilename("ai-generated-image.png");
            media.setStoredFilename(fileName);
            media.setFileUrl(baseUrl+"/"+filePath);
            media.setContentType(STANDARD_CONTENT_TYPE);
            media.setFileSize(Files.size(uploadPath));
            media.setMediaType("PNG");
            media.setTitle(title != null ? title : "AI Generated Image");
            media.setAltText(description);
            media.setCreatedAt(LocalDateTime.now());
            media.setUpdatedAt(LocalDateTime.now());
            mediaRepository.save(media);
            // Save to database
            return filePath;

        } catch (Exception e) {
            throw new RuntimeException("Failed to store diagram as media: " + e.getMessage());
        }
    }

    private String clearLatexCode(String latexCode)
    {
        String cleaned = latexCode.trim();

        // Remove markdown code blocks
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(10).trim();
        } else if (cleaned.startsWith("```")){
            cleaned = cleaned.substring(3).trim();
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }
        return  cleaned;
    }

    private String cleanMermaidCode(String mermaidCode) {
        String cleaned = mermaidCode.trim();

        // Remove markdown code blocks
        if (cleaned.startsWith("```mermaid")) {
            cleaned = cleaned.substring(10).trim();
        } else if (cleaned.startsWith("```")){
                cleaned = cleaned.substring(3).trim();
    }

    if (cleaned.endsWith("```")) {
        cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
    }

     //Ensure it starts with a valid Mermaid diagram type
    if (!cleaned.startsWith("flowchart") &&
            !cleaned.startsWith("graph") &&
            !cleaned.startsWith("sequenceDiagram") &&
            !cleaned.startsWith("classDiagram") &&
            !cleaned.startsWith("stateDiagram")) {

        // If no diagram type detected, assume it's a flowchart
        cleaned = "flowchart TD\n" + cleaned;
    }

    return cleaned;
}





        }
