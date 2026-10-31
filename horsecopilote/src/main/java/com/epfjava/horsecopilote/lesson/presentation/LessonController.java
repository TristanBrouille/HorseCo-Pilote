package com.epfjava.horsecopilote.lesson.presentation;

import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Leçons", description = "Agenda de la semaine, génération des leçons, sujet et lieu, attribution des chevaux (coach / admin)")
@RestController
@RequestMapping("/lessons")
public class LessonController {

    // Authorities hold the bare role name (no ROLE_ prefix), hence hasAuthority rather than hasRole.
    private static final String ADMIN_ONLY = "hasAuthority('ADMIN')";
    private static final String COACH_OR_ADMIN = "hasAnyAuthority('COACH', 'ADMIN')";

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @PreAuthorize(COACH_OR_ADMIN)
    @GetMapping
    public ResponseEntity<List<LessonResponseDto>> getLessonsOfWeek(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Authentication authentication) {
        return ResponseEntity.ok(lessonService.getLessonsOfWeek(authentication.getName(), weekStart));
    }

    @PreAuthorize(ADMIN_ONLY)
    @PostMapping("/generation")
    public ResponseEntity<LessonGenerationResponseDto> generateLessons(
            @Valid @RequestBody LessonGenerationRequestDto request) {
        return ResponseEntity.ok(lessonService.generateLessons(request));
    }

    @PreAuthorize(ADMIN_ONLY)
    @PutMapping("/{lessonId}")
    public ResponseEntity<LessonResponseDto> updateLesson(@PathVariable Long lessonId,
                                                          @Valid @RequestBody LessonUpdateRequestDto request,
                                                          Authentication authentication) {
        return ResponseEntity.ok(lessonService.updateLesson(authentication.getName(), lessonId, request));
    }

    @PreAuthorize(COACH_OR_ADMIN)
    @PatchMapping("/{lessonId}")
    public ResponseEntity<LessonResponseDto> updateDetails(@PathVariable Long lessonId,
                                                           @Valid @RequestBody LessonDetailsRequestDto request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(lessonService.updateDetails(authentication.getName(), lessonId, request));
    }

    @PreAuthorize(COACH_OR_ADMIN)
    @GetMapping("/{lessonId}/participants/{memberId}/horse-options")
    public ResponseEntity<List<HorseOptionResponseDto>> getHorseOptions(@PathVariable Long lessonId,
                                                                       @PathVariable Long memberId,
                                                                       Authentication authentication) {
        return ResponseEntity.ok(lessonService.getHorseOptions(authentication.getName(), lessonId, memberId));
    }

    @PreAuthorize(COACH_OR_ADMIN)
    @PutMapping("/{lessonId}/participants/{memberId}/horse")
    public ResponseEntity<LessonResponseDto> assignHorse(@PathVariable Long lessonId,
                                                         @PathVariable Long memberId,
                                                         @Valid @RequestBody HorseAssignmentRequestDto request,
                                                         Authentication authentication) {
        return ResponseEntity.ok(lessonService.assignHorse(authentication.getName(), lessonId, memberId, request));
    }

    @PreAuthorize(COACH_OR_ADMIN)
    @DeleteMapping("/{lessonId}/participants/{memberId}/horse")
    public ResponseEntity<LessonResponseDto> unassignHorse(@PathVariable Long lessonId,
                                                           @PathVariable Long memberId,
                                                           Authentication authentication) {
        return ResponseEntity.ok(lessonService.unassignHorse(authentication.getName(), lessonId, memberId));
    }
}
