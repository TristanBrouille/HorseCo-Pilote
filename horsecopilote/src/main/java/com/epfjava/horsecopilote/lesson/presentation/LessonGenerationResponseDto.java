package com.epfjava.horsecopilote.lesson.presentation;

import java.time.LocalDate;

/**
 * skippedCount: lessons that already existed, or whose coach already teaches at that time.
 * bookingCount: participants booked into the created lessons (the members enrolled in their slot).
 */
public record LessonGenerationResponseDto(
        LocalDate fromDate,
        LocalDate untilDate,
        int createdCount,
        int skippedCount,
        int bookingCount
) {
}
