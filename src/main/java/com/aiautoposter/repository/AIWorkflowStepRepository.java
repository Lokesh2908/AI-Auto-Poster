package com.aiautoposter.repository;

import com.aiautoposter.entity.AIWorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIWorkflowStepRepository extends JpaRepository<AIWorkflowStep, Long> {
    
    List<AIWorkflowStep> findByPostId(Long postId);
    
    List<AIWorkflowStep> findByAgentType(AIWorkflowStep.AgentType agentType);
    
    List<AIWorkflowStep> findByPostIdAndAgentType(Long postId, AIWorkflowStep.AgentType agentType);
    
    @Query("SELECT aws FROM AIWorkflowStep aws WHERE aws.postId = :postId ORDER BY aws.iteration ASC, aws.createdAt ASC")
    List<AIWorkflowStep> findByPostIdOrderByIterationAndCreatedAtAsc(@Param("postId") Long postId);
    
    @Query("SELECT aws FROM AIWorkflowStep aws WHERE aws.postId = :postId AND aws.agentType = :agentType ORDER BY aws.iteration DESC")
    List<AIWorkflowStep> findByPostIdAndAgentTypeOrderByIterationDesc(@Param("postId") Long postId, 
                                                                     @Param("agentType") AIWorkflowStep.AgentType agentType);
    
    @Query("SELECT MAX(aws.iteration) FROM AIWorkflowStep aws WHERE aws.postId = :postId AND aws.agentType = :agentType")
    Integer findMaxIterationByPostIdAndAgentType(@Param("postId") Long postId, 
                                               @Param("agentType") AIWorkflowStep.AgentType agentType);
    
    @Query("SELECT AVG(aws.feedbackScore) FROM AIWorkflowStep aws WHERE aws.postId = :postId AND aws.feedbackScore IS NOT NULL")
    Double findAverageFeedbackScoreByPostId(@Param("postId") Long postId);
}
