package com.aiautoposter.entity;

public class WordPressPostResponse {
    private Integer postId;
    private String postUrl;
    private String status;

    public WordPressPostResponse(Integer postId, String postUrl, String status) {
        this.postId = postId;
        this.postUrl = postUrl;
        this.status = status;
    }

    // Getters
    public Integer getPostId() { return postId; }
    public String getPostUrl() { return postUrl; }
    public String getStatus() { return status; }
}
