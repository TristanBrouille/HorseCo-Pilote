package com.epfjava.horsecopilote.lesson.presentation;

import com.epfjava.horsecopilote.lesson.service.LessonService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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
}
