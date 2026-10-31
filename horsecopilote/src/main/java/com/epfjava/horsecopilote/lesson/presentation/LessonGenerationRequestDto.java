package com.epfjava.horsecopilote.lesson.presentation;

import com.epfjava.horsecopilote.horse.repository.Location;
import com.epfjava.horsecopilote.lesson.repository.LessonType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Creates the lessons of every weekly slot from now until untilDate (inclusive). */
public record LessonGenerationRequestDto(
        @NotNull LocalDate untilDate,
        @NotNull LessonType topic,
        @NotNull Location location
) {
}
