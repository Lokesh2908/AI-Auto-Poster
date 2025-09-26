package com.aiautoposter.controller;

import com.aiautoposter.entity.Media;
import com.aiautoposter.service.MediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/media")
@CrossOrigin(origins = "*")
public class MediaController {

    @Autowired
    private MediaService mediaService;

    @PostMapping("/generate-image")
    public ResponseEntity<String> generateImage(
            @RequestParam String prompt,
            @RequestParam(required = false, defaultValue = "Generated Image") String title,
            @RequestParam(required = false) String imageType) {
        try {
            System.out.println("Generating image with prompt: " + prompt);
            System.out.println("Title: " + title);
            
            if (prompt == null || prompt.trim().isEmpty()) {
                return new ResponseEntity<>("Prompt cannot be empty", HttpStatus.BAD_REQUEST);
            }

            String imageUrl=null;

            if(imageType.equals("general"))
                imageUrl = mediaService.generateImage(prompt, title);
            else
                imageUrl = mediaService.storeDiagramAsMedia(prompt,title,"",imageType);
            if (imageUrl != null && !imageUrl.isEmpty()) {
                return new ResponseEntity<>(imageUrl, HttpStatus.CREATED);
            } else {
                return new ResponseEntity<>("Failed to generate image. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            System.err.println("Error in generateImage controller: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description) {
        try {
            System.out.println("Uploading file: " + file.getOriginalFilename());
            System.out.println("File size: " + file.getSize() + " bytes");
            System.out.println("Content type: " + file.getContentType());
            
            if (file.isEmpty()) {
                return new ResponseEntity<>("File cannot be empty", HttpStatus.BAD_REQUEST);
            }
            
            // Check if it's an image file
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return new ResponseEntity<>("Only image files are allowed", HttpStatus.BAD_REQUEST);
            }
            
            String fileUrl = mediaService.saveFileToStatic(file, title, description);
            return new ResponseEntity<>(fileUrl, HttpStatus.CREATED);
        } catch (Exception e) {
            System.err.println("Error in uploadMedia controller: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Upload failed: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<List<Media>> getAllMedia() {
        try {
            List<Media> mediaList = mediaService.getAllMedia();
            return new ResponseEntity<>(mediaList, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteMedia(@PathVariable Long id) {
        try {
            mediaService.deleteMedia(id);
            return new ResponseEntity<>("Media deleted successfully", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Delete failed: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> testMediaEndpoint() {
        try {
            return new ResponseEntity<>("Media endpoint is working!", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Test failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
