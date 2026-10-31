package com.epfjava.horsecopilote.lesson.presentation;

import com.epfjava.horsecopilote.horse.repository.HorseStatus;

public record HorseOptionResponseDto(
        Long id,
        String name,
        String breed,
        int weightKg,
        int requiredLevel,
        HorseStatus status,
        int dailyHours,
        int hoursToday,
        boolean assigned,
        boolean selectable,
        HorseUnavailabilityReason reason
) {
}
