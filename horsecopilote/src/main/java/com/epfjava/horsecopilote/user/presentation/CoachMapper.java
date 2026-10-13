package com.epfjava.horsecopilote.user.presentation;

import com.epfjava.horsecopilote.user.repository.User;
import org.springframework.stereotype.Component;

@Component
public class CoachMapper {

    public CoachResponseDto toResponse(User coach) {
        return new CoachResponseDto(coach.getId(), coach.getFirstName(), coach.getLastName());
    }
}
