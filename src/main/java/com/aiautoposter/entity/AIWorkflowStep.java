package com.aiautoposter.entity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_workflow_steps")
public class AIWorkflowStep {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "post_id", nullable = false)
    private Long postId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false)
    private AgentType agentType;
    
    @Column(name = "feedback_score")
    private Double feedbackScore;
    
    @Column(name = "iteration", nullable = false)
    private Integer iteration = 1;
    
    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData;
    
    @Column(name = "output_data", columnDefinition = "TEXT")
    private String outputData;
    
    @Column(name = "status")
    private String status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    private Post post;
    
    // Constructors
    public AIWorkflowStep() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public AIWorkflowStep(Long postId, AgentType agentType, Integer iteration, String inputData, String outputData) {
        this();
        this.postId = postId;
        this.agentType = agentType;
        this.iteration = iteration;
        this.inputData = inputData;
        this.outputData = outputData;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getPostId() {
        return postId;
    }
    
    public void setPostId(Long postId) {
        this.postId = postId;
    }
    
    public AgentType getAgentType() {
        return agentType;
    }
    
    public void setAgentType(AgentType agentType) {
        this.agentType = agentType;
    }
    
    public Double getFeedbackScore() {
        return feedbackScore;
    }
    
    public void setFeedbackScore(Double feedbackScore) {
        this.feedbackScore = feedbackScore;
    }
    
    public Integer getIteration() {
        return iteration;
    }
    
    public void setIteration(Integer iteration) {
        this.iteration = iteration;
    }
    
    public String getInputData() {
        return inputData;
    }
    
    public void setInputData(String inputData) {
        this.inputData = inputData;
    }
    
    public String getOutputData() {
        return outputData;
    }
    
    public void setOutputData(String outputData) {
        this.outputData = outputData;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Post getPost() {
        return post;
    }
    
    public void setPost(Post post) {
        this.post = post;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public enum AgentType {
        CONTENT_GENERATOR, IMAGE_GENERATOR, OPTIMIZER, REVIEWER
    }
}
