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
            @RequestParam(required = false, defaultValue = "Generated Image") String title) {
        try {
            String imageUrl = mediaService.generateImage(prompt, title);
            return new ResponseEntity<>(imageUrl, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description) {
        try {
            String fileUrl = mediaService.saveFileToStatic(file, title, description);
            return new ResponseEntity<>(fileUrl, HttpStatus.CREATED);
        } catch (Exception e) {
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

}
