package com.aiautoposter.controller;

import com.aiautoposter.entity.SocialMediaApp;
import com.aiautoposter.service.SocialMediaAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/social-media-apps")
public class SocialMediaAppController {
    
    @Autowired
    private SocialMediaAppService socialMediaAppService;
    
    @GetMapping
    public ResponseEntity<List<SocialMediaApp>> getAllActiveApps() {
        List<SocialMediaApp> apps = socialMediaAppService.getAllActiveApps();
        return ResponseEntity.ok(apps);
    }
    
    @GetMapping("/platform/{platform}")
    public ResponseEntity<List<SocialMediaApp>> getActivePlatformApps(@PathVariable String platform) {
        try {
            SocialMediaApp.Platform platformEnum = SocialMediaApp.Platform.valueOf(platform.toUpperCase());
            List<SocialMediaApp> apps = socialMediaAppService.getActivePlatformApps(platformEnum);
            return ResponseEntity.ok(apps);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/platform/{platform}/default")
    public ResponseEntity<SocialMediaApp> getDefaultApp(@PathVariable String platform) {
        try {
            SocialMediaApp.Platform platformEnum = SocialMediaApp.Platform.valueOf(platform.toUpperCase());
            return socialMediaAppService.getDefaultApp(platformEnum)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<SocialMediaApp> getAppById(@PathVariable Long id) {
        return socialMediaAppService.getAppById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<SocialMediaApp> createApp(@RequestBody SocialMediaApp app) {
        try {
            SocialMediaApp createdApp = socialMediaAppService.createApp(app);
            return ResponseEntity.ok(createdApp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<SocialMediaApp> updateApp(@PathVariable Long id, @RequestBody SocialMediaApp app) {
        try {
            SocialMediaApp updatedApp = socialMediaAppService.updateApp(id, app);
            return ResponseEntity.ok(updatedApp);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApp(@PathVariable Long id) {
        try {
            socialMediaAppService.deleteApp(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}/set-default")
    public ResponseEntity<Void> setAsDefault(@PathVariable Long id) {
        try {
            socialMediaAppService.setAsDefault(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/platforms")
    public ResponseEntity<Map<String, String>> getAvailablePlatforms() {
        Map<String, String> platforms = Map.of(
                "LINKEDIN", "LinkedIn",
                "WORDPRESS", "WordPress",
                "TWITTER", "Twitter",
                "FACEBOOK", "Facebook"
        );
        return ResponseEntity.ok(platforms);
    }
    
    @GetMapping("/platform/{platform}/status")
    public ResponseEntity<Map<String, Object>> getPlatformStatus(@PathVariable String platform) {
        try {
            SocialMediaApp.Platform platformEnum = SocialMediaApp.Platform.valueOf(platform.toUpperCase());
            boolean hasActiveApps = socialMediaAppService.hasActivePlatformApps(platformEnum);
            boolean hasDefault = socialMediaAppService.getDefaultApp(platformEnum).isPresent();
            
            Map<String, Object> status = Map.of(
                    "hasActiveApps", hasActiveApps,
                    "hasDefault", hasDefault,
                    "platform", platformEnum.getDisplayName()
            );
            
            return ResponseEntity.ok(status);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
