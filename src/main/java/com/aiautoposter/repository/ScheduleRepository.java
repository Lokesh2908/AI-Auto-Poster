package com.aiautoposter.repository;

import com.aiautoposter.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    
    List<Schedule> findByPostId(Long postId);
    
    List<Schedule> findByStatus(Schedule.ScheduleStatus status);
    
    List<Schedule> findByScheduledForBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    @Query("SELECT s FROM Schedule s WHERE s.status = :status AND s.scheduledFor <= :currentTime ORDER BY s.scheduledFor ASC")
    List<Schedule> findPendingSchedulesToPublish(@Param("status") Schedule.ScheduleStatus status, 
                                                @Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT s FROM Schedule s WHERE s.postId = :postId ORDER BY s.scheduledFor DESC")
    List<Schedule> findByPostIdOrderByScheduledForDesc(@Param("postId") Long postId);
    
    @Query("SELECT s FROM Schedule s WHERE s.scheduledFor BETWEEN :startDate AND :endDate ORDER BY s.scheduledFor ASC")
    List<Schedule> findByScheduledForBetweenOrderByScheduledForAsc(@Param("startDate") LocalDateTime startDate, 
                                                                 @Param("endDate") LocalDateTime endDate);
}
