package com.epfjava.horsecopilote.lesson.presentation;

import com.epfjava.horsecopilote.horse.repository.Location;
import com.epfjava.horsecopilote.lesson.repository.LessonType;
import jakarta.validation.constraints.NotNull;

/** The coach of the lesson defines its topic and location. */
public record LessonDetailsRequestDto(@NotNull LessonType topic, @NotNull Location location) {
}
