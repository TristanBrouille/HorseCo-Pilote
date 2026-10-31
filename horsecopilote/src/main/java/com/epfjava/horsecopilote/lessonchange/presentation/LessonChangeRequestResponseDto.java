package com.epfjava.horsecopilote.lessonchange.presentation;

import com.epfjava.horsecopilote.lesson.repository.LessonType;
import com.epfjava.horsecopilote.lessonchange.repository.LessonChangeRequestStatus;

import java.time.LocalDateTime;

public record LessonChangeRequestResponseDto(
        Long id,
        Long fromLessonId,
        LocalDateTime fromStartDatetime,
        LessonType fromTopic,
        Long toLessonId,
        LocalDateTime toStartDatetime,
        LessonType toTopic,
        String reason,
        LessonChangeRequestStatus status,
        LocalDateTime createdAt
) {
}
