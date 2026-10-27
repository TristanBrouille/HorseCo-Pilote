package com.epfjava.horsecopilote.user.service;

import com.epfjava.horsecopilote.security.repository.User;
import com.epfjava.horsecopilote.user.presentation.CoachResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CoachMapper {

    CoachResponseDto toResponse(User coach);
}
