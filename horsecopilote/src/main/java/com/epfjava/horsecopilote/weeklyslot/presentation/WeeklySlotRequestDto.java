package com.epfjava.horsecopilote.weeklyslot.presentation;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record WeeklySlotRequestDto(
        @NotNull Long coachId,
        @NotNull DayOfWeek dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull @Min(1) @Max(12) Integer maxCapacity,
        @NotNull @Min(0) @Max(7) Integer requiredLevel
) {

    @AssertTrue(message = "must start on the hour or half hour")
    public boolean isStartTimeOnHalfHour() {
        return startTime == null
                || (startTime.getMinute() % 30 == 0 && startTime.getSecond() == 0 && startTime.getNano() == 0);
    }
}
