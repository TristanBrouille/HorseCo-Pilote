package com.epfjava.horsecopilote.lesson.presentation;

import com.epfjava.horsecopilote.lesson.repository.LessonType;
import jakarta.validation.constraints.NotNull;

public record LessonTopicRequestDto(@NotNull LessonType topic) {
}
