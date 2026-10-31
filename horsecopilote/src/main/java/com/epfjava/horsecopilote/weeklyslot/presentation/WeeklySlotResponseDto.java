package com.epfjava.horsecopilote.weeklyslot.presentation;

import com.epfjava.horsecopilote.user.presentation.CoachResponseDto;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record WeeklySlotResponseDto(
        Long id,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int maxCapacity,
        int requiredLevel,
        CoachResponseDto coach,
        int enrolledCount
) {
}
