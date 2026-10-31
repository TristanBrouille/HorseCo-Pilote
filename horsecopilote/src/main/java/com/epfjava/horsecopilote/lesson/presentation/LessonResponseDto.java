package com.epfjava.horsecopilote.lesson.presentation;

import com.epfjava.horsecopilote.horse.repository.Location;
import com.epfjava.horsecopilote.lesson.repository.LessonStatus;
import com.epfjava.horsecopilote.lesson.repository.LessonType;
import com.epfjava.horsecopilote.user.presentation.CoachResponseDto;

import java.time.LocalDateTime;
import java.util.List;

public record LessonResponseDto(
        Long id,
        Long weeklySlotId,
        LocalDateTime startDatetime,
        LocalDateTime endDatetime,
        Location location,
        LessonType topic,
        LessonStatus status,
        CoachResponseDto coach,
        boolean mine,
        // true for the coach of the lesson and for an admin: they see the participants and may edit it
        boolean editable,
        boolean hasPendingMembers,
        int participantCount,
        List<LessonParticipantResponseDto> participants
) {
}
