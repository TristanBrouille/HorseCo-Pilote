package com.epfjava.horsecopilote.member.presentation;

import java.math.BigDecimal;

public record MemberResponseDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        int galopLevel,
        int heightCm,
        BigDecimal weightKg
) {
}
