package com.epfjava.horsecopilote.lesson.presentation;

import com.epfjava.horsecopilote.horse.repository.Location;
import com.epfjava.horsecopilote.lesson.repository.LessonStatus;
import com.epfjava.horsecopilote.lesson.repository.LessonType;
import jakarta.validation.constraints.NotNull;

/** location and topic may stay null: the coach of the lesson defines them. */
public record LessonUpdateRequestDto(
        @NotNull Long coachId,
        Location location,
        LessonType topic,
        @NotNull LessonStatus status
) {
}
