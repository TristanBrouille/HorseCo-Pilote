package com.epfjava.horsecopilote.lesson.presentation;

public record LessonParticipantResponseDto(
        Long id,
        String firstName,
        String lastName,
        int galopLevel,
        AssignedHorseResponseDto horse
) {
}
