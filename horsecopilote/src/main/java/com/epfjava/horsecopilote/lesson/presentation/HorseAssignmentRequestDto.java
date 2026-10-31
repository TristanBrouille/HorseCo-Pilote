package com.epfjava.horsecopilote.lesson.presentation;

import jakarta.validation.constraints.NotNull;

public record HorseAssignmentRequestDto(@NotNull Long horseId) {
}
