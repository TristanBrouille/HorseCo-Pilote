package com.epfjava.horsecopilote.lesson.presentation;

import com.epfjava.horsecopilote.horse.repository.Location;
import com.epfjava.horsecopilote.lesson.repository.LessonStatus;
import com.epfjava.horsecopilote.lesson.repository.LessonType;
import jakarta.validation.constraints.NotNull;

public record LessonUpdateRequestDto(
        @NotNull Long coachId,
        @NotNull Location location,
        @NotNull LessonType topic,
        @NotNull LessonStatus status
) {
}
