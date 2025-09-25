package com.aiautoposter.service;

import com.aiautoposter.entity.SocialMediaApp;
import com.aiautoposter.repository.SocialMediaAppRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SocialMediaAppService {
    
    @Autowired
    private SocialMediaAppRepository socialMediaAppRepository;
    
    public List<SocialMediaApp> getAllActiveApps() {
        return socialMediaAppRepository.findByIsActiveTrue();
    }
    
    public List<SocialMediaApp> getActivePlatformApps(SocialMediaApp.Platform platform) {
        return socialMediaAppRepository.findActivePlatformAppsOrderedByDefault(platform);
    }
    
    public Optional<SocialMediaApp> getDefaultApp(SocialMediaApp.Platform platform) {
        return socialMediaAppRepository.findByPlatformAndIsDefaultTrue(platform);
    }
    
    public Optional<SocialMediaApp> getAppById(Long id) {
        return socialMediaAppRepository.findById(id);
    }
    
    public SocialMediaApp createApp(SocialMediaApp app) {
        // If this is set as default, unset other defaults for the same platform
        if (app.getIsDefault() != null && app.getIsDefault()) {
            unsetDefaultForPlatform(app.getPlatform());
        }
        
        return socialMediaAppRepository.save(app);
    }
    
    public SocialMediaApp updateApp(Long id, SocialMediaApp updatedApp) {
        return socialMediaAppRepository.findById(id)
                .map(app -> {
                    app.setName(updatedApp.getName());
                    app.setDescription(updatedApp.getDescription());
                    app.setClientId(updatedApp.getClientId());
                    app.setClientSecret(updatedApp.getClientSecret());
                    app.setIsActive(updatedApp.getIsActive());
                    
                    // Handle default setting
                    if (updatedApp.getIsDefault() != null && updatedApp.getIsDefault()) {
                        unsetDefaultForPlatform(app.getPlatform());
                        app.setIsDefault(true);
                    } else if (updatedApp.getIsDefault() != null) {
                        app.setIsDefault(updatedApp.getIsDefault());
                    }
                    
                    return socialMediaAppRepository.save(app);
                })
                .orElseThrow(() -> new RuntimeException("Social Media App not found with id: " + id));
    }
    
    public void deleteApp(Long id) {
        socialMediaAppRepository.findById(id)
                .ifPresent(app -> {
                    app.setIsActive(false);
                    socialMediaAppRepository.save(app);
                });
    }
    
    public void setAsDefault(Long id) {
        socialMediaAppRepository.findById(id)
                .ifPresent(app -> {
                    unsetDefaultForPlatform(app.getPlatform());
                    app.setIsDefault(true);
                    socialMediaAppRepository.save(app);
                });
    }
    
    private void unsetDefaultForPlatform(SocialMediaApp.Platform platform) {
        socialMediaAppRepository.findByPlatformAndIsDefaultTrue(platform)
                .ifPresent(defaultApp -> {
                    defaultApp.setIsDefault(false);
                    socialMediaAppRepository.save(defaultApp);
                });
    }
    
    public List<SocialMediaApp> getUserApps(Long userId) {
        return socialMediaAppRepository.findByCreatedByAndIsActiveTrue(userId);
    }
    
    public boolean hasActivePlatformApps(SocialMediaApp.Platform platform) {
        return socialMediaAppRepository.countActivePlatformApps(platform) > 0;
    }
    
    public SocialMediaApp getOrCreateDefaultLinkedInApp() {
        Optional<SocialMediaApp> defaultApp = getDefaultApp(SocialMediaApp.Platform.LINKEDIN);
        
        if (defaultApp.isPresent()) {
            return defaultApp.get();
        }
        
        // Create a default LinkedIn app if none exists
        SocialMediaApp linkedInApp = new SocialMediaApp();
        linkedInApp.setName("Default LinkedIn App");
        linkedInApp.setDescription("Default LinkedIn application for posting");
        linkedInApp.setPlatform(SocialMediaApp.Platform.LINKEDIN);
        linkedInApp.setClientId("default-client-id"); // Will be updated via UI
        linkedInApp.setClientSecret("default-client-secret"); // Will be updated via UI
        linkedInApp.setIsDefault(true);
        linkedInApp.setIsActive(true);
        
        return createApp(linkedInApp);
    }
}
