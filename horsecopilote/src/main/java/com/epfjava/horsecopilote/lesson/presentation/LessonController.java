package com.epfjava.horsecopilote.lesson.presentation;

import com.epfjava.horsecopilote.lesson.service.LessonService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/lessons")
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @PreAuthorize("hasAuthority('COACH')")
    @GetMapping
    public ResponseEntity<List<LessonResponseDto>> getLessonsOfWeek(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Authentication authentication) {
        return ResponseEntity.ok(lessonService.getLessonsOfWeekForCoach(authentication.getName(), weekStart));
    }

    @PreAuthorize("hasAuthority('COACH')")
    @PatchMapping("/{lessonId}")
    public ResponseEntity<LessonResponseDto> updateTopic(@PathVariable Long lessonId,
                                                         @Valid @RequestBody LessonTopicRequestDto request,
                                                         Authentication authentication) {
        return ResponseEntity.ok(lessonService.updateTopic(authentication.getName(), lessonId, request));
    }

    @PreAuthorize("hasAuthority('COACH')")
    @GetMapping("/{lessonId}/participants/{memberId}/horse-options")
    public ResponseEntity<List<HorseOptionResponseDto>> getHorseOptions(@PathVariable Long lessonId,
                                                                       @PathVariable Long memberId,
                                                                       Authentication authentication) {
        return ResponseEntity.ok(lessonService.getHorseOptions(authentication.getName(), lessonId, memberId));
    }

    @PreAuthorize("hasAuthority('COACH')")
    @PutMapping("/{lessonId}/participants/{memberId}/horse")
    public ResponseEntity<LessonResponseDto> assignHorse(@PathVariable Long lessonId,
                                                         @PathVariable Long memberId,
                                                         @Valid @RequestBody HorseAssignmentRequestDto request,
                                                         Authentication authentication) {
        return ResponseEntity.ok(lessonService.assignHorse(authentication.getName(), lessonId, memberId, request));
    }

    @PreAuthorize("hasAuthority('COACH')")
    @DeleteMapping("/{lessonId}/participants/{memberId}/horse")
    public ResponseEntity<LessonResponseDto> unassignHorse(@PathVariable Long lessonId,
                                                           @PathVariable Long memberId,
                                                           Authentication authentication) {
        return ResponseEntity.ok(lessonService.unassignHorse(authentication.getName(), lessonId, memberId));
    }
}
