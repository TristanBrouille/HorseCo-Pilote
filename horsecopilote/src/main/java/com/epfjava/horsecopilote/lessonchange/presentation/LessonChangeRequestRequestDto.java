package com.epfjava.horsecopilote.lessonchange.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LessonChangeRequestRequestDto(
        @NotNull Long fromLessonId,
        @NotNull Long toLessonId,
        @NotBlank @Size(max = 500) String reason
) {
}
