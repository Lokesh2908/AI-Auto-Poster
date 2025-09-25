package com.aiautoposter.dto;

import javax.validation.constraints.NotNull;

public class RestoreVersionRequest {
    
    @NotNull(message = "Version ID is required")
    private Long versionId;
    
    private String changeDescription;
    
    // Constructors
    public RestoreVersionRequest() {}
    
    public RestoreVersionRequest(Long versionId, String changeDescription) {
        this.versionId = versionId;
        this.changeDescription = changeDescription;
    }
    
    // Getters and Setters
    public Long getVersionId() {
        return versionId;
    }
    
    public void setVersionId(Long versionId) {
        this.versionId = versionId;
    }
    
    public String getChangeDescription() {
        return changeDescription;
    }
    
    public void setChangeDescription(String changeDescription) {
        this.changeDescription = changeDescription;
    }
}
