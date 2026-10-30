package com.epfjava.horsecopilote.horse.presentation;

import com.epfjava.horsecopilote.horse.repository.HorseStatus;
import com.epfjava.horsecopilote.horse.repository.Location;

public record HorseResponseDto(
        Long id,
        String name,
        String breed,
        int heightCm,
        int weightKg,
        int dailyHours,
        HorseStatus status,
        Location location,
        int requiredLevel
) {
}
