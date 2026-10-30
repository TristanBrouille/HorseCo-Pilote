package com.epfjava.horsecopilote.horse.presentation;

import com.epfjava.horsecopilote.horse.repository.HorseStatus;
import com.epfjava.horsecopilote.horse.repository.Location;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HorseRequestDto(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String breed,
        @NotNull @Min(80) @Max(220) Integer heightCm,
        @NotNull @Min(100) @Max(1200) Integer weightKg,
        @NotNull @Min(1) @Max(3) Integer dailyHours,
        @NotNull HorseStatus status,
        @NotNull Location location,
        @NotNull @Min(1) @Max(7) Integer requiredLevel
) {
}
