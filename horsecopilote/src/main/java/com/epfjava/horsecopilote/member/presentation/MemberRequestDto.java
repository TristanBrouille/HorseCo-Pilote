package com.epfjava.horsecopilote.member.presentation;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MemberRequestDto(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8) String password,
        @NotNull @Min(0) @Max(7) Integer galopLevel,
        @NotNull @Min(80) @Max(220) Integer heightCm,
        @NotNull @DecimalMin("15") @DecimalMax("150") @Digits(integer = 3, fraction = 2) BigDecimal weightKg
) {
}
