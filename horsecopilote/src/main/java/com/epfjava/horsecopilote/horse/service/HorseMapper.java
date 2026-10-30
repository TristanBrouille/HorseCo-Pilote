package com.epfjava.horsecopilote.horse.service;

import com.epfjava.horsecopilote.horse.presentation.HorseRequestDto;
import com.epfjava.horsecopilote.horse.presentation.HorseResponseDto;
import com.epfjava.horsecopilote.horse.repository.Horse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HorseMapper {

    // The entity has no setters: it is built through its constructor.
    default Horse toEntity(HorseRequestDto request) {
        return new Horse(request.name().trim(), request.breed().trim(), request.heightCm(), request.weightKg(),
                request.dailyHours(), request.status(), request.location(), request.requiredLevel());
    }

    HorseResponseDto toResponse(Horse horse);
}
