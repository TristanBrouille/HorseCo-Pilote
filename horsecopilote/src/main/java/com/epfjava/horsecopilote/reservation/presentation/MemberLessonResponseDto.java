package com.epfjava.horsecopilote.reservation.presentation;

import com.epfjava.horsecopilote.horse.repository.Location;
import com.epfjava.horsecopilote.lesson.presentation.AssignedHorseResponseDto;
import com.epfjava.horsecopilote.lesson.repository.LessonStatus;
import com.epfjava.horsecopilote.lesson.repository.LessonType;
import com.epfjava.horsecopilote.user.presentation.CoachResponseDto;

import java.time.LocalDateTime;

/**
 * A lesson of the week as seen by the connected member.
 * mine: the member is booked in this lesson. horse: the horse assigned to the member,
 * null when the lesson is not theirs or the coach has not assigned one yet.
 * The other participants are never exposed (R6).
 */
public record MemberLessonResponseDto(
        Long id,
        Long weeklySlotId,
        LocalDateTime startDatetime,
        LocalDateTime endDatetime,
        Location location,
        LessonType topic,
        LessonStatus status,
        CoachResponseDto coach,
        boolean mine,
        AssignedHorseResponseDto horse
) {
}
