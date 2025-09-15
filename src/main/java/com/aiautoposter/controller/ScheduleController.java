package com.aiautoposter.controller;

import com.aiautoposter.entity.Schedule;
import com.aiautoposter.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/schedules")
@CrossOrigin(origins = "*")
public class ScheduleController {
    
    @Autowired
    private ScheduleService scheduleService;
    
    @PostMapping
    public ResponseEntity<Schedule> createSchedule(@Valid @RequestBody Schedule schedule) {
        try {
            Schedule createdSchedule = scheduleService.createSchedule(schedule.getPostId(), schedule.getScheduledFor());
            return new ResponseEntity<>(createdSchedule, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @PostMapping("/post/{postId}")
    public ResponseEntity<Schedule> schedulePost(@PathVariable Long postId, 
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledFor) {
        try {
            Schedule schedule = scheduleService.schedulePost(postId, scheduledFor);
            return new ResponseEntity<>(schedule, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping
    public ResponseEntity<List<Schedule>> getAllSchedules() {
        List<Schedule> schedules = scheduleService.getPendingSchedules();
        return new ResponseEntity<>(schedules, HttpStatus.OK);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Schedule> getScheduleById(@PathVariable Long id) {
        Optional<Schedule> schedule = scheduleService.findById(id);
        return schedule.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Schedule>> getSchedulesByPost(@PathVariable Long postId) {
        List<Schedule> schedules = scheduleService.findByPostId(postId);
        return new ResponseEntity<>(schedules, HttpStatus.OK);
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Schedule>> getSchedulesByStatus(@PathVariable Schedule.ScheduleStatus status) {
        List<Schedule> schedules = scheduleService.findByStatus(status);
        return new ResponseEntity<>(schedules, HttpStatus.OK);
    }
    
    @GetMapping("/date-range")
    public ResponseEntity<List<Schedule>> getSchedulesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        List<Schedule> schedules = scheduleService.findByScheduledForBetween(startTime, endTime);
        return new ResponseEntity<>(schedules, HttpStatus.OK);
    }
    
    @GetMapping("/pending")
    public ResponseEntity<List<Schedule>> getPendingSchedules() {
        List<Schedule> schedules = scheduleService.getPendingSchedules();
        return new ResponseEntity<>(schedules, HttpStatus.OK);
    }
    
    @GetMapping("/to-publish")
    public ResponseEntity<List<Schedule>> getSchedulesToPublish() {
        List<Schedule> schedules = scheduleService.getSchedulesToPublish();
        return new ResponseEntity<>(schedules, HttpStatus.OK);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Schedule> updateSchedule(@PathVariable Long id, @Valid @RequestBody Schedule schedule) {
        try {
            schedule.setId(id);
            Schedule updatedSchedule = scheduleService.updateSchedule(schedule);
            return new ResponseEntity<>(updatedSchedule, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Schedule> cancelSchedule(@PathVariable Long id) {
        try {
            Schedule schedule = scheduleService.cancelSchedule(id);
            return new ResponseEntity<>(schedule, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        try {
            scheduleService.deleteSchedule(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
