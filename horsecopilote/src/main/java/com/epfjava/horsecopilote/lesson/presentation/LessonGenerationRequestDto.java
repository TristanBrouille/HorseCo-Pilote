package com.epfjava.horsecopilote.lesson.presentation;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Creates the lessons of every weekly slot from now until untilDate (inclusive).
 * No topic nor location: the coach of each lesson defines them.
 */
public record LessonGenerationRequestDto(@NotNull LocalDate untilDate) {
}
