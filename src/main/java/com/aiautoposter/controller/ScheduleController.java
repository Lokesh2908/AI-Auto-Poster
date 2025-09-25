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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/schedules")
@CrossOrigin(origins = "*")
public class ScheduleController {

    // Lightweight DTO to avoid serializing lazy JPA proxies
    public static class ScheduleDto {
        public Long id;
        public Long postId;
        public LocalDateTime scheduledFor;
        public LocalDateTime publishedAt;
        public Schedule.ScheduleStatus status;

        public static ScheduleDto from(Schedule s) {
            ScheduleDto dto = new ScheduleDto();
            dto.id = s.getId();
            dto.postId = s.getPostId();
            dto.scheduledFor = s.getScheduledFor();
            dto.publishedAt = s.getPublishedAt();
            dto.status = s.getStatus();
            return dto;
        }
    }
    
    @Autowired
    private ScheduleService scheduleService;
    
    @PostMapping
    public ResponseEntity<ScheduleDto> createSchedule(@Valid @RequestBody Schedule schedule) {
        try {
            Schedule createdSchedule = scheduleService.createSchedule(schedule.getPostId(), schedule.getScheduledFor(),schedule.getSocialMediaAppId(), schedule.getPlatform());
            return new ResponseEntity<>(ScheduleDto.from(createdSchedule), HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @PostMapping("/post/{postId}")
    public ResponseEntity<ScheduleDto> schedulePost(@PathVariable Long postId, 
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledFor) {
        try {
            Schedule schedule = scheduleService.schedulePost(postId, scheduledFor);
            return new ResponseEntity<>(ScheduleDto.from(schedule), HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @GetMapping
    public ResponseEntity<List<ScheduleDto>> getAllSchedules() {
        List<ScheduleDto> schedules = scheduleService.findAll().stream().map(ScheduleDto::from).collect(Collectors.toList());
        return new ResponseEntity<>(schedules, HttpStatus.OK);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ScheduleDto> getScheduleById(@PathVariable Long id) {
        Optional<Schedule> schedule = scheduleService.findById(id);
        return schedule.map(value -> new ResponseEntity<>(ScheduleDto.from(value), HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<ScheduleDto>> getSchedulesByPost(@PathVariable Long postId) {
        List<ScheduleDto> schedules = scheduleService.findByPostId(postId).stream().map(ScheduleDto::from).collect(Collectors.toList());
        return new ResponseEntity<>(schedules, HttpStatus.OK);
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ScheduleDto>> getSchedulesByStatus(@PathVariable Schedule.ScheduleStatus status) {
        List<ScheduleDto> schedules = scheduleService.findByStatus(status).stream().map(ScheduleDto::from).collect(Collectors.toList());
        return new ResponseEntity<>(schedules, HttpStatus.OK);
    }
    
    @GetMapping("/date-range")
    public ResponseEntity<List<ScheduleDto>> getSchedulesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        List<ScheduleDto> schedules = scheduleService.findByScheduledForBetween(startTime, endTime).stream().map(ScheduleDto::from).collect(Collectors.toList());
        return new ResponseEntity<>(schedules, HttpStatus.OK);
    }
    
    @GetMapping("/pending")
    public ResponseEntity<List<ScheduleDto>> getPendingSchedules() {
        List<ScheduleDto> schedules = scheduleService.getPendingSchedules().stream().map(ScheduleDto::from).collect(Collectors.toList());
        return new ResponseEntity<>(schedules, HttpStatus.OK);
    }
    
    @GetMapping("/to-publish")
    public ResponseEntity<List<ScheduleDto>> getSchedulesToPublish() {
        List<ScheduleDto> schedules = scheduleService.getSchedulesToPublish().stream().map(ScheduleDto::from).collect(Collectors.toList());
        return new ResponseEntity<>(schedules, HttpStatus.OK);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ScheduleDto> updateSchedule(@PathVariable Long id, @Valid @RequestBody Schedule schedule) {
        try {
            schedule.setId(id);
            Schedule updatedSchedule = scheduleService.updateSchedule(schedule);
            return new ResponseEntity<>(ScheduleDto.from(updatedSchedule), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ScheduleDto> cancelSchedule(@PathVariable Long id) {
        try {
            Schedule schedule = scheduleService.cancelSchedule(id);
            return new ResponseEntity<>(ScheduleDto.from(schedule), HttpStatus.OK);
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
