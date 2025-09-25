package com.aiautoposter.dto;

import javax.validation.constraints.NotBlank;

public class PostVersionRequest {
    
    @NotBlank(message = "Version type is required")
    private String versionType; // 'refinement' or 'full_regeneration'
    
    private String changeDescription;
    
    private String refinementInstructions;
    
    private String previousContent;
    
    private String enhancementOptions; // JSON string
    
    private String currentTitle;
    
    private String currentSourceDiscussion;
    
    private String currentTargetPlatforms;
    
    // Constructors
    public PostVersionRequest() {}
    
    public PostVersionRequest(String versionType, String changeDescription) {
        this.versionType = versionType;
        this.changeDescription = changeDescription;
    }
    
    // Getters and Setters
    public String getVersionType() {
        return versionType;
    }
    
    public void setVersionType(String versionType) {
        this.versionType = versionType;
    }
    
    public String getChangeDescription() {
        return changeDescription;
    }
    
    public void setChangeDescription(String changeDescription) {
        this.changeDescription = changeDescription;
    }
    
    public String getRefinementInstructions() {
        return refinementInstructions;
    }
    
    public void setRefinementInstructions(String refinementInstructions) {
        this.refinementInstructions = refinementInstructions;
    }
    
    public String getPreviousContent() {
        return previousContent;
    }
    
    public void setPreviousContent(String previousContent) {
        this.previousContent = previousContent;
    }
    
    public String getEnhancementOptions() {
        return enhancementOptions;
    }
    
    public void setEnhancementOptions(String enhancementOptions) {
        this.enhancementOptions = enhancementOptions;
    }
    
    public String getCurrentTitle() {
        return currentTitle;
    }
    
    public void setCurrentTitle(String currentTitle) {
        this.currentTitle = currentTitle;
    }
    
    public String getCurrentSourceDiscussion() {
        return currentSourceDiscussion;
    }
    
    public void setCurrentSourceDiscussion(String currentSourceDiscussion) {
        this.currentSourceDiscussion = currentSourceDiscussion;
    }
    
    public String getCurrentTargetPlatforms() {
        return currentTargetPlatforms;
    }
    
    public void setCurrentTargetPlatforms(String currentTargetPlatforms) {
        this.currentTargetPlatforms = currentTargetPlatforms;
    }
}
